package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RiceBowl
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.BukharaGoldGlow
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HumoGold
import com.example.ui.theme.HumoOrange
import com.example.ui.theme.UzcardBlue
import com.example.ui.theme.UzcardBlueDeep

@Composable
fun LanguageSwitcherPill(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLanguage.entries.forEach { lang ->
                val selected = lang == currentLanguage
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .testTag("lang_chip_${lang.code}")
                        .clickable { onSelectLanguage(lang) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                ) {
                    Text(
                        text = if (compact) lang.shortCode else lang.nativeName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = if (compact) 10.dp else 14.dp,
                            vertical = if (compact) 6.dp else 8.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ProductVisualArtwork(
    imageKey: String,
    modifier: Modifier = Modifier,
    iconSizeDp: Int = 44
) {
    val spec = visualSpecForKey(imageKey)
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(spec.startColor, spec.endColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Uzbek geometric lattice watermark
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = minOf(w, h) * 0.42f,
                center = Offset(w * 0.82f, h * 0.18f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = minOf(w, h) * 0.30f,
                center = Offset(w * 0.16f, h * 0.86f)
            )
            val diamond = Path().apply {
                moveTo(w * 0.5f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.5f)
                lineTo(w * 0.5f, h * 0.86f)
                lineTo(w * 0.14f, h * 0.5f)
                close()
            }
            drawPath(
                path = diamond,
                color = Color.White.copy(alpha = 0.12f),
                style = Stroke(width = 2f)
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.24f),
            modifier = Modifier.size((iconSizeDp + 24).dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSizeDp.dp)
                )
            }
        }
    }
}

private data class ProductVisualSpec(
    val startColor: Color,
    val endColor: Color,
    val icon: ImageVector
)

private fun visualSpecForKey(key: String): ProductVisualSpec = when (key) {
    "pomegranate" -> ProductVisualSpec(Color(0xFF9F1239), Color(0xFFE11D48), Icons.Rounded.LocalFlorist)
    "rice" -> ProductVisualSpec(Color(0xFF926911), Color(0xFFD97706), Icons.Rounded.RiceBowl)
    "carrot" -> ProductVisualSpec(Color(0xFFB45309), Color(0xFFF59E0B), Icons.Rounded.Eco)
    "bread" -> ProductVisualSpec(Color(0xFF9A3412), Color(0xFFEA580C), Icons.Rounded.BakeryDining)
    "meat" -> ProductVisualSpec(Color(0xFF7F1D1D), Color(0xFFB91C1C), Icons.Rounded.SetMeal)
    "spice" -> ProductVisualSpec(Color(0xFF854D0E), Color(0xFFCA8A04), Icons.Rounded.Spa)
    "dairy" -> ProductVisualSpec(Color(0xFF0369A1), Color(0xFF38BDF8), Icons.Rounded.WaterDrop)
    "tea" -> ProductVisualSpec(Color(0xFF065F46), Color(0xFF10B981), Icons.Rounded.LocalCafe)
    "water" -> ProductVisualSpec(Color(0xFF0E7490), Color(0xFF06B6D4), Icons.Rounded.LocalDrink)
    "tomato" -> ProductVisualSpec(Color(0xFFBE123C), Color(0xFFF43F5E), Icons.Rounded.Restaurant)
    "sweets" -> ProductVisualSpec(Color(0xFF7E22CE), Color(0xFFA855F7), Icons.Rounded.Cookie)
    "honey" -> ProductVisualSpec(Color(0xFFB45309), Color(0xFFFBBF24), Icons.Rounded.AutoAwesome)
    "nuts" -> ProductVisualSpec(Color(0xFF3F6212), Color(0xFF65A30D), Icons.Rounded.Eco)
    "coffee" -> ProductVisualSpec(Color(0xFF451A03), Color(0xFF92400E), Icons.Rounded.Coffee)
    "earbuds" -> ProductVisualSpec(Color(0xFF312E81), Color(0xFF4F46E5), Icons.Rounded.Headphones)
    "powerbank" -> ProductVisualSpec(Color(0xFF1E3A8A), Color(0xFF2563EB), Icons.Rounded.BatteryChargingFull)
    "vitamins" -> ProductVisualSpec(Color(0xFF0F766E), Color(0xFF14B8A6), Icons.Rounded.Medication)
    "cotton" -> ProductVisualSpec(EmeraldDeep, EmeraldPrimary, Icons.Rounded.Spa)
    "melon" -> ProductVisualSpec(Color(0xFFCA8A04), Color(0xFFEAB308), Icons.Rounded.Eco)
    "knife" -> ProductVisualSpec(Color(0xFF334155), Color(0xFF64748B), Icons.Rounded.ContentCut)
    "watch" -> ProductVisualSpec(Color(0xFF1E1B4B), Color(0xFF6366F1), Icons.Rounded.Watch)
    "kettle" -> ProductVisualSpec(Color(0xFF0F766E), Color(0xFF0EA5E9), Icons.Rounded.ElectricBolt)
    "cream" -> ProductVisualSpec(Color(0xFFBE185D), Color(0xFFF472B6), Icons.Rounded.Spa)
    else -> ProductVisualSpec(EmeraldDeep, BukharaGold, Icons.Rounded.Eco)
}

@Composable
fun UzcardHumoCardVisual(
    brand: String,
    maskedNumber: String,
    holderName: String,
    expiry: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUzcard = brand.uppercase() == "UZCARD"
    val brush = if (isUzcard) {
        Brush.linearGradient(listOf(UzcardBlueDeep, UzcardBlue, Color(0xFF0EA5E9)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF9A3412), HumoOrange, HumoGold))
    }

    Surface(
        modifier = modifier
            .width(220.dp)
            .height(126.dp)
            .minimumInteractiveComponentSize()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = if (selected) BorderStroke(2.5.dp, BukharaGoldGlow) else null,
        shadowElevation = if (selected) 8.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = brand.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected card",
                            tint = BukharaGoldGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(26.dp, 18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFDE68A).copy(alpha = 0.85f))
                        )
                    }
                }

                Text(
                    text = maskedNumber,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = holderName.take(18),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = expiry,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
