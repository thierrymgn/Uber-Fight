package com.example.mobile_uber_fight.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val role: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val rating: Double = 5.0,
    val ratingCount: Int = 0,
    val location: GeoPoint? = null,
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Date? = null
)
