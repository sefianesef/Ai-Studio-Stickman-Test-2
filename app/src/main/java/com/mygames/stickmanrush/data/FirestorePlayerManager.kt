package com.mygames.stickmanrush.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firestore Player Data Model representing `players/{playerId}` document.
 */
data class FirestorePlayerDoc(
    val coins: Int = 0,
    val highScore: Int = 0,
    val inventorySlots: Int = 5,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Firestore Inventory Slot Model representing `players/{playerId}/inventory/{slotId}` document.
 */
data class FirestoreInventorySlot(
    val itemId: String = "wood_plank",
    val quantity: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Enterprise Firebase Firestore Manager for Stickman Rush.
 *
 * Direct integration with user's Firestore Schema:
 * 1. Collection: `players/{playerId}`
 *    Fields: `coins`, `highScore`, `inventorySlots`
 * 2. Subcollection: `players/{playerId}/inventory/slot_1`
 *    Fields: `itemId: "wood_plank"`, `quantity: 1`
 *
 * Responsibilities:
 * - Realtime Firestore listener for instant synchronization of Coins and Wood Planks on Game Start.
 * - Atomic decrement of `wood_plank` quantity when Stickman creates a bridge.
 * - Automatic update of `highScore` in Firestore when the run ends (Game Over).
 * - Fast local cache fallback ensuring 60fps smooth gameplay even when offline.
 */
class FirestorePlayerManager(private val context: Context) {

    companion object {
        private const val TAG = "FirestorePlayerManager"
        private const val PREFS_NAME = "stickman_firestore_cache"
        private const val KEY_CACHED_PLAYER_ID = "cached_firestore_player_id"
        private const val KEY_CACHED_COINS = "cached_firestore_coins"
        private const val KEY_CACHED_HIGH_SCORE = "cached_firestore_high_score"
        private const val KEY_CACHED_PLANKS = "cached_firestore_wood_planks"
        private const val KEY_CACHED_NICKNAME = "cached_firestore_nickname"

        const val COLLECTION_PLAYERS = "players"
        const val SUBCOLLECTION_INVENTORY = "inventory"
        const val DOC_SLOT_1 = "slot_1"
        const val ITEM_WOOD_PLANK = "wood_plank"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Observable UI StateFlows
    private val _coins = MutableStateFlow(prefs.getInt(KEY_CACHED_COINS, 100))
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _woodPlanks = MutableStateFlow(prefs.getInt(KEY_CACHED_PLANKS, 1))
    val woodPlanks: StateFlow<Int> = _woodPlanks.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.getInt(KEY_CACHED_HIGH_SCORE, 0))
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    private val _nickname = MutableStateFlow(prefs.getString(KEY_CACHED_NICKNAME, "") ?: "")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _inventorySlots = MutableStateFlow(5)
    val inventorySlots: StateFlow<Int> = _inventorySlots.asStateFlow()

    private val _isCloudSynced = MutableStateFlow(false)
    val isCloudSynced: StateFlow<Boolean> = _isCloudSynced.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String>("Connecting to Firestore...")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private var playerListener: ListenerRegistration? = null
    private var inventorySlotListener: ListenerRegistration? = null

    private var activePlayerId: String = ""

    init {
        initializePlayer()
    }

    /**
     * Resolves the authoritative Player ID and attaches Firestore snapshot listeners.
     */
    fun initializePlayer(customPlayerId: String? = null) {
        val resolvedId = when {
            !customPlayerId.isNullOrBlank() -> customPlayerId
            else -> getOrCreatePlayerId()
        }
        activePlayerId = resolvedId
        Log.i(TAG, "Initializing Firestore sync for Player ID: $activePlayerId")

        attachFirestoreListeners(activePlayerId)
    }

    /**
     * Returns the persistent Player ID (uses Firebase Auth UID if signed in, or unique UUID).
     */
    fun getOrCreatePlayerId(): String {
        try {
            val authUser = FirebaseAuth.getInstance().currentUser
            if (authUser != null && authUser.uid.isNotBlank()) {
                prefs.edit().putString(KEY_CACHED_PLAYER_ID, authUser.uid).apply()
                return authUser.uid
            }
        } catch (_: Throwable) {}

        var cachedId = prefs.getString(KEY_CACHED_PLAYER_ID, null)
        if (cachedId.isNullOrBlank()) {
            cachedId = "player_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_CACHED_PLAYER_ID, cachedId).apply()
        }
        return cachedId
    }

    /**
     * Attaches realtime snapshot listeners to:
     * 1. `players/{playerId}` -> updates `coins`, `highScore`, `inventorySlots`
     * 2. `players/{playerId}/inventory/slot_1` -> updates `wood_plank` quantity
     */
    private fun attachFirestoreListeners(playerId: String) {
        if (playerId.isBlank()) return

        playerListener?.remove()
        inventorySlotListener?.remove()

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w(TAG, "FirebaseApp is not initialized yet.")
                _syncStatusMessage.value = "Offline Mode (Local Cache Active)"
                return
            }

            val db = FirebaseFirestore.getInstance()
            val playerDocRef = db.collection(COLLECTION_PLAYERS).document(playerId)
            val slot1DocRef = playerDocRef.collection(SUBCOLLECTION_INVENTORY).document(DOC_SLOT_1)

            _syncStatusMessage.value = "Connecting to Firestore: players/$playerId"

            // 1. Listen to `players/{playerId}`
            playerListener = playerDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to player doc: ${error.message}", error)
                    _isCloudSynced.value = false
                    _syncStatusMessage.value = "Sync Warning: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val remoteCoins = snapshot.getLong("coins")?.toInt() ?: _coins.value
                    val remoteHighScore = snapshot.getLong("highScore")?.toInt() ?: _highScore.value
                    val remoteSlots = snapshot.getLong("inventorySlots")?.toInt() ?: 5
                    val remoteNickname = snapshot.getString("nickname") ?: _nickname.value

                    _coins.value = remoteCoins
                    _highScore.value = remoteHighScore
                    _inventorySlots.value = remoteSlots
                    if (remoteNickname.isNotBlank()) {
                        _nickname.value = remoteNickname
                        prefs.edit().putString(KEY_CACHED_NICKNAME, remoteNickname).apply()
                    }
                    _isCloudSynced.value = true
                    _syncStatusMessage.value = "Cloud Synced (players/$playerId)"

                    prefs.edit()
                        .putInt(KEY_CACHED_COINS, remoteCoins)
                        .putInt(KEY_CACHED_HIGH_SCORE, remoteHighScore)
                        .apply()

                    Log.d(TAG, "Firestore Player Updated -> Coins: $remoteCoins, HighScore: $remoteHighScore, Nickname: $remoteNickname")
                } else {
                    // Document does not exist yet; create initial setup
                    scope.launch {
                        createInitialPlayerDoc(playerId)
                    }
                }
            }

            // 2. Listen to `players/{playerId}/inventory/slot_1`
            inventorySlotListener = slot1DocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to inventory slot_1: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val remoteQuantity = snapshot.getLong("quantity")?.toInt() ?: 0
                    val itemId = snapshot.getString("itemId") ?: ITEM_WOOD_PLANK

                    if (itemId == ITEM_WOOD_PLANK) {
                        _woodPlanks.value = remoteQuantity
                        prefs.edit().putInt(KEY_CACHED_PLANKS, remoteQuantity).apply()
                        Log.d(TAG, "Firestore Inventory Updated -> $itemId quantity: $remoteQuantity")
                    }
                } else {
                    // Slot_1 does not exist yet; initialize it
                    scope.launch {
                        createInitialInventorySlot(playerId)
                    }
                }
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to attach Firestore listeners: ${e.message}", e)
            _isCloudSynced.value = false
            _syncStatusMessage.value = "Offline Cache Active"
        }
    }

    /**
     * Creates default `players/{playerId}` document if missing.
     */
    private suspend fun createInitialPlayerDoc(playerId: String) = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val initialData = hashMapOf(
                "coins" to _coins.value.coerceAtLeast(100),
                "highScore" to _highScore.value,
                "inventorySlots" to 5,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_PLAYERS).document(playerId).set(initialData, SetOptions.merge()).await()
            _isCloudSynced.value = true
            Log.i(TAG, "Created initial Firestore player document for $playerId")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to create initial player doc: ${e.message}")
        }
    }

    /**
     * Creates default `players/{playerId}/inventory/slot_1` document if missing.
     */
    private suspend fun createInitialInventorySlot(playerId: String) = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val initialSlot = hashMapOf(
                "itemId" to ITEM_WOOD_PLANK,
                "quantity" to _woodPlanks.value.coerceAtLeast(1),
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_PLAYERS).document(playerId)
                .collection(SUBCOLLECTION_INVENTORY).document(DOC_SLOT_1)
                .set(initialSlot, SetOptions.merge()).await()
            Log.i(TAG, "Created initial Firestore inventory slot_1 for $playerId")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to create initial inventory slot: ${e.message}")
        }
    }

    /**
     * Decrements the `wood_plank` quantity by 1 in `players/{playerId}/inventory/slot_1` in Firestore.
     * Called whenever Stickman builds a bridge.
     */
    fun consumeWoodPlank(amount: Int = 1) {
        if (amount <= 0) return

        // 1. Optimistically update local state immediately for 60fps responsiveness
        val currentLocal = _woodPlanks.value
        val updatedLocal = (currentLocal - amount).coerceAtLeast(0)
        _woodPlanks.value = updatedLocal
        prefs.edit().putInt(KEY_CACHED_PLANKS, updatedLocal).apply()

        // 2. Perform atomic decrement in Firestore
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val slotDocRef = db.collection(COLLECTION_PLAYERS).document(activePlayerId)
                        .collection(SUBCOLLECTION_INVENTORY).document(DOC_SLOT_1)

                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(slotDocRef)
                        if (snapshot.exists()) {
                            val currentQty = snapshot.getLong("quantity")?.toInt() ?: 0
                            val newQty = (currentQty - amount).coerceAtLeast(0)
                            transaction.update(
                                slotDocRef,
                                mapOf(
                                    "quantity" to newQty,
                                    "itemId" to ITEM_WOOD_PLANK,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                            newQty
                        } else {
                            val initialSlot = hashMapOf(
                                "itemId" to ITEM_WOOD_PLANK,
                                "quantity" to 0,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            transaction.set(slotDocRef, initialSlot)
                            0
                        }
                    }.await()

                    Log.d(TAG, "Successfully decremented wood_plank on Firestore. Remaining: $updatedLocal")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to decrement wood_plank in Firestore: ${e.message}", e)
            }
        }
    }

    /**
     * Adds wood planks to `players/{playerId}/inventory/slot_1`.
     */
    fun addWoodPlanks(amount: Int) {
        if (amount <= 0) return

        val newLocal = _woodPlanks.value + amount
        _woodPlanks.value = newLocal
        prefs.edit().putInt(KEY_CACHED_PLANKS, newLocal).apply()

        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val slotDocRef = db.collection(COLLECTION_PLAYERS).document(activePlayerId)
                        .collection(SUBCOLLECTION_INVENTORY).document(DOC_SLOT_1)

                    slotDocRef.set(
                        mapOf(
                            "itemId" to ITEM_WOOD_PLANK,
                            "quantity" to FieldValue.increment(amount.toLong()),
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                    Log.d(TAG, "Successfully added $amount wood planks to Firestore.")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to add wood planks to Firestore: ${e.message}", e)
            }
        }
    }

    /**
     * Updates the `highScore` in `players/{playerId}` in Firestore when game over occurs.
     */
    fun updateHighScore(score: Int) {
        val currentBest = _highScore.value
        if (score > currentBest) {
            _highScore.value = score
            prefs.edit().putInt(KEY_CACHED_HIGH_SCORE, score).apply()
        }

        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val playerDocRef = db.collection(COLLECTION_PLAYERS).document(activePlayerId)

                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(playerDocRef)
                        if (snapshot.exists()) {
                            val cloudHigh = snapshot.getLong("highScore")?.toInt() ?: 0
                            if (score > cloudHigh) {
                                transaction.update(
                                    playerDocRef,
                                    mapOf(
                                        "highScore" to score,
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                )
                            }
                        } else {
                            val newDoc = hashMapOf(
                                "coins" to _coins.value,
                                "highScore" to score,
                                "inventorySlots" to _inventorySlots.value,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            transaction.set(playerDocRef, newDoc)
                        }
                    }.await()

                    Log.d(TAG, "Successfully synced High Score ($score) to Firestore players/$activePlayerId")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update high score on Firestore: ${e.message}", e)
            }
        }
    }

    /**
     * Updates coins in Firestore (`players/{playerId}`).
     */
    fun addCoins(amount: Int) {
        if (amount == 0) return
        val newLocal = (_coins.value + amount).coerceAtLeast(0)
        _coins.value = newLocal
        prefs.edit().putInt(KEY_CACHED_COINS, newLocal).apply()

        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val playerDocRef = db.collection(COLLECTION_PLAYERS).document(activePlayerId)

                    playerDocRef.set(
                        mapOf(
                            "coins" to FieldValue.increment(amount.toLong()),
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update coins on Firestore: ${e.message}", e)
            }
        }
    }

    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        if (_coins.value < amount) return false

        val newLocal = _coins.value - amount
        _coins.value = newLocal
        prefs.edit().putInt(KEY_CACHED_COINS, newLocal).apply()

        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val playerDocRef = db.collection(COLLECTION_PLAYERS).document(activePlayerId)

                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(playerDocRef)
                        val cloudCoins = snapshot.getLong("coins")?.toInt() ?: 0
                        val updatedCoins = (cloudCoins - amount).coerceAtLeast(0)
                        transaction.update(
                            playerDocRef,
                            mapOf(
                                "coins" to updatedCoins,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    }.await()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to spend coins on Firestore: ${e.message}", e)
            }
        }
        return true
    }

    fun cleanup() {
        playerListener?.remove()
        inventorySlotListener?.remove()
        playerListener = null
        inventorySlotListener = null
    }

    fun updateNickname(nickname: String) {
        if (nickname.isBlank()) return
        _nickname.value = nickname
        prefs.edit().putString(KEY_CACHED_NICKNAME, nickname).apply()

        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val playerDocRef = db.collection(COLLECTION_PLAYERS).document(activePlayerId)
                    playerDocRef.set(
                        mapOf(
                            "nickname" to nickname,
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                    Log.d(TAG, "Successfully updated nickname on Firestore: $nickname")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update nickname on Firestore: ${e.message}", e)
            }
        }
    }
}
