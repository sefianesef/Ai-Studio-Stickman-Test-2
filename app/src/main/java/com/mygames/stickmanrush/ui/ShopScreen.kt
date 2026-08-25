package com.mygames.stickmanrush.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygames.stickmanrush.model.AccessoryItem
import com.mygames.stickmanrush.model.AccessoryType
import com.mygames.stickmanrush.model.CurrencyType
import com.mygames.stickmanrush.model.GemPack
import com.mygames.stickmanrush.model.ItemRarity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shop Category Tab configuration with emoji, title, and descriptive subtitle.
 */
enum class ShopCategory(
    val type: AccessoryType,
    val title: String,
    val iconEmoji: String,
    val subtitle: String
) {
    SKINS(AccessoryType.BODY_SKIN, "Skins", "🦸", "Hero Outfits & Avatars"),
    BRIDGES(AccessoryType.STICK, "Bridges", "🥢", "Staffs & Energy Beams"),
    HATS(AccessoryType.HAT, "Hats", "👑", "Crowns, Masks & Visors"),
    CAPES(AccessoryType.SCARF, "Capes", "🧣", "Wings, Cloaks & Streamers"),
    THEMES(AccessoryType.THEME, "Arenas", "🌌", "Procedural Biome Themes"),
    VAULT(AccessoryType.GEM_VAULT, "Gem Vault", "💎", "Gem Packs & Daily Boosts")
}

@Composable
fun ShopDialog(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { viewModel.openShop(false) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        ShopScreenContent(
            viewModel = viewModel,
            onClose = { viewModel.openShop(false) },
            modifier = modifier
        )
    }
}

