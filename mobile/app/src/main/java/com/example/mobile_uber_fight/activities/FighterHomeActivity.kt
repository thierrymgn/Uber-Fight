package com.example.mobile_uber_fight.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityFighterHomeBinding

class FighterHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFighterHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFighterHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
