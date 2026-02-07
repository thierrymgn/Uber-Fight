package com.example.mobile_uber_fight.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.ActivityClientMainBinding
import com.example.mobile_uber_fight.ui.shared.RatingBottomSheetFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class ClientMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientMainBinding
    private val TAG = "ClientMainActivity"

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            updateFcmToken()
        } else {
            Log.w(TAG, "Permission de notification refusée")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_client) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavViewClient.setupWithNavController(navController)

        checkNotificationPermission()

        handleNotificationAction(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationAction(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                updateFcmToken()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            updateFcmToken()
        }
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("fcmToken", task.result)
                }
            }
        }
    }

    private fun handleNotificationAction(intent: Intent?) {
        val clickAction = intent?.getStringExtra("click_action")
        Log.d(TAG, "handleNotificationAction: action received = $clickAction")

        if (clickAction == "OPEN_RATING") {
            val fightId = intent?.getStringExtra("fight_id")
            val userIdToRate = intent?.getStringExtra("user_id_to_rate")

            Log.d(TAG, "Extraits : fightId=$fightId, userToRate=$userIdToRate")

            if (fightId != null && userIdToRate != null) {
                checkIfAlreadyRated(fightId, userIdToRate)
            }
        }
    }

    private fun checkIfAlreadyRated(fightId: String, userIdToRate: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance().collection("reviews")
            .whereEqualTo("fightId", fightId)
            .whereEqualTo("fromUserId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "Ouverture de la notation pour le combat $fightId")
                    val ratingFragment = RatingBottomSheetFragment.newInstance(userIdToRate, fightId) {
                        intent.removeExtra("click_action")
                    }
                    ratingFragment.show(supportFragmentManager, "Rating")
                } else {
                    Log.d(TAG, "Combat déjà noté, on n'affiche rien.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erreur verification review", e)
            }
    }
}