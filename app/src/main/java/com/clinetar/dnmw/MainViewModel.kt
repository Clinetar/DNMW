package com.clinetar.dnmw

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val themeSettings = ThemeSettings(application)

    val themeState: StateFlow<AppTheme> = themeSettings.themeStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val customColorState: StateFlow<CustomColor> = themeSettings.customColorStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CustomColor.DYNAMIC
        )

    val pureBlackState: StateFlow<Boolean> = themeSettings.pureBlackStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeSettings.setTheme(theme)
        }
    }

    fun setCustomColor(color: CustomColor) {
        viewModelScope.launch {
            themeSettings.setCustomColor(color)
        }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch {
            themeSettings.setPureBlack(enabled)
        }
    }
}
