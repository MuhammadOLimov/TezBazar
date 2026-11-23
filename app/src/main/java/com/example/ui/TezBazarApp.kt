package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.firebase.AiRecommendationRepository
import com.example.data.firebase.MarketplaceRepository
import com.example.data.firebase.signOutUser
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.LanguagePreferences
import com.example.i18n.StringsRepository
import com.example.ui.components.ProductDetailBottomSheet
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TrackingScreen
import com.example.ui.theme.TezBazarTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AppSettingsViewModel
import com.example.ui.viewmodel.MarketplaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_AI_HUB = "ai_hub"
const val ROUTE_TRACKING = "tracking"
const val ROUTE_CART = "cart"
const val ROUTE_PROFILE = "profile"

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun TezBazarApp() {
    val settingsViewModel: AppSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                AppSettingsViewModel(LanguagePreferences(app.applicationContext))
            }
        }
    )

    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val selectedDistrict by settingsViewModel.selectedDistrict.collectAsStateWithLifecycle()
    val favoriteProductIds by settingsViewModel.favoriteProductIds.collectAsStateWithLifecycle()

    TezBazarTheme(themeMode = themeMode) {
        var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
        DisposableEffect(Unit) {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose { Firebase.auth.removeAuthStateListener(listener) }
        }

        val user = currentUser
        if (user == null) {
            AuthScreen(
                language = language,
                onSelectLanguage = { settingsViewModel.setLanguage(it) },
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        } else {
            val uid = user.uid
            key(uid) {
                AuthenticatedMarketplaceShell(
                    currentUserId = uid,
                    userDisplayName = user.displayName ?: "TezBazar User",
                    userEmail = user.email.orEmpty(),
                    language = language,
                    themeMode = themeMode,
                    selectedDistrict = selectedDistrict,
                    favoriteProductIds = favoriteProductIds,
                    onSelectLanguage = { settingsViewModel.setLanguage(it) },
                    onSelectThemeMode = { settingsViewModel.setThemeMode(it) },
                    onSelectDistrict = { settingsViewModel.setDistrict(it) },
                    onToggleFavorite = { settingsViewModel.toggleFavorite(it) }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedMarketplaceShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    language: AppLanguage,
    themeMode: ThemeMode,
    selectedDistrict: String,
    favoriteProductIds: Set<String>,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: MarketplaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app: Application = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MarketplaceViewModel(
                    repository = MarketplaceRepository(db),
                    aiRepository = AiRecommendationRepository(app.applicationContext),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail
                )
            }
        }
    )

    LaunchedEffect(currentUserId) {
        viewModel.initializeSession(language, selectedDistrict)
    }

    LaunchedEffect(favoriteProductIds) {
        viewModel.syncFavorites(favoriteProductIds)
    }

    val strings = remember(language) { StringsRepository.get(language) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val addressesState by viewModel.addressesState.collectAsStateWithLifecycle()
    val cardsState by viewModel.cardsState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val totalMatchingCount by viewModel.totalMatchingCount.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val aiRecommendationsState by viewModel.aiRecommendationsState.collectAsStateWithLifecycle()
    val aiVisualSearchState by viewModel.aiVisualSearchState.collectAsStateWithLifecycle()
    val aiVoiceIntentState by viewModel.aiVoiceIntentState.collectAsStateWithLifecycle()
    val aiChatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val aiChatState by viewModel.aiChatState.collectAsStateWithLifecycle()
    val promoStatus by viewModel.promoStatus.collectAsStateWithLifecycle()
    val isPlacingOrder by viewModel.isPlacingOrder.collectAsStateWithLifecycle()
    val autoTrackingEnabled by viewModel.autoTrackingEnabled.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val cartItems = (cartState as? UiState.Success)?.data ?: emptyList()
    val cartTotalBadge = remember(cartItems) { cartItems.sumOf { it.quantity }.toInt() }
    val activeOrderCount = remember(ordersState) {
        val list = (ordersState as? UiState.Success)?.data ?: emptyList()
        list.count { it.parsedStatus.stepIndex in 0..3 }
    }

    var selectedDetailProduct by remember { mutableStateOf<Product?>(null) }

    val navItems = remember(strings) {
        listOf(
            NavTabItem(ROUTE_HOME, strings.navMarket, Icons.Rounded.Storefront, Icons.Outlined.Storefront),
            NavTabItem(ROUTE_AI_HUB, strings.navAiHub, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
            NavTabItem(ROUTE_TRACKING, strings.navTrack, Icons.Rounded.LocalShipping, Icons.Outlined.LocalShipping),
            NavTabItem(ROUTE_CART, strings.navCart, Icons.Rounded.ShoppingBag, Icons.Outlined.ShoppingBag),
            NavTabItem(ROUTE_PROFILE, strings.navProfile, Icons.Rounded.Person, Icons.Outlined.Person)
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        val isWide = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (isCompact) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    val badgeCount = when (tab.route) {
                                        ROUTE_CART -> cartTotalBadge
                                        ROUTE_TRACKING -> activeOrderCount
                                        else -> 0
                                    }
                                    if (badgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isCompact) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    val badgeCount = when (tab.route) {
                                        ROUTE_CART -> cartTotalBadge
                                        ROUTE_TRACKING -> activeOrderCount
                                        else -> 0
                                    }
                                    if (badgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            language = language,
                            strings = strings,
                            selectedDistrict = selectedDistrict,
                            products = filteredProducts,
                            totalMatchingCount = totalMatchingCount,
                            selectedCategoryId = selectedCategoryId,
                            favoritesOnly = favoritesOnly,
                            favoriteProductIds = favoriteProductIds,
                            searchQuery = searchQuery,
                            cartItems = cartItems,
                            aiState = aiRecommendationsState,
                            isLoadingMore = isLoadingMore,
                            isWideScreen = isWide,
                            selectedDetailProduct = selectedDetailProduct ?: filteredProducts.firstOrNull(),
                            onSelectLanguage = { newLang ->
                                onSelectLanguage(newLang)
                                viewModel.syncUserSettings(newLang, selectedDistrict)
                                viewModel.refreshAiRecommendations(newLang, selectedDistrict)
                            },
                            onSelectDistrict = { newDistrict ->
                                onSelectDistrict(newDistrict)
                                viewModel.syncUserSettings(language, newDistrict)
                                viewModel.refreshAiRecommendations(language, newDistrict)
                            },
                            onSelectCategory = { viewModel.selectCategory(it) },
                            onToggleFavoritesFilter = { viewModel.toggleFavoritesFilter() },
                            onToggleFavorite = onToggleFavorite,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onAiSearchQuery = { query ->
                                viewModel.analyzeVoiceOrTextIntent(query, language)
                                navigateToTab(ROUTE_AI_HUB)
                            },
                            onRefreshAi = { viewModel.refreshAiRecommendations(language, selectedDistrict) },
                            onAddBundleToCart = { list ->
                                viewModel.addBundleToCart(list, strings.aiAddBundleBtn)
                            },
                            onIncrementProduct = { viewModel.incrementProduct(it, cartItems) },
                            onDecrementProduct = { viewModel.decrementProduct(it, cartItems) },
                            onProductClick = { selectedDetailProduct = it },
                            onLoadMore = { viewModel.loadMoreCatalog() },
                            onOpenAiVoice = { navigateToTab(ROUTE_AI_HUB) },
                            onOpenAiPhoto = { navigateToTab(ROUTE_AI_HUB) }
                        )
                    }

                    composable(ROUTE_AI_HUB) {
                        AiAssistantScreen(
                            language = language,
                            strings = strings,
                            visualSearchState = aiVisualSearchState,
                            voiceIntentState = aiVoiceIntentState,
                            chatMessages = aiChatMessages,
                            chatState = aiChatState,
                            onAnalyzeImage = { bmp -> viewModel.analyzeImageForSearch(bmp, language) },
                            onAnalyzeVoiceIntent = { q -> viewModel.analyzeVoiceOrTextIntent(q, language) },
                            onAnalyzeAudio = { bytes -> viewModel.analyzeAudioForSearch(bytes, language) },
                            onSendChatMessage = { msg -> viewModel.sendAiChatMessage(msg, language, selectedDistrict) },
                            onClearChat = { viewModel.clearAiChat() },
                            onQuickAddProduct = { p -> viewModel.incrementProduct(p, cartItems) },
                            onAddBundleToCart = { list -> viewModel.addBundleToCart(list, strings.aiAddBundleBtn) },
                            onProductClick = { selectedDetailProduct = it }
                        )
                    }

                    composable(ROUTE_TRACKING) {
                        TrackingScreen(
                            language = language,
                            strings = strings,
                            ordersState = ordersState,
                            autoSimulate = autoTrackingEnabled,
                            onToggleAutoSimulate = { viewModel.toggleAutoTracking(it) },
                            onAdvanceStage = { viewModel.advanceOrderStage(it) },
                            onReorder = { order -> viewModel.reorderItems(order, strings.addToCartBtn) },
                            onBrowseMarket = { navigateToTab(ROUTE_HOME) }
                        )
                    }

                    composable(ROUTE_CART) {
                        CartCheckoutScreen(
                            language = language,
                            strings = strings,
                            selectedDistrict = selectedDistrict,
                            cartState = cartState,
                            addressesState = addressesState,
                            cardsState = cardsState,
                            catalog = catalog,
                            promoStatus = promoStatus,
                            isPlacingOrder = isPlacingOrder,
                            calculateTotals = { items, isPvz -> viewModel.calculateCheckoutTotals(items, isPvz) },
                            onUpdateQuantity = { p, q -> viewModel.changeCartQuantity(p, q) },
                            onApplyPromo = { code ->
                                viewModel.applyPromoCode(code, strings.promoAppliedMsg, strings.promoInvalidMsg)
                            },
                            onAddAddress = { lbl, dist, st, ent, lat, lng ->
                                viewModel.addAddress(lbl, dist, st, ent, lat, lng)
                            },
                            onDeleteAddress = { viewModel.deleteAddress(it) },
                            onAddPaymentCard = { d, exp, h, ok, err -> viewModel.addPaymentCard(d, exp, h, ok, err) },
                            onPlaceOrder = { items, payMethod, last4, dist, addr, deliveryType, pvzName ->
                                viewModel.placeOrder(
                                    cartItems = items,
                                    paymentMethod = payMethod,
                                    cardLastFour = last4,
                                    district = dist,
                                    addressLine = addr,
                                    deliveryType = deliveryType,
                                    pickupPointName = pvzName,
                                    language = language,
                                    successMsg = strings.orderSuccessMsg
                                ) {
                                    navigateToTab(ROUTE_TRACKING)
                                }
                            },
                            onBrowseMarket = { navigateToTab(ROUTE_HOME) }
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            language = language,
                            themeMode = themeMode,
                            strings = strings,
                            selectedDistrict = selectedDistrict,
                            userName = userDisplayName,
                            userEmail = userEmail,
                            userProfileState = userProfileState,
                            addressesState = addressesState,
                            cardsState = cardsState,
                            onSelectLanguage = { newLang ->
                                onSelectLanguage(newLang)
                                viewModel.syncUserSettings(newLang, selectedDistrict)
                                viewModel.refreshAiRecommendations(newLang, selectedDistrict)
                            },
                            onSelectThemeMode = onSelectThemeMode,
                            onAddAddress = { lbl, dist, st, ent, lat, lng ->
                                viewModel.addAddress(lbl, dist, st, ent, lat, lng)
                            },
                            onDeleteAddress = { viewModel.deleteAddress(it) },
                            onAddCard = { d, exp, h, ok, err -> viewModel.addPaymentCard(d, exp, h, ok, err) },
                            onDeleteCard = { viewModel.deletePaymentCard(it) },
                            onSignOut = {
                                viewModel.toggleAutoTracking(false)
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }
                }
            }
        }

        // Compact & Medium Product Detail Modal Sheet
        if (!isWide && selectedDetailProduct != null) {
            val prod = selectedDetailProduct!!
            val qty = cartItems.firstOrNull { it.productId == prod.id }?.quantity?.toInt() ?: 0
            ProductDetailBottomSheet(
                product = prod,
                quantityInCart = qty,
                language = language,
                strings = strings,
                isFavorite = prod.id in favoriteProductIds,
                onToggleFavorite = { onToggleFavorite(prod.id) },
                onDismiss = { selectedDetailProduct = null },
                onIncrement = { viewModel.incrementProduct(prod, cartItems) },
                onDecrement = { viewModel.decrementProduct(prod, cartItems) }
            )
        }
    }
}
