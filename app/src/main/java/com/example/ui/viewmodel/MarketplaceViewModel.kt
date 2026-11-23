package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiRecommendationBundle
import com.example.data.firebase.AiRecommendationRepository
import com.example.data.firebase.AiSearchResult
import com.example.data.firebase.MarketplaceRepository
import com.example.data.model.AiTasteProfile
import com.example.data.model.CartEntry
import com.example.data.model.CatalogSeedData
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.Product
import com.example.data.model.SavedAddress
import com.example.data.model.SavedPaymentCard
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val suggestedProducts: List<Product> = emptyList()
)

sealed interface AiChatUiState {
    data object Idle : AiChatUiState
    data object Streaming : AiChatUiState
    data class Error(val message: String) : AiChatUiState
}

class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    private val aiRepository: AiRecommendationRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String
) : ViewModel() {

    private companion object {
        const val TAG = "MarketplaceVM"
        const val STOP_TIMEOUT_MS = 5000L
    }

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .toUiStateFlow("observeUserProfile")

    val cartState: StateFlow<UiState<List<CartEntry>>> = repository.observeCart(currentUserId)
        .toUiStateFlow("observeCart")

    val ordersState: StateFlow<UiState<List<OrderRecord>>> = repository.observeOrders(currentUserId)
        .toUiStateFlow("observeOrders")

    val addressesState: StateFlow<UiState<List<SavedAddress>>> = repository.observeAddresses(currentUserId)
        .toUiStateFlow("observeAddresses")

    val cardsState: StateFlow<UiState<List<SavedPaymentCard>>> = repository.observePaymentCards(currentUserId)
        .toUiStateFlow("observePaymentCards")

    val aiProfileState: StateFlow<UiState<AiTasteProfile?>> = repository.observeAiProfile(currentUserId)
        .toUiStateFlow("observeAiProfile")

    private val _catalog = MutableStateFlow(CatalogSeedData.products)
    val catalog: StateFlow<List<Product>> = _catalog.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _favoriteIdsFilter = MutableStateFlow<Set<String>>(emptySet())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _visibleLimit = MutableStateFlow(12)
    private var lastRemoteCursor: DocumentSnapshot? = null
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        _catalog,
        _selectedCategoryId,
        _searchQuery,
        _visibleLimit,
        _favoritesOnly,
        _favoriteIdsFilter
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val products = values[0] as List<Product>
        val catId = values[1] as String?
        val query = values[2] as String
        val limit = values[3] as Int
        val favOnly = values[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val favSet = values[5] as Set<String>
        val q = query.trim().lowercase()
        val matched = products.filter { p ->
            val catMatch = catId == null || p.categoryId == catId
            val favMatch = !favOnly || p.id in favSet
            val queryMatch = q.isEmpty() ||
                p.titleUz.lowercase().contains(q) ||
                p.titleRu.lowercase().contains(q) ||
                p.titleEn.lowercase().contains(q) ||
                p.tags.any { it.lowercase().contains(q) }
            catMatch && favMatch && queryMatch
        }
        matched.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CatalogSeedData.products.take(12))

    val totalMatchingCount: StateFlow<Int> = combine(
        _catalog,
        _selectedCategoryId,
        _searchQuery,
        _favoritesOnly,
        _favoriteIdsFilter
    ) { products, catId, query, favOnly, favSet ->
        val q = query.trim().lowercase()
        products.count { p ->
            val catMatch = catId == null || p.categoryId == catId
            val favMatch = !favOnly || p.id in favSet
            val queryMatch = q.isEmpty() ||
                p.titleUz.lowercase().contains(q) ||
                p.titleRu.lowercase().contains(q) ||
                p.titleEn.lowercase().contains(q) ||
                p.tags.any { it.lowercase().contains(q) }
            catMatch && favMatch && queryMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CatalogSeedData.products.size)

    private val _aiRecommendationsState = MutableStateFlow<UiState<AiRecommendationBundle>>(UiState.Loading)
    val aiRecommendationsState: StateFlow<UiState<AiRecommendationBundle>> = _aiRecommendationsState.asStateFlow()

    private val _aiVisualSearchState = MutableStateFlow<UiState<AiSearchResult>?>(null)
    val aiVisualSearchState: StateFlow<UiState<AiSearchResult>?> = _aiVisualSearchState.asStateFlow()

    private val _aiVoiceIntentState = MutableStateFlow<UiState<AiSearchResult>?>(null)
    val aiVoiceIntentState: StateFlow<UiState<AiSearchResult>?> = _aiVoiceIntentState.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _aiChatState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val aiChatState: StateFlow<AiChatUiState> = _aiChatState.asStateFlow()

    private val _promoCode = MutableStateFlow("")
    val promoCode: StateFlow<String> = _promoCode.asStateFlow()

    private val _promoStatus = MutableStateFlow<String?>(null)
    val promoStatus: StateFlow<String?> = _promoStatus.asStateFlow()

    private val _appliedPromoRate = MutableStateFlow(0.0)
    private val _appliedPromoFlat = MutableStateFlow(0L)
    private val _appliedFreeDelivery = MutableStateFlow(false)

    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _autoTrackingEnabled = MutableStateFlow(false)
    val autoTrackingEnabled: StateFlow<Boolean> = _autoTrackingEnabled.asStateFlow()
    private var autoTrackJob: Job? = null

    private var hasLoadedInitialAi = false

    fun initializeSession(language: AppLanguage, district: String) {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userDisplayName, userEmail, language, district)
            val (remote, cursor) = repository.fetchCatalogOverlay(null, 20L, null)
            if (remote.isNotEmpty()) {
                lastRemoteCursor = cursor
                val existingIds = _catalog.value.map { it.id }.toSet()
                val merged = _catalog.value + remote.filterNot { it.id in existingIds }
                _catalog.value = merged
            }
            if (!hasLoadedInitialAi) {
                hasLoadedInitialAi = true
                refreshAiRecommendations(language, district, forceRefresh = false)
            }
        }
    }

    fun syncUserSettings(language: AppLanguage, district: String) {
        viewModelScope.launch {
            repository.updateUserLanguageAndDistrict(currentUserId, language, district)
        }
    }

    fun syncFavorites(favoriteIds: Set<String>) {
        _favoriteIdsFilter.value = favoriteIds
    }

    fun toggleFavoritesFilter() {
        _favoritesOnly.value = !_favoritesOnly.value
        _visibleLimit.value = 12
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        _visibleLimit.value = 12
        if (categoryId != null) {
            viewModelScope.launch {
                repository.recordAiSignal(currentUserId, categoryId, null)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _visibleLimit.value = 12
    }

    fun loadMoreCatalog() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            _visibleLimit.value = _visibleLimit.value + 12
            val (remote, cursor) = repository.fetchCatalogOverlay(_selectedCategoryId.value, 20L, lastRemoteCursor)
            if (remote.isNotEmpty()) {
                lastRemoteCursor = cursor
                val existingIds = _catalog.value.map { it.id }.toSet()
                _catalog.value = _catalog.value + remote.filterNot { it.id in existingIds }
            }
            _isLoadingMore.value = false
        }
    }

    fun changeCartQuantity(product: Product, newQuantity: Int) {
        viewModelScope.launch {
            val result = repository.upsertCartItem(currentUserId, product, newQuantity)
            result.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update cart"
            }
        }
    }

    fun incrementProduct(product: Product, currentCart: List<CartEntry>) {
        val currentQty = currentCart.firstOrNull { it.productId == product.id }?.quantity?.toInt() ?: 0
        changeCartQuantity(product, currentQty + 1)
    }

    fun decrementProduct(product: Product, currentCart: List<CartEntry>) {
        val currentQty = currentCart.firstOrNull { it.productId == product.id }?.quantity?.toInt() ?: 0
        changeCartQuantity(product, (currentQty - 1).coerceAtLeast(0))
    }

    fun addBundleToCart(products: List<Product>, successNotice: String) {
        viewModelScope.launch {
            val res = repository.addBundleToCart(currentUserId, products)
            res.onSuccess {
                _snackbarMessage.value = successNotice
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to add bundle"
            }
        }
    }

    fun refreshAiRecommendations(language: AppLanguage, district: String, forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _aiRecommendationsState.value = UiState.Loading
            val currentCart = (cartState.value as? UiState.Success)?.data ?: emptyList()
            val profile = (aiProfileState.value as? UiState.Success)?.data
            val res = aiRepository.getSmartRecommendations(
                catalog = _catalog.value,
                cartItems = currentCart,
                tasteProfile = profile,
                favoriteIds = _favoriteIdsFilter.value,
                district = district,
                language = language,
                forceRefresh = forceRefresh
            )
            _aiRecommendationsState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { err ->
                    UiState.Error(err.localizedMessage ?: "AI recommendation error")
                }
            )
        }
    }

    fun analyzeImageForSearch(bitmap: Bitmap, language: AppLanguage) {
        viewModelScope.launch {
            _aiVisualSearchState.value = UiState.Loading
            val res = aiRepository.searchByImage(bitmap, _catalog.value, language)
            _aiVisualSearchState.value = res.fold(
                onSuccess = { result ->
                    viewModelScope.launch {
                        repository.recordAiSignal(currentUserId, null, result.detectedConcept)
                    }
                    UiState.Success(result)
                },
                onFailure = { err ->
                    UiState.Error(err.localizedMessage ?: "Visual search error")
                }
            )
        }
    }

    fun analyzeVoiceOrTextIntent(query: String, language: AppLanguage) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _aiVoiceIntentState.value = UiState.Loading
            repository.recordAiSignal(currentUserId, null, query)
            val res = aiRepository.interpretNaturalIntent(query, _catalog.value, language)
            _aiVoiceIntentState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { err -> UiState.Error(err.localizedMessage ?: "Voice intent error") }
            )
        }
    }

    fun analyzeAudioForSearch(audioBytes: ByteArray, language: AppLanguage) {
        if (audioBytes.isEmpty()) return
        viewModelScope.launch {
            _aiVoiceIntentState.value = UiState.Loading
            val res = aiRepository.searchByAudio(audioBytes, "audio/mp4", _catalog.value, language)
            _aiVoiceIntentState.value = res.fold(
                onSuccess = { result ->
                    viewModelScope.launch {
                        if (result.detectedConcept.isNotBlank()) {
                            repository.recordAiSignal(currentUserId, null, result.detectedConcept)
                        }
                    }
                    UiState.Success(result)
                },
                onFailure = { err -> UiState.Error(err.localizedMessage ?: "Audio analysis error") }
            )
        }
    }

    fun clearAiChat() {
        if (_aiChatState.value !is AiChatUiState.Streaming) {
            _aiChatMessages.value = emptyList()
            _aiChatState.value = AiChatUiState.Idle
        }
    }

    fun sendAiChatMessage(text: String, language: AppLanguage, district: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _aiChatState.value is AiChatUiState.Streaming) return

        val priorHistory = _aiChatMessages.value
        val userMsgId = "u_${System.currentTimeMillis()}"
        val aiMsgId = "a_${System.currentTimeMillis() + 1}"
        _aiChatMessages.value = priorHistory +
            AiChatMessage(userMsgId, isUser = true, text = trimmed) +
            AiChatMessage(aiMsgId, isUser = false, text = "", isStreaming = true)
        _aiChatState.value = AiChatUiState.Streaming

        viewModelScope.launch {
            try {
                var finalResponse = ""
                aiRepository.streamShoppingAssistant(trimmed, priorHistory, _catalog.value, district, language)
                    .collect { partial ->
                        finalResponse = partial
                        _aiChatMessages.value = _aiChatMessages.value.map { msg ->
                            if (msg.id == aiMsgId) msg.copy(text = partial, isStreaming = true) else msg
                        }
                    }
                val suggested = aiRepository.matchCatalogProductsInResponse(finalResponse, _catalog.value, language)
                _aiChatMessages.value = _aiChatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) msg.copy(isStreaming = false, suggestedProducts = suggested) else msg
                }
                _aiChatState.value = AiChatUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI chat streaming failed", e)
                _aiChatMessages.value = _aiChatMessages.value.filterNot { it.id == aiMsgId && it.text.isBlank() }
                _aiChatState.value = AiChatUiState.Error(aiRepository.friendlyErrorMessage(e, language))
            }
        }
    }

    fun applyPromoCode(codeInput: String, appliedMsg: String, invalidMsg: String) {
        val code = codeInput.trim().uppercase()
        _promoCode.value = code
        when (code) {
            "TEZ2026" -> {
                _appliedPromoRate.value = 0.15
                _appliedPromoFlat.value = 0L
                _appliedFreeDelivery.value = false
                _promoStatus.value = appliedMsg
            }
            "SALOM10" -> {
                _appliedPromoRate.value = 0.0
                _appliedPromoFlat.value = 10000L
                _appliedFreeDelivery.value = false
                _promoStatus.value = appliedMsg
            }
            "UZUMFREE", "TEZFREE" -> {
                _appliedPromoRate.value = 0.0
                _appliedPromoFlat.value = 0L
                _appliedFreeDelivery.value = true
                _promoStatus.value = appliedMsg
            }
            else -> {
                _appliedPromoRate.value = 0.0
                _appliedPromoFlat.value = 0L
                _appliedFreeDelivery.value = false
                _promoStatus.value = invalidMsg
            }
        }
    }

    fun calculateCheckoutTotals(cartItems: List<CartEntry>, isPickupPoint: Boolean = false): Triple<Long, Long, Long> {
        val subtotal = cartItems.sumOf { it.unitPriceUzs * it.quantity }
        val baseDelivery = if (isPickupPoint || subtotal >= 120_000L || _appliedFreeDelivery.value || subtotal == 0L) 0L else 12_000L
        val discount = ((subtotal * _appliedPromoRate.value).toLong() + _appliedPromoFlat.value)
            .coerceAtMost(subtotal)
        return Triple(subtotal, baseDelivery, discount)
    }

    fun placeOrder(
        cartItems: List<CartEntry>,
        paymentMethod: PaymentMethodType,
        cardLastFour: String?,
        district: String,
        addressLine: String,
        deliveryType: String,
        pickupPointName: String?,
        language: AppLanguage,
        successMsg: String,
        onOrderCreated: (String) -> Unit
    ) {
        if (_isPlacingOrder.value) return
        viewModelScope.launch {
            _isPlacingOrder.value = true
            val isPvz = deliveryType == "PICKUP_POINT"
            val (subtotal, delivery, discount) = calculateCheckoutTotals(cartItems, isPvz)
            val res = repository.placeOrder(
                userId = currentUserId,
                cartItems = cartItems,
                subtotalUzs = subtotal,
                deliveryFeeUzs = delivery,
                discountUzs = discount,
                paymentMethod = paymentMethod,
                cardLastFour = cardLastFour,
                district = district,
                addressLine = addressLine,
                language = language,
                deliveryType = deliveryType,
                pickupPointName = pickupPointName
            )
            _isPlacingOrder.value = false
            res.onSuccess { orderId ->
                _promoCode.value = ""
                _promoStatus.value = null
                _appliedPromoRate.value = 0.0
                _appliedPromoFlat.value = 0L
                _appliedFreeDelivery.value = false
                _snackbarMessage.value = successMsg
                onOrderCreated(orderId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Order failed"
            }
        }
    }

    fun advanceOrderStage(order: OrderRecord) {
        viewModelScope.launch {
            val res = repository.advanceOrderStage(currentUserId, order)
            res.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update order"
            }
        }
    }

    fun toggleAutoTracking(enabled: Boolean) {
        _autoTrackingEnabled.value = enabled
        autoTrackJob?.cancel()
        if (enabled) {
            autoTrackJob = viewModelScope.launch {
                while (true) {
                    delay(4500L)
                    val orders = (ordersState.value as? UiState.Success)?.data ?: emptyList()
                    val active = orders.firstOrNull {
                        it.parsedStatus != OrderStatus.DELIVERED && it.parsedStatus != OrderStatus.CANCELLED
                    }
                    if (active == null) {
                        _autoTrackingEnabled.value = false
                        break
                    }
                    repository.advanceOrderStage(currentUserId, active)
                }
            }
        }
    }

    fun reorderItems(order: OrderRecord, notice: String) {
        viewModelScope.launch {
            val matching = _catalog.value.filter { p ->
                order.itemsSummary.contains(p.titleUz, ignoreCase = true) ||
                    order.itemsSummary.contains(p.titleRu, ignoreCase = true) ||
                    order.itemsSummary.contains(p.titleEn, ignoreCase = true)
            }.ifEmpty { _catalog.value.take(2) }
            val res = repository.addBundleToCart(currentUserId, matching)
            res.onSuccess { _snackbarMessage.value = notice }
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun addAddress(
        label: String,
        district: String,
        street: String,
        entrance: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            val res = repository.addAddress(currentUserId, label, district, street, entrance, latitude, longitude)
            res.onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repository.deleteAddress(currentUserId, addressId)
        }
    }

    fun addPaymentCard(digits: String, expiry: String, holder: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.addPaymentCard(currentUserId, digits, expiry, holder)
            res.onSuccess { onSuccess() }
                .onFailure { onError(it.localizedMessage ?: "Invalid card") }
        }
    }

    fun deletePaymentCard(cardId: String) {
        viewModelScope.launch {
            repository.deletePaymentCard(currentUserId, cardId)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error in $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    override fun onCleared() {
        super.onCleared()
        autoTrackJob?.cancel()
    }
}
