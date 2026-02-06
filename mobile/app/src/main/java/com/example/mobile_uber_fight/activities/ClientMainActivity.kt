package com.example.mobile_uber_fight.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mobile_uber_fight.R
import com.example.mobile_uber_fight.databinding.ActivityClientMainBinding
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobile_uber_fight.ui.shared.RatingBottomSheetFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientMainBinding

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update("fcmToken", token)
            }
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
        updateFcmToken()
        
        handleNotificationAction()
    }

    private fun handleNotificationAction() {
        val clickAction = intent.getStringExtra("click_action")
        if (clickAction == "OPEN_RATING") {
            val fightId = intent.getStringExtra("fight_id")
            val userIdToRate = intent.getStringExtra("user_id_to_rate")
            
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
                    val ratingFragment = RatingBottomSheetFragment.newInstance(userIdToRate, fightId) {
                    }
                    ratingFragment.show(supportFragmentManager, "Rating")
                } else {
                    Log.d("ClientMainActivity", "Combat déjà noté (ID: $fightId)")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClientMainActivity", "Erreur lors de la vérification de la note", e)
            }
    }
}
