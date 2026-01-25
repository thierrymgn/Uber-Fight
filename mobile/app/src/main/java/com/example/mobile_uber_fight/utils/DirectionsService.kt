package com.example.mobile_uber_fight.utils

import android.util.Log
import com.example.mobile_uber_fight.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class RouteInfo(
    val polyline: List<LatLng>,
    val duration: String,
    val distance: String
)

object DirectionsService {
    private val client = OkHttpClient()

    suspend fun getDirections(origin: LatLng, destination: LatLng): RouteInfo? = withContext(Dispatchers.IO) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}&" +
                "destination=${destination.latitude},${destination.longitude}&" +
                "mode=walking&" +
                "key=${BuildConfig.MAPS_API_KEY}"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                
                val jsonData = response.body?.string() ?: return@withContext null
                val jsonObject = JSONObject(jsonData)
                
                if (jsonObject.getString("status") != "OK") {
                    Log.e("DirectionsService", "API Error: ${jsonObject.getString("status")}")
                    return@withContext null
                }

                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() == 0) return@withContext null
                
                val route = routes.getJSONObject(0)
                val legs = route.getJSONArray("legs")
                if (legs.length() == 0) return@withContext null
                
                val leg = legs.getJSONObject(0)
                val duration = leg.getJSONObject("duration").getString("text")
                val distance = leg.getJSONObject("distance").getString("text")
                
                val encodedPolyline = route.getJSONObject("overview_polyline").getString("points")
                val points = decodePolyline(encodedPolyline)
                
                return@withContext RouteInfo(points, duration, distance)
            }
        } catch (e: Exception) {
            Log.e("DirectionsService", "Network Error", e)
            null
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}