package com.mygames.stickmanrush.data

import android.content.Context
import android.content.SharedPreferences
import com.mygames.stickmanrush.data.local.AppDatabase
import com.mygames.stickmanrush.data.local.entity.DailyMissionEntity
import com.mygames.stickmanrush.data.local.entity.PlayerProfileEntity
import com.mygames.stickmanrush.data.local.entity.PurchasedItemEntity
import com.mygames.stickmanrush.model.AccessoryItem
import com.mygames.stickmanrush.model.AccessoryType
import com.mygames.stickmanrush.model.ContestTournament
import com.mygames.stickmanrush.model.GameSettingsState
import com.mygames.stickmanrush.model.GemPack
import com.mygames.stickmanrush.model.ItemRarity
import com.mygames.stickmanrush.model.LeaderboardEntry
import com.mygames.stickmanrush.model.PlayerCareerStats
import com.mygames.stickmanrush.model.RivalGhost
import com.mygames.stickmanrush.model.TournamentLeague
import com.mygames.stickmanrush.model.WeeklyMissionItem
import com.mygames.stickmanrush.security.AdServerSideVerificationManager
import com.mygames.stickmanrush.security.CloudBackendCurrencyAuthority
import com.mygames.stickmanrush.security.CloudSyncStatus
import com.mygames.stickmanrush.security.CurrencySource
import com.mygames.stickmanrush.security.CurrencyTransactionRequest
import com.mygames.stickmanrush.security.EncryptedSaveStorage
import com.mygames.stickmanrush.security.FirebaseCloudWalletService
import com.mygames.stickmanrush.security.IServerCurrencyAuthority
import com.mygames.stickmanrush.security.SecureCurrencyVault
import com.mygames.stickmanrush.security.SecureTimeAuthority
import com.mygames.stickmanrush.security.ServerVerificationResult
import com.mygames.stickmanrush.security.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameRepository(
    context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    val serverAuthority: IServerCurrencyAuthority = CloudBackendCurrencyAuthority(context),
    val cloudWalletService: FirebaseCloudWalletService = FirebaseCloudWalletService(context)
) : CurrencyRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val playerProfileDao = database.playerProfileDao()
    private val inventoryDao = database.inventoryDao()
    private val dailyMissionDao = database.dailyMissionDao()

    private val prefs: SharedPreferences =
        EncryptedSaveStorage.createEncryptedSharedPreferences(context).also { securePrefs ->
            EncryptedSaveStorage.migrateFromLegacySharedPreferences(context, securePrefs = securePrefs)
        }

    val timeAuthority = SecureTimeAuthority(context)
    val adSsvManager = AdServerSideVerificationManager(context)
    val encryptedStorage = EncryptedSaveStorage(context)

    companion object {
        private const val KEY_GEMS = "GEMS"
        private const val KEY_BLUE_GEMS = "BLUE_GEMS"
        private const val KEY_RED_GEMS = "RED_GEMS"
        private const val KEY_HIGH_SCORE = "HIGH_SCORE"
        private const val KEY_TOTAL_BRIDGES = "TOTAL_BRIDGES"
        private const val KEY_PERFECT_HITS = "PERFECT_HITS"
        private const val KEY_SOUND_ENABLED = "SOUND_ENABLED"
        private const val KEY_HAPTICS_ENABLED = "HAPTICS_ENABLED"
        private const val KEY_SELECTED_HAT = "SELECTED_HAT"
        private const val KEY_SELECTED_SCARF = "SELECTED_SCARF"
        private const val KEY_SELECTED_STICK = "SELECTED_STICK"
        private const val KEY_SELECTED_SKIN = "SELECTED_SKIN"
        private const val KEY_SELECTED_THEME = "SELECTED_THEME"
        private const val KEY_UNLOCKED_PREFIX = "UNLOCKED_"
        private const val KEY_LAST_CLAIM_DAY = "LAST_CLAIM_DAY"
        private const val KEY_CURRENT_STREAK = "CURRENT_STREAK"
        private const val KEY_LEFT_HANDED = "LEFT_HANDED"
        private const val KEY_HIGH_FPS = "HIGH_FPS"
        private const val KEY_PARTICLES_ULTRA = "PARTICLES_ULTRA"
        private const val KEY_SCREEN_SHAKE = "SCREEN_SHAKE"
        private const val KEY_TOTAL_GAMES = "TOTAL_GAMES"
        private const val KEY_TOTAL_GEMS_HARVESTED = "TOTAL_GEMS_HARVESTED"
        private const val KEY_TOTAL_BLUE_GEMS_EARNED = "TOTAL_BLUE_GEMS_EARNED"
        private const val KEY_TOTAL_RED_GEMS_EARNED = "TOTAL_RED_GEMS_EARNED"
        private const val KEY_WEEKLY_PREFIX = "WEEKLY_PROG_"
        private const val KEY_WEEKLY_CLAIM_PREFIX = "WEEKLY_CLAIM_"
        private const val KEY_CONTEST_PREFIX = "CONTEST_PROG_"
        private const val KEY_CONTEST_CLAIM_PREFIX = "CONTEST_CLAIM_"
        private const val KEY_SAVED_LEVEL = "SAVED_LEVEL"
        private const val KEY_HIGHEST_UNLOCKED_LEVEL = "HIGHEST_UNLOCKED_LEVEL"
        private const val KEY_LIVES_COUNT = "LIVES_COUNT"
        private const val KEY_LAST_LIFE_REGEN_TIME = "LAST_LIFE_REGEN_TIME"
        private const val KEY_CURRENCY_INTEGRITY_SIGNATURE = "CURRENCY_INTEGRITY_SIG"
        private const val KEY_PLAYER_ID = "PLAYER_UUID"

        const val MAX_LIVES = 5
        const val LIFE_REGEN_INTERVAL_MS = 20 * 60 * 1000L // 20 minutes in ms

        val DAILY_REWARD_AMOUNTS = listOf(3, 5, 8, 10, 15, 20, 35)
    }

    // Default catalog of shop items
    val availableAccessories: List<AccessoryItem> = listOf(
        // Hats (Standard Gems & Exclusive Contest Gems)
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
            cost = 80,
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
            cost = 120,
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
            cost = 95,
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
            cost = 150,
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
            cost = 220,
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
            cost = 180,
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
            cost = 135,
            primaryColor = 0xFF1E293B,
            secondaryColor = 0xFFFBBF24,
            description = "High seas buccaneer cap",
            iconSymbol = "🏴‍☠️",
            rarity = ItemRarity.EPIC
        ),
        // Special Contest Blue Gem & Red Gem Hats
        AccessoryItem(
            id = "hat_sapphire_crown",
            name = "Sapphire Diadem",
            type = AccessoryType.HAT,
            cost = 25,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM,
            primaryColor = 0xFF38BDF8,
            secondaryColor = 0xFF1D4ED8,
            description = "Contest Exclusive: Frost sapphire jewel",
            iconSymbol = "👑",
            rarity = ItemRarity.EPIC,
            isContestExclusive = true
        ),
        AccessoryItem(
            id = "hat_dragon_horns",
            name = "Crimson Dragon Horns",
            type = AccessoryType.HAT,
            cost = 20,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.RED_GEM,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFF7F1D1D,
            description = "Tournament Exclusive: Fiery draconic crest",
            iconSymbol = "🐲",
            rarity = ItemRarity.MYTHIC,
            isContestExclusive = true
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
            cost = 70,
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
            cost = 110,
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
            cost = 175,
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
            cost = 240,
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
            cost = 200,
            primaryColor = 0xFFF97316,
            secondaryColor = 0xFFDC2626,
            description = "Blazing solar ember plumage",
            iconSymbol = "🔥",
            rarity = ItemRarity.LEGENDARY
        ),
        // Special Contest Scarves
        AccessoryItem(
            id = "scarf_glacial",
            name = "Glacial Comet Cape",
            type = AccessoryType.SCARF,
            cost = 28,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM,
            primaryColor = 0xFF67E8F9,
            secondaryColor = 0xFF0284C7,
            description = "Contest Exclusive: Subzero crystalline comet tail",
            iconSymbol = "☄️",
            rarity = ItemRarity.EPIC,
            isContestExclusive = true
        ),
        AccessoryItem(
            id = "scarf_bloodfire",
            name = "Bloodfire Wings",
            type = AccessoryType.SCARF,
            cost = 25,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.RED_GEM,
            primaryColor = 0xFFF43F5E,
            secondaryColor = 0xFF881337,
            description = "Tournament Exclusive: Prismatic molten ember drape",
            iconSymbol = "🪽",
            rarity = ItemRarity.MYTHIC,
            isContestExclusive = true
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
            cost = 85,
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
            cost = 140,
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
            cost = 115,
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
            cost = 190,
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
            cost = 160,
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
            cost = 230,
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
            cost = 175,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF059669,
            description = "Cybernetic emerald data stream",
            iconSymbol = "🟢",
            rarity = ItemRarity.EPIC
        ),
        // Special Contest Sticks
        AccessoryItem(
            id = "stick_cryo",
            name = "Sapphire Cryo Beam",
            type = AccessoryType.STICK,
            cost = 26,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM,
            primaryColor = 0xFF38BDF8,
            secondaryColor = 0xFF1E3A8A,
            description = "Contest Exclusive: Pulsing freezing ice girder",
            iconSymbol = "❄️",
            rarity = ItemRarity.EPIC,
            isContestExclusive = true,
            realMoneyPriceUsd = "$2.99"
        ),
        AccessoryItem(
            id = "stick_dark_matter",
            name = "Dark Matter Scepter",
            type = AccessoryType.STICK,
            cost = 24,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.RED_GEM,
            primaryColor = 0xFFF43F5E,
            secondaryColor = 0xFF4C0519,
            description = "Tournament Exclusive: Supercharged ruby particle bridge",
            iconSymbol = "⚡",
            rarity = ItemRarity.MYTHIC,
            isContestExclusive = true,
            realMoneyPriceUsd = "$4.99"
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
            cost = 85,
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
            cost = 135,
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
            cost = 180,
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
            cost = 120,
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
            cost = 100,
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
            cost = 160,
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
            cost = 250,
            primaryColor = 0xFFC084FC,
            secondaryColor = 0xFF6366F1,
            description = "Astral stardust deep space traveler",
            iconSymbol = "✨",
            rarity = ItemRarity.LEGENDARY
        ),
        // Special Contest Skins
        AccessoryItem(
            id = "skin_diamond_phantom",
            name = "Diamond Phantom",
            type = AccessoryType.BODY_SKIN,
            cost = 35,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM,
            primaryColor = 0xFF67E8F9,
            secondaryColor = 0xFF1E40AF,
            description = "Contest Exclusive: Translucent diamond ninja",
            iconSymbol = "💎",
            rarity = ItemRarity.EPIC,
            isContestExclusive = true
        ),
        AccessoryItem(
            id = "skin_infernal_ninja",
            name = "Infernal Sovereign",
            type = AccessoryType.BODY_SKIN,
            cost = 30,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.RED_GEM,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFF450A0A,
            description = "Tournament Exclusive: Blazing crimson overlord",
            iconSymbol = "🥷",
            rarity = ItemRarity.MYTHIC,
            isContestExclusive = true
        ),

        // Stage & Realm Themes / Backgrounds
        AccessoryItem(
            id = "theme_emerald",
            name = "Emerald Twilight",
            type = AccessoryType.THEME,
            cost = 0,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF1B4332,
            description = "Mystic whispering pines & moonlit fog",
            iconSymbol = "🌲",
            rarity = ItemRarity.COMMON
        ),
        AccessoryItem(
            id = "theme_sunset",
            name = "Sunset Canyon",
            type = AccessoryType.THEME,
            cost = 120,
            primaryColor = 0xFFF59E0B,
            secondaryColor = 0xFFBE185D,
            description = "Warm golden desert cliffs under purple sky",
            iconSymbol = "🏜️",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "theme_cyber",
            name = "Neon Cyber City",
            type = AccessoryType.THEME,
            cost = 180,
            primaryColor = 0xFF22D3EE,
            secondaryColor = 0xFF3B0764,
            description = "Retro synthwave skyscrapers & laser stars",
            iconSymbol = "🌆",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "theme_aurora",
            name = "Arctic Aurora",
            type = AccessoryType.THEME,
            cost = 160,
            primaryColor = 0xFF38BDF8,
            secondaryColor = 0xFF064E3B,
            description = "Glacial peaks under shimmering polar lights",
            iconSymbol = "❄️",
            rarity = ItemRarity.RARE
        ),
        AccessoryItem(
            id = "theme_golden",
            name = "Golden Dawn",
            type = AccessoryType.THEME,
            cost = 200,
            primaryColor = 0xFFFBBF24,
            secondaryColor = 0xFFB45309,
            description = "Majestic sunlit mountain summits",
            iconSymbol = "🌅",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "theme_cosmic",
            name = "Cosmic Nebula",
            type = AccessoryType.THEME,
            cost = 260,
            primaryColor = 0xFFA855F7,
            secondaryColor = 0xFF3B0764,
            description = "Deep galactic starry void and neon planets",
            iconSymbol = "🪐",
            rarity = ItemRarity.LEGENDARY
        ),
        AccessoryItem(
            id = "theme_volcano",
            name = "Volcanic Magma",
            type = AccessoryType.THEME,
            cost = 220,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFF7F1D1D,
            description = "Molten lava rivers and volcanic ash skies",
            iconSymbol = "🌋",
            rarity = ItemRarity.LEGENDARY
        ),
        AccessoryItem(
            id = "theme_sakura",
            name = "Cherry Blossom Shrine",
            type = AccessoryType.THEME,
            cost = 280,
            primaryColor = 0xFFF472B6,
            secondaryColor = 0xFF6441A5,
            description = "Floating sakura petals & mystic Japanese temple",
            iconSymbol = "🌸",
            rarity = ItemRarity.EPIC
        ),
        AccessoryItem(
            id = "theme_abyss",
            name = "Atlantis Abyss",
            type = AccessoryType.THEME,
            cost = 320,
            primaryColor = 0xFF06B6D4,
            secondaryColor = 0xFF021B29,
            description = "Bioluminescent coral trench & floating orbs",
            iconSymbol = "🌊",
            rarity = ItemRarity.LEGENDARY
        ),
        // Special Contest & Tournament Exclusive Backgrounds (Blue & Red Gems)
        AccessoryItem(
            id = "theme_matrix",
            name = "Cyber Matrix Grid",
            type = AccessoryType.THEME,
            cost = 35,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF021408,
            description = "Contest Exclusive: Neon digital rain cascades & cyber girders",
            iconSymbol = "💻",
            rarity = ItemRarity.EPIC,
            isContestExclusive = true
        ),
        AccessoryItem(
            id = "theme_moon_palace",
            name = "Celestial Moon Palace",
            type = AccessoryType.THEME,
            cost = 45,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM,
            primaryColor = 0xFF60A5FA,
            secondaryColor = 0xFF0F172A,
            description = "Contest Exclusive: High ethereal shrines floating above starlight",
            iconSymbol = "🌙",
            rarity = ItemRarity.LEGENDARY,
            isContestExclusive = true
        ),
        AccessoryItem(
            id = "theme_dragon",
            name = "Dragon Sovereign Domain",
            type = AccessoryType.THEME,
            cost = 30,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.RED_GEM,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFF3B0000,
            description = "Tournament Exclusive: Ancient obsidian towers & blood dragon moon",
            iconSymbol = "🐉",
            rarity = ItemRarity.MYTHIC,
            isContestExclusive = true
        ),
        AccessoryItem(
            id = "theme_grandmaster",
            name = "Mythic Grandmaster Crystal",
            type = AccessoryType.THEME,
            cost = 40,
            currencyType = com.mygames.stickmanrush.model.CurrencyType.RED_GEM,
            primaryColor = 0xFFF472B6,
            secondaryColor = 0xFF18032E,
            description = "Tournament Exclusive: Divine prismatic crystalline auroras",
            iconSymbol = "👑",
            rarity = ItemRarity.MYTHIC,
            isContestExclusive = true
        )
    )

    private val currencyVault = SecureCurrencyVault(context)

    private val _gems = MutableStateFlow(prefs.getInt(KEY_GEMS, 10))
    override val gems: StateFlow<Int> = _gems.asStateFlow()

    private val _blueGems = MutableStateFlow(prefs.getInt(KEY_BLUE_GEMS, 6))
    override val blueGems: StateFlow<Int> = _blueGems.asStateFlow()

    private val _redGems = MutableStateFlow(prefs.getInt(KEY_RED_GEMS, 3))
    override val redGems: StateFlow<Int> = _redGems.asStateFlow()

    private val _pendingTransactions = MutableStateFlow<List<PendingCurrencyTransaction>>(emptyList())
    override val pendingTransactions: StateFlow<List<PendingCurrencyTransaction>> = _pendingTransactions.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.getInt(KEY_HIGH_SCORE, 0))
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // 🚩 Checkpoint & Level Progression
    private val _savedLevel = MutableStateFlow(prefs.getInt(KEY_SAVED_LEVEL, 1))
    val savedLevel: StateFlow<Int> = _savedLevel.asStateFlow()

    private val _highestUnlockedLevel = MutableStateFlow(prefs.getInt(KEY_HIGHEST_UNLOCKED_LEVEL, 1))
    val highestUnlockedLevel: StateFlow<Int> = _highestUnlockedLevel.asStateFlow()

    // ❤️ 20-Minute Regenerating Lives System (Max 5 lives)
    private val _lives = MutableStateFlow(prefs.getInt(KEY_LIVES_COUNT, MAX_LIVES))
    val lives: StateFlow<Int> = _lives.asStateFlow()

    private val _lastLifeRegenTime = MutableStateFlow(prefs.getLong(KEY_LAST_LIFE_REGEN_TIME, System.currentTimeMillis()))
    val lastLifeRegenTime: StateFlow<Long> = _lastLifeRegenTime.asStateFlow()

    private val _secondsUntilNextLife = MutableStateFlow(0L)
    val secondsUntilNextLife: StateFlow<Long> = _secondsUntilNextLife.asStateFlow()

    private val _selectedHat = MutableStateFlow(prefs.getString(KEY_SELECTED_HAT, "hat_none") ?: "hat_none")
    val selectedHat: StateFlow<String> = _selectedHat.asStateFlow()

    private val _selectedScarf = MutableStateFlow(prefs.getString(KEY_SELECTED_SCARF, "scarf_gold") ?: "scarf_gold")
    val selectedScarf: StateFlow<String> = _selectedScarf.asStateFlow()

    private val _selectedStick = MutableStateFlow(prefs.getString(KEY_SELECTED_STICK, "stick_wood") ?: "stick_wood")
    val selectedStick: StateFlow<String> = _selectedStick.asStateFlow()

    private val _selectedSkin = MutableStateFlow(prefs.getString(KEY_SELECTED_SKIN, "skin_white") ?: "skin_white")
    val selectedSkin: StateFlow<String> = _selectedSkin.asStateFlow()

    private val _selectedTheme = MutableStateFlow(prefs.getString(KEY_SELECTED_THEME, "theme_emerald") ?: "theme_emerald")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _leftHandedMode = MutableStateFlow(prefs.getBoolean(KEY_LEFT_HANDED, false))
    val leftHandedMode: StateFlow<Boolean> = _leftHandedMode.asStateFlow()

    private val _highFrameRate = MutableStateFlow(prefs.getBoolean(KEY_HIGH_FPS, true))
    val highFrameRate: StateFlow<Boolean> = _highFrameRate.asStateFlow()

    private val _particleQualityUltra = MutableStateFlow(prefs.getBoolean(KEY_PARTICLES_ULTRA, true))
    val particleQualityUltra: StateFlow<Boolean> = _particleQualityUltra.asStateFlow()

    private val _screenShakeEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_SHAKE, true))
    val screenShakeEnabled: StateFlow<Boolean> = _screenShakeEnabled.asStateFlow()

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
        val rawGems = prefs.getInt(KEY_GEMS, 10)
        val rawBlueGems = prefs.getInt(KEY_BLUE_GEMS, 6)
        val rawRedGems = prefs.getInt(KEY_RED_GEMS, 3)
        val rawHighScore = prefs.getInt(KEY_HIGH_SCORE, 0)
        val rawTotalBridges = prefs.getInt(KEY_TOTAL_BRIDGES, 0)
        val rawStreak = prefs.getInt(KEY_CURRENT_STREAK, 1)
        val rawLastClaimDay = prefs.getLong(KEY_LAST_CLAIM_DAY, 0L)
        val storedSignature = prefs.getString(KEY_CURRENCY_INTEGRITY_SIGNATURE, "") ?: ""

        val isDataValid = currencyVault.verifyIntegritySignature(
            rawGems, rawBlueGems, rawRedGems, rawHighScore, rawTotalBridges, rawStreak, rawLastClaimDay, storedSignature
        )

        val initialGems = if (isDataValid) rawGems else 10
        val initialBlue = if (isDataValid) rawBlueGems else 6
        val initialRed = if (isDataValid) rawRedGems else 3

        _gems.value = initialGems
        _blueGems.value = initialBlue
        _redGems.value = initialRed

        currencyVault.syncFromDisk(initialGems, initialBlue, initialRed)
        saveIntegritySignature()

        // Unlock all free items in memory and Room
        val freeItems = availableAccessories.filter { it.cost == 0 }
        freeItems.forEach {
            if (!isItemUnlocked(it.id)) {
                unlockItem(it.id)
            }
        }

        // Initialize / sync Room database in background
        scope.launch {
            try {
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
            } catch (_: Throwable) {}
        }

        // Realtime Firestore Cloud Wallet Sync Listener
        scope.launch {
            cloudWalletService.cloudGems.collect { remoteGems ->
                if (remoteGems != null) {
                    _gems.value = remoteGems
                    currencyVault.syncFromDisk(remoteGems, _blueGems.value, _redGems.value)
                    prefs.edit().putInt(KEY_GEMS, remoteGems).apply()
                    saveIntegritySignature()
                    playerProfileDao.updateGems(remoteGems)
                }
            }
        }

        scope.launch {
            cloudWalletService.cloudRedGems.collect { remoteRed ->
                if (remoteRed != null) {
                    _redGems.value = remoteRed
                    currencyVault.syncFromDisk(_gems.value, _blueGems.value, remoteRed)
                    prefs.edit().putInt(KEY_RED_GEMS, remoteRed).apply()
                    saveIntegritySignature()
                }
            }
        }

        refreshDailyRewardState()
        recalculateRegeneratingLives()
        startLifeRegenTimer()
    }

    /**
     * Recalculates lives based on elapsed timestamps (1 life per 20 minutes up to MAX_LIVES = 5).
     */
    fun recalculateRegeneratingLives() {
        val currentLives = _lives.value
        val now = timeAuthority.getCurrentTimeMs()
        if (currentLives >= MAX_LIVES) {
            _lastLifeRegenTime.value = now
            _secondsUntilNextLife.value = 0L
            prefs.edit().putLong(KEY_LAST_LIFE_REGEN_TIME, now).apply()
            return
        }

        val lastRegen = prefs.getLong(KEY_LAST_LIFE_REGEN_TIME, now)
        val elapsedMs = (now - lastRegen).coerceAtLeast(0L)
        val livesToAdd = (elapsedMs / LIFE_REGEN_INTERVAL_MS).toInt()

        if (livesToAdd > 0) {
            val newLives = (currentLives + livesToAdd).coerceAtMost(MAX_LIVES)
            _lives.value = newLives
            val remainingMs = elapsedMs % LIFE_REGEN_INTERVAL_MS
            val updatedLastRegen = if (newLives >= MAX_LIVES) now else now - remainingMs
            _lastLifeRegenTime.value = updatedLastRegen
            prefs.edit()
                .putInt(KEY_LIVES_COUNT, newLives)
                .putLong(KEY_LAST_LIFE_REGEN_TIME, updatedLastRegen)
                .apply()
        }

        if (_lives.value < MAX_LIVES) {
            val updatedLast = _lastLifeRegenTime.value
            val timePassedSinceLast = (now - updatedLast).coerceAtLeast(0L)
            val timeRemainingMs = (LIFE_REGEN_INTERVAL_MS - timePassedSinceLast).coerceAtLeast(0L)
            _secondsUntilNextLife.value = timeRemainingMs / 1000L
        } else {
            _secondsUntilNextLife.value = 0L
        }
    }

    private fun startLifeRegenTimer() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val currentLives = _lives.value
                if (currentLives < MAX_LIVES) {
                    val now = timeAuthority.getCurrentTimeMs()
                    val lastRegen = _lastLifeRegenTime.value
                    val elapsedMs = (now - lastRegen).coerceAtLeast(0L)
                    if (elapsedMs >= LIFE_REGEN_INTERVAL_MS) {
                        // Grant 1 regenerated life
                        recalculateRegeneratingLives()
                    } else {
                        val remainingMs = (LIFE_REGEN_INTERVAL_MS - elapsedMs).coerceAtLeast(0L)
                        _secondsUntilNextLife.value = remainingMs / 1000L
                    }
                } else {
                    _secondsUntilNextLife.value = 0L
                }
            }
        }
    }

    fun consumeLife(): Boolean {
        val current = _lives.value
        if (current > 0) {
            val next = current - 1
            _lives.value = next
            if (current == MAX_LIVES) {
                // Was previously at max, so start regeneration timer from now
                val now = timeAuthority.getCurrentTimeMs()
                _lastLifeRegenTime.value = now
                _secondsUntilNextLife.value = LIFE_REGEN_INTERVAL_MS / 1000L
                prefs.edit()
                    .putInt(KEY_LIVES_COUNT, next)
                    .putLong(KEY_LAST_LIFE_REGEN_TIME, now)
                    .apply()
            } else {
                prefs.edit().putInt(KEY_LIVES_COUNT, next).apply()
            }
            return true
        }
        return false
    }

    fun addLives(count: Int) {
        if (count <= 0) return
        val newLives = (_lives.value + count).coerceAtLeast(0)
        _lives.value = newLives
        if (newLives >= MAX_LIVES) {
            _lastLifeRegenTime.value = timeAuthority.getCurrentTimeMs()
            _secondsUntilNextLife.value = 0L
        }
        prefs.edit()
            .putInt(KEY_LIVES_COUNT, newLives)
            .putLong(KEY_LAST_LIFE_REGEN_TIME, _lastLifeRegenTime.value)
            .apply()
    }

    fun refillMaxLives() {
        _lives.value = MAX_LIVES
        _lastLifeRegenTime.value = timeAuthority.getCurrentTimeMs()
        _secondsUntilNextLife.value = 0L
        prefs.edit()
            .putInt(KEY_LIVES_COUNT, MAX_LIVES)
            .putLong(KEY_LAST_LIFE_REGEN_TIME, _lastLifeRegenTime.value)
            .apply()
    }

    // 🚩 Checkpoint & Level Progression API
    fun saveProgressLevel(level: Int) {
        if (level > 0) {
            _savedLevel.value = level
            val highest = _highestUnlockedLevel.value.coerceAtLeast(level)
            _highestUnlockedLevel.value = highest
            prefs.edit()
                .putInt(KEY_SAVED_LEVEL, level)
                .putInt(KEY_HIGHEST_UNLOCKED_LEVEL, highest)
                .apply()
        }
    }

    fun getUnlockedCheckpoints(): List<Int> {
        val highest = _highestUnlockedLevel.value
        val checkpoints = mutableListOf(1)
        var nextCp = 5
        while (nextCp <= highest) {
            checkpoints.add(nextCp)
            nextCp += 5
        }
        return checkpoints
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
                    description = "Build 8 bridges successfully",
                    missionType = "BUILD_BRIDGES",
                    targetCount = 8,
                    currentProgress = 0,
                    rewardGems = 3,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                ),
                DailyMissionEntity(
                    id = "m_bullseye_${today}",
                    title = "Bullseye Sniper",
                    description = "Land 3 perfect center red-dot hits",
                    missionType = "PERFECT_HITS",
                    targetCount = 3,
                    currentProgress = 0,
                    rewardGems = 5,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                ),
                DailyMissionEntity(
                    id = "m_gem_${today}",
                    title = "Crystal Harvester",
                    description = "Collect 6 gems across runs",
                    missionType = "COLLECT_GEMS",
                    targetCount = 6,
                    currentProgress = 0,
                    rewardGems = 4,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                ),
                DailyMissionEntity(
                    id = "m_flip_${today}",
                    title = "Acrobatic Shinobi",
                    description = "Perform 4 upside-down flip walks",
                    missionType = "FLIP_WALK",
                    targetCount = 4,
                    currentProgress = 0,
                    rewardGems = 4,
                    isCompleted = false,
                    isClaimed = false,
                    assignedEpochDay = today
                ),
                DailyMissionEntity(
                    id = "m_score_${today}",
                    title = "Endurance Runner",
                    description = "Reach a score of 6 in a single run",
                    missionType = "REACH_SCORE",
                    targetCount = 6,
                    currentProgress = 0,
                    rewardGems = 6,
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
                    val newProgress = if (missionType == "REACH_SCORE") {
                        maxOf(m.currentProgress, delta).coerceAtMost(m.targetCount)
                    } else {
                        (m.currentProgress + delta).coerceAtMost(m.targetCount)
                    }
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
        if (rewardGems > 0) {
            addGems(rewardGems, CurrencySource.DAILY_MISSION)
        }
    }

    fun claimAllCompletedMissions(missions: List<DailyMissionEntity>): Int {
        val claimable = missions.filter { (it.currentProgress >= it.targetCount || it.isCompleted) && !it.isClaimed }
        if (claimable.isEmpty()) return 0
        val totalGems = claimable.sumOf { it.rewardGems }
        scope.launch {
            for (m in claimable) {
                dailyMissionDao.markClaimed(m.id)
            }
        }
        addGems(totalGems, CurrencySource.DAILY_MISSION)
        return totalGems
    }

    private fun getTodayEpochDay(): Long {
        return try {
            timeAuthority.getAuthoritativeEpochDay()
        } catch (_: Throwable) {
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
        addGems(rewardGems, CurrencySource.DAILY_REWARD)

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

    /**
     * Retrieves or generates a cryptographically unique Player ID.
     */
    fun getPlayerId(): String {
        var id = prefs.getString(KEY_PLAYER_ID, null)
        if (id.isNullOrBlank()) {
            id = "USR_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_PLAYER_ID, id).apply()
        }
        return id
    }

    private fun saveIntegritySignature() {
        val signature = currencyVault.computeIntegritySignature(
            gems = _gems.value,
            blueGems = _blueGems.value,
            redGems = _redGems.value,
            highScore = _highScore.value,
            totalBridges = prefs.getInt(KEY_TOTAL_BRIDGES, 0),
            streak = prefs.getInt(KEY_CURRENT_STREAK, 1),
            lastClaimEpochDay = prefs.getLong(KEY_LAST_CLAIM_DAY, 0L)
        )
        prefs.edit().putString(KEY_CURRENCY_INTEGRITY_SIGNATURE, signature).apply()
    }

    /**
     * Executes the server-side handshake/validation logic for currency transactions.
     *
     * STUBBED FOR FIREBASE CLOUD FUNCTIONS / FIRESTORE INTEGRATION:
     * When migrating to a remote Firebase backend, replace this call with the Firebase callable function:
     *
     * ```kotlin
     * val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
     * val payload = hashMapOf(
     *     "transactionId" to transaction.transactionId,
     *     "playerId" to transaction.playerId,
     *     "type" to type.name,
     *     "amount" to transaction.amount,
     *     "source" to transaction.source.name,
     *     "verificationToken" to transaction.verificationToken,
     *     "timestamp" to transaction.timestampMs
     * )
     * val result = functions.getHttpsCallable("verifyAndCommitCurrencyTransaction").call(payload).await()
     * ```
     */
    private suspend fun performServerCurrencyHandshake(
        transaction: PendingCurrencyTransaction,
        currentBalance: Int,
        type: TransactionType
    ): ServerVerificationResult {
        val request = CurrencyTransactionRequest(
            transactionId = transaction.transactionId,
            playerId = transaction.playerId,
            type = type,
            amount = transaction.amount,
            source = transaction.source,
            currentBalance = currentBalance,
            clientTimestampMs = transaction.timestampMs,
            verificationToken = transaction.verificationToken
        )
        return serverAuthority.verifyAndAuthorizeTransaction(request)
    }

    /**
     * Enqueues an addition of gems into the 'PENDING' transaction state,
     * immediately initiates the asynchronous server-side handshake/validation flow,
     * and applies authoritative balance updates upon server confirmation.
     *
     * @return The unique transaction ID for tracking.
     */
    override fun addGems(
        amount: Int,
        source: CurrencySource,
        verificationToken: String?
    ): String {
        val txId = "TX_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val pendingTx = PendingCurrencyTransaction(
            transactionId = txId,
            playerId = getPlayerId(),
            amount = amount,
            source = source,
            verificationToken = verificationToken,
            timestampMs = System.currentTimeMillis(),
            status = TransactionStatus.PENDING
        )

        // 1. Enqueue to pending transactions StateFlow
        _pendingTransactions.value = _pendingTransactions.value + pendingTx

        // 2. Launch asynchronous server handshake pipeline
        scope.launch {
            // Transition to IN_FLIGHT
            _pendingTransactions.value = _pendingTransactions.value.map {
                if (it.transactionId == txId) it.copy(status = TransactionStatus.IN_FLIGHT) else it
            }

            val result = performServerCurrencyHandshake(
                transaction = pendingTx,
                currentBalance = _gems.value,
                type = TransactionType.CREDIT
            )

            if (result.isApproved) {
                val serverAuthorized = result.authorizedBalance
                _gems.value = serverAuthorized
                currencyVault.syncFromDisk(serverAuthorized, _blueGems.value, _redGems.value)
                prefs.edit().putInt(KEY_GEMS, serverAuthorized).apply()
                saveIntegritySignature()
                playerProfileDao.updateGems(serverAuthorized)

                // Update transaction state to CONFIRMED
                _pendingTransactions.value = _pendingTransactions.value.map {
                    if (it.transactionId == txId) {
                        it.copy(
                            status = TransactionStatus.CONFIRMED,
                            serverAuthToken = result.serverAuthorizationToken
                        )
                    } else it
                }.takeLast(20)
            } else {
                // Server rejected - mark REJECTED and do not credit balance
                _pendingTransactions.value = _pendingTransactions.value.map {
                    if (it.transactionId == txId) {
                        it.copy(
                            status = TransactionStatus.REJECTED,
                            failureReason = result.rejectionReason ?: "SERVER_REJECTED"
                        )
                    } else it
                }.takeLast(20)
            }
        }

        return txId
    }

    /**
     * Suspending version that initiates the handshake and awaits the authoritative server verification outcome.
     */
    override suspend fun addGemsAuthoritative(
        amount: Int,
        source: CurrencySource,
        verificationToken: String?
    ): CurrencyTransactionOutcome {
        val txId = "TX_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val pendingTx = PendingCurrencyTransaction(
            transactionId = txId,
            playerId = getPlayerId(),
            amount = amount,
            source = source,
            verificationToken = verificationToken,
            timestampMs = System.currentTimeMillis(),
            status = TransactionStatus.IN_FLIGHT
        )
        _pendingTransactions.value = _pendingTransactions.value + pendingTx

        val result = performServerCurrencyHandshake(
            transaction = pendingTx,
            currentBalance = _gems.value,
            type = TransactionType.CREDIT
        )

        return if (result.isApproved) {
            val serverAuthorized = result.authorizedBalance
            _gems.value = serverAuthorized
            currencyVault.syncFromDisk(serverAuthorized, _blueGems.value, _redGems.value)
            prefs.edit().putInt(KEY_GEMS, serverAuthorized).apply()
            saveIntegritySignature()
            playerProfileDao.updateGems(serverAuthorized)

            _pendingTransactions.value = _pendingTransactions.value.map {
                if (it.transactionId == txId) {
                    it.copy(
                        status = TransactionStatus.CONFIRMED,
                        serverAuthToken = result.serverAuthorizationToken
                    )
                } else it
            }.takeLast(20)

            CurrencyTransactionOutcome(
                transactionId = txId,
                isApproved = true,
                status = TransactionStatus.CONFIRMED,
                newBalance = serverAuthorized
            )
        } else {
            _pendingTransactions.value = _pendingTransactions.value.map {
                if (it.transactionId == txId) {
                    it.copy(
                        status = TransactionStatus.REJECTED,
                        failureReason = result.rejectionReason ?: "SERVER_REJECTED"
                    )
                } else it
            }.takeLast(20)

            CurrencyTransactionOutcome(
                transactionId = txId,
                isApproved = false,
                status = TransactionStatus.REJECTED,
                newBalance = _gems.value,
                message = result.rejectionReason
            )
        }
    }

    /**
     * Mandates server-side verification before applying any debit mutation to the player's account.
     */
    override suspend fun spendGemsAuthoritative(
        amount: Int,
        source: CurrencySource
    ): CurrencyTransactionOutcome {
        val txId = "TX_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val pendingTx = PendingCurrencyTransaction(
            transactionId = txId,
            playerId = getPlayerId(),
            amount = amount,
            source = source,
            verificationToken = null,
            timestampMs = System.currentTimeMillis(),
            status = TransactionStatus.IN_FLIGHT
        )
        _pendingTransactions.value = _pendingTransactions.value + pendingTx

        val result = performServerCurrencyHandshake(
            transaction = pendingTx,
            currentBalance = _gems.value,
            type = TransactionType.DEBIT
        )

        return if (result.isApproved) {
            val serverAuthorized = result.authorizedBalance
            _gems.value = serverAuthorized
            currencyVault.syncFromDisk(serverAuthorized, _blueGems.value, _redGems.value)
            prefs.edit().putInt(KEY_GEMS, serverAuthorized).apply()
            saveIntegritySignature()
            playerProfileDao.updateGems(serverAuthorized)

            _pendingTransactions.value = _pendingTransactions.value.map {
                if (it.transactionId == txId) {
                    it.copy(
                        status = TransactionStatus.CONFIRMED,
                        serverAuthToken = result.serverAuthorizationToken
                    )
                } else it
            }.takeLast(20)

            CurrencyTransactionOutcome(
                transactionId = txId,
                isApproved = true,
                status = TransactionStatus.CONFIRMED,
                newBalance = serverAuthorized
            )
        } else {
            _pendingTransactions.value = _pendingTransactions.value.map {
                if (it.transactionId == txId) {
                    it.copy(
                        status = TransactionStatus.REJECTED,
                        failureReason = result.rejectionReason ?: "INSUFFICIENT_FUNDS"
                    )
                } else it
            }.takeLast(20)

            CurrencyTransactionOutcome(
                transactionId = txId,
                isApproved = false,
                status = TransactionStatus.REJECTED,
                newBalance = _gems.value,
                message = result.rejectionReason
            )
        }
    }

    /**
     * Spends gems from the account with synchronous local vault gating and server-side authorization handshake.
     */
    override fun spendGems(amount: Int): Boolean {
        val (success, newGems) = currencyVault.spendGemsSecurely(_gems.value, amount)
        if (success) {
            _gems.value = newGems
            prefs.edit().putInt(KEY_GEMS, newGems).apply()
            saveIntegritySignature()

            val txId = "TX_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            val pendingTx = PendingCurrencyTransaction(
                transactionId = txId,
                playerId = getPlayerId(),
                amount = amount,
                source = CurrencySource.IN_APP_PURCHASE,
                verificationToken = null,
                timestampMs = System.currentTimeMillis(),
                status = TransactionStatus.IN_FLIGHT
            )
            _pendingTransactions.value = _pendingTransactions.value + pendingTx

            scope.launch {
                val result = performServerCurrencyHandshake(
                    transaction = pendingTx,
                    currentBalance = newGems + amount,
                    type = TransactionType.DEBIT
                )
                if (result.isApproved) {
                    playerProfileDao.updateGems(result.authorizedBalance)
                    _pendingTransactions.value = _pendingTransactions.value.map {
                        if (it.transactionId == txId) it.copy(status = TransactionStatus.CONFIRMED) else it
                    }.takeLast(20)
                } else {
                    // Rollback if server debit check fails
                    val rolledBack = result.authorizedBalance
                    _gems.value = rolledBack
                    currencyVault.syncFromDisk(rolledBack, _blueGems.value, _redGems.value)
                    prefs.edit().putInt(KEY_GEMS, rolledBack).apply()
                    saveIntegritySignature()
                    playerProfileDao.updateGems(rolledBack)
                    _pendingTransactions.value = _pendingTransactions.value.map {
                        if (it.transactionId == txId) it.copy(status = TransactionStatus.REJECTED, failureReason = result.rejectionReason) else it
                    }.takeLast(20)
                }
            }
            return true
        }
        return false
    }

    override fun addBlueGems(amount: Int) {
        val newGems = (_blueGems.value + amount).coerceAtLeast(0)
        _blueGems.value = newGems
        val totalEarned = prefs.getInt(KEY_TOTAL_BLUE_GEMS_EARNED, 0) + amount
        prefs.edit()
            .putInt(KEY_BLUE_GEMS, newGems)
            .putInt(KEY_TOTAL_BLUE_GEMS_EARNED, totalEarned)
            .apply()
        saveIntegritySignature()
    }

    override fun spendBlueGems(amount: Int): Boolean {
        if (amount <= 0) return false
        val (success, newGems) = currencyVault.spendBlueGemsSecurely(_blueGems.value, amount)
        if (success) {
            _blueGems.value = newGems
            prefs.edit().putInt(KEY_BLUE_GEMS, newGems).apply()
            saveIntegritySignature()
            return true
        }
        return false
    }

    override fun addRedGems(amount: Int) {
        if (amount <= 0) return
        val newGems = (_redGems.value + amount).coerceAtLeast(0)
        _redGems.value = newGems
        val totalEarned = prefs.getInt(KEY_TOTAL_RED_GEMS_EARNED, 0) + amount
        prefs.edit()
            .putInt(KEY_RED_GEMS, newGems)
            .putInt(KEY_TOTAL_RED_GEMS_EARNED, totalEarned)
            .apply()
        saveIntegritySignature()
    }

    override fun spendRedGems(amount: Int): Boolean {
        if (amount <= 0) return false
        val (success, newGems) = currencyVault.spendRedGemsSecurely(_redGems.value, amount)
        if (success) {
            _redGems.value = newGems
            prefs.edit().putInt(KEY_RED_GEMS, newGems).apply()
            saveIntegritySignature()
            return true
        }
        return false
    }

    /**
     * Performs a full balance reconciliation handshake with the remote authoritative server
     * (stubbed for Firebase Cloud Functions / Firestore backend).
     */
    override suspend fun syncCurrencyWithServer(): Boolean {
        return try {
            val serverBalance = serverAuthority.fetchAuthoritativeBalance(getPlayerId())
            if (serverBalance > 0 && serverBalance != _gems.value) {
                _gems.value = serverBalance
                currencyVault.syncFromDisk(serverBalance, _blueGems.value, _redGems.value)
                prefs.edit().putInt(KEY_GEMS, serverBalance).apply()
                saveIntegritySignature()
                playerProfileDao.updateGems(serverBalance)
            }
            true
        } catch (e: Exception) {
            false
        }
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
        val purchaseSuccess = when (item.currencyType) {
            com.mygames.stickmanrush.model.CurrencyType.GEM -> spendGems(item.cost)
            com.mygames.stickmanrush.model.CurrencyType.BLUE_GEM -> spendBlueGems(item.cost)
            com.mygames.stickmanrush.model.CurrencyType.RED_GEM -> spendRedGems(item.cost)
        }
        if (purchaseSuccess) {
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
            AccessoryType.THEME -> {
                _selectedTheme.value = item.id
                prefs.edit().putString(KEY_SELECTED_THEME, item.id).apply()
            }
            AccessoryType.GEM_VAULT -> { /* No-op */ }
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

    // Gem Vault Packs Catalog (Real-Money IAP and Free Claim)
    val availableGemPacks: List<GemPack> = listOf(
        GemPack(
            id = "gem_free_daily",
            name = "Daily Supply Crate",
            gemAmount = 35,
            bonusGems = 0,
            iconEmoji = "📦",
            tag = "DAILY FREE",
            priceUsd = "FREE",
            isDailyFree = true,
            perks = "Free daily refill every 24 hours"
        ),
        GemPack(
            id = "gem_starter",
            name = "Handful of Crystals",
            gemAmount = 100,
            bonusGems = 25,
            iconEmoji = "✨",
            tag = "STARTER DEAL",
            priceUsd = "$0.99",
            scoreCost = 30,
            perks = "125 Gems + 1 Free Revival Heart"
        ),
        GemPack(
            id = "gem_pouch",
            name = "Pouch of Rubies",
            gemAmount = 350,
            bonusGems = 100,
            iconEmoji = "💰",
            tag = "MOST POPULAR",
            priceUsd = "$2.99",
            scoreCost = 80,
            perks = "450 Gems + 3 Free Revivals"
        ),
        GemPack(
            id = "gem_chest",
            name = "Shinobi Treasure Box",
            gemAmount = 900,
            bonusGems = 350,
            iconEmoji = "💎",
            tag = "BEST VALUE 🔥",
            priceUsd = "$4.99",
            scoreCost = 150,
            perks = "1,250 Gems + VIP Golden Bridge Skin"
        ),
        GemPack(
            id = "gem_vault",
            name = "Dragon Emperor Vault",
            gemAmount = 2600,
            bonusGems = 1200,
            iconEmoji = "👑",
            tag = "+45% EXTRA",
            priceUsd = "$9.99",
            scoreCost = 350,
            perks = "3,800 Gems + 10 Free Revivals + Crown"
        ),
        GemPack(
            id = "gem_ultimate",
            name = "Master Mythic Cache",
            gemAmount = 7500,
            bonusGems = 4000,
            iconEmoji = "🌌",
            tag = "GOD TIER ⚡",
            priceUsd = "$19.99",
            scoreCost = 700,
            perks = "11,500 Gems + Permanent 2x Gem Multiplier"
        )
    )

    fun claimDailyFreeGems(): Boolean {
        val today = getTodayEpochDay()
        val lastClaimKey = "LAST_GEM_FREE_CLAIM"
        val lastDay = prefs.getLong(lastClaimKey, 0L)
        if (lastDay != today) {
            prefs.edit().putLong(lastClaimKey, today).apply()
            addGems(15, CurrencySource.DAILY_FREE_GEMS) // Rebalanced competitive reward
            return true
        }
        return false
    }

    fun isDailyFreeGemsAvailable(): Boolean {
        val today = getTodayEpochDay()
        val lastDay = prefs.getLong("LAST_GEM_FREE_CLAIM", 0L)
        return lastDay != today
    }

    fun buyGemPackWithTokens(pack: GemPack): Boolean {
        if (pack.isDailyFree) {
            return claimDailyFreeGems()
        }
        val totalAwarded = pack.gemAmount + pack.bonusGems
        addGems(totalAwarded, CurrencySource.LUCKY_SPIN)
        return true
    }

    private val _adEarnedSpins = MutableStateFlow(prefs.getInt("AD_EARNED_SPINS", 0))
    val adEarnedSpins: StateFlow<Int> = _adEarnedSpins.asStateFlow()

    private val _isDailyFreeSpinAvailableFlow = MutableStateFlow(checkDailyFreeSpinAvailable())
    val isDailyFreeSpinAvailableFlow: StateFlow<Boolean> = _isDailyFreeSpinAvailableFlow.asStateFlow()

    private fun checkDailyFreeSpinAvailable(): Boolean {
        val today = getTodayEpochDay()
        val lastDay = prefs.getLong("LAST_DAILY_FREE_SPIN", 0L)
        return lastDay != today
    }

    fun isDailyFreeSpinAvailable(): Boolean {
        val isAvail = checkDailyFreeSpinAvailable()
        _isDailyFreeSpinAvailableFlow.value = isAvail
        return isAvail
    }

    fun getAdSpinsCount(): Int = _adEarnedSpins.value

    fun hasAvailableSpin(): Boolean {
        return isDailyFreeSpinAvailable() || _adEarnedSpins.value > 0
    }

    fun grantAdRewardSpin(count: Int = 1, ssvVerificationToken: String? = null) {
        if (count <= 0) return
        val current = _adEarnedSpins.value + count
        _adEarnedSpins.value = current
        prefs.edit().putInt("AD_EARNED_SPINS", current).apply()
    }

    fun verifyAndGrantAdReward(adSession: AdServerSideVerificationManager.AdSSVChallenge, spinsToGrant: Int = 1): Boolean {
        if (spinsToGrant <= 0) return false
        val verification = adSsvManager.verifyAdRewardCallback(adSession)
        if (verification.isVerified) {
            grantAdRewardSpin(spinsToGrant, verification.verificationToken)
            return true
        }
        return false
    }

    fun buySpinsWithGems(gemCost: Int, spinsCount: Int): Boolean {
        if (gemCost <= 0 || spinsCount <= 0) return false
        if (spendGems(gemCost)) {
            grantAdRewardSpin(spinsCount)
            return true
        }
        return false
    }

    fun buySpinsRealMoney(spinsCount: Int) {
        grantAdRewardSpin(spinsCount)
    }

    fun spinLuckyWheel(): Int {
        val today = getTodayEpochDay()
        if (checkDailyFreeSpinAvailable()) {
            prefs.edit().putLong("LAST_DAILY_FREE_SPIN", today).apply()
            _isDailyFreeSpinAvailableFlow.value = false
        } else if (_adEarnedSpins.value > 0) {
            val remaining = _adEarnedSpins.value - 1
            _adEarnedSpins.value = remaining
            prefs.edit().putInt("AD_EARNED_SPINS", remaining).apply()
        }

        // Competitive Balanced Rewards: 3, 5, 10, 20 gems
        val roll = (1..100).random()
        val reward = when {
            roll <= 45 -> 3
            roll <= 75 -> 5
            roll <= 92 -> 10
            else -> 20 // Grand Lucky Prize
        }
        addGems(reward, CurrencySource.LUCKY_SPIN)
        return reward
    }

    fun getUserTournamentLeague(): TournamentLeague {
        val hs = _highScore.value
        return when {
            hs >= 80 -> TournamentLeague.MASTER
            hs >= 40 -> TournamentLeague.DIAMOND
            hs >= 20 -> TournamentLeague.GOLD
            hs >= 8 -> TournamentLeague.SILVER
            else -> TournamentLeague.BRONZE
        }
    }

    fun getGlobalLeaderboard(): List<LeaderboardEntry> {
        val isCheater = currencyVault.earningLimiter.isCheater()
        val userHighScore = if (isCheater) 0 else _highScore.value
        val userPerfects = if (isCheater) 0 else prefs.getInt(KEY_PERFECT_HITS, 0)
        val userLeague = if (isCheater) TournamentLeague.BRONZE else getUserTournamentLeague()

        val entries = mutableListOf(
            LeaderboardEntry(1, "ShadowNinja", "🥷", "🇯🇵", 248, 114, TournamentLeague.MASTER),
            LeaderboardEntry(2, "BridgeMaster99", "👑", "🇺🇸", 212, 98, TournamentLeague.MASTER),
            LeaderboardEntry(3, "ValkyrieSpeed", "⚡", "🇩🇪", 189, 82, TournamentLeague.MASTER),
            LeaderboardEntry(4, "CyberSamurai", "🤖", "🇰🇷", 164, 76, TournamentLeague.MASTER),
            LeaderboardEntry(5, "PhoenixRider", "🔥", "🇧🇷", 142, 63, TournamentLeague.DIAMOND),
            LeaderboardEntry(6, "ZenMonk", "🧘", "🇮🇳", 128, 59, TournamentLeague.DIAMOND),
            LeaderboardEntry(7, "AcrobatQueen", "🤸", "🇫🇷", 115, 50, TournamentLeague.DIAMOND),
            LeaderboardEntry(8, "PixelDragon", "🐉", "🇨🇦", 98, 44, TournamentLeague.DIAMOND),
            LeaderboardEntry(9, "LaserEdge", "💠", "🇬🇧", 84, 38, TournamentLeague.GOLD),
            LeaderboardEntry(10, "NightStalker", "🐺", "🇦🇺", 72, 31, TournamentLeague.GOLD),
            LeaderboardEntry(11, "FrostSpectre", "❄️", "🇸🇪", 61, 27, TournamentLeague.GOLD),
            LeaderboardEntry(12, "StarChaser", "✨", "🇮🇹", 49, 21, TournamentLeague.SILVER),
            LeaderboardEntry(13, "StickHeroPro", "🎯", "🇲🇽", 38, 17, TournamentLeague.SILVER),
            LeaderboardEntry(14, "NeonRunner", "👟", "🇪🇸", 28, 12, TournamentLeague.BRONZE),
            LeaderboardEntry(15, "SkyWalker", "☁️", "🇳🇿", 18, 7, TournamentLeague.BRONZE)
        )

        // Calculate user position
        val userRank = (entries.indexOfFirst { userHighScore >= it.score }.takeIf { it != -1 }?.plus(1)) ?: (entries.size + 1)
        val userEntry = LeaderboardEntry(
            rank = userRank,
            playerName = if (isCheater) "YOU (Flagged ⚠️)" else "YOU (Hero)",
            avatarEmoji = if (isCheater) "⚠️" else "⭐",
            countryFlag = "🌍",
            score = userHighScore,
            perfectHits = userPerfects,
            league = userLeague,
            isCurrentUser = true
        )

        val combined = (entries + userEntry).sortedByDescending { it.score }
        return combined.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }

    fun getUpcomingRivals(): List<RivalGhost> {
        return listOf(
            RivalGhost("ShadowNinja", "🇯🇵", "🥷", 248),
            RivalGhost("BridgeMaster", "🇺🇸", "👑", 212),
            RivalGhost("Valkyrie", "🇩🇪", "⚡", 189),
            RivalGhost("CyberSamurai", "🇰🇷", "🤖", 164),
            RivalGhost("ZenMonk", "🇮🇳", "🧘", 128),
            RivalGhost("PixelDragon", "🇨🇦", "🐉", 98),
            RivalGhost("FrostSpectre", "🇸🇪", "❄️", 61),
            RivalGhost("NeonRunner", "🇪🇸", "👟", 28)
        )
    }

    // --- WEEKLY EPIC MISSIONS ---
    fun getWeeklyMissions(): List<WeeklyMissionItem> {
        val defs = listOf(
            Triple("w_architect", "Grand Bridge Architect", "Span 40 total bridge gaps this week") to ((("BUILD_BRIDGES" to 40) to (12 to "🥢")) to 2),
            Triple("w_sniper", "Bullseye Grandmaster", "Score 15 red dot bullseye perfect hits") to ((("PERFECT_HITS" to 15) to (18 to "🎯")) to 3),
            Triple("w_gem_hoarder", "Gem Vault Magnate", "Harvest 35 under-bridge glowing rubies") to ((("COLLECT_GEMS" to 35) to (15 to "💎")) to 2),
            Triple("w_acrobat", "Upside-Down Acrobat", "Complete 20 stealth flip walks safely") to ((("FLIP_WALK" to 20) to (15 to "🤸")) to 2),
            Triple("w_marathon", "Endurance Champion", "Reach a high score of 12 or above") to ((("REACH_SCORE" to 12) to (25 to "👑")) to 4)
        )

        return defs.map { (info, config) ->
            val (id, title, desc) = info
            val (mainConfig, blueGemReward) = config
            val (typeConfig, rewardConfig) = mainConfig
            val (type, target) = typeConfig
            val (gemsReward, emoji) = rewardConfig

            val prog = prefs.getInt(KEY_WEEKLY_PREFIX + id, 0)
            val claimed = prefs.getBoolean(KEY_WEEKLY_CLAIM_PREFIX + id, false)
            val isDone = prog >= target

            WeeklyMissionItem(
                id = id,
                title = title,
                description = desc,
                missionType = type,
                targetCount = target,
                currentProgress = prog,
                rewardGems = gemsReward,
                rewardBlueGems = blueGemReward,
                iconEmoji = emoji,
                isCompleted = isDone,
                isClaimed = claimed,
                badgeLabel = "WEEKLY"
            )
        }
    }

    fun trackWeeklyMissionProgress(missionType: String, delta: Int = 1) {
        val missions = getWeeklyMissions()
        for (m in missions) {
            if (m.missionType == missionType && !m.isCompleted) {
                val newProgress = if (missionType == "REACH_SCORE") {
                    maxOf(m.currentProgress, delta).coerceAtMost(m.targetCount)
                } else {
                    (m.currentProgress + delta).coerceAtMost(m.targetCount)
                }
                prefs.edit().putInt(KEY_WEEKLY_PREFIX + m.id, newProgress).apply()
            }
        }
    }

    fun claimWeeklyMission(id: String, rewardGems: Int, rewardBlueGems: Int = 0): Boolean {
        if (!prefs.getBoolean(KEY_WEEKLY_CLAIM_PREFIX + id, false)) {
            prefs.edit().putBoolean(KEY_WEEKLY_CLAIM_PREFIX + id, true).apply()
            if (rewardGems > 0) addGems(rewardGems, CurrencySource.WEEKLY_MISSION)
            if (rewardBlueGems > 0) addBlueGems(rewardBlueGems)
            return true
        }
        return false
    }

    fun claimAllWeeklyMissions(): Pair<Int, Int> {
        val weekly = getWeeklyMissions().filter { it.isCompleted && !it.isClaimed }
        if (weekly.isEmpty()) return 0 to 0
        var totalGems = 0
        var totalBlue = 0
        weekly.forEach {
            prefs.edit().putBoolean(KEY_WEEKLY_CLAIM_PREFIX + it.id, true).apply()
            totalGems += it.rewardGems
            totalBlue += it.rewardBlueGems
        }
        if (totalGems > 0) addGems(totalGems, CurrencySource.WEEKLY_MISSION)
        if (totalBlue > 0) addBlueGems(totalBlue)
        return totalGems to totalBlue
    }

    // --- EXPANDED TOURNAMENTS & CONTESTS ---
    fun getContestTournaments(): List<ContestTournament> {
        val list = listOf(
            ContestTournament(
                id = "contest_bridge_rush",
                title = "Weekend Bridge Rush",
                subtitle = "Fast-paced bridge building marathon",
                iconEmoji = "🌟",
                bannerColorHex = 0xFF4F46E5,
                timeRemainingStr = "2d 14h",
                participantsCount = "14,820",
                prizePoolGems = 45,
                prizePoolBlueGems = 5,
                prizePoolRedGems = 2,
                targetGoal = 25,
                currentProgress = prefs.getInt(KEY_CONTEST_PREFIX + "contest_bridge_rush", 0),
                goalUnit = "Bridges",
                isJoined = true,
                isCompleted = prefs.getInt(KEY_CONTEST_PREFIX + "contest_bridge_rush", 0) >= 25,
                isClaimed = prefs.getBoolean(KEY_CONTEST_CLAIM_PREFIX + "contest_bridge_rush", false),
                rewardPerk = "45 💎 + 5 🔷 + 2 🔴"
            ),
            ContestTournament(
                id = "contest_bullseye_cup",
                title = "Sniper Bullseye Masters",
                subtitle = "Chain consecutive red dot perfect bullseyes",
                iconEmoji = "🎯",
                bannerColorHex = 0xFFD97706,
                timeRemainingStr = "1d 08h",
                participantsCount = "9,430",
                prizePoolGems = 35,
                prizePoolBlueGems = 6,
                prizePoolRedGems = 3,
                targetGoal = 10,
                currentProgress = prefs.getInt(KEY_CONTEST_PREFIX + "contest_bullseye_cup", 0),
                goalUnit = "Bullseyes",
                isJoined = true,
                isCompleted = prefs.getInt(KEY_CONTEST_PREFIX + "contest_bullseye_cup", 0) >= 10,
                isClaimed = prefs.getBoolean(KEY_CONTEST_CLAIM_PREFIX + "contest_bullseye_cup", false),
                rewardPerk = "35 💎 + 6 🔷 + 3 🔴"
            ),
            ContestTournament(
                id = "contest_gem_hunt",
                title = "Gem Canyon Scavenger",
                subtitle = "Collect upside-down gems in high-risk zones",
                iconEmoji = "💎",
                bannerColorHex = 0xFF059669,
                timeRemainingStr = "3d 21h",
                participantsCount = "18,120",
                prizePoolGems = 30,
                prizePoolBlueGems = 4,
                prizePoolRedGems = 2,
                targetGoal = 15,
                currentProgress = prefs.getInt(KEY_CONTEST_PREFIX + "contest_gem_hunt", 0),
                goalUnit = "Gems",
                isJoined = true,
                isCompleted = prefs.getInt(KEY_CONTEST_PREFIX + "contest_gem_hunt", 0) >= 15,
                isClaimed = prefs.getBoolean(KEY_CONTEST_CLAIM_PREFIX + "contest_gem_hunt", false),
                rewardPerk = "30 💎 + 4 🔷 + 2 🔴"
            ),
            ContestTournament(
                id = "contest_speed_blitz",
                title = "Daily Survival Marathon",
                subtitle = "Global endurance division speed ladder",
                iconEmoji = "⚡",
                bannerColorHex = 0xFF9333EA,
                timeRemainingStr = "18h 30m",
                participantsCount = "24,500",
                prizePoolGems = 60,
                prizePoolBlueGems = 8,
                prizePoolRedGems = 4,
                targetGoal = 8,
                currentProgress = prefs.getInt(KEY_CONTEST_PREFIX + "contest_speed_blitz", 0),
                goalUnit = "Score",
                isJoined = true,
                isCompleted = prefs.getInt(KEY_CONTEST_PREFIX + "contest_speed_blitz", 0) >= 8,
                isClaimed = prefs.getBoolean(KEY_CONTEST_CLAIM_PREFIX + "contest_speed_blitz", false),
                rewardPerk = "60 💎 + 8 🔷 + 4 🔴"
            )
        )
        return list
    }

    fun trackContestProgress(type: String, delta: Int = 1) {
        val mapping = when (type) {
            "BUILD_BRIDGES" -> "contest_bridge_rush"
            "PERFECT_HITS" -> "contest_bullseye_cup"
            "COLLECT_GEMS" -> "contest_gem_hunt"
            "REACH_SCORE" -> "contest_speed_blitz"
            else -> null
        }
        mapping?.let { contestId ->
            val curr = prefs.getInt(KEY_CONTEST_PREFIX + contestId, 0)
            val newProg = if (type == "REACH_SCORE") maxOf(curr, delta) else curr + delta
            prefs.edit().putInt(KEY_CONTEST_PREFIX + contestId, newProg).apply()
        }
    }

    fun claimContestReward(contestId: String): Triple<Int, Int, Int> {
        val contest = getContestTournaments().find { it.id == contestId } ?: return Triple(0, 0, 0)
        if (contest.isCompleted && !contest.isClaimed) {
            prefs.edit().putBoolean(KEY_CONTEST_CLAIM_PREFIX + contestId, true).apply()
            if (contest.prizePoolGems > 0) addGems(contest.prizePoolGems, CurrencySource.CONTEST_REWARD)
            if (contest.prizePoolBlueGems > 0) addBlueGems(contest.prizePoolBlueGems)
            if (contest.prizePoolRedGems > 0) addRedGems(contest.prizePoolRedGems)
            return Triple(contest.prizePoolGems, contest.prizePoolBlueGems, contest.prizePoolRedGems)
        }
        return Triple(0, 0, 0)
    }

    // --- PLAYER CAREER STATS & RECORDS ---
    fun recordGamePlayed() {
        val curr = prefs.getInt(KEY_TOTAL_GAMES, 0)
        prefs.edit().putInt(KEY_TOTAL_GAMES, curr + 1).apply()
    }

    fun recordGemsHarvested(amount: Int) {
        val curr = prefs.getInt(KEY_TOTAL_GEMS_HARVESTED, 0)
        prefs.edit().putInt(KEY_TOTAL_GEMS_HARVESTED, curr + amount).apply()
    }

    fun getPlayerCareerStats(): PlayerCareerStats {
        val totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0)
        val hs = _highScore.value
        val bridges = prefs.getInt(KEY_TOTAL_BRIDGES, 0)
        val perfects = prefs.getInt(KEY_PERFECT_HITS, 0)
        val bullseyePct = if (bridges > 0) ((perfects.toFloat() / bridges.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
        val gemsEarned = prefs.getInt(KEY_TOTAL_GEMS_HARVESTED, 0) + _gems.value
        val blueGemsEarned = prefs.getInt(KEY_TOTAL_BLUE_GEMS_EARNED, 0) + _blueGems.value
        val redGemsEarned = prefs.getInt(KEY_TOTAL_RED_GEMS_EARNED, 0) + _redGems.value
        val streak = _currentStreak.value

        return PlayerCareerStats(
            totalGamesPlayed = totalGames,
            highScore = hs,
            totalBridgesBuilt = bridges,
            totalPerfectHits = perfects,
            bullseyeRatePercent = bullseyePct,
            totalGemsHarvested = gemsEarned,
            totalBlueGemsEarned = blueGemsEarned,
            totalRedGemsEarned = redGemsEarned,
            currentStreakDays = streak,
            league = getUserTournamentLeague()
        )
    }

    // --- GAME SETTINGS ---
    fun toggleLeftHanded(): Boolean {
        val next = !_leftHandedMode.value
        _leftHandedMode.value = next
        prefs.edit().putBoolean(KEY_LEFT_HANDED, next).apply()
        return next
    }

    fun toggleHighFrameRate(): Boolean {
        val next = !_highFrameRate.value
        _highFrameRate.value = next
        prefs.edit().putBoolean(KEY_HIGH_FPS, next).apply()
        return next
    }

    fun toggleParticleQuality(): Boolean {
        val next = !_particleQualityUltra.value
        _particleQualityUltra.value = next
        prefs.edit().putBoolean(KEY_PARTICLES_ULTRA, next).apply()
        return next
    }

    fun toggleScreenShake(): Boolean {
        val next = !_screenShakeEnabled.value
        _screenShakeEnabled.value = next
        prefs.edit().putBoolean(KEY_SCREEN_SHAKE, next).apply()
        return next
    }

    fun resetCareerProgress() {
        prefs.edit()
            .putInt(KEY_HIGH_SCORE, 0)
            .putInt(KEY_TOTAL_BRIDGES, 0)
            .putInt(KEY_PERFECT_HITS, 0)
            .putInt(KEY_TOTAL_GAMES, 0)
            .apply()
        _highScore.value = 0
        scope.launch {
            playerProfileDao.updateHighScore(0)
        }
    }
}
