package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.model.CelestialType
import com.example.model.StageTheme

object StageThemes {
    val stages = listOf(
        StageTheme(
            stageNumber = 1,
            name = "Emerald Twilight",
            bgTopColor = Color(0xFF06140E),
            bgBottomColor = Color(0xFF1B4332),
            platformColor = Color(0xFF0F172A),
            platformHighlightColor = Color(0xFF10B981),
            mountainColor = Color(0xFF0D2818),
            celestialType = CelestialType.MOON,
            ambientDescription = "Whispering emerald pines"
        ),
        StageTheme(
            stageNumber = 2,
            name = "Sunset Canyon",
            bgTopColor = Color(0xFF2E1065),
            bgBottomColor = Color(0xFFBE185D),
            platformColor = Color(0xFF1E1B4B),
            platformHighlightColor = Color(0xFFF59E0B),
            mountainColor = Color(0xFF581C87),
            celestialType = CelestialType.SUN,
            ambientDescription = "Golden evening desert glow"
        ),
        StageTheme(
            stageNumber = 3,
            name = "Neon Cyber City",
            bgTopColor = Color(0xFF020617),
            bgBottomColor = Color(0xFF3B0764),
            platformColor = Color(0xFF090D16),
            platformHighlightColor = Color(0xFF22D3EE),
            mountainColor = Color(0xFF1E1B4B),
            celestialType = CelestialType.NEON_PLANET,
            ambientDescription = "Synthwave neon towers"
        ),
        StageTheme(
            stageNumber = 4,
            name = "Arctic Aurora",
            bgTopColor = Color(0xFF022C22),
            bgBottomColor = Color(0xFF064E3B),
            platformColor = Color(0xFF022C22),
            platformHighlightColor = Color(0xFF38BDF8),
            mountainColor = Color(0xFF064E3B),
            celestialType = CelestialType.AURORA,
            ambientDescription = "Dancing polar lights"
        ),
        StageTheme(
            stageNumber = 5,
            name = "Golden Dawn",
            bgTopColor = Color(0xFF451A03),
            bgBottomColor = Color(0xFFB45309),
            platformColor = Color(0xFF292524),
            platformHighlightColor = Color(0xFFFBBF24),
            mountainColor = Color(0xFF78350F),
            celestialType = CelestialType.SUN,
            ambientDescription = "Sunlit cloud summits"
        ),
        StageTheme(
            stageNumber = 6,
            name = "Cosmic Nebula",
            bgTopColor = Color(0xFF0A0017),
            bgBottomColor = Color(0xFF3B0764),
            platformColor = Color(0xFF130924),
            platformHighlightColor = Color(0xFFA855F7),
            mountainColor = Color(0xFF2E1065),
            celestialType = CelestialType.NEON_PLANET,
            ambientDescription = "Deep galactic starry void"
        ),
        StageTheme(
            stageNumber = 7,
            name = "Volcanic Magma",
            bgTopColor = Color(0xFF1C0505),
            bgBottomColor = Color(0xFF7F1D1D),
            platformColor = Color(0xFF180A0A),
            platformHighlightColor = Color(0xFFEF4444),
            mountainColor = Color(0xFF450A0A),
            celestialType = CelestialType.SUN,
            ambientDescription = "Molten fire and ash peaks"
        )
    )

    fun getThemeById(themeId: String?): StageTheme? {
        return when (themeId) {
            "theme_emerald" -> stages[0]
            "theme_sunset" -> stages[1]
            "theme_cyber" -> stages[2]
            "theme_aurora" -> stages[3]
            "theme_golden" -> stages[4]
            "theme_cosmic" -> stages[5]
            "theme_volcano" -> stages[6]
            else -> null
        }
    }

    fun getThemeForScore(score: Int, equippedThemeId: String? = null): StageTheme {
        if (!equippedThemeId.isNullOrEmpty()) {
            val custom = getThemeById(equippedThemeId)
            if (custom != null) return custom
        }
        val stageIndex = (score / 5) % stages.size
        return stages[stageIndex]
    }
}
