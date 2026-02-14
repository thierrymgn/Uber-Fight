package com.example.mobile_uber_fight.logger

import android.util.Log
import com.example.mobile_uber_fight.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GrafanaLogger {

    private const val TAG = "GrafanaLogger"
    private val API_URL = BuildConfig.GRAFANA_API_URL
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class LogLevel(val value: String) {
        INFO("info"), WARN("warn"), ERROR("error"), DEBUG("debug")
    }

    fun logInfo(message: String, attributes: Map<String, Any>? = null) = log(message, LogLevel.INFO, attributes)
    fun logWarn(message: String, attributes: Map<String, Any>? = null) = log(message, LogLevel.WARN, attributes)
    fun logDebug(message: String, attributes: Map<String, Any>? = null) = log(message, LogLevel.DEBUG, attributes)

    fun logError(message: String, error: Throwable? = null, attributes: Map<String, Any>? = null) {
        val enriched = attributes?.toMutableMap() ?: mutableMapOf()
        error?.let {
            enriched["errorType"] = it.javaClass.simpleName
            enriched["errorMessage"] = it.message ?: "No message"
            enriched["errorStack"] = it.stackTraceToString().take(1000)
        }
        log(message, LogLevel.ERROR, enriched)
    }

    private fun log(message: String, level: LogLevel, attributes: Map<String, Any>? = null) {
        val localMsg = if (attributes != null) "$message | $attributes" else message
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, localMsg)
            LogLevel.WARN -> Log.w(TAG, localMsg)
            LogLevel.DEBUG -> Log.d(TAG, localMsg)
            else -> Log.i(TAG, localMsg)
        }

        logScope.launch {
            sendToAPI(message, level, attributes)
        }
    }

    private suspend fun sendToAPI(message: String, level: LogLevel, attributes: Map<String, Any>?) {
        try {
            val jsonBody = buildJsonBody(message, level, attributes)
            val request = Request.Builder()
                .url(API_URL)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Loki API Error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send log to Grafana: ${e.message}")
        }
    }

    private fun buildJsonBody(message: String, level: LogLevel, attributes: Map<String, Any>?): String {
        val json = JSONObject()
        json.put("level", level.value)
        json.put("message", message)

        val attrs = JSONObject()
        attrs.put("platform", "android")
        attrs.put("appVersion", BuildConfig.VERSION_NAME)
        attrs.put("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        attrs.put("osVersion", android.os.Build.VERSION.RELEASE)
        attrs.put("timestamp", System.currentTimeMillis())

        FirebaseAuth.getInstance().currentUser?.uid?.let {
            attrs.put("userId", it)
        }

        attributes?.forEach { (key, value) -> attrs.put(key, value) }
        json.put("attributes", attrs)

        return json.toString()
    }

    fun shutdown() = logScope.cancel()
}