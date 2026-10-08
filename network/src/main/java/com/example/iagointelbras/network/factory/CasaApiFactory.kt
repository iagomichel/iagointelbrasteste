package com.example.iagointelbras.network.factory

import com.example.iagointelbras.network.api.CasaApi
import com.example.iagointelbras.network.auth.AccessTokenProvider
import com.example.iagointelbras.network.auth.BearerTokenInterceptor
import com.example.iagointelbras.network.config.CasaHosts
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object CasaApiFactory {
    fun create(tokenProvider: AccessTokenProvider, moshi: Moshi): CasaApi =
        Retrofit.Builder()
            .baseUrl(CasaHosts.API_BASE_URL)
            .client(createClient(tokenProvider))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CasaApi::class.java)

    private fun createClient(tokenProvider: AccessTokenProvider) = OkHttpClient.Builder()
        .addInterceptor(BearerTokenInterceptor(tokenProvider))
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 15L
}