@Composable
fun ShopScreenContent(
    viewModel: GameViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedShopTab.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val blueGems by viewModel.blueGems.collectAsState()
    val redGems by viewModel.redGems.collectAsState()
    val shopCurrencyFilter by viewModel.shopCurrencyFilter.collectAsState()
    val selectedHatId by viewModel.selectedHatId.collectAsState()
    val selectedScarfId by viewModel.selectedScarfId.collectAsState()
    val selectedStickId by viewModel.selectedStickId.collectAsState()
    val selectedSkinId by viewModel.selectedSkinId.collectAsState()
    val selectedThemeId by viewModel.selectedThemeId.collectAsState()
    val isDailyAvailable by viewModel.isDailyRewardAvailable.collectAsState()

    // Rarity filter state
    var selectedRarityFilter by remember { mutableStateOf<ItemRarity?>(null) }

    // Live preview item
    var previewedItem by remember(selectedTab) {
        val initial = when (selectedTab) {
            AccessoryType.BODY_SKIN -> viewModel.getEquippedSkin()
            AccessoryType.STICK -> viewModel.getEquippedStick()
            AccessoryType.HAT -> viewModel.getEquippedHat()
            AccessoryType.SCARF -> viewModel.getEquippedScarf()
            AccessoryType.THEME -> viewModel.getEquippedTheme()
            AccessoryType.GEM_VAULT -> null
        }
        mutableStateOf<AccessoryItem?>(initial)
    }

    // Filtered items list
    val allItems = viewModel.availableAccessories
    val filteredItems = remember(selectedTab, shopCurrencyFilter, selectedRarityFilter, allItems) {
        allItems.filter { item ->
            val matchesTab = item.type == selectedTab
            val matchesCurrency = when (shopCurrencyFilter) {
                "ALL" -> true
                "STANDARD" -> item.currencyType == CurrencyType.GEM
                "CONTEST_BLUE" -> item.currencyType == CurrencyType.BLUE_GEM
                "TOURNAMENT_RED" -> item.currencyType == CurrencyType.RED_GEM
                else -> true
            }
            val matchesRarity = selectedRarityFilter == null || item.rarity == selectedRarityFilter
            matchesTab && matchesCurrency && matchesRarity
        }
    }

    // Animation transition for showcase preview
    val infiniteTransition = rememberInfiniteTransition(label = "shop_preview_trans")
    val previewTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shop_preview_time"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF070E1E),
        border = BorderStroke(1.5.dp, Color(0xFF1E293B)),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .testTag("shop_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP HEADER: Shop Identity, Live Multi-Currency Wallets & Close Button
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🛍️", fontSize = 18.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "HERO BAZAAR",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Skins, Bridges & Accessories",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Currency Balances & Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Standard Gems
                    CurrencyBadge(
                        symbol = "💎",
                        amount = gems,
                        color = Color(0xFF38BDF8),
                        bgColor = Color(0xFF0F172A),
                        borderColor = Color(0xFF0284C7),
                        tag = "shop_gems_balance"
                    )

                    // Contest Blue Gems
                    CurrencyBadge(
                        symbol = "🔷",
                        amount = blueGems,
                        color = Color(0xFF7DD3FC),
                        bgColor = Color(0xFF0C2A4D),
                        borderColor = Color(0xFF38BDF8),
                        tag = "shop_blue_gems_balance"
                    )

                    // Tournament Red Rubies
                    CurrencyBadge(
                        symbol = "🔴",
                        amount = redGems,
                        color = Color(0xFFFDA4AF),
                        bgColor = Color(0xFF4C0519),
                        borderColor = Color(0xFFFB7185),
                        tag = "shop_red_gems_balance"
                    )

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("shop_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Shop",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // -------------------------------------------------------------
            // 2. LIVE INTERACTIVE HERO PREVIEW SHOWCASE
            // -------------------------------------------------------------
            if (selectedTab != AccessoryType.GEM_VAULT) {
                val currentPreview = previewedItem ?: filteredItems.firstOrNull()
                if (currentPreview != null) {
                    val activeHat = if (selectedTab == AccessoryType.HAT) currentPreview else viewModel.getEquippedHat()
                    val activeScarf = if (selectedTab == AccessoryType.SCARF) currentPreview else viewModel.getEquippedScarf()
                    val activeStick = if (selectedTab == AccessoryType.STICK) currentPreview else viewModel.getEquippedStick()
                    val activeSkin = if (selectedTab == AccessoryType.BODY_SKIN) currentPreview else viewModel.getEquippedSkin()

                    HeroShowcaseCard(
                        previewItem = currentPreview,
                        activeHat = activeHat,
                        activeScarf = activeScarf,
                        activeStick = activeStick,
                        activeSkin = activeSkin,
                        previewTime = previewTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // -------------------------------------------------------------
            // 3. CATEGORY SELECTION TABS (Skins, Bridges, Hats, Capes, Arenas, Vault)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShopCategory.values().forEach { cat ->
                    val isSelected = selectedTab == cat.type
                    val count = if (cat.type == AccessoryType.GEM_VAULT) {
                        viewModel.availableGemPacks.size
                    } else {
                        allItems.count { it.type == cat.type }
                    }

                    Surface(
                        onClick = {
                            viewModel.setShopTab(cat.type)
                            if (cat.type != AccessoryType.GEM_VAULT) {
                                previewedItem = when (cat.type) {
                                    AccessoryType.BODY_SKIN -> viewModel.getEquippedSkin()
                                    AccessoryType.STICK -> viewModel.getEquippedStick()
                                    AccessoryType.HAT -> viewModel.getEquippedHat()
                                    AccessoryType.SCARF -> viewModel.getEquippedScarf()
                                    AccessoryType.THEME -> viewModel.getEquippedTheme()
                                    else -> null
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B),
                        border = if (isSelected) BorderStroke(1.2.dp, Color(0xFF34D399)) else BorderStroke(0.8.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("shop_tab_${cat.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = cat.iconEmoji, fontSize = 14.sp)
                            Text(
                                text = cat.title,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF047857) else Color(0xFF0F172A)
                            ) {
                                Text(
                                    text = "$count",
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. SUB-FILTERS: Currency Filter + Rarity Filter Chips
            // -------------------------------------------------------------
            if (selectedTab != AccessoryType.GEM_VAULT) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Currency Filter Chips
                    val currencyFilters = listOf(
                        "ALL" to "All Currencies",
                        "STANDARD" to "💎 Gems",
                        "CONTEST_BLUE" to "🔷 Contest",
                        "TOURNAMENT_RED" to "🔴 Tourney"
                    )
                    currencyFilters.forEach { (key, label) ->
                        val isCurrSelected = shopCurrencyFilter == key
                        Surface(
                            onClick = { viewModel.setShopCurrencyFilter(key) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                            border = if (isCurrSelected) BorderStroke(1.dp, Color(0xFF60A5FA)) else null,
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("shop_currency_filter_${key.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isCurrSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrSelected) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Rarity Filter Chips
                    ItemRarity.values().forEach { rarity ->
                        val isRaritySelected = selectedRarityFilter == rarity
                        Surface(
                            onClick = {
                                selectedRarityFilter = if (isRaritySelected) null else rarity
                                viewModel.soundManager.playButton()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRaritySelected) Color(rarity.colorHex).copy(alpha = 0.35f) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (isRaritySelected) Color(rarity.colorHex) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("shop_rarity_filter_${rarity.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rarity.label,
                                    color = Color(rarity.colorHex),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -------------------------------------------------------------
            // 5. MAIN CONTENT AREA: LazyVerticalGrid FOR ITEMS or Gem Vault
            // -------------------------------------------------------------
            if (selectedTab == AccessoryType.GEM_VAULT) {
                GemVaultContent(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🔍", fontSize = 32.sp)
                            Text(
                                text = "No items match current filters",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    viewModel.setShopCurrencyFilter("ALL")
                                    selectedRarityFilter = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text("Reset Filters", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // LazyVerticalGrid for optimal 2-column or adaptive grid display
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_items_lazy_vertical_grid")
                    ) {
                        items(
                            items = filteredItems,
                            key = { it.id }
                        ) { item ->
                            val isUnlocked = viewModel.isItemUnlocked(item.id)
                            val isEquipped = when (item.type) {
                                AccessoryType.BODY_SKIN -> selectedSkinId == item.id
                                AccessoryType.STICK -> selectedStickId == item.id
                                AccessoryType.HAT -> selectedHatId == item.id
                                AccessoryType.SCARF -> selectedScarfId == item.id
                                AccessoryType.THEME -> selectedThemeId == item.id
                                AccessoryType.GEM_VAULT -> false
                            }
                            val isPreviewSelected = previewedItem?.id == item.id
                            val canAfford = when (item.currencyType) {
                                CurrencyType.GEM -> gems >= item.cost
                                CurrencyType.BLUE_GEM -> blueGems >= item.cost
                                CurrencyType.RED_GEM -> redGems >= item.cost
                            }

                            ShopItemCard(
                                item = item,
                                isUnlocked = isUnlocked,
                                isEquipped = isEquipped,
                                isPreviewSelected = isPreviewSelected,
                                canAfford = canAfford,
                                onClick = {
                                    previewedItem = item
                                    viewModel.soundManager.playButton()
                                    viewModel.hapticManager.uiClick()
                                },
                                onBuyOrEquip = {
                                    previewedItem = item
                                    viewModel.buyOrEquip(item)
                                },
                                onBuyRealMoney = {
                                    previewedItem = item
                                    viewModel.buyItemRealMoney(item)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -------------------------------------------------------------
            // 6. BOTTOM QUICK BOOST BAR: Daily Gem Bonus, Spin Wheel & Out of Gems
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDailyAvailable) {
                    Surface(
                        onClick = {
                            onClose()
                            viewModel.openDailyReward(true)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF065F46),
                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("shop_claim_daily_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🎁", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CLAIM DAILY GEMS",
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        onClose()
                        viewModel.openSpinWheel(true)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF581C87),
                    border = BorderStroke(1.dp, Color(0xFFA855F7)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("shop_spin_wheel_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LUCKY SPIN WHEEL",
                            color = Color(0xFFE9D5FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Currency Balance Badge component for Gem wallets.
 */
@Composable
private fun CurrencyBadge(
    symbol: String,
    amount: Int,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = symbol, fontSize = 12.sp)
            Text(
                text = "$amount",
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Live Hero Showcase Card at top of the Shop screen.
 */
@Composable
private fun HeroShowcaseCard(
    previewItem: AccessoryItem,
    activeHat: AccessoryItem,
    activeScarf: AccessoryItem,
    activeStick: AccessoryItem,
    activeSkin: AccessoryItem,
    previewTime: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B132B),
        border = BorderStroke(1.2.dp, Color(previewItem.rarity.badgeBgHex)),
        modifier = modifier
            .height(115.dp)
            .testTag("shop_hero_showcase")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Stickman Drawing Canvas
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(previewItem.primaryColor).copy(alpha = 0.4f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(1.2.dp, Color(previewItem.primaryColor).copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width * 0.44f
                    val groundY = size.height * 0.78f
                    val headRadius = 6.dp.toPx()
                    val headY = groundY - 24.dp.toPx()
                    val neckY = groundY - 18.dp.toPx()
                    val hipY = groundY - 9.dp.toPx()
                    val bodyCol = Color(activeSkin.primaryColor)

                    // Bridge Weapon / Staff Preview
                    val bridgeCol = Color(activeStick.primaryColor)
                    drawLine(
                        color = bridgeCol,
                        start = Offset(centerX + 16.dp.toPx(), groundY),
                        end = Offset(centerX + 16.dp.toPx(), groundY - 30.dp.toPx()),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(centerX + 16.dp.toPx(), groundY - 30.dp.toPx())
                    )

                    // Cape Flutter
                    val flutter = (sin((previewTime * 4f).toDouble()).toFloat()) * 2.5.dp.toPx()
                    drawLine(
                        color = Color(activeScarf.primaryColor),
                        start = Offset(centerX - 1.dp.toPx(), neckY),
                        end = Offset(centerX - 9.dp.toPx(), neckY + 12.dp.toPx() + flutter),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Torso
                    drawLine(
                        color = bodyCol,
                        start = Offset(centerX, neckY),
                        end = Offset(centerX, hipY),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Legs
                    drawLine(
                        color = bodyCol,
                        start = Offset(centerX, hipY),
                        end = Offset(centerX - 3.5.dp.toPx(), groundY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodyCol,
                        start = Offset(centerX, hipY),
                        end = Offset(centerX + 3.5.dp.toPx(), groundY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Arms
                    drawLine(
                        color = bodyCol,
                        start = Offset(centerX, neckY + 2.dp.toPx()),
                        end = Offset(centerX + 7.dp.toPx(), neckY + 8.dp.toPx()),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodyCol,
                        start = Offset(centerX, neckY + 2.dp.toPx()),
                        end = Offset(centerX - 5.dp.toPx(), neckY + 7.dp.toPx()),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Head
                    drawCircle(
                        color = bodyCol,
                        radius = headRadius,
                        center = Offset(centerX, headY)
                    )
                    // Eye
                    drawCircle(
                        color = Color.Black,
                        radius = 1.dp.toPx(),
                        center = Offset(centerX + 2.5.dp.toPx(), headY - 1.dp.toPx())
                    )

                    // Hat
                    val hatCol = Color(activeHat.primaryColor)
                    drawRoundRect(
                        color = hatCol,
                        topLeft = Offset(centerX - headRadius - 1.dp.toPx(), headY - headRadius * 0.5f),
                        size = Size((headRadius * 2f) + 2.dp.toPx(), 3.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Item Lore, Rarity & Attribute Specs
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Rarity Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(previewItem.rarity.badgeBgHex)
                    ) {
                        Text(
                            text = previewItem.rarity.label,
                            color = Color(previewItem.rarity.colorHex),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (previewItem.isContestExclusive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (previewItem.currencyType == CurrencyType.BLUE_GEM) Color(0xFF0284C7) else Color(0xFFE11D48)
                        ) {
                            Text(
                                text = if (previewItem.currencyType == CurrencyType.BLUE_GEM) "🔷 CONTEST" else "🔴 TOURNEY",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${previewItem.iconSymbol} PREVIEWING",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = previewItem.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )

                Text(
                    text = previewItem.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Individual Shop Item Card in LazyVerticalGrid.
 */
@Composable
private fun ShopItemCard(
    item: AccessoryItem,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isPreviewSelected: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit,
    onBuyOrEquip: () -> Unit,
    onBuyRealMoney: () -> Unit,
    modifier: Modifier = Modifier
) {
    val curSymbol = when (item.currencyType) {
        CurrencyType.GEM -> "💎"
        CurrencyType.BLUE_GEM -> "🔷"
        CurrencyType.RED_GEM -> "🔴"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isEquipped -> Color(0xFF1E3A8A).copy(alpha = 0.85f)
            isPreviewSelected -> Color(0xFF1E293B)
            else -> Color(0xFF0F172A)
        },
        border = BorderStroke(
            width = if (isEquipped || isPreviewSelected) 1.5.dp else 1.dp,
            color = when {
                isEquipped -> Color(0xFF38BDF8)
                isPreviewSelected -> Color(item.rarity.colorHex)
                else -> Color(0xFF1E293B)
            }
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("shop_item_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Top Row: Rarity Tag & Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(item.rarity.badgeBgHex)
                ) {
                    Text(
                        text = item.rarity.label,
                        color = Color(item.rarity.colorHex),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                if (item.isContestExclusive) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.currencyType == CurrencyType.BLUE_GEM) Color(0xFF0369A1) else Color(0xFF9F1239)
                    ) {
                        Text(
                            text = if (item.currencyType == CurrencyType.BLUE_GEM) "🔷 CONTEST" else "🔴 TOURNEY",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }

                if (isEquipped) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF38BDF8)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Glowing Icon Orb
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(item.primaryColor).copy(alpha = 0.5f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(1.5.dp, Color(item.primaryColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconSymbol, fontSize = 22.sp)
            }

            // Item Name
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Lore Description
            Text(
                text = item.description,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(26.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Action Button: Buy with Gems / Equip / Real Money Option
            if (!isUnlocked && item.realMoneyPriceUsd.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onBuyOrEquip,
                        enabled = canAfford,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (item.currencyType) {
                                CurrencyType.GEM -> Color(0xFFF59E0B)
                                CurrencyType.BLUE_GEM -> Color(0xFF0284C7)
                                CurrencyType.RED_GEM -> Color(0xFFE11D48)
                            },
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("shop_item_btn_${item.id}")
                    ) {
                        Text(
                            text = if (canAfford) "$curSymbol ${item.cost}" else "NEED $curSymbol",
                            color = if (canAfford) Color.White else Color(0xFF64748B),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.5.sp
                        )
                    }

                    Button(
                        onClick = onBuyRealMoney,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("shop_item_real_money_btn_${item.id}")
                    ) {
                        Text(
                            text = item.realMoneyPriceUsd,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onBuyOrEquip,
                    enabled = isUnlocked || canAfford,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isEquipped -> Color(0xFF38BDF8)
                            isUnlocked -> Color(0xFF10B981)
                            canAfford -> when (item.currencyType) {
                                CurrencyType.GEM -> Color(0xFFF59E0B)
                                CurrencyType.BLUE_GEM -> Color(0xFF0284C7)
                                CurrencyType.RED_GEM -> Color(0xFFE11D48)
                            }
                            else -> Color(0xFF334155)
                        },
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("shop_item_btn_${item.id}")
                ) {
                    Text(
                        text = when {
                            isEquipped -> "✓ EQUIPPED"
                            isUnlocked -> "EQUIP"
                            canAfford -> "$curSymbol ${item.cost}"
                            else -> "NEED ${item.cost} $curSymbol"
                        },
                        color = when {
                            isEquipped -> Color.Black
                            isUnlocked -> Color.White
                            canAfford -> Color.White
                            else -> Color(0xFF64748B)
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
