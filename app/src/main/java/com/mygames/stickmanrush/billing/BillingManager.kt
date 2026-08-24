package com.mygames.stickmanrush.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production-ready Google Play Billing Manager (Play Billing SDK 7.x)
 * Handles in-app purchases (IAP), consumables (Gems, Lives), non-consumables (VIP Pass, Ad Removal),
 * connection retries, purchase acknowledgement, and state updates.
 */
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // In-App Product IDs
        const val SKU_GEMS_TIER1 = "gem_pack_100"
        const val SKU_GEMS_TIER2 = "gem_pack_550"
        const val SKU_GEMS_TIER3 = "gem_pack_1200"
        const val SKU_GEMS_TIER4 = "gem_pack_3000"
        const val SKU_LIFE_PACK_10 = "pack_life_money_10"
        const val SKU_VIP_PASS = "vip_season_pass"
        const val SKU_REMOVE_ADS = "no_ads_forever"

        val IN_APP_SKUS = listOf(
            SKU_GEMS_TIER1,
            SKU_GEMS_TIER2,
            SKU_GEMS_TIER3,
            SKU_GEMS_TIER4,
            SKU_LIFE_PACK_10,
            SKU_VIP_PASS,
            SKU_REMOVE_ADS
        )
    }

    data class VerifiedPurchaseData(
        val productId: String,
        val purchaseToken: String,
        val signature: String,
        val originalJson: String,
        val orderId: String?
    )

    sealed class PurchaseEvent {
        data class Success(val productId: String, val purchaseToken: String) : PurchaseEvent()
        data class Failed(val errorCode: Int, val message: String) : PurchaseEvent()
        data object Cancelled : PurchaseEvent()
    }

    private var billingClient: BillingClient? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 64)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    private var onPurchaseSuccessCallback: ((purchaseData: VerifiedPurchaseData) -> Unit)? = null

    init {
        initializeBillingClient()
    }

    fun setOnPurchaseSuccessListener(listener: (purchaseData: VerifiedPurchaseData) -> Unit) {
        onPurchaseSuccessCallback = listener
    }

    private fun initializeBillingClient() {
        try {
            val pendingParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()

            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(pendingParams)
                .build()

            startBillingConnection()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize BillingClient", e)
        }
    }

    fun startBillingConnection() {
        val client = billingClient ?: return
        if (client.isReady) {
            _isConnected.value = true
            queryProducts()
            return
        }

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing connected successfully.")
                    _isConnected.value = true
                    queryProducts()
                    queryExistingPurchases()
                } else {
                    Log.w(TAG, "Google Play Billing setup failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Google Play Billing service disconnected.")
                _isConnected.value = false
            }
        })
    }

    private fun queryProducts() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val productList = IN_APP_SKUS.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.d(TAG, "Loaded ${map.size} products from Google Play Billing.")
            } else {
                Log.w(TAG, "Query Product Details failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryExistingPurchases() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    /**
     * Launch the Google Play Billing purchase sheet for a given product ID.
     * Returns true if Google Play checkout UI was launched, false otherwise.
     * Does NEVER grant free items on failure.
     */
    fun launchBillingFlow(
        activity: Activity,
        productId: String,
        onError: ((String) -> Unit)? = null
    ): Boolean {
        val client = billingClient
        val details = _productDetailsMap.value[productId]

        if (client != null && client.isReady && details != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val result = client.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                val errorMsg = result.debugMessage ?: "Billing launch failed (Code ${result.responseCode})"
                Log.w(TAG, "Launch billing flow error: $errorMsg")
                onError?.invoke(errorMsg)
                coroutineScope.launch {
                    _purchaseEvents.emit(PurchaseEvent.Failed(result.responseCode, errorMsg))
                }
                return false
            }
            return true
        } else {
            val errorMsg = "Google Play Store service is connecting. Please try again in a moment."
            Log.w(TAG, "BillingClient not ready (client=$client, isReady=${client?.isReady}) or product details not loaded for $productId.")
            onError?.invoke(errorMsg)
            startBillingConnection()
            coroutineScope.launch {
                _purchaseEvents.emit(PurchaseEvent.Failed(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, errorMsg))
            }
            return false
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled the purchase.")
                coroutineScope.launch {
                    _purchaseEvents.emit(PurchaseEvent.Cancelled)
                }
            }
            else -> {
                Log.w(TAG, "Purchase failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                coroutineScope.launch {
                    _purchaseEvents.emit(
                        PurchaseEvent.Failed(billingResult.responseCode, billingResult.debugMessage ?: "Purchase error")
                    )
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val products = purchase.products
        for (productId in products) {
            Log.d(TAG, "Processing purchase for product: $productId, token: ${purchase.purchaseToken.take(12)}...")
            
            val purchaseData = VerifiedPurchaseData(
                productId = productId,
                purchaseToken = purchase.purchaseToken,
                signature = purchase.signature,
                originalJson = purchase.originalJson,
                orderId = purchase.orderId
            )

            // Dispatch grant to secure verification callback
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    onPurchaseSuccessCallback?.invoke(purchaseData)
                    _purchaseEvents.emit(PurchaseEvent.Success(productId, purchase.purchaseToken))
                }
            }

            // Consumable vs Non-Consumable Handling
            val isConsumable = productId.startsWith("gem_pack_") || productId.startsWith("pack_life_")
            if (isConsumable) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.consumeAsync(consumeParams) { result, _ ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Successfully consumed purchase: $productId")
                    }
                }
            } else {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient?.acknowledgePurchase(ackParams) { result ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Successfully acknowledged purchase: $productId")
                        }
                    }
                }
            }
        }
    }

    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
    }
}
