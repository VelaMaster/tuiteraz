package com.colectivobarrios.Tuiteraz.data.network

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Modelos protegidos contra la ofuscación de R8
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

// 2. La interfaz TAMBIÉN lleva @Keep para que no exploten las Corrutinas (suspend)
@Keep
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