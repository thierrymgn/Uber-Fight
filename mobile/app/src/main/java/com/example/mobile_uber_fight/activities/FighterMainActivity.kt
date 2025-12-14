package com.example.mobile_uber_fight.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.ActivityFighterMainBinding

class FighterMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFighterMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFighterMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_fighter) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavViewFighter.setupWithNavController(navController)
    }
}
