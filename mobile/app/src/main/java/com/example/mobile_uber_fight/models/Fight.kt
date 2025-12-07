package com.example.mobile_uber_fight.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Fight(
    val id: String = "",
    val requesterId: String = "",
    val fighterId: String? = null,
    val status: String = "PENDING",
    val fightType: String = "UNKNOWN_OPPONENT",
    val location: GeoPoint? = null,
    val address: String = "",
    @ServerTimestamp val scheduledTime: Date? = null,
    @ServerTimestamp val createdAt: Date? = null
)
