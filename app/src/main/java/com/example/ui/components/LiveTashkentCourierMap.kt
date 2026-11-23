package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.BukharaGoldGlow
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldVibrant

@Composable
fun LiveTashkentCourierMap(
    routeProgress: Float,
    districtName: String,
    courierName: String,
    etaMinutes: Long,
    etaSuffix: String,
    deliveryType: String = "COURIER",
    pickupPointName: String? = null,
    modifier: Modifier = Modifier
) {
    val isPickupPoint = deliveryType == "PICKUP_POINT"
    val animatedProgress by animateFloatAsState(
        targetValue = routeProgress.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "courierProgress"
    )

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val mapBgTop = MaterialTheme.colorScheme.surfaceVariant
    val mapBgBottom = MaterialTheme.colorScheme.background
    val roadColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val majorRoadColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)
    val riverColor = Color(0xFF38BDF8).copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(
                Brush.verticalGradient(listOf(mapBgTop, mapBgBottom)),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Stylized Uzbekistan regional city grid & avenues
            for (i in 1..6) {
                val x = w * (i / 7f)
                drawLine(
                    color = roadColor,
                    start = Offset(x, 0f),
                    end = Offset(x + 18f, h),
                    strokeWidth = 2f
                )
            }
            for (j in 1..4) {
                val y = h * (j / 5f)
                drawLine(
                    color = roadColor,
                    start = Offset(0f, y),
                    end = Offset(w, y - 12f),
                    strokeWidth = 2f
                )
            }

            // Central logistics avenue & regional river
            drawLine(
                color = majorRoadColor,
                start = Offset(0f, h * 0.62f),
                end = Offset(w, h * 0.34f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
            val canalPath = Path().apply {
                moveTo(w * 0.08f, h * 0.95f)
                cubicTo(w * 0.35f, h * 0.72f, w * 0.48f, h * 0.30f, w * 0.78f, h * 0.05f)
            }
            drawPath(
                path = canalPath,
                color = riverColor,
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // Route Bezier points: Regional Hub (p0) -> Control1 (p1) -> Control2 (p2) -> Customer/PVZ (p3)
            val p0 = Offset(w * 0.14f, h * 0.74f)
            val p1 = Offset(w * 0.32f, h * 0.25f)
            val p2 = Offset(w * 0.64f, h * 0.72f)
            val p3 = Offset(w * 0.86f, h * 0.30f)

            val fullRoute = Path().apply {
                moveTo(p0.x, p0.y)
                cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
            }
            drawPath(
                path = fullRoute,
                color = BukharaGold.copy(alpha = 0.55f),
                style = Stroke(
                    width = 7f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            val traveledPath = Path().apply {
                moveTo(p0.x, p0.y)
                val steps = 36
                val maxStep = (steps * animatedProgress).toInt().coerceAtLeast(1)
                for (s in 1..maxStep) {
                    val t = (s.toFloat() / steps).coerceAtMost(animatedProgress)
                    val pt = evalCubicBezier(p0, p1, p2, p3, t)
                    lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = traveledPath,
                color = EmeraldVibrant,
                style = Stroke(width = 9f, cap = StrokeCap.Round)
            )

            drawCircle(color = EmeraldDeep, radius = 16f, center = p0)
            drawCircle(color = Color.White, radius = 8f, center = p0)

            drawCircle(color = BukharaGold, radius = 18f, center = p3)
            drawCircle(color = Color.White, radius = 9f, center = p3)

            val courierPos = evalCubicBezier(p0, p1, p2, p3, animatedProgress)
            drawCircle(
                color = EmeraldLuminous.copy(alpha = pulseAlpha),
                radius = 26f * pulseScale,
                center = courierPos
            )
            drawCircle(color = Color.White, radius = 22f, center = courierPos)
            drawCircle(color = EmeraldPrimary, radius = 16f, center = courierPos)
        }

        // Top HUD overlays
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPickupPoint) Icons.Rounded.Storefront else Icons.Rounded.Home,
                        contentDescription = null,
                        tint = BukharaGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPickupPoint && !pickupPointName.isNullOrBlank()) {
                            "$districtName • $pickupPointName"
                        } else {
                            districtName
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = EmeraldDeep,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPickupPoint) Icons.Rounded.LocalShipping else Icons.Rounded.DeliveryDining,
                        contentDescription = null,
                        tint = BukharaGoldGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (etaMinutes <= 0) "0 $etaSuffix" else "$etaMinutes $etaSuffix",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom legend pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(EmeraldVibrant, CircleShape)
                )
                Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "TezBazar Hub • $districtName → $courierName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun evalCubicBezier(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    t: Float
): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t
    val x = uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y
    return Offset(x, y)
}
