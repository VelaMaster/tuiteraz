package com.example.Tuiteraz.ui.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Tuiteraz.* // Para tus constantes de animación

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TarjetaClimaDinamica(
    estaCargando : Boolean,
    esTablet     : Boolean = false,
    ciudad       : String,
    temperatura  : Int,
    descripcion  : String,
    huboError    : Boolean,
    onTap        : () -> Unit
) {
    val haptic         = LocalHapticFeedback.current
    val altoTarjeta    = if (esTablet) 140.dp else 120.dp

    var contenedorPx by remember { mutableStateOf(0) }
    val density       = LocalDensity.current

    val anchoPct by animateFloatAsState(
        targetValue   = if (estaCargando) 0f else 1f,
        animationSpec = SpringMuyRebotante,
        label         = "ancho"
    )
    val anchoDp: Dp by remember(anchoPct, contenedorPx) {
        derivedStateOf {
            with(density) { altoTarjeta + (contenedorPx.toDp() - altoTarjeta) * anchoPct }
        }
    }
    val radio by animateDpAsState(
        targetValue   = if (estaCargando) altoTarjeta / 2 else 28.dp,
        animationSpec = SpringMuyRebotanteDp,
        label         = "radio"
    )
    val elevacion by animateDpAsState(
        targetValue   = if (estaCargando) 8.dp else 2.dp,
        animationSpec = SpringMedioRebotanteDp,
        label         = "elev"
    )

    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .onSizeChanged { contenedorPx = it.width }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier  = Modifier
                .width(anchoDp)
                .height(altoTarjeta)
                .clip(RoundedCornerShape(radio))
                .pointerInput(estaCargando) {
                    detectTapGestures(
                        onTap = {
                            if (!estaCargando) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTap()
                            }
                        }
                    )
                },
            shape     = RoundedCornerShape(radio),
            colors    = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor   = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevacion)
        ) {
            AnimatedContent(
                targetState  = estaCargando,
                transitionSpec = {
                    fadeIn(tween(DUR_EFECTO, easing = FastOutSlowInEasing)) with
                            fadeOut(tween(DUR_RAPIDO)) using SizeTransform(clip = false)
                },
                label = "clima_state"
            ) { cargando ->
                if (cargando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ContainedLoadingIndicator(
                            modifier       = Modifier.size(72.dp),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (esTablet) 40.dp else 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. COLUMNA DE TEXTOS (Con weight(1f) para que sea responsiva)
                        // 1. COLUMNA DE TEXTOS (Responsiva y multilínea)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp), // Espacio para que no choque con los grados
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = ciudad,
                                style = if (esTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                lineHeight = if (esTablet) 40.sp else 32.sp // Da aire si salta de línea
                                // Eliminamos el maxLines=1 y el Ellipsis para que fluya hacia abajo
                            )

                            Spacer(modifier = Modifier.height(4.dp)) // Mini espacio entre ciudad y descripción

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = descripcion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f, fill = false) // Permite que el texto baje de línea sin empujar el error
                                )
                                if (huboError) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "(Sin red)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // 2. FILA DE TEMPERATURA (Toma solo el espacio que necesita)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = temperatura.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "°C",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(bottom = 12.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}