package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.i18n.AppLanguage
import com.example.i18n.LanguagePreferences
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val prefs: LanguagePreferences
) : ViewModel() {

    val language: StateFlow<AppLanguage> = prefs.languageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppLanguage.UZ
    )

    val themeMode: StateFlow<ThemeMode> = prefs.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ThemeMode.SYSTEM
    )

    val selectedDistrict: StateFlow<String> = prefs.districtFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = "Yunusobod"
    )

    val favoriteProductIds: StateFlow<Set<String>> = prefs.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptySet()
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(language)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun setDistrict(district: String) {
        viewModelScope.launch {
            prefs.setDistrict(district)
        }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            prefs.toggleFavorite(productId)
        }
    }
}
