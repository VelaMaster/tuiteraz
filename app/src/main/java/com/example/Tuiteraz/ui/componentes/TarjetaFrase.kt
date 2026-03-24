package com.example.Tuiteraz.ui.componentes

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Tuiteraz.Frase
import kotlinx.coroutines.launch

@Composable
fun TarjetaFrase(
    frase: Frase,
    esTablet: Boolean = false,
    esFavorita: Boolean = false,             // <--- NUEVO: Recibe si es favorita
    onToggleFavorito: () -> Unit = {}        // <--- NUEVO: Avisa cuando se toca el corazón
) {
    val contexto = LocalContext.current
    val escala = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Animaciones para el corazón
    val colorCorazon by animateColorAsState(
        targetValue = if (esFavorita) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "colorCorazon"
    )
    val escalaCorazon = remember { Animatable(1f) }

    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .efectoPulsacionSutil(escala),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                top = if (esTablet) 44.dp else 28.dp,
                bottom = if (esTablet) 24.dp else 16.dp,
                start = if (esTablet) 48.dp else 32.dp,
                end = if (esTablet) 48.dp else 32.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\u201C",
                style      = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                lineHeight = 32.sp,
                modifier   = Modifier.fillMaxWidth(),
                textAlign  = TextAlign.Start
            )
            Text(
                frase.texto,
                style      = if (esTablet) MaterialTheme.typography.headlineSmall
                else          MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                fontStyle  = FontStyle.Italic,
                textAlign  = TextAlign.Center,
                lineHeight = if (esTablet) 36.sp else 30.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(if (esTablet) 24.dp else 18.dp))
            Box(
                Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
            Spacer(Modifier.height(if (esTablet) 18.dp else 14.dp))
            Text(
                frase.autor.uppercase(),
                style         = MaterialTheme.typography.labelLarge,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(if (esTablet) 32.dp else 24.dp))

            // Fila de botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Compartir
                IconButton(
                    onClick = {
                        val textoACompartir = "«${frase.texto}»\n— ${frase.autor}\n\nDescubre más en Tu i teraz."
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textoACompartir)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir frase con...")
                        contexto.startActivity(shareIntent)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir frase",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Botón Favorito
                IconButton(
                    onClick = {
                        onToggleFavorito() // <--- Avisamos al cerebro que hubo un toque
                        scope.launch {
                            escalaCorazon.animateTo(1.4f, tween(100))
                            escalaCorazon.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (esFavorita) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Añadir a favoritos",
                        tint = colorCorazon,
                        modifier = Modifier.scale(escalaCorazon.value)
                    )
                }
            }
        }
    }
}