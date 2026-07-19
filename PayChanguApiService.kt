package com.shopizzo.data.repository

import com.shopizzo.data.model.PayChanguRequest
import com.shopizzo.data.model.PayChanguResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the PayChangu payment gateway.
 * Sandbox base URL: https://api.paychangu.com/
 */
interface PayChanguApiService {

    /**
     * Initiates a payment session.
     * Returns a checkout URL that the user is redirected to.
     *
     * @param secretKey  Your PayChangu secret/public key (Bearer token)
     * @param request    Payment details (amount, email, etc.)
     */
    @POST("payment")
    suspend fun initiatePayment(
        @Header("Authorization") secretKey: String,
        @Body request: PayChanguRequest
    ): Response<PayChanguResponse>
}

/**
 * Singleton that provides a ready-to-use [PayChanguApiService].
 * Replace [PAYCHANGU_PUBLIC_KEY] with your actual sandbox key from the dashboard.
 */
object PayChanguClient {

    // ⚠️  Replace this with your real PayChangu sandbox public key
    const val PAYCHANGU_PUBLIC_KEY = "pub-test-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"

    private const val BASE_URL = "https://api.paychangu.com/"

    /** Logging interceptor – useful during development; disable in release */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val service: PayChanguApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PayChanguApiService::class.java)
    }
}
