package com.example.model

import androidx.compose.ui.graphics.Color

enum class CurrencyType(val label: String, val symbol: String, val colorHex: Long) {
    GEM("Gems", "💎", 0xFF38BDF8),
    BLUE_GEM("Blue Sapphire", "🔷", 0xFF60A5FA),
    RED_GEM("Ruby Crimson", "🔴", 0xFFF43F5E)
}

enum class AccessoryType {
    HAT,
    SCARF,
    STICK,
    BODY_SKIN,
    THEME,
    GEM_VAULT
}

enum class TournamentLeague(val title: String, val badgeEmoji: String, val colorHex: Long, val minScore: Int) {
    BRONZE("Bronze League", "🥉", 0xFFCD7F32, 0),
    SILVER("Silver League", "🥈", 0xFFCBD5E1, 10),
    GOLD("Gold League", "🥇", 0xFFFFD700, 25),
    DIAMOND("Diamond League", "💎", 0xFF38BDF8, 50),
    MASTER("Grandmaster League", "👑", 0xFFA855F7, 100)
}

data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val avatarEmoji: String,
    val countryFlag: String,
    val score: Int,
    val perfectHits: Int,
    val league: TournamentLeague = TournamentLeague.MASTER,
    val isCurrentUser: Boolean = false
)

data class WeeklyMissionItem(
    val id: String,
    val title: String,
    val description: String,
    val missionType: String,
    val targetCount: Int,
    val currentProgress: Int,
    val rewardGems: Int,
    val rewardBlueGems: Int = 0,
    val rewardRedGems: Int = 0,
    val iconEmoji: String = "⚡",
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val badgeLabel: String = "EPIC"
)

data class ContestTournament(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val bannerColorHex: Long,
    val timeRemainingStr: String,
    val participantsCount: String,
    val prizePoolGems: Int,
    val prizePoolBlueGems: Int = 0,
    val prizePoolRedGems: Int = 0,
    val targetGoal: Int,
    val currentProgress: Int,
    val goalUnit: String,
    val isJoined: Boolean = true,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val rewardPerk: String = ""
)

data class PlayerCareerStats(
    val totalGamesPlayed: Int,
    val highScore: Int,
    val totalBridgesBuilt: Int,
    val totalPerfectHits: Int,
    val bullseyeRatePercent: Int,
    val totalGemsHarvested: Int,
    val totalBlueGemsEarned: Int = 0,
    val totalRedGemsEarned: Int = 0,
    val currentStreakDays: Int,
    val league: TournamentLeague
)

data class GameSettingsState(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val leftHandedMode: Boolean = false,
    val highFrameRate: Boolean = true,
    val particleQualityUltra: Boolean = true,
    val screenShakeEnabled: Boolean = true,
    val floatingScoreFx: Boolean = true
)

data class LevelVictoryData(
    val levelNumber: Int,
    val nextLevelNumber: Int,
    val bonusGems: Int,
    val title: String,
    val milestoneReward: String = ""
)

enum class ChallengeDialogType {
    PRE_LEVEL_TAUNT,
    POST_LEVEL_VICTORY,
    POST_LEVEL_FAIL
}

data class ChallengeDialogData(
    val levelNumber: Int,
    val title: String,
    val message: String,
    val type: ChallengeDialogType = ChallengeDialogType.PRE_LEVEL_TAUNT,
    val awardedTitle: String? = null,
    val rewardGems: Int = 0,
    val buttonText: String = "ACCEPT CHALLENGE ⚔️"
)

data class LifeShopPack(
    val id: String,
    val livesCount: Int,
    val gemCost: Int = 0,
    val realMoneyPrice: String = "",
    val isAd: Boolean = false,
    val tag: String = "",
    val iconEmoji: String = "❤️"
)

data class GemPack(
    val id: String,
    val name: String,
    val gemAmount: Int,
    val bonusGems: Int = 0,
    val iconEmoji: String,
    val tag: String = "",
    val priceUsd: String = "",
    val scoreCost: Int = 0,
    val isDailyFree: Boolean = false,
    val perks: String = ""
)

data class RivalGhost(
    val name: String,
    val countryFlag: String,
    val avatarEmoji: String,
    val score: Int
)

data class NearMissInfo(
    val isNearMiss: Boolean,
    val pixelsDifference: Float,
    val isUnderShoot: Boolean,
    val message: String
)

enum class ItemRarity(val label: String, val colorHex: Long, val badgeBgHex: Long) {
    COMMON("COMMON", 0xFF94A3B8, 0xFF1E293B),
    RARE("RARE", 0xFF38BDF8, 0xFF0C4A6E),
    EPIC("EPIC", 0xFFA855F7, 0xFF581C87),
    LEGENDARY("LEGENDARY", 0xFFF59E0B, 0xFF78350F),
    MYTHIC("MYTHIC", 0xFFEC4899, 0xFF831843)
}

data class AccessoryItem(
    val id: String,
    val name: String,
    val type: AccessoryType,
    val cost: Int,
    val primaryColor: Long, // Color Long hex
    val secondaryColor: Long = 0xFFFFFFFF,
    val description: String = "",
    val iconSymbol: String = "",
    val rarity: ItemRarity = ItemRarity.COMMON,
    val currencyType: CurrencyType = CurrencyType.GEM,
    val isContestExclusive: Boolean = false,
    val realMoneyPriceUsd: String = "",
    val isRealMoneyExclusive: Boolean = false
)

enum class GameState {
    START,
    IDLE,
    GROWING,
    FALLING_BRIDGE,
    WALKING,
    COLLECTING_GEM,
    DROPPING_FAIL,
    SCROLLING,
    GAMEOVER,
    PAUSED
}

data class PlatformData(
    val id: Long,
    var leftX: Float,
    var width: Float,
    val hasRedDot: Boolean = true,
    var gem: GemData? = null
)

data class GemData(
    val id: Long,
    var x: Float,
    val isUnderBridge: Boolean, // if true, stickman flips under bridge to collect
    var collected: Boolean = false,
    var floatOffset: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val radius: Float,
    var alpha: Float = 1f,
    var life: Float = 1f,
    val maxLife: Float = 1f,
    val shape: ParticleShape = ParticleShape.CIRCLE,
    var rotation: Float = 0f,
    var vRot: Float = 0f
)

enum class ParticleShape {
    CIRCLE,
    STAR,
    CONFETTI,
    DUST,
    GEM_BURST,
    RING_WAVE,
    SPARKLE,
    FIRE_EMBER,
    NEON_ORB,
    BALLOON_POP,
    RIBBON
}

data class FloatingPopupText(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1f,
    var scale: Float = 1f,
    var lifeTime: Float = 1f
)

data class StageTheme(
    val stageNumber: Int,
    val name: String,
    val bgTopColor: Color,
    val bgBottomColor: Color,
    val platformColor: Color,
    val platformHighlightColor: Color,
    val mountainColor: Color,
    val celestialType: CelestialType,
    val ambientDescription: String
)

enum class CelestialType {
    MOON,
    SUN,
    NEON_PLANET,
    AURORA,
    SAKURA_BLOOM,
    DEEP_ABYSS,
    MATRIX_CASCADE,
    CELESTIAL_SHRINE,
    DRAGON_EMBER,
    CRYSTAL_PRISM
}
