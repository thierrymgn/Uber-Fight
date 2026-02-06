package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            if (currentUser != null) {
                RoleManager.routeUser(
                    onSuccess = { role ->
                        val intent = when (role) {
                            RoleManager.UserRole.FIGHTER -> Intent(this, FighterMainActivity::class.java)
                            RoleManager.UserRole.CLIENT -> Intent(this, ClientMainActivity::class.java)
                            RoleManager.UserRole.UNKNOWN -> Intent(this, RoleSelectionActivity::class.java)
                        }
                        
                        getIntent().extras?.let {
                            intent.putExtras(it)
                        }
                        
                        startActivity(intent)
                        finish()
                    },
                    onFailure = {
                        startActivity(Intent(this, AuthentificationActivity::class.java))
                        finish()
                    }
                )
            } else {
                startActivity(Intent(this, AuthentificationActivity::class.java))
                finish()
            }
        }, 2000)
    }
}