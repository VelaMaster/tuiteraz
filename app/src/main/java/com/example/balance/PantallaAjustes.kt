package com.example.balance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.balance.ui.componentes.efectoPulsacionSutil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ConfigAjuste(
    val icono         : ImageVector,
    val titulo        : String,
    val descripcion   : String,
    val colorIcono    : @Composable () -> Color,
    val tieneSwitch   : Boolean = true,
    val activoDefault : Boolean = false
)

@Composable
fun PantallaAjustes(paddingValues: PaddingValues = PaddingValues()) {
    val secciones = listOf(
        "Notificaciones" to listOf(
            ConfigAjuste(Icons.Outlined.Notifications, "Notificaciones diarias",
                "Recibe un recordatorio cada mañana",
                { MaterialTheme.colorScheme.secondary }, true, true)
        ),
        "Apariencia" to listOf(
            ConfigAjuste(Icons.Outlined.DarkMode, "Tema oscuro",
                "Cambiar apariencia de la app",
                { MaterialTheme.colorScheme.primary }, true, false)
        ),
        "Datos" to listOf(
            ConfigAjuste(Icons.Outlined.CloudSync, "Sincronizar datos",
                "Guardar favoritos en la nube",
                { MaterialTheme.colorScheme.tertiary }, true, false)
        ),
        "Información" to listOf(
            ConfigAjuste(Icons.Outlined.Info, "Acerca de Tu i teraz",
                "Versión 1.0.0",
                { MaterialTheme.colorScheme.outline }, false, false)
        )
    )

    val visibles = remember { mutableStateListOf(*Array(secciones.size) { false }) }
    LaunchedEffect(Unit) {
        secciones.indices.forEach { i ->
            delay(DELAY_ITEM.toLong() * i)
            visibles[i] = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top    = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    start  = 16.dp,
                    end    = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            secciones.forEachIndexed { i, (nombre, ajustes) ->
                AnimatedVisibility(
                    visible = visibles.getOrElse(i) { false },
                    enter   =
                        fadeIn(tween(DUR_ENTRADA)) +
                                slideInVertically(SpringMedioRebotanteIntOffset) { it / 3 }
                ) {
                    SeccionAjustes(nombre = nombre, ajustes = ajustes)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECCIÓN — la Card entera tiene efectoArrastre
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SeccionAjustes(nombre: String, ajustes: List<ConfigAjuste>) {
    val escala      = remember { Animatable(1f) }
    val rotacion    = remember { Animatable(0f) }
    val traslacionX = remember { Animatable(0f) }
    var anchoPx by remember { mutableStateOf(1f) }

    Column {
        Text(
            nombre.uppercase(),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
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
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                ajustes.forEachIndexed { idx, ajuste ->
                    ItemAjuste(ajuste = ajuste)
                    if (idx < ajustes.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 56.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ÍTEM DE AJUSTE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ItemAjuste(ajuste: ConfigAjuste) {
    var activo by remember { mutableStateOf(ajuste.activoDefault) }
    val escalaIcono = remember { Animatable(1f) }
    val scope       = rememberCoroutineScope()

    // Desplazamiento X sutil al activar (+6dp derecha)
    val desplazX by animateFloatAsState(
        targetValue   = if (activo) 6f else 0f,
        animationSpec = SpringMuyRebotante,
        label         = "desplaz_${ajuste.titulo}"
    )
    val escalaSwitch by animateFloatAsState(
        targetValue   = if (activo) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium),
        label         = "switch_${ajuste.titulo}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = desplazX }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = ajuste.colorIcono().copy(alpha = 0.15f),
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer { scaleX = escalaIcono.value; scaleY = escalaIcono.value }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    ajuste.icono, contentDescription = null,
                    tint     = ajuste.colorIcono(),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(ajuste.titulo,
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(ajuste.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        Spacer(Modifier.width(8.dp))

        if (ajuste.tieneSwitch) {
            Switch(
                checked         = activo,
                onCheckedChange = {
                    activo = it
                    scope.launch {
                        escalaIcono.animateTo(1.35f, tween(DUR_ACCION))
                        escalaIcono.animateTo(1f, SpringMuyRebotante)
                    }
                },
                modifier = Modifier.scale(escalaSwitch)
            )
        } else {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}