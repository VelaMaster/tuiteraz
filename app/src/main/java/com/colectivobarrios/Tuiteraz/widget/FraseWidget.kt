package com.colectivobarrios.Tuiteraz.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.*
import com.colectivobarrios.Tuiteraz.MainActivity
import com.colectivobarrios.Tuiteraz.data.local.CacheFrases
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FraseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Datos leídos UNA sola vez antes de provideContent para optimizar rendimiento
        val cache = CacheFrases(context)
        val fraseGuardada = cache.obtenerFraseCacheada()

        val textoWidget = fraseGuardada?.texto ?: "La inspiración existe, pero tiene que encontrarte trabajando."
        val autorWidget  = fraseGuardada?.autor ?: "Tuiteraz"
        val inicialAutor = autorWidget.firstOrNull()?.uppercaseChar()?.toString() ?: "T"

        // Tamaño dinámico balanceado entre ambas versiones
        val longitud = textoWidget.length
        val tamanoFrase = when {
            longitud < 50  -> 18.sp
            longitud < 100 -> 15.sp
            else           -> 13.sp
        }

        provideContent {
            val ctx = LocalContext.current.applicationContext

            val esquemaClaro = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicLightColorScheme(ctx) else lightColorScheme()
            val esquemaOscuro = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicDarkColorScheme(ctx) else darkColorScheme()

            GlanceTheme(colors = ColorProviders(light = esquemaClaro, dark = esquemaOscuro)) {

                // ── RAÍZ DEL WIDGET ──
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(28.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.TopStart
                ) {

                    // ── DECORACIÓN DE FONDO ──

                    // Orb Superior Derecha (Terciario)
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(110.dp)
                                .background(GlanceTheme.colors.tertiaryContainer)
                                .cornerRadius(55.dp),
                            content = {}
                        )
                    }

                    // Orb Inferior Izquierda (Secundario) - Da balance al diseño
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(70.dp)
                                .background(GlanceTheme.colors.secondaryContainer.getColor(ctx).copy(alpha = 0.5f))
                                .cornerRadius(35.dp),
                            content = {}
                        )
                    }

                    // ── CONTENIDO PRINCIPAL ──
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.Start
                    ) {

                        // ── ENCABEZADO ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth()
                        ) {
                            // Círculo primario con comillas
                            Box(
                                modifier = GlanceModifier
                                    .size(32.dp)
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "\u201C",
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onPrimary
                                    )
                                )
                            }

                            Spacer(GlanceModifier.width(8.dp))

                            Text(
                                text = "TUITERAZ",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.primary
                                    // Eliminado letterSpacing ya que Glance no lo soporta
                                )
                            )
                        }

                        // Línea divisora sutil
                        Spacer(GlanceModifier.height(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .width(60.dp)
                                .height(2.dp)
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(1.dp),
                            content = {}
                        )
                        Spacer(GlanceModifier.height(8.dp))

                        // ── FRASE (Ocupa el espacio central elástico) ──
                        Text(
                            text = textoWidget,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight(), // Empuja el pie de página hacia abajo
                            style = TextStyle(
                                fontSize = tamanoFrase,
                                textAlign = TextAlign.Start,
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 4
                        )

                        Spacer(GlanceModifier.height(10.dp))

                        // ── PIE DE PÁGINA ──
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Píldora del autor con Avatar Inicial
                            Row(
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.secondaryContainer)
                                    .cornerRadius(20.dp)
                                    .padding(end = 12.dp, top = 3.dp, bottom = 3.dp, start = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar con la inicial
                                Box(
                                    modifier = GlanceModifier
                                        .size(24.dp)
                                        .background(GlanceTheme.colors.secondary)
                                        .cornerRadius(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = inicialAutor,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlanceTheme.colors.onSecondary
                                        )
                                    )
                                }

                                Spacer(GlanceModifier.width(6.dp))

                                Text(
                                    text = autorWidget,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onSecondaryContainer
                                    )
                                )
                            }

                            Spacer(GlanceModifier.defaultWeight())

                            // Indicadores decorativos de paginación
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(16.dp).height(6.dp)
                                        .background(GlanceTheme.colors.primary)
                                        .cornerRadius(3.dp),
                                    content = {}
                                )
                                Spacer(GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .size(6.dp)
                                        .background(GlanceTheme.colors.onSurfaceVariant) // Solucionado outlineVariant
                                        .cornerRadius(3.dp),
                                    content = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class FraseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FraseWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Actualiza colores si el usuario cambia el fondo de pantalla
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
            MainScope().launch {
                glanceAppWidget.updateAll(context.applicationContext)
            }
        }
    }
}