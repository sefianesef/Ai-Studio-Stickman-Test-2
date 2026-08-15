package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DailyMissionEntity
import com.example.data.local.entity.PlayerProfileEntity
import com.example.data.local.entity.PurchasedItemEntity
import com.example.model.AccessoryItem
import com.example.model.AccessoryType
import com.example.model.ItemRarity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameRepository(
    context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val playerProfileDao = database.playerProfileDao()
    private val inventoryDao = database.inventoryDao()
    private val dailyMissionDao = database.dailyMissionDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("STICKMAN_HERO_DATA", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMS = "GEMS"
        private const val KEY_HIGH_SCORE = "HIGH_SCORE"
        private const val KEY_TOTAL_BRIDGES = "TOTAL_BRIDGES"
        private const val KEY_PERFECT_HITS = "PERFECT_HITS"
        private const val KEY_SOUND_ENABLED = "SOUND_ENABLED"
        private const val KEY_HAPTICS_ENABLED = "HAPTICS_ENABLED"
        private const val KEY_SELECTED_HAT = "SELECTED_HAT"
        private const val KEY_SELECTED_SCARF = "SELECTED_SCARF"
        private const val KEY_SELECTED_STICK = "SELECTED_STICK"
        private const val KEY_SELECTED_SKIN = "SELECTED_SKIN"
        private const val KEY_UNLOCKED_PREFIX = "UNLOCKED_"
        private const val KEY_LAST_CLAIM_DAY = "LAST_CLAIM_DAY"
        private const val KEY_CURRENT_STREAK = "CURRENT_STREAK"

        val DAILY_REWARD_AMOUNTS = listOf(5, 10, 15, 20, 25, 35, 50)
    }

    // Default catalog of shop items
    val availableAccessories: List<AccessoryItem> = listOf(
        // Hats
        AccessoryItem(
            id = "hat_none",
            name = "Default Band",
            type = AccessoryType.HAT,
            cost = 0,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFFDC2626,
            description = "Classic red hero headband",
            iconSymbol = "🧢",
            rarity = ItemRarity.COMMON
        ),
        AccessoryItem(
            id = "hat_crown",
            name = "Royal Crown",
            type = AccessoryType.HAT,
            cost = 25,
            primaryColor = 0xFFFFD700,
            secondaryColor = 0xFFB45309,
            description = "Fit for the true stick ruler",
            iconSymbol = "👑",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "hat_wizard",
            name = "Wizard Hat",
            type = AccessoryType.HAT,
            cost = 35,
            primaryColor = 0xFF6366F1,
            secondaryColor = 0xFF4338CA,
            description = "Channel magic bridge energy",
            iconSymbol = "🧙",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "hat_ninja",
            name = "Ninja Mask",
            type = AccessoryType.HAT,
            cost = 30,
            primaryColor = 0xFF0F172A,
            secondaryColor = 0xFFEF4444,
            description = "Silent step shadow ribbons",
            iconSymbol = "🥷",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "hat_viking",
            name = "Viking Horns",
            type = AccessoryType.HAT,
            cost = 45,
            primaryColor = 0xFF92400E,
            secondaryColor = 0xFFF59E0B,
            description = "Fearless Nordic warrior horns",
            iconSymbol = "⚔️",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "hat_cyber",
            name = "Cyber Visor",
            type = AccessoryType.HAT,
            cost = 60,
            primaryColor = 0xFF06B6D4,
            secondaryColor = 0xFFEC4899,
            description = "Neon augmented reality HUD",
            iconSymbol = "🥽",
            rarity = ItemRarity.LEGENDARY
        ),
        AccessoryItem(
            id = "hat_halo",
            name = "Angelic Halo",
            type = AccessoryType.HAT,
            cost = 50,
            primaryColor = 0xFFFDE047,
            secondaryColor = 0xFFF59E0B,
            description = "Divine ring of celestial light",
            iconSymbol = "😇",
            rarity = ItemRarity.LEGENDARY
        ),
        AccessoryItem(
            id = "hat_pirate",
            name = "Pirate Tricorn",
            type = AccessoryType.HAT,
            cost = 40,
            primaryColor = 0xFF1E293B,
            secondaryColor = 0xFFFBBF24,
            description = "High seas buccaneer cap",
            iconSymbol = "🏴‍☠️",
            rarity = ItemRarity.EPIC
        ),

        // Scarves / Capes
        AccessoryItem(
            id = "scarf_gold",
            name = "Golden Scarf",
            type = AccessoryType.SCARF,
            cost = 0,
            primaryColor = 0xFFFACC15,
            secondaryColor = 0xFFCA8A04,
            description = "Soft warm fluttery scarf",
            iconSymbol = "🧣",
            rarity = ItemRarity.COMMON
        ),
        AccessoryItem(
            id = "scarf_hero_cape",
            name = "Crimson Cape",
            type = AccessoryType.SCARF,
            cost = 20,
            primaryColor = 0xFFDC2626,
            secondaryColor = 0xFF991B1B,
            description = "Flows boldly in high winds",
            iconSymbol = "🦸",
            rarity = ItemRarity.COMMON
        ),
        AccessoryItem(
            id = "scarf_emerald",
            name = "Emerald Cloak",
            type = AccessoryType.SCARF,
            cost = 30,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF047857,
            description = "Infused with forest wind",
            iconSymbol = "🍃",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "scarf_void",
            name = "Shadow Cloak",
            type = AccessoryType.SCARF,
            cost = 50,
            primaryColor = 0xFF8B5CF6,
            secondaryColor = 0xFF4C1D95,
            description = "Woven from midnight starlight",
            iconSymbol = "🌌",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "scarf_neon_pulse",
            name = "Neon Streamer",
            type = AccessoryType.SCARF,
            cost = 65,
            primaryColor = 0xFFF43F5E,
            secondaryColor = 0xFFBE185D,
            description = "Vibrant synthwave energy tail",
            iconSymbol = "⚡",
            rarity = ItemRarity.LEGENDARY
        ),
        AccessoryItem(
            id = "scarf_flame",
            name = "Phoenix Wings",
            type = AccessoryType.SCARF,
            cost = 55,
            primaryColor = 0xFFF97316,
            secondaryColor = 0xFFDC2626,
            description = "Blazing solar ember plumage",
            iconSymbol = "🔥",
            rarity = ItemRarity.LEGENDARY
        ),

        // Sticks (Bridge Visual Themes)
        AccessoryItem(
            id = "stick_wood",
            name = "Classic Staff",
            type = AccessoryType.STICK,
            cost = 0,
            primaryColor = 0xFFF1F5F9,
            secondaryColor = 0xFF94A3B8,
            description = "Reliable clean bridge staff",
            iconSymbol = "🥢",
            rarity = ItemRarity.COMMON
        ),
        AccessoryItem(
            id = "stick_laser",
            name = "Cyan Laser Beam",
            type = AccessoryType.STICK,
            cost = 25,
            primaryColor = 0xFF22D3EE,
            secondaryColor = 0xFF0284C7,
            description = "Glowing high-tech plasma bridge",
            iconSymbol = "💠",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "stick_gold",
            name = "Golden Scepter",
            type = AccessoryType.STICK,
            cost = 40,
            primaryColor = 0xFFFBBF24,
            secondaryColor = 0xFFD97706,
            description = "Shimmering opulent gold bar",
            iconSymbol = "✨",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "stick_candy",
            name = "Candy Cane",
            type = AccessoryType.STICK,
            cost = 35,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFFFFFFFF,
            description = "Festive peppermint bridge",
            iconSymbol = "🍬",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "stick_dark",
            name = "Void Matter Beam",
            type = AccessoryType.STICK,
            cost = 55,
            primaryColor = 0xFFA855F7,
            secondaryColor = 0xFF3B0764,
            description = "Pulsing purple antimatter bridge",
            iconSymbol = "🔮",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "stick_lava",
            name = "Molten Magma",
            type = AccessoryType.STICK,
            cost = 45,
            primaryColor = 0xFFEA580C,
            secondaryColor = 0xFFFBBF24,
            description = "Superheated molten lava girder",
            iconSymbol = "🌋",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "stick_rainbow",
            name = "Prism Spectrum",
            type = AccessoryType.STICK,
            cost = 60,
            primaryColor = 0xFFEC4899,
            secondaryColor = 0xFF38BDF8,
            description = "Shifting rainbow light bridge",
            iconSymbol = "🌈",
            rarity = ItemRarity.LEGENDARY
        ),
        AccessoryItem(
            id = "stick_cyber",
            name = "Matrix Grid Rail",
            type = AccessoryType.STICK,
            cost = 50,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF059669,
            description = "Cybernetic emerald data stream",
            iconSymbol = "🟢",
            rarity = ItemRarity.EPIC
        ),

        // Body Skins / Outfits
        AccessoryItem(
            id = "skin_white",
            name = "Classic Pure",
            type = AccessoryType.BODY_SKIN,
            cost = 0,
            primaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFFE2E8F0,
            description = "The original white stickman hero",
            iconSymbol = "⚪",
            rarity = ItemRarity.COMMON
        ),
        AccessoryItem(
            id = "skin_shadow",
            name = "Shadow Shinobi",
            type = AccessoryType.BODY_SKIN,
            cost = 25,
            primaryColor = 0xFF334155,
            secondaryColor = 0xFF0F172A,
            description = "Dark sleek ninja silhouette",
            iconSymbol = "⚫",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "skin_frost",
            name = "Frost Spectre",
            type = AccessoryType.BODY_SKIN,
            cost = 40,
            primaryColor = 0xFF38BDF8,
            secondaryColor = 0xFF0284C7,
            description = "Chilling arctic cyan warrior",
            iconSymbol = "🔵",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "skin_gold",
            name = "Solar Aura",
            type = AccessoryType.BODY_SKIN,
            cost = 50,
            primaryColor = 0xFFFBBF24,
            secondaryColor = 0xFFF59E0B,
            description = "Radiant golden sun champion",
            iconSymbol = "🟡",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "skin_neon_pink",
            name = "Synth Cyberpunk",
            type = AccessoryType.BODY_SKIN,
            cost = 35,
            primaryColor = 0xFFF43F5E,
            secondaryColor = 0xFFBE185D,
            description = "High-voltage neon pink speedster",
            iconSymbol = "🟣",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "skin_emerald",
            name = "Emerald Archer",
            type = AccessoryType.BODY_SKIN,
            cost = 30,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF047857,
            description = "Agile forest woodland tracker",
            iconSymbol = "🟢",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "skin_crimson",
            name = "Inferno Knight",
            type = AccessoryType.BODY_SKIN,
            cost = 45,
            primaryColor = 0xFFDC2626,
            secondaryColor = 0xFF991B1B,
            description = "Fiery red battle-hardened hero",
            iconSymbol = "🔴",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "skin_galaxy",
            name = "Cosmic Voyager",
            type = AccessoryType.BODY_SKIN,
            cost = 65,
            primaryColor = 0xFFC084FC,
            secondaryColor = 0xFF6366F1,
            description = "Astral stardust deep space traveler",
            iconSymbol = "✨",
            rarity = ItemRarity.LEGENDARY
        )
    )

    private val _gems = MutableStateFlow(prefs.getInt(KEY_GEMS, 10)) // 10 starter gems!
    val gems: StateFlow<Int> = _gems.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.getInt(KEY_HIGH_SCORE, 0))
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    private val _selectedHat = MutableStateFlow(prefs.getString(KEY_SELECTED_HAT, "hat_none") ?: "hat_none")
    val selectedHat: StateFlow<String> = _selectedHat.asStateFlow()

    private val _selectedScarf = MutableStateFlow(prefs.getString(KEY_SELECTED_SCARF, "scarf_gold") ?: "scarf_gold")
    val selectedScarf: StateFlow<String> = _selectedScarf.asStateFlow()

    private val _selectedStick = MutableStateFlow(prefs.getString(KEY_SELECTED_STICK, "stick_wood") ?: "stick_wood")
    val selectedStick: StateFlow<String> = _selectedStick.asStateFlow()

    private val _selectedSkin = MutableStateFlow(prefs.getString(KEY_SELECTED_SKIN, "skin_white") ?: "skin_white")
    val selectedSkin: StateFlow<String> = _selectedSkin.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    // Daily Login Reward & Streak
    private val _currentStreak = MutableStateFlow(1)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _isDailyRewardAvailable = MutableStateFlow(true)
    val isDailyRewardAvailable: StateFlow<Boolean> = _isDailyRewardAvailable.asStateFlow()

    // Reactive Room DB Flows
    val playerProfileFlow: Flow<PlayerProfileEntity?> = playerProfileDao.getPlayerProfileFlow()
    val purchasedItemsFlow: Flow<List<PurchasedItemEntity>> = inventoryDao.getAllPurchasedItemsFlow()
    val dailyMissionsFlow: Flow<List<DailyMissionEntity>> = dailyMissionDao.getMissionsForDayFlow(getTodayEpochDay())

    init {
        // Unlock all free items in memory and Room
        val freeItems = availableAccessories.filter { it.cost == 0 }
        freeItems.forEach {
            if (!isItemUnlocked(it.id)) {
                unlockItem(it.id)
            }
        }

        // Initialize / sync Room database in background
        scope.launch {
            val existingProfile = playerProfileDao.getPlayerProfile()
            if (existingProfile == null) {
                val initialProfile = PlayerProfileEntity(
                    id = 1,
                    totalGems = _gems.value,
                    highScore = _highScore.value,
                    totalBridgesBuilt = prefs.getInt(KEY_TOTAL_BRIDGES, 0),
                    totalPerfectHits = prefs.getInt(KEY_PERFECT_HITS, 0),
                    currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 1),
                    lastClaimEpochDay = prefs.getLong(KEY_LAST_CLAIM_DAY, 0L),
                    selectedHatId = _selectedHat.value,
                    selectedScarfId = _selectedScarf.value,
                    selectedStickId = _selectedStick.value,
                    selectedSkinId = _selectedSkin.value,
                    soundEnabled = _soundEnabled.value,
                    hapticsEnabled = _hapticsEnabled.value
                )
                playerProfileDao.insertOrUpdate(initialProfile)
            }

            // Sync free items to inventory DAO
            val purchasedEntities = freeItems.map {
                PurchasedItemEntity(
                    itemId = it.id,
                    itemType = it.type.name,
                    costPaid = it.cost
                )
            }
            inventoryDao.insertItems(purchasedEntities)

            // Setup or refresh Daily Missions for today
            initDailyMissions()
        }

        refreshDailyRewardState()
    }

    private suspend fun initDailyMissions() {
        val today = getTodayEpochDay()
        dailyMissionDao.cleanOldMissions(today - 7)
        val existing = dailyMissionDao.getMissionsForDay(today)
        if (existing.isEmpty()) {
            val defaults = listOf(
                DailyMissionEntity(
                    id = "m_bridge_${today}",
                    title = "Master Builder",
                    description = "Build 6 bridges successfully",
                    missionType = "BUILD_BRIDGES",
                    targetCount = 6,
                    currentProgress = 0,
                    rewardGems = 10,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                ),
                DailyMissionEntity(
                    id = "m_bullseye_${today}",
                    title = "Bullseye Precision",
                    description = "Land 2 perfect center red-dot hits",
                    missionType = "PERFECT_HITS",
                    targetCount = 2,
                    currentProgress = 0,
                    rewardGems = 15,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                ),
                DailyMissionEntity(
                    id = "m_gem_${today}",
                    title = "Crystal Harvester",
                    description = "Collect 4 gems across runs",
                    missionType = "COLLECT_GEMS",
                    targetCount = 4,
                    currentProgress = 0,
                    rewardGems = 12,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                )
            )
            dailyMissionDao.insertMissions(defaults)
        }
    }

    fun trackMissionProgress(missionType: String, delta: Int = 1) {
        scope.launch {
            val today = getTodayEpochDay()
            val missions = dailyMissionDao.getMissionsForDay(today)
            for (m in missions) {
                if (m.missionType == missionType && !m.isCompleted) {
                    val newProgress = (m.currentProgress + delta).coerceAtMost(m.targetCount)
                    val isDone = newProgress >= m.targetCount
                    dailyMissionDao.updateProgress(m.id, newProgress, isDone)
                }
            }
        }
    }

    fun claimMissionReward(missionId: String, rewardGems: Int) {
        scope.launch {
            dailyMissionDao.markClaimed(missionId)
        }
        addGems(rewardGems)
    }

    private fun getTodayEpochDay(): Long {
        return try {
            java.time.LocalDate.now().toEpochDay()
        } catch (_: Exception) {
            System.currentTimeMillis() / (1000L * 60 * 60 * 24)
        }
    }

    fun refreshDailyRewardState() {
        val today = getTodayEpochDay()
        val lastClaimDay = prefs.getLong(KEY_LAST_CLAIM_DAY, 0L)
        val savedStreak = prefs.getInt(KEY_CURRENT_STREAK, 1).coerceIn(1, 7)

        when {
            lastClaimDay == 0L -> {
                // First time ever
                _currentStreak.value = 1
                _isDailyRewardAvailable.value = true
            }
            lastClaimDay == today -> {
                // Already claimed today
                _currentStreak.value = savedStreak
                _isDailyRewardAvailable.value = false
            }
            lastClaimDay == today - 1 -> {
                // Logged in on consecutive day - streak continues!
                val nextStreak = if (savedStreak >= 7) 1 else savedStreak + 1
                _currentStreak.value = nextStreak
                _isDailyRewardAvailable.value = true
            }
            else -> {
                // Missed one or more days - reset streak back to day 1
                _currentStreak.value = 1
                _isDailyRewardAvailable.value = true
            }
        }
    }

    fun claimDailyReward(): Int {
        val today = getTodayEpochDay()
        val streak = _currentStreak.value.coerceIn(1, 7)
        val rewardGems = DAILY_REWARD_AMOUNTS.getOrElse(streak - 1) { 10 }

        // Add gems
        addGems(rewardGems)

        // Record claim in prefs
        prefs.edit()
            .putLong(KEY_LAST_CLAIM_DAY, today)
            .putInt(KEY_CURRENT_STREAK, streak)
            .apply()

        // Sync to Room
        scope.launch {
            playerProfileDao.updateDailyStreak(streak, today)
        }

        _isDailyRewardAvailable.value = false
        return rewardGems
    }

    fun getStreakDayReward(day: Int): Int {
        val index = (day - 1).coerceIn(0, DAILY_REWARD_AMOUNTS.size - 1)
        return DAILY_REWARD_AMOUNTS[index]
    }

    fun isItemUnlocked(id: String): Boolean {
        return prefs.getBoolean(KEY_UNLOCKED_PREFIX + id, false)
    }

    fun unlockItem(id: String, itemType: AccessoryType? = null, costPaid: Int = 0) {
        prefs.edit().putBoolean(KEY_UNLOCKED_PREFIX + id, true).apply()
        val typeStr = itemType?.name ?: availableAccessories.find { it.id == id }?.type?.name ?: "OTHER"
        scope.launch {
            inventoryDao.insertItem(
                PurchasedItemEntity(
                    itemId = id,
                    itemType = typeStr,
                    costPaid = costPaid
                )
            )
        }
    }

    fun addGems(amount: Int) {
        val newGems = _gems.value + amount
        _gems.value = newGems
        prefs.edit().putInt(KEY_GEMS, newGems).apply()
        scope.launch {
            playerProfileDao.updateGems(newGems)
        }
    }

    fun spendGems(amount: Int): Boolean {
        if (_gems.value >= amount) {
            val newGems = _gems.value - amount
            _gems.value = newGems
            prefs.edit().putInt(KEY_GEMS, newGems).apply()
            scope.launch {
                playerProfileDao.updateGems(newGems)
            }
            return true
        }
        return false
    }

    fun updateHighScore(score: Int): Boolean {
        if (score > _highScore.value) {
            _highScore.value = score
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
            scope.launch {
                playerProfileDao.updateHighScore(score)
            }
            return true
        }
        return false
    }

    fun recordBridgeBuilt() {
        val current = prefs.getInt(KEY_TOTAL_BRIDGES, 0)
        prefs.edit().putInt(KEY_TOTAL_BRIDGES, current + 1).apply()
        scope.launch {
            playerProfileDao.incrementBridgesBuilt()
        }
    }

    fun recordPerfectHit() {
        val current = prefs.getInt(KEY_PERFECT_HITS, 0)
        prefs.edit().putInt(KEY_PERFECT_HITS, current + 1).apply()
        scope.launch {
            playerProfileDao.incrementPerfectHits()
        }
    }

    fun buyAndEquip(item: AccessoryItem): Boolean {
        if (isItemUnlocked(item.id)) {
            equip(item)
            return true
        }
        if (spendGems(item.cost)) {
            unlockItem(item.id, item.type, item.cost)
            equip(item)
            return true
        }
        return false
    }

    fun equip(item: AccessoryItem) {
        when (item.type) {
            AccessoryType.HAT -> {
                _selectedHat.value = item.id
                prefs.edit().putString(KEY_SELECTED_HAT, item.id).apply()
            }
            AccessoryType.SCARF -> {
                _selectedScarf.value = item.id
                prefs.edit().putString(KEY_SELECTED_SCARF, item.id).apply()
            }
            AccessoryType.STICK -> {
                _selectedStick.value = item.id
                prefs.edit().putString(KEY_SELECTED_STICK, item.id).apply()
            }
            AccessoryType.BODY_SKIN -> {
                _selectedSkin.value = item.id
                prefs.edit().putString(KEY_SELECTED_SKIN, item.id).apply()
            }
        }
        scope.launch {
            playerProfileDao.updateEquippedCustomizations(
                hatId = _selectedHat.value,
                scarfId = _selectedScarf.value,
                stickId = _selectedStick.value,
                skinId = _selectedSkin.value
            )
        }
    }

    fun toggleSound(): Boolean {
        val next = !_soundEnabled.value
        _soundEnabled.value = next
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, next).apply()
        scope.launch {
            playerProfileDao.updateSoundEnabled(next)
        }
        return next
    }

    fun toggleHaptics(): Boolean {
        val next = !_hapticsEnabled.value
        _hapticsEnabled.value = next
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, next).apply()
        scope.launch {
            playerProfileDao.updateHapticsEnabled(next)
        }
        return next
    }
}
