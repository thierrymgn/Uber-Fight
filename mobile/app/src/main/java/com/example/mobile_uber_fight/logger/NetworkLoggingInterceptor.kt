package com.example.mobile_uber_fight.logger

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class NetworkLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "NetworkLoggingInterceptor"
        private const val SLOW_REQUEST_THRESHOLD_MS = 3000L
    }

    private val interceptorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        val url = request.url.toString()
        val method = request.method
        
        return try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            val statusCode = response.code
            
            interceptorScope.launch {
                handleResponse(url, method, statusCode, duration)
            }
            
            response
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            interceptorScope.launch {
                logNetworkError(url, method, e, duration)
            }
            throw e
        }
    }

    private fun handleResponse(url: String, method: String, statusCode: Int, duration: Long) {
        val sanitized = sanitizeUrl(url)
        val route = extractRoute(sanitized)

        val attributes = mutableMapOf<String, Any>(
            "category" to "network",
            "url" to sanitized,
            "method" to method,
            "statusCode" to statusCode,
            "durationMs" to duration
        )

        when {
            statusCode >= 500 -> {
                GrafanaLogger.logError("Network Error (Server)", null, attributes)
            }
            statusCode >= 400 -> {
                GrafanaLogger.logWarn("Network Warning (Client)", attributes)
            }
            duration > SLOW_REQUEST_THRESHOLD_MS -> {
                attributes["isSlow"] = true
                GrafanaLogger.logWarn("Network Slow Request", attributes)
            }
            else -> {
                GrafanaLogger.logInfo("Network Success", attributes)
            }
        }

        GrafanaMetrics.networkRequest(method, route, statusCode, duration)
    }

    private fun logNetworkError(url: String, method: String, error: Exception, duration: Long) {
        val attributes = mapOf(
            "category" to "network",
            "url" to sanitizeUrl(url),
            "method" to method,
            "durationMs" to duration,
            "errorType" to error.javaClass.simpleName
        )
        GrafanaLogger.logError("Network Connection Failed: ${error.message}", error, attributes)
    }

    private fun extractRoute(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.path ?: url
        } catch (e: Exception) {
            url.substringBefore("?").substringAfter("//").substringAfter("/", "/")
        }
    }

    private fun sanitizeUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val query = uri.query
            if (query.isNullOrEmpty()) return url

            val sensitiveKeys = listOf("key", "token", "auth", "secret", "password", "api_key")
            val sanitizedQuery = query.split("&").joinToString("&") { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2 && sensitiveKeys.any { parts[0].lowercase().contains(it) }) {
                    "${parts[0]}=[REDACTED]"
                } else param
            }
            
            val portStr = if (uri.port != -1) ":${uri.port}" else ""
            "${uri.scheme}://${uri.host}$portStr${uri.path}?$sanitizedQuery"
        } catch (e: Exception) {
            url.substringBefore("?")
        }
    }
}
