package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SavedAddress
import com.example.data.model.SavedPaymentCard
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UzbekistanGeoCatalog
import com.example.i18n.AppLanguage
import com.example.i18n.LocalizedStrings
import com.example.ui.components.LanguageSwitcherPill
import com.example.ui.components.UzcardHumoCardVisual
import com.example.ui.theme.BukharaGold
import com.example.ui.theme.BukharaGoldGlow
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ThemeMode

import java.util.Locale

@Composable
fun ProfileScreen(
    language: AppLanguage,
    themeMode: ThemeMode,
    strings: LocalizedStrings,
    selectedDistrict: String,
    userName: String,
    userEmail: String,
    userProfileState: UiState<UserProfile?>,
    addressesState: UiState<List<SavedAddress>>,
    cardsState: UiState<List<SavedPaymentCard>>,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onAddAddress: (String, String, String, String?, Double?, Double?) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onAddCard: (String, String, String, () -> Unit, (String) -> Unit) -> Unit,
    onDeleteCard: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val profile = (userProfileState as? UiState.Success)?.data
    val addresses = (addressesState as? UiState.Success)?.data ?: emptyList()
    val cards = (cardsState as? UiState.Success)?.data ?: emptyList()
    val currentRegion = remember(selectedDistrict) {
        UzbekistanGeoCatalog.findRegionForCity(selectedDistrict)
    }

    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(strings.profileTitle, style = MaterialTheme.typography.headlineSmall)
        }

        // User & TezBazar Gold Loyalty Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldPrimary)))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BukharaGold,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userName.firstOrNull()?.uppercase() ?: "T",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.displayName ?: userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                if (userEmail.isNotBlank()) {
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                Text(
                                    text = "${currentRegion.localizedName(language)} • $selectedDistrict",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BukharaGoldGlow
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stars,
                                        contentDescription = null,
                                        tint = BukharaGoldGlow
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.loyaltyTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${profile?.loyaltyPoints ?: 250} ${strings.loyaltyPointsSuffix}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BukharaGoldGlow
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3-Language Switcher Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.languageSectionTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LanguageSwitcherPill(
                        currentLanguage = language,
                        onSelectLanguage = onSelectLanguage,
                        compact = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Theme Mode Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.themeSectionTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to strings.themeSystem,
                            ThemeMode.LIGHT to strings.themeLight,
                            ThemeMode.DARK to strings.themeDark
                        ).forEach { (mode, label) ->
                            val selected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectThemeMode(mode) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Addresses
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.savedAddressesTitle, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddAddressDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addAddressBtn)
                        }
                    }
                    addresses.forEach { addr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${addr.label} • ${addr.district}", style = MaterialTheme.typography.titleSmall)
                                Text(addr.streetAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (addr.latitude != null && addr.longitude != null) {
                                    Text(
                                        text = String.format(Locale.US, "GPS: %.4f° N, %.4f° E", addr.latitude, addr.longitude),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete address")
                            }
                        }
                    }
                }
            }
        }

        // Saved Uzcard / Humo Cards
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.savedCardsTitle, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddCardDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addCardBtn)
                        }
                    }
                    if (cards.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(cards, key = { it.id }) { card ->
                                Column(horizontalAlignment = Alignment.End) {
                                    UzcardHumoCardVisual(
                                        brand = card.brand,
                                        maskedNumber = card.maskedNumber,
                                        holderName = card.holderName,
                                        expiry = card.expiry,
                                        selected = false,
                                        onClick = {}
                                    )
                                    TextButton(onClick = { onDeleteCard(card.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.cancelBtn)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sign Out Button
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_btn")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.signOutBtn, style = MaterialTheme.typography.titleSmall)
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
                onAddCard(digits, expiry, holder, { showAddCardDialog = false }, onError)
            }
        )
    }
}
