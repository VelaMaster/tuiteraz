// RUTA: app/src/main/java/com/example/balance/MainActivity.kt
package com.example.balance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.balance.ui.theme.BalanceTheme

// minSdk = 28 → java.time siempre disponible → sin @RequiresApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BalanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val fraseDePrueba = Frase(
                        texto = "Inserte Texto.",
                        autor = "Autor"
                    )
                    PantallaPrincipalConNavegacion(fraseActual = fraseDePrueba)
                }
            }
        }
    }
}