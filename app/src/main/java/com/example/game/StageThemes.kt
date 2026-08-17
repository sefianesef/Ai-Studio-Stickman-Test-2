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
            celestialType = CelestialType.DRAGON_EMBER,
            ambientDescription = "Molten fire and ash peaks"
        ),
        StageTheme(
            stageNumber = 8,
            name = "Cherry Blossom Shrine",
            bgTopColor = Color(0xFF2A0845),
            bgBottomColor = Color(0xFF6441A5),
            platformColor = Color(0xFF1F112E),
            platformHighlightColor = Color(0xFFF472B6),
            mountainColor = Color(0xFF4A154B),
            celestialType = CelestialType.SAKURA_BLOOM,
            ambientDescription = "Floating sakura petals & mystic shrine"
        ),
        StageTheme(
            stageNumber = 9,
            name = "Atlantis Abyss",
            bgTopColor = Color(0xFF021B29),
            bgBottomColor = Color(0xFF0369A1),
            platformColor = Color(0xFF0B192C),
            platformHighlightColor = Color(0xFF06B6D4),
            mountainColor = Color(0xFF083344),
            celestialType = CelestialType.DEEP_ABYSS,
            ambientDescription = "Bioluminescent coral deeps"
        ),
        StageTheme(
            stageNumber = 10,
            name = "Matrix Digital Rain",
            bgTopColor = Color(0xFF021408),
            bgBottomColor = Color(0xFF064E3B),
            platformColor = Color(0xFF051C0C),
            platformHighlightColor = Color(0xFF10B981),
            mountainColor = Color(0xFF022C12),
            celestialType = CelestialType.MATRIX_CASCADE,
            ambientDescription = "Exclusive Sapphire Contest Digital Grid"
        ),
        StageTheme(
            stageNumber = 11,
            name = "Celestial Moon Palace",
            bgTopColor = Color(0xFF0F172A),
            bgBottomColor = Color(0xFF312E81),
            platformColor = Color(0xFF1E1B4B),
            platformHighlightColor = Color(0xFF60A5FA),
            mountainColor = Color(0xFF1E293B),
            celestialType = CelestialType.CELESTIAL_SHRINE,
            ambientDescription = "Exclusive Sapphire High Starlight Realm"
        ),
        StageTheme(
            stageNumber = 12,
            name = "Dragon Sovereign Domain",
            bgTopColor = Color(0xFF3B0000),
            bgBottomColor = Color(0xFF7F1D1D),
            platformColor = Color(0xFF1F0808),
            platformHighlightColor = Color(0xFFF43F5E),
            mountainColor = Color(0xFF450A0A),
            celestialType = CelestialType.DRAGON_EMBER,
            ambientDescription = "Exclusive Ruby Tournament Obsidian Volcano"
        ),
        StageTheme(
            stageNumber = 13,
            name = "Mythic Grandmaster Crystal",
            bgTopColor = Color(0xFF18032E),
            bgBottomColor = Color(0xFF6B21A8),
            platformColor = Color(0xFF240E3E),
            platformHighlightColor = Color(0xFFF472B6),
            mountainColor = Color(0xFF3B0764),
            celestialType = CelestialType.CRYSTAL_PRISM,
            ambientDescription = "Exclusive Grandmaster Prismatic Aurora"
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
            "theme_sakura" -> stages[7]
            "theme_abyss" -> stages[8]
            "theme_matrix" -> stages[9]
            "theme_moon_palace" -> stages[10]
            "theme_dragon" -> stages[11]
            "theme_grandmaster" -> stages[12]
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
