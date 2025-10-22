package com.example.mobile_uber_fight

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class UberFightApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}