package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.data.GameRepository
import com.example.data.local.entity.DailyMissionEntity
import com.example.game.StickmanGameEngine
import com.example.model.AccessoryItem
import com.example.model.AccessoryType
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

    // Shop tab
    private val _selectedShopTab = MutableStateFlow(AccessoryType.HAT)
    val selectedShopTab: StateFlow<AccessoryType> = _selectedShopTab.asStateFlow()

    // Pass through repository states
    val gems: StateFlow<Int> = repository.gems
    val blueGems: StateFlow<Int> = repository.blueGems
    val redGems: StateFlow<Int> = repository.redGems
    val highScore: StateFlow<Int> = repository.highScore
    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabled
    val leftHandedMode: StateFlow<Boolean> = repository.leftHandedMode
    val highFrameRate: StateFlow<Boolean> = repository.highFrameRate
    val particleQualityUltra: StateFlow<Boolean> = repository.particleQualityUltra
    val screenShakeEnabled: StateFlow<Boolean> = repository.screenShakeEnabled

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

    fun spinLuckyWheel(): Int {
        val reward = repository.spinLuckyWheel()
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
        return reward
    }

    fun buyGemPack(pack: com.example.model.GemPack): Boolean {
        val success = repository.buyGemPackWithTokens(pack)
        if (success) {
            soundManager.playBuyGemsSuccess()
            soundManager.playGemCollect()
            hapticManager.missionClaim()
        }
        return success
    }

    fun buyGemPackRealMoney(pack: com.example.model.GemPack): Boolean {
        val totalAwarded = pack.gemAmount + pack.bonusGems
        repository.addGems(totalAwarded)
        soundManager.playBuyGemsSuccess()
        soundManager.playVictoryMusic()
        hapticManager.levelUp()
        return true
    }

    val activeLevelVictory: StateFlow<com.example.model.LevelVictoryData?> = engine.activeLevelVictory

    fun dismissLevelVictory() {
        engine.dismissLevelVictory()
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

    fun claimDailyMission(missionId: String, rewardGems: Int) {
        repository.claimMissionReward(missionId, rewardGems)
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
    }

    fun claimAllDailyMissions(): Int {
        val totalGems = repository.claimAllCompletedMissions(dailyMissions.value)
        if (totalGems > 0) {
            soundManager.playVictoryMusic()
            soundManager.playGemCollect()
            hapticManager.levelUp()
        }
        return totalGems
    }

    fun claimDailyReward(): Int {
        val gemsAwarded = repository.claimDailyReward()
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
        return gemsAwarded
    }

    fun getStreakDayReward(day: Int): Int {
        return repository.getStreakDayReward(day)
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

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}

