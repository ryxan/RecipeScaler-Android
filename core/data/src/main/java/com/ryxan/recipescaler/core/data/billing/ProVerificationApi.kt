package com.ryxan.recipescaler.core.data.billing

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Backend API for server-side purchase verification.
 *
 * The backend calls the Google Play Developer API to verify purchase tokens,
 * which is the authoritative source of truth for entitlement. Client-side
 * verification (the Play [com.android.billingclient.api.Purchase] state alone)
 * can be spoofed on rooted devices.
 */
interface ProVerificationApi {

    /**
     * Verifies [request] against the backend.
     *
     * @throws HttpError on a non-2xx HTTP status (inspect [HttpError.code])
     * @throws IOException on a network failure
     * @throws kotlinx.serialization.SerializationException on a malformed body
     */
    suspend fun verifyPurchase(request: VerifyPurchaseRequest): VerifyPurchaseResponse
}

/** Non-2xx response from the verification backend; [code] is the HTTP status. */
class HttpError(val code: Int) : Exception("HTTP $code")

/**
 * Request sent to the backend for purchase verification.
 *
 * @param packageName App package name (com.ryxan.recipescaler, or
 *   com.ryxan.recipescaler.debug in debug builds — the backend intentionally
 *   rejects the debug package; `setDebugPro` covers debug testing).
 * @param productId Product SKU ([ProProductIds.MONTHLY] / [ProProductIds.LIFETIME])
 * @param purchaseToken Opaque token from Google Play — never logged
 * @param productType "subscription" or "inapp"
 */
@Serializable
data class VerifyPurchaseRequest(
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val productType: String,
)

/**
 * Response from the backend after verifying with Google Play.
 *
 * @param valid true if Google confirmed the purchase is legitimate — the only
 *   field that decides the grant
 * @param status machine-readable status ('active', 'pending', 'invalid',
 *   'rejected') for logging/debugging only
 */
@Serializable
data class VerifyPurchaseResponse(
    val valid: Boolean,
    val status: String = "",
)

/** OkHttp-backed [ProVerificationApi] with 10s connect/read timeouts. */
class OkHttpProVerificationApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ProVerificationApi {

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): VerifyPurchaseResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(request).toRequestBody(JSON_MEDIA_TYPE)
            val httpRequest = Request.Builder()
                .url(VERIFY_URL)
                .post(body)
                .build()
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) throw HttpError(response.code)
                val payload = response.body?.string()
                    ?: throw IOException("Empty verification response (HTTP ${response.code})")
                json.decodeFromString<VerifyPurchaseResponse>(payload)
            }
        }

    private companion object {
        const val VERIFY_URL = "https://api.riverwatch.cc/verify-purchase"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
