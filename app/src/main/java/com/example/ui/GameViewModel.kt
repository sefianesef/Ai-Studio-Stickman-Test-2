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

    private val _isLeaderboardOpen = MutableStateFlow(false)
    val isLeaderboardOpen: StateFlow<Boolean> = _isLeaderboardOpen.asStateFlow()

    private val _isSpinWheelOpen = MutableStateFlow(false)
    val isSpinWheelOpen: StateFlow<Boolean> = _isSpinWheelOpen.asStateFlow()

    // Shop tab
    private val _selectedShopTab = MutableStateFlow(AccessoryType.HAT)
    val selectedShopTab: StateFlow<AccessoryType> = _selectedShopTab.asStateFlow()

    // Pass through repository states
    val gems: StateFlow<Int> = repository.gems
    val highScore: StateFlow<Int> = repository.highScore
    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabled

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
            soundManager.playGemCollect()
            soundManager.playPerfectHit()
            hapticManager.missionClaim()
        }
        return success
    }

    fun revivePlayer(): Boolean {
        // Costs 3 gems, or free if player has at least 3 gems or for second chance
        val cost = 3
        if (repository.spendGems(cost)) {
            engine.reviveRun()
            return true
        } else if (repository.gems.value == 0) {
            // Free emergency safety revival!
            engine.reviveRun()
            return true
        }
        return false
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

    fun claimDailyMission(missionId: String, rewardGems: Int) {
        repository.claimMissionReward(missionId, rewardGems)
        soundManager.playGemCollect()
        soundManager.playPerfectHit()
        hapticManager.missionClaim()
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

