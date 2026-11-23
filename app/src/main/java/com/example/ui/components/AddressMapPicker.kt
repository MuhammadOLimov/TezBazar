package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.UzbekistanGeoCatalog
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.EmeraldPrimary
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.hypot

@Composable
fun AddressMapPicker(
    latitude: Double,
    longitude: Double,
    language: AppLanguage,
    strings: LocalizedStrings,
    onLocationSelected: (lat: Double, lng: Double, resolvedDistrict: String, resolvedStreet: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // false = City Street Map centered on selected/nearest Uzbekistan city; true = National Uzbekistan Map (14 regions)
    var nationalMode by remember { mutableStateOf(false) }
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val nearestCity = remember(latitude, longitude) {
        UzbekistanGeoCatalog.nearestCity(latitude, longitude)
    }
    val activeRegion = remember(nearestCity) {
        UzbekistanGeoCatalog.findRegionForCity(nearestCity.name)
    }

    val infinite = rememberInfiniteTransition(label = "pinPulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    fun handleCoordinateUpdate(lat: Double, lng: Double) {
        statusMessage = null
        val resolvedCity = UzbekistanGeoCatalog.nearestCity(lat, lng)
        val immediateStreet = UzbekistanGeoCatalog.fallbackStreet(lat, lng, language)
        onLocationSelected(lat, lng, resolvedCity.name, immediateStreet)

        scope.launch {
            val geocodedStreet = reverseGeocodeAddress(context, lat, lng, language)
            if (!geocodedStreet.isNullOrBlank()) {
                onLocationSelected(lat, lng, resolvedCity.name, geocodedStreet)
            }
        }
    }

    fun fetchCurrentGps() {
        isLocatingGps = true
        statusMessage = null
        scope.launch {
            val loc = getDeviceLocation(context)
            isLocatingGps = false
            if (loc != null) {
                handleCoordinateUpdate(loc.first, loc.second)
            } else {
                statusMessage = strings.gpsUnavailableMsg
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentGps()
        } else {
            statusMessage = strings.locationPermissionDenied
        }
    }

    val mapBgTop = MaterialTheme.colorScheme.surfaceVariant
    val mapBgBottom = MaterialTheme.colorScheme.background
    val roadColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val ringRoadColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)
    val parkColor = Color(0xFF10B981).copy(alpha = 0.16f)
    val waterColor = Color(0xFF38BDF8).copy(alpha = 0.36f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row with GPS button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.pickOnMapTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${activeRegion.localizedName(language)} • ${nearestCity.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        fetchCurrentGps()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isLocatingGps,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("use_gps_btn")
            ) {
                if (isLocatingGps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.locatingGpsLabel, style = MaterialTheme.typography.labelSmall, color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = strings.useMyLocationBtn,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "GPS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // 2-Mode Toggle: City Zoom Map vs All Uzbekistan (14 regions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!nationalMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("map_mode_city_btn")
                    .clickable { nationalMode = false }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.LocationCity,
                        contentDescription = null,
                        tint = if (!nationalMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${strings.mapModeCity}: ${nearestCity.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (!nationalMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (nationalMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("map_mode_uz_btn")
                    .clickable { nationalMode = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Public,
                        contentDescription = null,
                        tint = if (nationalMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = strings.mapModeUzbekistan,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (nationalMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // Interactive Canvas Map
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(mapBgTop, mapBgBottom)))
            ) {
                val (pinNx, pinNy) = if (nationalMode) {
                    UzbekistanGeoCatalog.uzLatLngToNormalized(latitude, longitude)
                } else {
                    UzbekistanGeoCatalog.cityLatLngToNormalized(latitude, longitude, nearestCity.lat, nearestCity.lng)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("interactive_address_map")
                        .pointerInput(zoomFactor, nationalMode, nearestCity.id) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val (nx, ny) = screenOffsetToNormalized(tapOffset, w, h, zoomFactor)
                                val (newLat, newLng) = if (nationalMode) {
                                    UzbekistanGeoCatalog.uzNormalizedToLatLng(nx, ny)
                                } else {
                                    UzbekistanGeoCatalog.cityNormalizedToLatLng(nx, ny, nearestCity.lat, nearestCity.lng)
                                }
                                handleCoordinateUpdate(newLat, newLng)
                            }
                        }
                        .pointerInput(zoomFactor, nationalMode, nearestCity.id) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val (nx, ny) = screenOffsetToNormalized(change.position, w, h, zoomFactor)
                                val (newLat, newLng) = if (nationalMode) {
                                    UzbekistanGeoCatalog.uzNormalizedToLatLng(nx, ny)
                                } else {
                                    UzbekistanGeoCatalog.cityNormalizedToLatLng(nx, ny, nearestCity.lat, nearestCity.lng)
                                }
                                handleCoordinateUpdate(newLat, newLng)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    fun project(nx: Float, ny: Float): Offset {
                        val cx = 0.5f + (nx - 0.5f) * zoomFactor
                        val cy = 0.5f + (ny - 0.5f) * zoomFactor
                        return Offset(cx * w, cy * h)
                    }

                    if (nationalMode) {
                        // All-Uzbekistan National Map
                        val uzOutlineCoords = listOf(
                            45.55 to 56.05,
                            45.10 to 59.50,
                            43.60 to 61.90,
                            41.40 to 65.50,
                            41.90 to 69.25,
                            41.50 to 70.60,
                            40.80 to 73.10,
                            39.90 to 72.10,
                            40.15 to 70.80,
                            40.20 to 68.70,
                            39.40 to 67.80,
                            37.20 to 67.30,
                            38.60 to 64.90,
                            40.60 to 62.20,
                            41.30 to 60.10,
                            42.40 to 58.60,
                            43.20 to 56.05
                        )
                        val outlinePath = Path().apply {
                            uzOutlineCoords.forEachIndexed { idx, (la, ln) ->
                                val (nx, ny) = UzbekistanGeoCatalog.uzLatLngToNormalized(la, ln)
                                val pt = project(nx, ny)
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                            close()
                        }
                        drawPath(path = outlinePath, color = parkColor)
                        drawPath(path = outlinePath, color = EmeraldPrimary.copy(alpha = 0.45f), style = Stroke(width = 3.2f))

                        // Aral Sea + Amudaryo corridor
                        val (aralX, aralY) = UzbekistanGeoCatalog.uzLatLngToNormalized(44.4, 59.2)
                        drawCircle(color = waterColor, radius = 24f * zoomFactor, center = project(aralX, aralY))

                        val riverPoints = listOf(37.25 to 67.25, 39.10 to 63.60, 41.40 to 60.70, 43.20 to 59.30)
                        val riverPath = Path().apply {
                            riverPoints.forEachIndexed { idx, (la, ln) ->
                                val (nx, ny) = UzbekistanGeoCatalog.uzLatLngToNormalized(la, ln)
                                val pt = project(nx, ny)
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(path = riverPath, color = waterColor, style = Stroke(width = 5f * zoomFactor, cap = StrokeCap.Round))

                        // M39 Silk Road corridor connecting Toshkent -> Jizzax -> Samarqand -> Buxoro -> Urganch -> Nukus
                        val highwayCities = listOf("Yunusobod", "Guliston", "Jizzax sh.", "Samarqand sh.", "Navoiy sh.", "Buxoro sh.", "Urganch", "Nukus")
                        val hwPath = Path().apply {
                            highwayCities.forEachIndexed { idx, name ->
                                val c = UzbekistanGeoCatalog.findCity(name)
                                val (nx, ny) = UzbekistanGeoCatalog.uzLatLngToNormalized(c.lat, c.lng)
                                val pt = project(nx, ny)
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = hwPath,
                            color = BukharaGold.copy(alpha = 0.5f),
                            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                        )

                        // Draw all 14 Regional Hubs
                        UzbekistanGeoCatalog.regionalHubs.forEach { hub ->
                            val (lx, ly) = UzbekistanGeoCatalog.uzLatLngToNormalized(hub.lat, hub.lng)
                            val pos = project(lx, ly)
                            if (pos.x in -20f..(w + 20f) && pos.y in -20f..(h + 20f)) {
                                drawCircle(color = Color.White, radius = 6f, center = pos)
                                drawCircle(
                                    color = if (hub.id == nearestCity.id) BukharaGold else EmeraldPrimary.copy(alpha = 0.85f),
                                    radius = 4f,
                                    center = pos
                                )
                                val shortName = hub.name.replace(" sh.", "")
                                val measured = textMeasurer.measure(
                                    text = shortName,
                                    style = TextStyle(color = labelTextColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                )
                                val labelX = (pos.x - measured.size.width / 2f).coerceIn(4f, (w - measured.size.width - 4f).coerceAtLeast(4f))
                                val labelY = (pos.y + 6f).coerceIn(4f, (h - measured.size.height - 4f).coerceAtLeast(4f))
                                drawText(textLayoutResult = measured, topLeft = Offset(labelX, labelY))
                            }
                        }
                    } else {
                        // Local City Street Grid centered around nearestCity
                        for (i in 1..7) {
                            drawLine(color = roadColor, start = project(i / 8f, 0f), end = project(i / 8f + 0.03f, 1f), strokeWidth = 1.6f)
                        }
                        for (j in 1..5) {
                            drawLine(color = roadColor, start = project(0f, j / 6f), end = project(1f, j / 6f - 0.02f), strokeWidth = 1.6f)
                        }

                        drawCircle(color = parkColor, radius = 44f * zoomFactor, center = project(0.38f, 0.52f))
                        drawCircle(color = parkColor, radius = 34f * zoomFactor, center = project(0.70f, 0.28f))

                        val ringCenter = project(0.50f, 0.50f)
                        drawRoundRect(
                            color = ringRoadColor,
                            topLeft = Offset(ringCenter.x - w * 0.27f * zoomFactor, ringCenter.y - h * 0.27f * zoomFactor),
                            size = Size(w * 0.54f * zoomFactor, h * 0.54f * zoomFactor),
                            cornerRadius = CornerRadius(64f * zoomFactor, 64f * zoomFactor),
                            style = Stroke(width = 4f)
                        )

                        val canalPath = Path().apply {
                            val c0 = project(0.64f, 0.05f)
                            val c1 = project(0.48f, 0.35f)
                            val c2 = project(0.42f, 0.65f)
                            val c3 = project(0.28f, 0.96f)
                            moveTo(c0.x, c0.y)
                            cubicTo(c1.x, c1.y, c2.x, c2.y, c3.x, c3.y)
                        }
                        drawPath(path = canalPath, color = waterColor, style = Stroke(width = 11f * zoomFactor, cap = StrokeCap.Round))

                        drawLine(color = BukharaGold.copy(alpha = 0.45f), start = project(0.50f, 0.05f), end = project(0.50f, 0.95f), strokeWidth = 4.5f)
                        drawLine(color = BukharaGold.copy(alpha = 0.45f), start = project(0.10f, 0.54f), end = project(0.90f, 0.44f), strokeWidth = 4.5f)

                        // Nearby cities in same region + pickup point center label
                        activeRegion.cities.forEach { city ->
                            val (lx, ly) = UzbekistanGeoCatalog.cityLatLngToNormalized(city.lat, city.lng, nearestCity.lat, nearestCity.lng)
                            if (lx in 0.06f..0.94f && ly in 0.06f..0.94f) {
                                val pos = project(lx, ly)
                                drawCircle(color = Color.White, radius = 6.5f, center = pos)
                                drawCircle(color = EmeraldPrimary.copy(alpha = 0.8f), radius = 4.2f, center = pos)
                                val measured = textMeasurer.measure(
                                    text = city.name,
                                    style = TextStyle(color = labelTextColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                )
                                val labelX = (pos.x - measured.size.width / 2f).coerceIn(6f, (w - measured.size.width - 6f).coerceAtLeast(6f))
                                val labelY = (pos.y + 8f).coerceIn(4f, (h - measured.size.height - 4f).coerceAtLeast(4f))
                                drawText(textLayoutResult = measured, topLeft = Offset(labelX, labelY))
                            }
                        }
                    }

                    // Selected delivery pin with crosshair + animated pulse
                    val pinPos = project(pinNx, pinNy)
                    drawLine(
                        color = EmeraldPrimary.copy(alpha = 0.28f),
                        start = Offset(pinPos.x, 0f),
                        end = Offset(pinPos.x, h),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                    drawLine(
                        color = EmeraldPrimary.copy(alpha = 0.28f),
                        start = Offset(0f, pinPos.y),
                        end = Offset(w, pinPos.y),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    drawCircle(
                        color = EmeraldLuminous.copy(alpha = pulseAlpha),
                        radius = 22f * pulseScale,
                        center = pinPos
                    )
                    drawCircle(color = Color.Black.copy(alpha = 0.18f), radius = 8f, center = Offset(pinPos.x, pinPos.y + 4f))
                    drawCircle(color = Color.White, radius = 15f, center = Offset(pinPos.x, pinPos.y - 10f))
                    drawCircle(color = EmeraldDeep, radius = 11f, center = Offset(pinPos.x, pinPos.y - 10f))
                    drawCircle(color = BukharaGold, radius = 4.5f, center = Offset(pinPos.x, pinPos.y - 10f))
                }

                // Coordinates Pill (Top Start)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            tint = BukharaGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.4f° N, %.4f° E", latitude, longitude),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // Zoom Controls (Bottom End)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Column {
                        IconButton(
                            onClick = { zoomFactor = (zoomFactor + 0.25f).coerceAtMost(2.25f) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { zoomFactor = (zoomFactor - 0.25f).coerceAtLeast(1.0f) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Quick 14 Regional Hubs Jump Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(UzbekistanGeoCatalog.regionalHubs, key = { it.id }) { hub ->
                val isNear = hypot(latitude - hub.lat, longitude - hub.lng) < 0.08
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isNear) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isNear) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            onLocationSelected(hub.lat, hub.lng, hub.name, hub.localizedStreet(language))
                        }
                ) {
                    Text(
                        text = hub.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun screenOffsetToNormalized(
    offset: Offset,
    width: Float,
    height: Float,
    zoomFactor: Float
): Pair<Float, Float> {
    val rawCx = (offset.x / width).coerceIn(0f, 1f)
    val rawCy = (offset.y / height).coerceIn(0f, 1f)
    val nx = 0.5f + (rawCx - 0.5f) / zoomFactor
    val ny = 0.5f + (rawCy - 0.5f) / zoomFactor
    return nx.coerceIn(0.02f, 0.98f) to ny.coerceIn(0.02f, 0.98f)
}

@SuppressLint("MissingPermission")
private suspend fun getDeviceLocation(context: Context): Pair<Double, Double>? {
    return try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        val current = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
        if (current != null) {
            current.latitude to current.longitude
        } else {
            val last = fused.lastLocation.await()
            if (last != null) last.latitude to last.longitude else null
        }
    } catch (_: Exception) {
        null
    }
}

private suspend fun reverseGeocodeAddress(
    context: Context,
    lat: Double,
    lng: Double,
    language: AppLanguage
): String? = withContext(Dispatchers.IO) {
    try {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale(language.code))
        val results: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses)
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lng, 1).orEmpty()
        }

        val addr = results.firstOrNull() ?: return@withContext null
        val parts = listOfNotNull(
            addr.thoroughfare ?: addr.featureName,
            addr.subThoroughfare,
            addr.subLocality ?: addr.locality
        ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        parts.joinToString(", ").takeIf { it.length >= 4 }?.take(140)
    } catch (_: Exception) {
        null
    }
}
