package com.example.mobile_uber_fight.models

import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date
import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<GeoPoint?, GeoPointParceler>
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
) : Parcelable

object GeoPointParceler : Parceler<GeoPoint?> {
    override fun create(parcel: Parcel): GeoPoint? {
        val hasValue = parcel.readByte() == 1.toByte()
        return if (hasValue) {
            val lat = parcel.readDouble()
            val lon = parcel.readDouble()
            GeoPoint(lat, lon)
        } else {
            null
        }
    }

    override fun GeoPoint?.write(parcel: Parcel, flags: Int) {
        if (this != null) {
            parcel.writeByte(1.toByte())
            parcel.writeDouble(this.latitude)
            parcel.writeDouble(this.longitude)
        } else {
            parcel.writeByte(0.toByte())
        }
    }
}
