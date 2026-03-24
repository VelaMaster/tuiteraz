package com.example.Tuiteraz.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import com.example.Tuiteraz.* // Para heredar DUR_ENTRADA, DUR_SALIDA, etc.

@Composable
fun EntradaAnimada(visible: Boolean, retraso: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(DUR_ENTRADA, delayMillis = retraso)) +
                    slideInVertically(tween(DUR_ENTRADA, retraso, FastOutSlowInEasing)) { it / 5 } +
                    scaleIn(tween(DUR_ENTRADA, retraso), 0.93f, TransformOrigin(0.5f, 0f)),
        exit = fadeOut(tween(DUR_SALIDA)) + scaleOut(tween(DUR_SALIDA), 0.97f)
    ) { content() }
}

@Composable
fun Modifier.efectoPulsacionSutil(
    escala: Animatable<Float, *>,
    onTap : suspend () -> Unit = {}
): Modifier {
    val scope = rememberCoroutineScope()
    return this.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                scope.launch { escala.animateTo(0.98f, tween(100)) }
                tryAwaitRelease()
                scope.launch { escala.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
            },
            onTap = { scope.launch { onTap() } }
        )
    }
}