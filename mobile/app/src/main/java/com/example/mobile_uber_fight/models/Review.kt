package com.example.mobile_uber_fight.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Review(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fightId: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    @ServerTimestamp val createdAt: Date? = null
)
