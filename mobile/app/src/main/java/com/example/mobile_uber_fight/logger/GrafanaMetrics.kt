package com.example.mobile_uber_fight.logger

import android.util.Log
import com.example.mobile_uber_fight.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GrafanaMetrics {

    private const val TAG = "GrafanaMetrics"
    private val API_URL = BuildConfig.GRAFANA_API_URL + "/api/metrics"
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val metricsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun counter(name: String, value: Int = 1, attributes: Map<String, Any>? = null) {
        pushMetric("counter", name, value.toDouble(), attributes)
    }

    fun histogram(name: String, valueMs: Long, attributes: Map<String, Any>? = null) {
        pushMetric("histogram", name, valueMs.toDouble(), attributes)
    }

    fun screenView(screenName: String) {
        counter("mobile.screen.view", 1, mapOf("screen" to screenName))
    }

    fun userAction(action: String, attributes: Map<String, Any>? = null) {
        val enriched = mutableMapOf<String, Any>("action" to action)
        attributes?.let { enriched.putAll(it) }
        counter("mobile.app.user_action", 1, enriched)
    }

    fun networkRequest(method: String, route: String, statusCode: Int, durationMs: Long) {
        histogram("mobile.network.duration", durationMs, mapOf(
            "method" to method,
            "route" to route,
            "status_code" to statusCode
        ))
        counter("mobile.network.request_count", 1, mapOf(
            "method" to method,
            "route" to route,
            "status_code" to statusCode
        ))
        if (statusCode >= 500) {
            counter("mobile.network.error_count", 1, mapOf(
                "method" to method,
                "route" to route,
                "status_code" to statusCode
            ))
        }
    }

    fun fightAction(action: String, attributes: Map<String, Any>? = null) {
        val enriched = mutableMapOf<String, Any>("action" to action)
        attributes?.let { enriched.putAll(it) }
        counter("mobile.fight.action", 1, enriched)
    }

    fun authEvent(event: String, success: Boolean, method: String? = null) {
        val attrs = mutableMapOf<String, Any>(
            "event" to event,
            "success" to success
        )
        method?.let { attrs["method"] = it }
        counter("mobile.auth.event", 1, attrs)
    }

    private fun pushMetric(type: String, name: String, value: Double, attributes: Map<String, Any>?) {
        Log.d(TAG, "[$type] $name = $value")

        metricsScope.launch {
            sendToAPI(type, name, value, attributes)
        }
    }

    private suspend fun sendToAPI(type: String, name: String, value: Double, attributes: Map<String, Any>?) {
        try {
            val json = JSONObject()
            json.put("type", type)
            json.put("name", name)
            json.put("value", value)

            val attrs = JSONObject()
            attrs.put("platform", "android")
            attrs.put("appVersion", BuildConfig.VERSION_NAME)
            attrs.put("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            attrs.put("osVersion", android.os.Build.VERSION.RELEASE)

            FirebaseAuth.getInstance().currentUser?.uid?.let {
                attrs.put("userId", it)
            }

            attributes?.forEach { (key, v) -> attrs.put(key, v) }
            json.put("attributes", attrs)

            val request = Request.Builder()
                .url(API_URL)
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Metrics API Error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send metric: ${e.message}")
        }
    }

    fun shutdown() = metricsScope.cancel()
}
