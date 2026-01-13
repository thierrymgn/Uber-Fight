package com.example.mobile_uber_fight.models

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val preferredLanguage: String = "fr",
    val paymentMethodId: String? = null
)
