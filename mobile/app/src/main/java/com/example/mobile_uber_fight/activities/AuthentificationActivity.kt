package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityAuthentificationBinding
import com.example.mobile_uber_fight.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class AuthentificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthentificationBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthentificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binding.textInputLayoutEmail.editText?.text.toString().trim()
        val password = binding.textInputLayoutPassword.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            if (email.isEmpty()) {
                binding.textInputLayoutEmail.error = "L'email est requis"
            }
            if (password.isEmpty()) {
                binding.textInputLayoutPassword.error = "Le mot de passe est requis"
            }
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                RoleManager.routeUser(
                    onSuccess = { role ->
                        val intent = when (role) {
                            RoleManager.UserRole.FIGHTER -> Intent(this, FighterMainActivity::class.java)
                            RoleManager.UserRole.CLIENT -> Intent(this, ClientMainActivity::class.java)
                            RoleManager.UserRole.UNKNOWN -> {
                                Log.w("AuthActivity", "Rôle inconnu, redirection vers RoleSelectionActivity.")
                                Intent(this, RoleSelectionActivity::class.java)
                            }
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { exception ->
                        Toast.makeText(this, "Impossible de vérifier le rôle: ${exception.message}", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, AuthentificationActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
