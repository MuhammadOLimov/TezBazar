package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.data.model.UzbekistanGeoCatalog
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.i18n.StringsRepository
import com.example.ui.components.LiveTashkentCourierMap
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.BukharaGoldGlow
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.PriceTextStyle

@Composable
fun TrackingScreen(
    language: AppLanguage,
    strings: LocalizedStrings,
    ordersState: UiState<List<OrderRecord>>,
    autoSimulate: Boolean,
    onToggleAutoSimulate: (Boolean) -> Unit,
    onAdvanceStage: (OrderRecord) -> Unit,
    onReorder: (OrderRecord) -> Unit,
    onBrowseMarket: () -> Unit
) {
    val context = LocalContext.current

    when (ordersState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = ordersState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        is UiState.Success -> {
            val orders = ordersState.data
            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_express_delivery),
                            contentDescription = strings.noActiveOrderTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = strings.noActiveOrderTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.noActiveOrderDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onBrowseMarket,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(50.dp).testTag("track_browse_btn")
                        ) {
                            Text(strings.browseMarketBtn)
                        }
                    }
                }
            } else {
                val activeOrders = remember(orders) {
                    orders.filter { it.parsedStatus != OrderStatus.DELIVERED && it.parsedStatus != OrderStatus.CANCELLED }
                }
                var selectedOrderId by remember(orders) {
                    mutableStateOf((activeOrders.firstOrNull() ?: orders.first()).id)
                }
                val activeOrder = remember(orders, selectedOrderId) {
                    orders.firstOrNull { it.id == selectedOrderId } ?: activeOrders.firstOrNull() ?: orders.first()
                }
                val pastOrders = remember(orders, activeOrder) {
                    orders.filterNot { it.id == activeOrder.id }
                }
                val region = remember(activeOrder.district) {
                    UzbekistanGeoCatalog.findRegionForCity(activeOrder.district)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = strings.trackTitle,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${region.localizedName(language)} • ${activeOrder.district}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Active Orders Switcher if multiple ongoing orders exist
                    if (activeOrders.size > 1) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(activeOrders, key = { it.id }) { ord ->
                                    val isSel = ord.id == activeOrder.id
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { selectedOrderId = ord.id }
                                    ) {
                                        Text(
                                            text = "${ord.orderNumber} (${ord.district})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Courier / PVZ Map
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 4.dp
                        ) {
                            LiveTashkentCourierMap(
                                routeProgress = activeOrder.routeProgress.toFloat(),
                                districtName = activeOrder.district,
                                courierName = activeOrder.courierName,
                                etaMinutes = activeOrder.etaMinutes,
                                etaSuffix = strings.etaMinutesSuffix,
                                deliveryType = activeOrder.deliveryType,
                                pickupPointName = activeOrder.pickupPointName
                            )
                        }
                    }

                    // 5-Stage Progress Stepper & Controls
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${activeOrder.orderNumber} • ${statusLabel(activeOrder.parsedStatus, strings)}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = activeOrder.addressLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (activeOrder.deliveryType == "PICKUP_POINT") {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = EmeraldPrimary.copy(alpha = 0.14f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Storefront, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(13.dp))
                                                    Text(
                                                        text = activeOrder.pickupPointName ?: strings.pickupPointLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EmeraldPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = BukharaGold.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = StringsRepository.formatUzs(activeOrder.totalUzs, language),
                                            style = PriceTextStyle.copy(fontSize = 13.sp),
                                            color = BukharaGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OrderStageStepper(
                                    currentStatus = activeOrder.parsedStatus,
                                    strings = strings
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Courier Profile Row with Working Phone Dialer Intent
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeliveryDining,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeOrder.courierName,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${activeOrder.courierVehicle} • ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    tint = BukharaGoldGlow,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = " 4.98",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .testTag("call_courier_btn")
                                                .clickable {
                                                    val cleanPhone = activeOrder.courierPhone.ifBlank { "+998901234567" }
                                                    try {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                                        )
                                                    } catch (_: Exception) {
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Phone,
                                                    contentDescription = "${strings.callCourierLabel}: ${activeOrder.courierPhone}",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (activeOrder.parsedStatus != OrderStatus.DELIVERED &&
                                    activeOrder.parsedStatus != OrderStatus.CANCELLED
                                ) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = autoSimulate,
                                                onCheckedChange = onToggleAutoSimulate,
                                                modifier = Modifier.testTag("auto_track_switch")
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.autoSimulateLabel,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onAdvanceStage(activeOrder) },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("advance_order_btn")
                                        ) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.advanceStageBtn)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (pastOrders.isNotEmpty()) {
                        item {
                            Text(
                                text = strings.orderHistoryTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(pastOrders, key = { it.id }) { order ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { selectedOrderId = order.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${order.orderNumber} • ${statusLabel(order.parsedStatus, strings)}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${order.district} • ${order.itemsSummary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = StringsRepository.formatUzs(order.totalUzs, language),
                                            style = PriceTextStyle.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onReorder(order) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.reorderBtn, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStageStepper(
    currentStatus: OrderStatus,
    strings: LocalizedStrings
) {
    val stages = listOf(
        OrderStatus.PLACED to strings.stagePlaced,
        OrderStatus.PREPARING to strings.stagePreparing,
        OrderStatus.ON_THE_WAY to strings.stageOnTheWay,
        OrderStatus.ARRIVING to strings.stageArriving,
        OrderStatus.DELIVERED to strings.stageDelivered
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        stages.forEachIndexed { idx, (status, label) ->
            val reached = currentStatus.stepIndex >= status.stepIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (reached) EmeraldVibrant else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (reached) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

private fun statusLabel(status: OrderStatus, strings: LocalizedStrings): String = when (status) {
    OrderStatus.PLACED -> strings.stagePlaced
    OrderStatus.PREPARING -> strings.stagePreparing
    OrderStatus.ON_THE_WAY -> strings.stageOnTheWay
    OrderStatus.ARRIVING -> strings.stageArriving
    OrderStatus.DELIVERED -> strings.stageDelivered
    OrderStatus.CANCELLED -> strings.stageCancelled
}
