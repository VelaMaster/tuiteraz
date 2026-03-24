package com.example.Tuiteraz.ui.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.Tuiteraz.DUR_SALIDA

@Composable
fun NotificacionTopMD3(
    visible   : Boolean,
    texto     : String,
    topPadding: Dp,
    onDismiss : () -> Unit
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    val scope        = rememberCoroutineScope()

    LaunchedEffect(visible) { if (!visible) swipeOffset = 0f }

    AnimatedVisibility(
        visible  = visible,
        enter    = slideInVertically(
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)
        ) { -it } + fadeIn(tween(300)) + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)
        ),
        exit     = slideOutVertically(
            animationSpec = tween(DUR_SALIDA, easing = FastOutSlowInEasing)
        ) { -it } + fadeOut(tween(DUR_SALIDA)) + scaleOut(targetScale = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .padding(horizontal = 16.dp)
            .padding(top = topPadding + 8.dp)
    ) {
        ElevatedCard(
            modifier  = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(24.dp)
                )
                .offset { IntOffset(x = 0, y = swipeOffset.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state       = rememberDraggableState { delta ->
                        swipeOffset = (swipeOffset + delta).coerceAtMost(0f)
                    },
                    onDragStopped = { velocidad ->
                        if (swipeOffset < -80f || velocidad < -600f) {
                            onDismiss()
                        } else {
                            scope.launch {
                                val anim = Animatable(swipeOffset)
                                anim.animateTo(
                                    targetValue   = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness    = Spring.StiffnessMedium
                                    )
                                ) { swipeOffset = value }
                            }
                        }
                    }
                ),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor   = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier          = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Día Festivo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        texto,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.tertiary,
                    modifier           = Modifier.size(28.dp)
                )
            }
        }
    }
}