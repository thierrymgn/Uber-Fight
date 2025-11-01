package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobile_uber_fight.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var textInputLayoutFullName: TextInputLayout
    lateinit var textInputLayoutEmail: TextInputLayout
    lateinit var textInputLayoutPassword: TextInputLayout
    lateinit var textInputLayoutConfirmPassword: TextInputLayout
    lateinit var btnRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        textInputLayoutFullName = findViewById(R.id.textInputLayoutFullName)
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
    }

    override fun onStart() {
        super.onStart()

        btnRegister.setOnClickListener {

            initErrors()

            val fullName = textInputLayoutFullName.editText?.text.toString()
            val email = textInputLayoutEmail.editText?.text.toString()
            val password = textInputLayoutPassword.editText?.text.toString()
            val confirmPassword = textInputLayoutConfirmPassword.editText?.text.toString()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                if (fullName.isEmpty()) {
                    textInputLayoutFullName.error = "Le Nom Complet est requis"
                    textInputLayoutFullName.isErrorEnabled = true
                }
                if (email.isEmpty()) {
                    textInputLayoutEmail.error = "L'email est requis"
                    textInputLayoutEmail.isErrorEnabled = true
                }
                if (password.isEmpty()) {
                    textInputLayoutPassword.error = "Le mot de passe est requis"
                    textInputLayoutPassword.isErrorEnabled = true
                }
                if (confirmPassword.isEmpty()) {
                    textInputLayoutConfirmPassword.error = "La confirmation du mot de passe est requise"
                    textInputLayoutConfirmPassword.isErrorEnabled = true
                }
            } else {
                if (password != confirmPassword) {
                    textInputLayoutConfirmPassword.error = "Les mots de passe ne correspondent pas"
                    textInputLayoutConfirmPassword.isErrorEnabled = true
                } else {
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Intent(this, HomeActivity::class.java).also {
                                startActivity(it)
                            }
                        } else {
                            textInputLayoutConfirmPassword.error = task.exception?.message
                            textInputLayoutConfirmPassword.isErrorEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun RegisterActivity.initErrors() {
        textInputLayoutFullName.isErrorEnabled = false
        textInputLayoutEmail.isErrorEnabled = false
        textInputLayoutPassword.isErrorEnabled = false
        textInputLayoutConfirmPassword.isErrorEnabled = false
    }
}
