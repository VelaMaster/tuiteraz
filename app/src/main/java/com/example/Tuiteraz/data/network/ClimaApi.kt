package com.example.Tuiteraz.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Los modelos de datos que recibimos de la API
data class ClimaResponse(
    val current_weather: CurrentWeather
)

data class CurrentWeather(
    val temperature: Double,
    val weathercode: Int // Este código nos dirá si está soleado, lloviendo, etc.
)

// 2. La interfaz de Retrofit
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun obtenerClimaActual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): ClimaResponse
}

// 3. El objeto para crear la instancia de la API
object RedClima {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)
}