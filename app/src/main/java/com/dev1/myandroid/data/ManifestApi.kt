package com.dev1.myandroid.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// ── Change this to your VPS domain ──────────────────────────────────────────
private const val BASE_URL = "https://rickyscontrolcenter.com/"

interface ManifestApi {
    @GET("api/apk/manifest")
    suspend fun getManifest(): ApkManifest
}

object ApiClient {
    private val okhttp = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val manifest: ManifestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ManifestApi::class.java)
    }
}
