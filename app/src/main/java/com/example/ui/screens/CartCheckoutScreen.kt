package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CartEntry
import com.example.data.model.PaymentMethodType
import com.example.data.model.Product
import com.example.data.model.SavedAddress
import com.example.data.model.SavedPaymentCard
import com.example.data.model.UiState
import com.example.data.model.UzbekistanGeoCatalog
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.i18n.StringsRepository
import com.example.ui.components.AddressMapPicker
import com.example.ui.components.ProductVisualArtwork
import com.example.ui.components.UzcardHumoCardVisual
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PriceTextStyle
import java.util.Locale

@Composable
fun CartCheckoutScreen(
    language: AppLanguage,
    strings: LocalizedStrings,
    selectedDistrict: String,
    cartState: UiState<List<CartEntry>>,
    addressesState: UiState<List<SavedAddress>>,
    cardsState: UiState<List<SavedPaymentCard>>,
    catalog: List<Product>,
    promoStatus: String?,
    isPlacingOrder: Boolean,
    calculateTotals: (List<CartEntry>, Boolean) -> Triple<Long, Long, Long>,
    onUpdateQuantity: (Product, Int) -> Unit,
    onApplyPromo: (String) -> Unit,
    onAddAddress: (String, String, String, String?, Double?, Double?) -> Unit,
    onDeleteAddress: ((String) -> Unit)? = null,
    onAddPaymentCard: (String, String, String, () -> Unit, (String) -> Unit) -> Unit,
    onPlaceOrder: (List<CartEntry>, PaymentMethodType, String?, String, String, String, String?) -> Unit,
    onBrowseMarket: () -> Unit
) {
    when (cartState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = cartState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        is UiState.Success -> {
            val cartItems = cartState.data
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingBag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(strings.cartEmptyTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.cartEmptyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onBrowseMarket,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(50.dp).testTag("cart_browse_btn")
                        ) {
                            Text(strings.browseMarketBtn)
                        }
                    }
                }
            } else {
                val addresses = (addressesState as? UiState.Success)?.data ?: emptyList()
                val cards = (cardsState as? UiState.Success)?.data ?: emptyList()
                val catalogMap = remember(catalog) { catalog.associateBy { it.id } }

                var promoInput by remember { mutableStateOf("") }
                var deliveryType by remember { mutableStateOf("COURIER") } // "COURIER" or "PICKUP_POINT"
                val pvzList = remember(selectedDistrict) {
                    UzbekistanGeoCatalog.pickupPointsFor(selectedDistrict)
                }
                var selectedPvz by remember(pvzList) { mutableStateOf(pvzList.first()) }
                var selectedAddressId by remember(addresses) { mutableStateOf(addresses.firstOrNull()?.id) }
                var customAddressLine by remember(selectedDistrict, language) {
                    mutableStateOf(UzbekistanGeoCatalog.findCity(selectedDistrict).localizedStreet(language))
                }
                var selectedCardId by remember(cards) { mutableStateOf(cards.firstOrNull()?.id) }
                var useCash by remember(cards) { mutableStateOf(cards.isEmpty()) }

                var showAddAddressDialog by remember { mutableStateOf(false) }
                var showAddCardDialog by remember { mutableStateOf(false) }

                val isPickupPoint = deliveryType == "PICKUP_POINT"
                val (subtotal, deliveryFee, discount) = calculateTotals(cartItems, isPickupPoint)
                val finalTotal = (subtotal + deliveryFee - discount).coerceAtLeast(0L)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(strings.cartTitle, style = MaterialTheme.typography.headlineSmall)
                    }

                    // Cart line items
                    items(cartItems, key = { it.id }) { entry ->
                        val title = when (language) {
                            AppLanguage.UZ -> entry.titleUz
                            AppLanguage.RU -> entry.titleRu
                            AppLanguage.EN -> entry.titleEn
                        }
                        val productFallback = catalogMap[entry.productId] ?: Product(
                            id = entry.productId,
                            titleUz = entry.titleUz,
                            titleRu = entry.titleRu,
                            titleEn = entry.titleEn,
                            descUz = "", descRu = "", descEn = "",
                            categoryId = entry.categoryId,
                            priceUzs = entry.unitPriceUzs,
                            unitUz = "1 dona", unitRu = "1 шт", unitEn = "1 pc",
                            rating = 4.9, reviewCount = 10, deliveryMinutes = 20,
                            popularityScore = 80, imageKey = entry.imageKey
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductVisualArtwork(
                                    imageKey = entry.imageKey,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    iconSizeDp = 28
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = StringsRepository.formatUzs(entry.unitPriceUzs * entry.quantity, language),
                                        style = PriceTextStyle.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(productFallback, entry.quantity.toInt() - 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (entry.quantity <= 1) Icons.Rounded.DeleteOutline else Icons.Rounded.Remove,
                                            contentDescription = "Decrease"
                                        )
                                    }
                                    Text(
                                        text = entry.quantity.toString(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { onUpdateQuantity(productFallback, entry.quantity.toInt() + 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                        }
                    }

                    // Promo Code Section
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = promoInput,
                                        onValueChange = { promoInput = it },
                                        placeholder = { Text(strings.promoCodeHint, style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = { Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = BukharaGold) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).testTag("promo_input")
                                    )
                                    Button(
                                        onClick = { onApplyPromo(promoInput) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(52.dp).testTag("apply_promo_btn")
                                    ) {
                                        Text(strings.applyPromoBtn)
                                    }
                                }
                                if (promoStatus != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = promoStatus,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Uzum-style Delivery Type Toggle (Courier vs Topshirish punkti PVZ)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (!isPickupPoint) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (!isPickupPoint) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .testTag("delivery_courier_tab")
                                            .clickable { deliveryType = "COURIER" }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Icon(
                                                Icons.Rounded.LocalShipping,
                                                contentDescription = null,
                                                tint = if (!isPickupPoint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = strings.courierDeliveryLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (!isPickupPoint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isPickupPoint) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isPickupPoint) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .testTag("delivery_pvz_tab")
                                            .clickable { deliveryType = "PICKUP_POINT" }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Storefront,
                                                    contentDescription = null,
                                                    tint = if (isPickupPoint) MaterialTheme.colorScheme.primary else BukharaGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = EmeraldPrimary
                                                ) {
                                                    Text(
                                                        text = strings.freeLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = strings.pickupPointLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isPickupPoint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isPickupPoint) {
                                    Text(
                                        text = "${strings.selectPickupPointTitle} ($selectedDistrict)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = strings.pickupPointFreeDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    pvzList.forEach { pvz ->
                                        val selected = pvz == selectedPvz
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { selectedPvz = pvz }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Storefront, contentDescription = null, tint = BukharaGold)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = pvz,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (selected) {
                                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(strings.deliveryAddressTitle, style = MaterialTheme.typography.titleMedium)
                                        TextButton(
                                            onClick = { showAddAddressDialog = true },
                                            modifier = Modifier.testTag("checkout_add_address_btn")
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.addAddressBtn)
                                        }
                                    }

                                    if (addresses.isNotEmpty()) {
                                        addresses.forEach { addr ->
                                            val isSel = addr.id == selectedAddressId
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                border = if (isSel) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { selectedAddressId = addr.id }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${addr.label} • ${addr.district}", style = MaterialTheme.typography.titleSmall)
                                                        Text(addr.streetAddress, style = MaterialTheme.typography.bodySmall)
                                                        if (addr.latitude != null && addr.longitude != null) {
                                                            Text(
                                                                text = String.format(Locale.US, "GPS: %.4f° N, %.4f° E", addr.latitude, addr.longitude),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                    if (isSel) {
                                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                    if (onDeleteAddress != null) {
                                                        IconButton(
                                                            onClick = { onDeleteAddress(addr.id) },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Rounded.DeleteOutline,
                                                                contentDescription = strings.cancelBtn,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = customAddressLine,
                                            onValueChange = { customAddressLine = it },
                                            label = { Text("$selectedDistrict — ${strings.streetHint}") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Uzcard, Humo & Cash Payment Selector
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.paymentMethodTitle, style = MaterialTheme.typography.titleMedium)
                                    TextButton(
                                        onClick = { showAddCardDialog = true },
                                        modifier = Modifier.testTag("checkout_add_card_btn")
                                    ) {
                                        Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.addCardBtn)
                                    }
                                }

                                if (cards.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(cards, key = { it.id }) { card ->
                                            UzcardHumoCardVisual(
                                                brand = card.brand,
                                                maskedNumber = card.maskedNumber,
                                                holderName = card.holderName,
                                                expiry = card.expiry,
                                                selected = !useCash && selectedCardId == card.id,
                                                onClick = {
                                                    useCash = false
                                                    selectedCardId = card.id
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (useCash) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (useCash) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("cash_payment_option")
                                        .clickable { useCash = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(strings.cashOnDeliveryLabel, style = MaterialTheme.typography.titleSmall)
                                            Text(strings.cashOnDeliveryDesc, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (useCash) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Order Summary & Checkout CTA
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SummaryRow(strings.subtotalLabel, StringsRepository.formatUzs(subtotal, language))
                                SummaryRow(
                                    strings.deliveryFeeLabel,
                                    if (deliveryFee == 0L) strings.freeLabel else StringsRepository.formatUzs(deliveryFee, language)
                                )
                                if (discount > 0L) {
                                    SummaryRow(
                                        strings.discountLabel,
                                        "-${StringsRepository.formatUzs(discount, language)}",
                                        highlightColor = BukharaGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.totalLabel, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = StringsRepository.formatUzs(finalTotal, language),
                                        style = PriceTextStyle.copy(fontSize = 20.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                val chosenAddr = addresses.firstOrNull { it.id == selectedAddressId }
                                val chosenCard = if (!useCash) cards.firstOrNull { it.id == selectedCardId } else null
                                val finalDistrict = if (isPickupPoint) selectedDistrict else (chosenAddr?.district ?: selectedDistrict)
                                val finalAddressLine = if (isPickupPoint) {
                                    "$selectedDistrict • $selectedPvz"
                                } else {
                                    chosenAddr?.streetAddress ?: customAddressLine.trim()
                                }
                                val paymentType = when {
                                    chosenCard?.brand == "UZCARD" -> PaymentMethodType.UZCARD
                                    chosenCard?.brand == "HUMO" -> PaymentMethodType.HUMO
                                    else -> PaymentMethodType.CASH
                                }

                                Button(
                                    onClick = {
                                        onPlaceOrder(
                                            cartItems,
                                            paymentType,
                                            chosenCard?.lastFour,
                                            finalDistrict,
                                            finalAddressLine.ifBlank { "$finalDistrict Markaz" },
                                            deliveryType,
                                            if (isPickupPoint) selectedPvz else null
                                        )
                                    },
                                    enabled = !isPlacingOrder && (isPickupPoint || finalAddressLine.length >= 3),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("place_order_btn")
                                ) {
                                    if (isPlacingOrder) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(strings.placingOrderLabel)
                                    } else {
                                        Text(
                                            text = "${strings.placeOrderBtn} • ${StringsRepository.formatUzs(finalTotal, language)}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddAddressDialog) {
                    AddAddressDialog(
                        language = language,
                        strings = strings,
                        defaultDistrict = selectedDistrict,
                        onDismiss = { showAddAddressDialog = false },
                        onSave = { label, district, street, entrance, lat, lng ->
                            onAddAddress(label, district, street, entrance, lat, lng)
                            showAddAddressDialog = false
                        }
                    )
                }

                if (showAddCardDialog) {
                    AddUzcardHumoDialog(
                        strings = strings,
                        onDismiss = { showAddCardDialog = false },
                        onSave = { digits, expiry, holder, onError ->
                            onAddPaymentCard(
                                digits,
                                expiry,
                                holder,
                                {
                                    useCash = false
                                    showAddCardDialog = false
                                },
                                onError
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = highlightColor)
    }
}

@Composable
fun AddAddressDialog(
    language: AppLanguage,
    strings: LocalizedStrings,
    defaultDistrict: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, Double?, Double?) -> Unit
) {
    val initialCity = remember(defaultDistrict) {
        UzbekistanGeoCatalog.findCity(defaultDistrict)
    }
    var label by remember { mutableStateOf("Uy") }
    var selectedRegionId by remember { mutableStateOf(initialCity.regionId) }
    var district by remember { mutableStateOf(initialCity.name) }
    var latitude by remember { mutableDoubleStateOf(initialCity.lat) }
    var longitude by remember { mutableDoubleStateOf(initialCity.lng) }
    var street by remember {
        mutableStateOf(initialCity.localizedStreet(language))
    }
    var entrance by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val activeRegionCities = remember(selectedRegionId) {
        UzbekistanGeoCatalog.regions.firstOrNull { it.id == selectedRegionId }?.cities
            ?: UzbekistanGeoCatalog.allCities
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.addAddressBtn,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.cancelBtn)
                    }
                }

                // All-Uzbekistan Interactive Map Pin Picker + GPS
                AddressMapPicker(
                    latitude = latitude,
                    longitude = longitude,
                    language = language,
                    strings = strings,
                    onLocationSelected = { newLat, newLng, resolvedDistrict, resolvedStreet ->
                        latitude = newLat
                        longitude = newLng
                        district = resolvedDistrict
                        selectedRegionId = UzbekistanGeoCatalog.findCity(resolvedDistrict).regionId
                        street = resolvedStreet
                    }
                )

                // Preset Label Chips + Label Field
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Uy", "Ofis", "Oila").forEach { preset ->
                        val active = label == preset
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { label = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(strings.addressLabelHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("address_label_input")
                )

                // 14 Region Selector Pills synced with map pin
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(UzbekistanGeoCatalog.regions, key = { it.id }) { region ->
                        val selected = selectedRegionId == region.id
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) EmeraldDeep else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selectedRegionId = region.id
                                    val firstCity = region.cities.first()
                                    district = firstCity.name
                                    latitude = firstCity.lat
                                    longitude = firstCity.lng
                                    street = firstCity.localizedStreet(language)
                                }
                        ) {
                            Text(
                                text = region.localizedName(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // City / District Chips within active region
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activeRegionCities, key = { it.id }) { city ->
                        val active = district.equals(city.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    district = city.name
                                    latitude = city.lat
                                    longitude = city.lng
                                    street = city.localizedStreet(language)
                                }
                        ) {
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text(strings.streetHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("address_street_input")
                )

                OutlinedTextField(
                    value = entrance,
                    onValueChange = { entrance = it },
                    label = { Text(strings.entranceHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("address_entrance_input")
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancelBtn)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (label.trim().isEmpty() || street.trim().length < 4) {
                                errorMsg = strings.invalidAddressMsg
                            } else {
                                onSave(label, district, street, entrance.ifBlank { null }, latitude, longitude)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp).testTag("save_address_btn")
                    ) {
                        Text(strings.saveBtn, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddUzcardHumoDialog(
    strings: LocalizedStrings,
    onDismiss: () -> Unit,
    onSave: (String, String, String, (String) -> Unit) -> Unit
) {
    var cardDigits by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("08/28") }
    var holder by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val detectedBrand = when {
        cardDigits.startsWith("8600") -> "UZCARD"
        cardDigits.startsWith("9860") -> "HUMO"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.addCardBtn)
                if (detectedBrand != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = detectedBrand,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cardDigits,
                    onValueChange = { input ->
                        cardDigits = input.filter { it.isDigit() }.take(16)
                    },
                    label = { Text(strings.cardNumberHint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it.take(5) },
                    label = { Text(strings.cardExpiryHint) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it.take(40) },
                    label = { Text(strings.cardHolderHint) },
                    singleLine = true
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val validPrefix = cardDigits.startsWith("8600") || cardDigits.startsWith("9860")
                if (cardDigits.length != 16 || !validPrefix || !expiry.matches(Regex("^\\d{2}/\\d{2}$"))) {
                    errorMsg = strings.invalidCardMsg
                } else {
                    onSave(cardDigits, expiry, holder) { err -> errorMsg = err }
                }
            }) {
                Text(strings.saveBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelBtn) }
        }
    )
}
