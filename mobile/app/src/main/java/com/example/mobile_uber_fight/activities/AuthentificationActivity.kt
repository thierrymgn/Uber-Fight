package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobile_uber_fight.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class AuthentificationActivity : AppCompatActivity() {
    lateinit var tvRegister: TextView
    lateinit var textInputLayoutEmail: TextInputLayout
    lateinit var textInputLayoutPassword: TextInputLayout
    lateinit var btnLogin: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_authentification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvRegister = findViewById(R.id.tvRegister)
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)
        btnLogin = findViewById(R.id.btnLogin)

        tvRegister.setOnClickListener {
            Intent(this, RegisterActivity::class.java).also {
                startActivity(it)
            }
        }

        btnLogin.setOnClickListener {
            textInputLayoutPassword.isErrorEnabled = false
            textInputLayoutPassword.isErrorEnabled = false

            val email = textInputLayoutEmail.editText?.text.toString()
            val password = textInputLayoutPassword.editText?.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                if (email.isEmpty()) {
                    textInputLayoutEmail.error = "L'email est requis"
                    textInputLayoutPassword.isErrorEnabled = true
                }
                if (password.isEmpty()) {
                    textInputLayoutPassword.error = "Le mot de passe est requis"
                    textInputLayoutPassword.isErrorEnabled = true
                }
            } else {
                signIn(email, password)
            }
        }
    }

    fun signIn(email: String, password: String) {
        Log.d("AuthentificationActivity", "signIn: $email, $password")
        Intent(this, HomeActivity::class.java).also {
            startActivity(it)
        }
    }
}