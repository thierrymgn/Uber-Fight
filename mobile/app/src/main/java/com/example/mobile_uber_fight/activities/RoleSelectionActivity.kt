package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardClient.setOnClickListener {
            navigateToRegister("CLIENT")
        }

        binding.cardFighter.setOnClickListener {
            navigateToRegister("FIGHTER")
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, AuthentificationActivity::class.java))
        }
    }

    private fun navigateToRegister(role: String) {
        val intent = Intent(this, RegisterActivity::class.java).apply {
            putExtra("ROLE", role)
        }
        startActivity(intent)
    }
}
