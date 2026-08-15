package com.example.model

import androidx.compose.ui.graphics.Color

enum class AccessoryType {
    HAT,
    SCARF,
    STICK,
    BODY_SKIN
}

enum class ItemRarity(val label: String, val colorHex: Long, val badgeBgHex: Long) {
    COMMON("COMMON", 0xFF94A3B8, 0xFF1E293B),
    RARE("RARE", 0xFF38BDF8, 0xFF0C4A6E),
    EPIC("EPIC", 0xFFA855F7, 0xFF581C87),
    LEGENDARY("LEGENDARY", 0xFFF59E0B, 0xFF78350F)
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
    val rarity: ItemRarity = ItemRarity.COMMON
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
    val shape: ParticleShape = ParticleShape.CIRCLE
)

enum class ParticleShape {
    CIRCLE,
    STAR,
    CONFETTI,
    DUST,
    GEM_BURST,
    RING_WAVE
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
    AURORA
}
