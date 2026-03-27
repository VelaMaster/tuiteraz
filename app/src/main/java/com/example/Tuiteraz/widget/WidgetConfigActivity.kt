package com.example.Tuiteraz.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.example.Tuiteraz.data.local.CacheFrases
import com.example.Tuiteraz.ui.theme.BalanceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OpcionFuente(val nombre: String, val etiqueta: String, val fontFamily: FontFamily)
private data class OpcionTamano(val nombre: String, val etiqueta: String, val sp: Int)

private val FUENTES = listOf(
    OpcionFuente("Moderna", "Aa", FontFamily.SansSerif),
    OpcionFuente("Clásica", "Aa", FontFamily.Serif),
    OpcionFuente("Máquina", "Aa", FontFamily.Monospace),
)

private val TAMANOS = listOf(
    OpcionTamano("Pequeña", "S", 14),
    OpcionTamano("Mediana", "M", 16),
    OpcionTamano("Grande",  "L", 18),
)

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val sharedPrefs = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        val cacheLocal  = CacheFrases(applicationContext)

        setContent {
            BalanceTheme {
                var fuente by remember { mutableStateOf(sharedPrefs.getString("fuente", "Moderna") ?: "Moderna") }
                var tamano by remember { mutableStateOf(sharedPrefs.getInt("tamano", 16)) }
                var textoPreview by remember { mutableStateOf("Cargando frase…") }
                var autorPreview by remember { mutableStateOf("Tuiteraz") }
                var guardando by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    cacheLocal.obtenerFraseCacheada()?.let {
                        textoPreview = it.texto
                        autorPreview = it.autor
                    }
                }

                val tipografiaPreview = FUENTES.first { it.nombre == fuente }.fontFamily

                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { finish() }, contentAlignment = Alignment.BottomCenter) {
                    Surface(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().clickable(enabled = false) {}, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                            Spacer(Modifier.height(20.dp))
                            Text("Apariencia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

                            Spacer(Modifier.height(24.dp))
                            PreviewWidget(textoPreview, autorPreview, tamano, tipografiaPreview)

                            Spacer(Modifier.height(24.dp))
                            SelectorLabel("Estilo de letra")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FUENTES.forEach { opcion ->
                                    TarjetaSelector(seleccionado = fuente == opcion.nombre, modifier = Modifier.weight(1f), onClick = { fuente = opcion.nombre }) {
                                        Text(opcion.etiqueta, fontFamily = opcion.fontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text(opcion.nombre, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            SelectorLabel("Tamaño")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TAMANOS.forEach { opcion ->
                                    TarjetaSelector(seleccionado = tamano == opcion.sp, modifier = Modifier.weight(1f), onClick = { tamano = opcion.sp }) {
                                        Text(opcion.etiqueta, fontSize = opcion.sp.sp, fontWeight = FontWeight.Bold)
                                        Text(opcion.nombre, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { finish() }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
// Sustituye el bloque del Button "Guardar" por este:
                                Button(
                                    onClick = {
                                        if (guardando) return@Button
                                        guardando = true

                                        // 1. Guardado síncrono (esencial para widgets en otros procesos)
                                        val success = sharedPrefs.edit()
                                            .putString("fuente", fuente)
                                            .putInt("tamano", tamano)
                                            .commit()

                                        Log.d("TUITERAZ_LOG", "Guardado en App: $success (fuente=$fuente, tamano=$tamano)")

                                        lifecycleScope.launch {
                                            try {
                                                // 2. Notificamos a Glance que todo debe actualizarse
                                                FraseWidget().updateAll(applicationContext)

                                                // 3. Pequeño margen para que el SO procese la actualización
                                                kotlinx.coroutines.delay(500)

                                                val result = Intent().apply {
                                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                                }
                                                setResult(RESULT_OK, result)
                                                finish()
                                            } catch (e: Exception) {
                                                Log.e("TUITERAZ_LOG", "Error actualizando widget", e)
                                                finish()
                                            }
                                        }
                                    },
                                    enabled = !guardando,
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (guardando) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Guardar", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorLabel(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

@Composable
private fun TarjetaSelector(seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).border(2.dp, if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)).clickable { onClick() },
        color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    ) { Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content) }
}

@Composable
private fun PreviewWidget(texto: String, autor: String, tamanoSp: Int, fontFamily: FontFamily) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u201C", fontSize = (tamanoSp + 6).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = fontFamily)
            Text(texto, fontSize = tamanoSp.sp, textAlign = TextAlign.Center, fontStyle = FontStyle.Italic, fontFamily = fontFamily, maxLines = 3)
            Text("— ${autor.uppercase()}", fontSize = (tamanoSp - 4).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = fontFamily)
        }
    }
}