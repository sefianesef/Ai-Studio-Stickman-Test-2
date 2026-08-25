package com.mygames.stickmanrush.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mygames.stickmanrush.audio.HapticManager
import com.mygames.stickmanrush.audio.SoundManager
import com.mygames.stickmanrush.billing.BillingManager
import com.mygames.stickmanrush.data.GameRepository
import com.mygames.stickmanrush.data.local.entity.DailyMissionEntity
import com.mygames.stickmanrush.game.StickmanGameEngine
import com.mygames.stickmanrush.model.AccessoryItem
import com.mygames.stickmanrush.model.AccessoryType
import com.mygames.stickmanrush.security.CurrencySource
import com.mygames.stickmanrush.security.PurchaseVerificationService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GameRepository(application)
    val hapticManager = HapticManager(application).apply {
        isEnabled = repository.hapticsEnabled.value
    }
    val soundManager = SoundManager(application).apply {
        soundEnabled = repository.soundEnabled.value
        hapticsEnabled = repository.hapticsEnabled.value
    }

    private val purchaseVerifier = PurchaseVerificationService(application)

    val billingManager = BillingManager(application, viewModelScope).apply {
        setOnPurchaseSuccessListener { purchaseData ->
            viewModelScope.launch {
                val verification = purchaseVerifier.verifyWithGooglePlayServer(
                    productId = purchaseData.productId,
                    purchaseToken = purchaseData.purchaseToken,
                    orderId = purchaseData.orderId
                )
                if (verification.isValid) {
                    val gemCount = when (purchaseData.productId) {
                        BillingManager.SKU_GEMS_TIER1 -> 100
                        BillingManager.SKU_GEMS_TIER2 -> 550
                        BillingManager.SKU_GEMS_TIER3 -> 1200
                        BillingManager.SKU_GEMS_TIER4 -> 3000
                        BillingManager.SKU_VIP_PASS -> 5000
                        else -> 0
                    }

                    if (gemCount > 0) {
                        repository.addGems(gemCount, CurrencySource.IN_APP_PURCHASE, verification.verificationToken)
                        // Also sync directly to Firebase Cloud Wallet if user is online
                        repository.cloudWalletService.creditCloudGems(gemCount, "IAP_${purchaseData.productId}", verification.verificationToken)
                    }

                    when (purchaseData.productId) {
                        BillingManager.SKU_GEMS_TIER1,
                        BillingManager.SKU_GEMS_TIER2,
                        BillingManager.SKU_GEMS_TIER3,
                        BillingManager.SKU_GEMS_TIER4 -> {
                            soundManager.playBuyGemsSuccess()
                            hapticManager.levelUp()
                        }
                        BillingManager.SKU_LIFE_PACK_10 -> {
                            engine.addLives(10)
                            soundManager.playBuyGemsSuccess()
                            hapticManager.levelUp()
                        }
                        BillingManager.SKU_VIP_PASS -> {
                            soundManager.playVictoryMusic()
                            hapticManager.levelUp()
                        }
                        BillingManager.SKU_REMOVE_ADS -> {
                            soundManager.playBuyGemsSuccess()
                            hapticManager.levelUp()
                        }
                    }
                } else {
                    android.util.Log.e("GameViewModel", "REJECTED IAP: Google Play verification failed for ${purchaseData.productId}: ${verification.message}")
                }
            }
        }
    }

    val engine = StickmanGameEngine(
        repository = repository,
        soundManager = soundManager,
        hapticManager = hapticManager
    )

    // UI Overlays
    private val _isShopOpen = MutableStateFlow(false)
    val isShopOpen: StateFlow<Boolean> = _isShopOpen.asStateFlow()

    private val _isPauseMenuOpen = MutableStateFlow(false)
    val isPauseMenuOpen: StateFlow<Boolean> = _isPauseMenuOpen.asStateFlow()

    private val _isHowToPlayOpen = MutableStateFlow(false)
    val isHowToPlayOpen: StateFlow<Boolean> = _isHowToPlayOpen.asStateFlow()

    private val _isDailyRewardOpen = MutableStateFlow(false)
    val isDailyRewardOpen: StateFlow<Boolean> = _isDailyRewardOpen.asStateFlow()

    private val _isDailyMissionsOpen = MutableStateFlow(false)
    val isDailyMissionsOpen: StateFlow<Boolean> = _isDailyMissionsOpen.asStateFlow()

    private val _isWeeklyMissionsOpen = MutableStateFlow(false)
    val isWeeklyMissionsOpen: StateFlow<Boolean> = _isWeeklyMissionsOpen.asStateFlow()

    private val _isContestsOpen = MutableStateFlow(false)
    val isContestsOpen: StateFlow<Boolean> = _isContestsOpen.asStateFlow()

    private val _isMainMenuOpen = MutableStateFlow(false)
    val isMainMenuOpen: StateFlow<Boolean> = _isMainMenuOpen.asStateFlow()

    private val _isPlayerStatsOpen = MutableStateFlow(false)
    val isPlayerStatsOpen: StateFlow<Boolean> = _isPlayerStatsOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isLeaderboardOpen = MutableStateFlow(false)
    val isLeaderboardOpen: StateFlow<Boolean> = _isLeaderboardOpen.asStateFlow()

    private val _isSpinWheelOpen = MutableStateFlow(false)
    val isSpinWheelOpen: StateFlow<Boolean> = _isSpinWheelOpen.asStateFlow()

    private val _isRealMoneyShopOpen = MutableStateFlow(false)
    val isRealMoneyShopOpen: StateFlow<Boolean> = _isRealMoneyShopOpen.asStateFlow()

    private val _isOutOfGemsOfferOpen = MutableStateFlow(false)
    val isOutOfGemsOfferOpen: StateFlow<Boolean> = _isOutOfGemsOfferOpen.asStateFlow()

    private val _isLifeShopOpen = MutableStateFlow(false)
    val isLifeShopOpen: StateFlow<Boolean> = _isLifeShopOpen.asStateFlow()

    // Pass through engine lives and challenge states
    val lives: StateFlow<Int> = engine.lives
    val maxLives: Int = engine.maxLives
    val activeChallengeDialog: StateFlow<com.mygames.stickmanrush.model.ChallengeDialogData?> = engine.activeChallengeDialog

    val lifeShopPacks: List<com.mygames.stickmanrush.model.LifeShopPack> = listOf(
        com.mygames.stickmanrush.model.LifeShopPack(
            id = "pack_life_3",
            livesCount = 3,
            gemCost = 30,
            tag = "POPULAR",
            iconEmoji = "❤️"
        ),
        com.mygames.stickmanrush.model.LifeShopPack(
            id = "pack_life_7",
            livesCount = 7,
            gemCost = 50,
            tag = "BEST VALUE",
            iconEmoji = "💖"
        ),
        com.mygames.stickmanrush.model.LifeShopPack(
            id = "pack_life_15",
            livesCount = 15,
            gemCost = 90,
            tag = "MEGA PACK",
            iconEmoji = "🔥"
        ),
        com.mygames.stickmanrush.model.LifeShopPack(
            id = "pack_life_ad",
            livesCount = 2,
            isAd = true,
            tag = "FREE AD",
            iconEmoji = "📺"
        ),
        com.mygames.stickmanrush.model.LifeShopPack(
            id = "pack_life_money_10",
            livesCount = 10,
            realMoneyPrice = "$0.99",
            tag = "UNLIMITED PLAY",
            iconEmoji = "💎"
        ),
        com.mygames.stickmanrush.model.LifeShopPack(
            id = "pack_life_money_25",
            livesCount = 25,
            realMoneyPrice = "$1.99",
            tag = "CHAMPION PACK",
            iconEmoji = "👑"
        )
    )

    fun openLifeShop(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isLifeShopOpen.value = open
    }

    fun buyLifePack(pack: com.mygames.stickmanrush.model.LifeShopPack): Boolean {
        when {
            pack.isAd -> {
                engine.addLives(pack.livesCount)
                soundManager.playBuyGemsSuccess()
                soundManager.playVictoryMusic()
                hapticManager.missionClaim()
                return true
            }
            pack.realMoneyPrice.isNotEmpty() -> {
                engine.addLives(pack.livesCount)
                soundManager.playBuyGemsSuccess()
                soundManager.playVictoryMusic()
                hapticManager.levelUp()
                return true
            }
            pack.gemCost > 0 -> {
                if (repository.spendGems(pack.gemCost)) {
                    engine.addLives(pack.livesCount)
                    soundManager.playBuyGemsSuccess()
                    soundManager.playGemCollect()
                    hapticManager.gemCollect()
                    return true
                } else {
                    soundManager.playButton()
                    hapticManager.uiClick()
                    // Prompt real money shop or out of gems
                    return false
                }
            }
            else -> return false
        }
    }

    fun dismissChallengeDialog() {
        engine.dismissChallengeDialog()
    }

    // Shop tab
    private val _selectedShopTab = MutableStateFlow(AccessoryType.HAT)
    val selectedShopTab: StateFlow<AccessoryType> = _selectedShopTab.asStateFlow()

    // Pass through repository states
    val gems: StateFlow<Int> = repository.gems
    val blueGems: StateFlow<Int> = repository.blueGems
    val redGems: StateFlow<Int> = repository.redGems
    val highScore: StateFlow<Int> = repository.highScore
    val savedLevel: StateFlow<Int> = repository.savedLevel
    val highestUnlockedLevel: StateFlow<Int> = repository.highestUnlockedLevel
    val secondsUntilNextLife: StateFlow<Long> = repository.secondsUntilNextLife
    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabled
    val leftHandedMode: StateFlow<Boolean> = repository.leftHandedMode
    val highFrameRate: StateFlow<Boolean> = repository.highFrameRate
    val particleQualityUltra: StateFlow<Boolean> = repository.particleQualityUltra
    val screenShakeEnabled: StateFlow<Boolean> = repository.screenShakeEnabled

    fun getUnlockedCheckpoints(): List<Int> = repository.getUnlockedCheckpoints()

    fun startGameFromCheckpoint(level: Int) {
        engine.startGame(startLevel = level)
    }

    // Shop Currency Filter
    private val _shopCurrencyFilter = MutableStateFlow("ALL")
    val shopCurrencyFilter: StateFlow<String> = _shopCurrencyFilter.asStateFlow()

    fun setShopCurrencyFilter(filter: String) {
        _shopCurrencyFilter.value = filter
        soundManager.playButton()
        hapticManager.uiClick()
    }

    // Gem Vault & Leaderboard catalogs
    val availableGemPacks = repository.availableGemPacks
    fun getLeaderboard() = repository.getGlobalLeaderboard()
    fun getUserLeague() = repository.getUserTournamentLeague()
    fun getUpcomingRivals() = repository.getUpcomingRivals()
    fun isDailyFreeGemsAvailable() = repository.isDailyFreeGemsAvailable()

    fun openLeaderboard(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isLeaderboardOpen.value = open
    }

    fun openSpinWheel(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isSpinWheelOpen.value = open
    }

    val adEarnedSpins: StateFlow<Int> = repository.adEarnedSpins
    val isDailyFreeSpinAvailableFlow: StateFlow<Boolean> = repository.isDailyFreeSpinAvailableFlow

    fun isDailyFreeSpinAvailable(): Boolean = repository.isDailyFreeSpinAvailable()
    fun getAdSpinsCount(): Int = repository.getAdSpinsCount()
    fun hasAvailableSpin(): Boolean = repository.hasAvailableSpin()

    fun watchAdForSpin(onAdComplete: (Boolean) -> Unit) {
        soundManager.playButton()
        hapticManager.uiClick()
        val adSession = repository.adSsvManager.createAdSession(adUnitId = "rewarded_spin_ad")
        val success = repository.verifyAndGrantAdReward(adSession, 1)
        if (success) {
            soundManager.playBuyGemsSuccess()
            hapticManager.missionClaim()
        }
        onAdComplete(success)
    }

    fun buySpinsWithGems(gemCost: Int, spinsCount: Int): Boolean {
        val success = repository.buySpinsWithGems(gemCost, spinsCount)
        if (success) {
            soundManager.playBuyGemsSuccess()
            hapticManager.missionClaim()
        }
        return success
    }

    fun buySpinsRealMoney(spinsCount: Int) {
        repository.buySpinsRealMoney(spinsCount)
        soundManager.playBuyGemsSuccess()
        hapticManager.levelUp()
    }

    fun spinLuckyWheel(): Int {
        val reward = repository.spinLuckyWheel()
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
        return reward
    }

    fun buyGemPack(pack: com.mygames.stickmanrush.model.GemPack): Boolean {
        val success = repository.buyGemPackWithTokens(pack)
        if (success) {
            soundManager.playBuyGemsSuccess()
            soundManager.playGemCollect()
            hapticManager.missionClaim()
        }
        return success
    }

    fun buyGemPackRealMoney(pack: com.mygames.stickmanrush.model.GemPack, activity: Activity? = null): Boolean {
        val sku = when (pack.gemAmount) {
            100 -> BillingManager.SKU_GEMS_TIER1
            500, 550 -> BillingManager.SKU_GEMS_TIER2
            1000, 1200 -> BillingManager.SKU_GEMS_TIER3
            else -> BillingManager.SKU_GEMS_TIER4
        }
        if (activity == null) {
            android.util.Log.e("GameViewModel", "Cannot launch Play Billing flow for $sku without foreground Activity.")
            return false
        }
        soundManager.playButton()
        hapticManager.uiClick()
        return billingManager.launchBillingFlow(activity, sku)
    }

    fun buyLifePackRealMoney(pack: com.mygames.stickmanrush.model.LifeShopPack, activity: Activity? = null): Boolean {
        val sku = BillingManager.SKU_LIFE_PACK_10
        if (activity == null) {
            android.util.Log.e("GameViewModel", "Cannot launch Play Billing flow for $sku without foreground Activity.")
            return false
        }
        soundManager.playButton()
        hapticManager.uiClick()
        return billingManager.launchBillingFlow(activity, sku)
    }

    val activeLevelVictory: StateFlow<com.mygames.stickmanrush.model.LevelVictoryData?> = engine.activeLevelVictory

    fun dismissLevelVictory() {
        engine.dismissLevelVictory()
    }

    fun claim3xLevelBonus(baseGems: Int): Int {
        val extra = repository.claim3xLevelBonus(baseGems)
        soundManager.playVictoryMusic()
        soundManager.playBuyGemsSuccess()
        hapticManager.levelUp()
        return extra
    }

    fun acceptSecondChanceRevive() {
        engine.acceptSecondChanceRevive(isRewardedAd = true)
        repository.recordRewardedAdWatched()
        soundManager.playVictoryMusic()
        hapticManager.levelUp()
    }

    fun declineSecondChanceRevive() {
        engine.declineSecondChanceRevive()
    }

    fun rentItemForTestDrive(item: AccessoryItem) {
        repository.rentItemForTestDrive(item, 3)
        repository.recordRewardedAdWatched()
        soundManager.playBuyGemsSuccess()
        soundManager.playVictoryMusic()
        hapticManager.levelUp()
    }

    val rentedSkinId: StateFlow<String?> = repository.rentedSkinId
    val rentedSkinRunsRemaining: StateFlow<Int> = repository.rentedSkinRunsRemaining
    val rentedStickId: StateFlow<String?> = repository.rentedStickId
    val rentedStickRunsRemaining: StateFlow<Int> = repository.rentedStickRunsRemaining

    fun getDailyRewardedSpinsRemaining(): Int = repository.getDailyRewardedSpinsRemaining()

    fun watchAdForLuckySpin(onComplete: (Boolean) -> Unit = {}) {
        val watched = repository.watchAdForLuckySpin()
        if (watched) {
            soundManager.playBuyGemsSuccess()
            hapticManager.levelUp()
        }
        onComplete(watched)
    }

    fun spinLuckyWheelDetailed(): com.mygames.stickmanrush.model.LuckyWheelSpinResult {
        val result = repository.spinLuckyWheelDetailed()
        soundManager.playBuyGemsSuccess()
        soundManager.playVictoryMusic()
        hapticManager.levelUp()
        return result
    }

    fun revivePlayer(): Boolean {
        val cost = engine.getReviveCost()
        if (repository.spendGems(cost)) {
            engine.reviveRun()
            return true
        }
        return false
    }

    fun canAffordRevive(): Boolean {
        return repository.gems.value >= engine.getReviveCost()
    }

    // Daily streak states
    val currentStreak: StateFlow<Int> = repository.currentStreak
    val isDailyRewardAvailable: StateFlow<Boolean> = repository.isDailyRewardAvailable

    // Daily Missions Flow
    val dailyMissions: StateFlow<List<DailyMissionEntity>> = repository.dailyMissionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive Room DB StateFlows
    val playerProfile = repository.playerProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val purchasedItems = repository.purchasedItemsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedHatId: StateFlow<String> = repository.selectedHat
    val selectedScarfId: StateFlow<String> = repository.selectedScarf
    val selectedStickId: StateFlow<String> = repository.selectedStick
    val selectedSkinId: StateFlow<String> = repository.selectedSkin
    val selectedThemeId: StateFlow<String> = repository.selectedTheme

    val availableAccessories: List<AccessoryItem> get() = repository.availableAccessories

    fun openDailyReward(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isDailyRewardOpen.value = open
    }

    fun openDailyMissions(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isDailyMissionsOpen.value = open
    }

    fun openWeeklyMissions(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isWeeklyMissionsOpen.value = open
    }

    fun getWeeklyMissions() = repository.getWeeklyMissions()

    fun claimWeeklyMission(missionId: String, rewardGems: Int, rewardBlueGems: Int = 0) {
        val claimed = repository.claimWeeklyMission(missionId, rewardGems, rewardBlueGems)
        if (claimed) {
            soundManager.playVictoryMusic()
            soundManager.playGemCollect()
            soundManager.playPerfectHit()
            hapticManager.missionClaim()
        }
    }

    fun claimAllWeeklyMissions(): Pair<Int, Int> {
        val (totalGems, totalBlue) = repository.claimAllWeeklyMissions()
        if (totalGems > 0 || totalBlue > 0) {
            soundManager.playVictoryMusic()
            soundManager.playGemCollect()
            hapticManager.levelUp()
        }
        return totalGems to totalBlue
    }

    fun openContests(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isContestsOpen.value = open
    }

    fun getContests() = repository.getContestTournaments()

    fun claimContest(contestId: String): Triple<Int, Int, Int> {
        val (totalGems, totalBlue, totalRed) = repository.claimContestReward(contestId)
        if (totalGems > 0 || totalBlue > 0 || totalRed > 0) {
            soundManager.playVictoryMusic()
            soundManager.playGemCollect()
            hapticManager.levelUp()
        }
        return Triple(totalGems, totalBlue, totalRed)
    }

    fun openMainMenu(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isMainMenuOpen.value = open
    }

    fun openPlayerStats(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isPlayerStatsOpen.value = open
    }

    fun getPlayerCareerStats() = repository.getPlayerCareerStats()

    fun openSettings(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isSettingsOpen.value = open
    }

    fun toggleLeftHanded() {
        repository.toggleLeftHanded()
        hapticManager.uiClick()
    }

    fun toggleHighFrameRate() {
        repository.toggleHighFrameRate()
        hapticManager.uiClick()
    }

    fun toggleParticleQuality() {
        repository.toggleParticleQuality()
        hapticManager.uiClick()
    }

    fun toggleScreenShake() {
        repository.toggleScreenShake()
        hapticManager.uiClick()
    }

    fun resetCareerProgress() {
        repository.resetCareerProgress()
        hapticManager.gameOver()
    }

    fun claimDailyMission(missionId: String, rewardGems: Int, multiplier: Int = 1) {
        repository.claimMissionReward(missionId, rewardGems * multiplier)
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
    }

    fun claimAllDailyMissions(multiplier: Int = 1): Int {
        val totalGems = repository.claimAllCompletedMissions(dailyMissions.value) * multiplier
        if (multiplier > 1 && totalGems > 0) {
            // grant the extra multiplier gems
            val extraGems = totalGems - (totalGems / multiplier)
            repository.addGems(extraGems, CurrencySource.DAILY_MISSION)
        }
        if (totalGems > 0) {
            soundManager.playVictoryMusic()
            soundManager.playGemCollect()
            hapticManager.levelUp()
        }
        return totalGems
    }

    fun claimWeeklyMissionWithMultiplier(id: String, rewardGems: Int, rewardBlueGems: Int = 0, multiplier: Int = 1): Boolean {
        val claimed = repository.claimWeeklyMission(id, rewardGems * multiplier, rewardBlueGems * multiplier)
        if (claimed) {
            soundManager.playVictoryMusic()
            soundManager.playGemCollect()
            soundManager.playPerfectHit()
            hapticManager.missionClaim()
        }
        return claimed
    }

    fun claimDailyReward(multiplier: Int = 1): Int {
        val gemsAwarded = repository.claimDailyReward()
        val totalAwarded = gemsAwarded * multiplier
        if (multiplier > 1 && gemsAwarded > 0) {
            val extra = totalAwarded - gemsAwarded
            repository.addGems(extra, CurrencySource.DAILY_REWARD)
        }
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
        return totalAwarded
    }

    fun getStreakDayReward(day: Int): Int {
        return repository.getStreakDayReward(day)
    }

    fun openOutOfGemsOffer(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isOutOfGemsOfferOpen.value = open
    }

    fun openRealMoneyShop(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isRealMoneyShopOpen.value = open
    }

    fun openShop(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isShopOpen.value = open
    }

    fun setShopTab(tab: AccessoryType) {
        soundManager.playButton()
        hapticManager.uiClick()
        _selectedShopTab.value = tab
    }

    fun openPauseMenu(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isPauseMenuOpen.value = open
    }

    fun returnToMainMenu() {
        soundManager.playButton()
        hapticManager.uiClick()
        _isPauseMenuOpen.value = false
        _isShopOpen.value = false
        _isDailyRewardOpen.value = false
        _isDailyMissionsOpen.value = false
        _isHowToPlayOpen.value = false
        engine.resetGame(initial = true)
    }

    fun openHowToPlay(open: Boolean = true) {
        soundManager.playButton()
        hapticManager.uiClick()
        _isHowToPlayOpen.value = open
    }

    fun toggleSound() {
        val newState = repository.toggleSound()
        soundManager.soundEnabled = newState
        hapticManager.uiClick()
    }

    fun toggleHaptics() {
        val newState = repository.toggleHaptics()
        soundManager.hapticsEnabled = newState
        hapticManager.isEnabled = newState
        hapticManager.uiClick()
    }

    fun buyOrEquip(item: AccessoryItem): Boolean {
        val success = repository.buyAndEquip(item)
        if (success) {
            soundManager.playGemCollect()
            hapticManager.gemCollect()
        } else {
            soundManager.playButton()
            hapticManager.uiClick()
        }
        return success
    }

    fun buyItemRealMoney(item: AccessoryItem): Boolean {
        repository.unlockItem(item.id, item.type, 0)
        repository.equip(item)
        soundManager.playBuyGemsSuccess()
        soundManager.playVictoryMusic()
        hapticManager.levelUp()
        return true
    }

    fun isItemUnlocked(id: String): Boolean {
        return repository.isItemUnlocked(id)
    }

    fun getEquippedHat(): AccessoryItem {
        val id = selectedHatId.value
        return availableAccessories.firstOrNull { it.id == id }
            ?: availableAccessories.first { it.type == AccessoryType.HAT }
    }

    fun getEquippedScarf(): AccessoryItem {
        val id = selectedScarfId.value
        return availableAccessories.firstOrNull { it.id == id }
            ?: availableAccessories.first { it.type == AccessoryType.SCARF }
    }

    fun getEquippedStick(): AccessoryItem {
        val id = selectedStickId.value
        return availableAccessories.firstOrNull { it.id == id }
            ?: availableAccessories.first { it.type == AccessoryType.STICK }
    }

    fun getEquippedSkin(): AccessoryItem {
        val id = selectedSkinId.value
        return availableAccessories.firstOrNull { it.id == id }
            ?: availableAccessories.first { it.type == AccessoryType.BODY_SKIN }
    }

    fun getEquippedTheme(): AccessoryItem {
        val id = selectedThemeId.value
        return availableAccessories.firstOrNull { it.id == id }
            ?: availableAccessories.first { it.type == AccessoryType.THEME }
    }

    // Cloud Wallet & Firebase Auth
    val cloudSyncStatus = repository.cloudWalletService.syncStatus
    val firebaseUser = repository.cloudWalletService.currentUser
    val cloudAuthoritativeGems = repository.cloudWalletService.cloudGems

    fun signInWithGoogle(activity: Activity, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.cloudWalletService.signInWithGoogle(activity)
            if (result.isSuccess) {
                val user = result.getOrNull()
                // Synchronize local balance to remote Firestore on successful sign in
                repository.cloudWalletService.syncLocalBalanceToCloud(
                    localGems = repository.gems.value,
                    localRedGems = repository.redGems.value,
                    highScore = repository.highScore.value,
                    isCheater = false
                )
                soundManager.playVictoryMusic()
                hapticManager.levelUp()
                onComplete(true, user?.email ?: user?.displayName)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Google Sign-In failed"
                soundManager.playButton()
                onComplete(false, errorMsg)
            }
        }
    }

    fun signOutCloud() {
        viewModelScope.launch {
            repository.cloudWalletService.signOut()
            soundManager.playButton()
            hapticManager.uiClick()
        }
    }

    fun syncLocalProgressToCloud() {
        viewModelScope.launch {
            repository.cloudWalletService.syncLocalBalanceToCloud(
                localGems = repository.gems.value,
                localRedGems = repository.redGems.value,
                highScore = repository.highScore.value,
                isCheater = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
        soundManager.release()
    }
}

