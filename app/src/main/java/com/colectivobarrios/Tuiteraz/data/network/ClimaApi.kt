package com.colectivobarrios.Tuiteraz.data.network

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Keep
data class ClimaResponse(
    @SerializedName("current_weather")
    val current_weather: CurrentWeather
)

@Keep
data class CurrentWeather(
    @SerializedName("temperature")
    val temperature: Double,
    @SerializedName("weathercode")
    val weathercode: Int
)

@Keep
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun obtenerClimaActual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): ClimaResponse
}

object RedClima {
    // Timeout corto: si no responde en 8s, falla rápido y muestra el caché
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val api: OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)
}