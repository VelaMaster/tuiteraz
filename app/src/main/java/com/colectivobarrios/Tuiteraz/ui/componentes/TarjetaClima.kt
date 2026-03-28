package com.colectivobarrios.Tuiteraz.ui.componentes

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colectivobarrios.Tuiteraz.* @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
                        // Mantenemos tu Pentágono/Óvalo Expressive original
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // --- LÓGICA DE RESPONSIVIDAD DE LETRAS ---
                            val fontSizeCiudad = when {
                                ciudad.length > 20 -> 18.sp
                                ciudad.length > 14 -> 22.sp
                                else -> if (esTablet) 32.sp else 28.sp
                            }

                            Text(
                                text = ciudad,
                                fontSize = fontSizeCiudad,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                lineHeight = (fontSizeCiudad.value * 1.1).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = descripcion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    lineHeight = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = temperatura.toString(),
                                style = if (esTablet) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
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