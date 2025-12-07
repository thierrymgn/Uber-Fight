package com.example.mobile_uber_fight.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityFighterBinding

class FighterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFighterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFighterBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
