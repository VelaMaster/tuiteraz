package com.example.Tuiteraz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Tuiteraz.ui.componentes.efectoPulsacionSutil
import kotlinx.coroutines.delay

val frasesFavoritasEjemplo = listOf(
    Frase(1, "El único modo de hacer un gran trabajo es amar lo que haces.", "Steve Jobs"),
    Frase(2, "No cuentes los días, haz que los días cuenten.", "Muhammad Ali"),
    Frase(3, "La vida es lo que pasa mientras estás ocupado haciendo otros planes.", "John Lennon"),
    Frase(4, "Sé el cambio que quieres ver en el mundo.", "Mahatma Gandhi"),
    Frase(5, "En medio de la dificultad reside la oportunidad.", "Albert Einstein"),
    Frase(6, "El éxito es la suma de pequeños esfuerzos repetidos día tras día.", "Robert Collier")
)

@Composable
fun PantallaFavoritos(paddingValues: PaddingValues = PaddingValues()) {
    val visibles = remember { mutableStateListOf(*Array(frasesFavoritasEjemplo.size) { false }) }
    LaunchedEffect(Unit) {
        frasesFavoritasEjemplo.indices.forEach { i ->
            delay(DELAY_ITEM.toLong() * i)
            visibles[i] = true
        }
    }

    val infTransicion = rememberInfiniteTransition(label = "latido")
    val escalaCorazon by infTransicion.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.25f,
        animationSpec = infiniteRepeatable(tween(DUR_ENTRADA), repeatMode = RepeatMode.Reverse),
        label         = "latido"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                top    = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start  = 16.dp,
                end    = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Favorite, contentDescription = null,
                        tint     = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp).scale(escalaCorazon)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${frasesFavoritasEjemplo.size} frases guardadas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            itemsIndexed(frasesFavoritasEjemplo) { index, frase ->
                AnimatedVisibility(
                    visible = visibles.getOrElse(index) { false },
                    enter   =
                        fadeIn(tween(DUR_ENTRADA)) +
                                slideInVertically(SpringMedioRebotanteIntOffset) { it / 3 }
                ) {
                    TarjetaFavorito(frase = frase, numero = index + 1)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TARJETA FAVORITO — efectoArrastre: inclina + desplaza al presionar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TarjetaFavorito(frase: Frase, numero: Int) {
    val escala      = remember { Animatable(1f) }
    val rotacion    = remember { Animatable(0f) }
    val traslacionX = remember { Animatable(0f) }
    var anchoPx by remember { mutableStateOf(1f) }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .onSizeChanged { anchoPx = it.width.toFloat() }
            .graphicsLayer {
                scaleX       = escala.value
                scaleY       = escala.value
                rotationZ    = rotacion.value
                translationX = traslacionX.value
            }
            .efectoPulsacionSutil(escala),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "$numero",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Icon(
                    Icons.Outlined.FormatQuote, contentDescription = null,
                    tint     = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                frase.texto,
                style      = MaterialTheme.typography.bodyLarge,
                fontStyle  = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp,
                maxLines   = 4,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape    = RoundedCornerShape(1.dp),
                    modifier = Modifier.width(24.dp).height(2.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    "— ${frase.autor}",
                    style         = MaterialTheme.typography.labelLarge,
                    fontWeight    = FontWeight.Bold,
                    color         = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}