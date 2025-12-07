package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityRegisterBinding
import com.example.mobile_uber_fight.repositories.AuthRepository

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { 
            registerUser()
        }
    }

    private fun registerUser() {
        val fullName = binding.textInputLayoutFullName.editText?.text.toString().trim()
        val email = binding.textInputLayoutEmail.editText?.text.toString().trim()
        val password = binding.textInputLayoutPassword.editText?.text.toString().trim()
        val confirmPassword = binding.textInputLayoutConfirmPassword.editText?.text.toString().trim()

        if (!validateInput(fullName, email, password, confirmPassword)) {
            return
        }

        authRepository.register(email, password, fullName)
            .addOnSuccessListener { 
                goToHomeActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun validateInput(fullName: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        binding.textInputLayoutFullName.error = null
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null
        binding.textInputLayoutConfirmPassword.error = null

        if (fullName.isEmpty()) {
            binding.textInputLayoutFullName.error = "Le Nom Complet est requis"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "L'email est requis"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.error = "Format d'email invalide"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Le mot de passe est requis"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.textInputLayoutConfirmPassword.error = "La confirmation du mot de passe est requise"
            isValid = false
        }

        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            binding.textInputLayoutConfirmPassword.error = "Les mots de passe ne correspondent pas"
            isValid = false
        }

        return isValid
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
