package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.ActivityAuthentificationBinding
import com.example.mobile_uber_fight.logger.GrafanaLogger
import com.example.mobile_uber_fight.repositories.AuthRepository
import com.example.mobile_uber_fight.utils.RoleManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

class AuthentificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthentificationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val authRepository = AuthRepository()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                GrafanaLogger.logInfo("Google Sign-In account retrieved", mapOf("email" to account.email.toString()))
                firebaseAuthWithGoogle(idToken)
            } catch (e: ApiException) {
                GrafanaLogger.logError("Google Sign-In failed", e)
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthentificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvForgotPassword.setOnClickListener {
            resetPassword()
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        for (i in 0 until binding.btnGoogleSignIn.childCount) {
            val view = binding.btnGoogleSignIn.getChildAt(i)
            if (view is TextView) {
                view.gravity = Gravity.CENTER
                view.layoutParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
    }

    private fun signInWithGoogle() {
        GrafanaLogger.logInfo("Starting Google Sign-In flow")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        GrafanaLogger.logInfo("Authenticating with Firebase via Google")
        authRepository.firebaseAuthWithGoogle(
            idToken = idToken,
            onSuccess = { isNewUser ->
                if (isNewUser) {
                    GrafanaLogger.logInfo("New user detected via Google, redirecting to RoleSelection")
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                } else {
                    GrafanaLogger.logInfo("Existing user via Google, routing to main interface")
                    RoleManager.routeUser(
                        onSuccess = { role ->
                            val intent = when (role) {
                                RoleManager.UserRole.FIGHTER -> Intent(this, FighterMainActivity::class.java)
                                RoleManager.UserRole.CLIENT -> Intent(this, ClientMainActivity::class.java)
                                RoleManager.UserRole.UNKNOWN -> Intent(this, RoleSelectionActivity::class.java)
                            }
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        },
                        onFailure = { e ->
                            GrafanaLogger.logError("Role routing failed after Google login", e)
                            Toast.makeText(this, "Erreur de redirection: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            onFailure = { e ->
                GrafanaLogger.logError("Firebase Google Auth failed", e)
                Toast.makeText(this, "Erreur d'authentification Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
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

        GrafanaLogger.logInfo("Attempting login", mapOf("email" to email))

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                GrafanaLogger.logInfo("Login successful", mapOf("email" to email))
                RoleManager.routeUser(
                    onSuccess = { role ->
                        val intent = when (role) {
                            RoleManager.UserRole.FIGHTER -> Intent(this, FighterMainActivity::class.java)
                            RoleManager.UserRole.CLIENT -> Intent(this, ClientMainActivity::class.java)
                            RoleManager.UserRole.UNKNOWN -> {
                                Log.w("AuthActivity", "Rôle inconnu, redirection vers RoleSelectionActivity.")
                                GrafanaLogger.logWarn("User role unknown", mapOf("email" to email))
                                Intent(this, RoleSelectionActivity::class.java)
                            }
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { exception ->
                        GrafanaLogger.logError("Role routing failed", exception, mapOf("email" to email))
                        Toast.makeText(this, "Impossible de vérifier le rôle: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .addOnFailureListener { e ->
                GrafanaLogger.logError("Login failed", e, mapOf("email" to email))
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun resetPassword() {
        val email = binding.textInputLayoutEmail.editText?.text.toString().trim()
        
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = getString(R.string.veuillez_saisir_votre_email)
            return
        }
        binding.textInputLayoutEmail.error = null

        GrafanaLogger.logInfo("Requesting password reset", mapOf("email" to email))

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                GrafanaLogger.logInfo("Password reset email sent", mapOf("email" to email))
                Toast.makeText(this, getString(R.string.un_email_de_reinitialisation_a_ete_envoye), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                GrafanaLogger.logError("Password reset failed", e, mapOf("email" to email))
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
