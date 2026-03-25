package com.example.Tuiteraz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.Tuiteraz.data.network.SupabaseManager
import com.example.Tuiteraz.ui.componentes.efectoPulsacionSutil
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ConfigAjuste(
    val icono         : ImageVector,
    val titulo        : String,
    val descripcion   : String,
    val colorIcono    : @Composable () -> Color,
    val tieneSwitch   : Boolean = true,
    val estadoActual  : Boolean = false,
    val onActivoChange: ((Boolean) -> Unit)? = null,
    val onClick       : (() -> Unit)? = null
)

@Composable
fun PantallaAjustes(
    paddingValues: PaddingValues = PaddingValues(),
    isNotificacionesActivas: Boolean = false,
    onNotificacionesChange: (Boolean) -> Unit = {},
    onAcercaDeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var mostrarDialogoPermisos by remember { mutableStateOf(false) }

    val launcherPermisoNotif = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) onNotificacionesChange(true)
            else { onNotificacionesChange(false); mostrarDialogoPermisos = true }
        }
    )

    val secciones = listOf(
        "Notificaciones" to listOf(
            ConfigAjuste(
                icono = Icons.Outlined.Notifications,
                titulo = "Notificaciones diarias",
                descripcion = "Recibe un recordatorio cada mañana",
                colorIcono = { MaterialTheme.colorScheme.secondary },
                tieneSwitch = true,
                estadoActual = isNotificacionesActivas,
                onActivoChange = onNotificacionesChange
            )
        ),
        "Información" to listOf(
            ConfigAjuste(
                icono = Icons.Outlined.Info,
                titulo = "Acerca de Tu i teraz",
                descripcion = "Versión 1.0",
                colorIcono = { MaterialTheme.colorScheme.outline },
                tieneSwitch = false,
                onClick = onAcercaDeClick
            )
        )
    )

    val visibles = remember { mutableStateListOf(*Array(secciones.size + 1) { false }) }
    LaunchedEffect(Unit) {
        (0..secciones.size).forEach { i -> delay(DELAY_ITEM.toLong() * i); visibles[i] = true }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = paddingValues.calculateTopPadding() + 8.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = visibles[0], enter = fadeIn(tweenEntrada()) + slideInVertically(SpringMedioRebotanteIntOffset) { it / 3 }) {
                SeccionCuenta()
            }
            secciones.forEachIndexed { i, (nombre, ajustes) ->
                AnimatedVisibility(visible = visibles.getOrElse(i + 1) { false }, enter = fadeIn(tweenEntrada()) + slideInVertically(SpringMedioRebotanteIntOffset) { it / 3 }) {
                    SeccionAjustes(nombre = nombre, ajustes = ajustes)
                }
            }
        }
    }

    if (mostrarDialogoPermisos) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPermisos = false },
            icon = { Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Permiso necesario") },
            text = { Text("Las notificaciones están bloqueadas. Para recibir tu frase, habilítalas en ajustes.") },
            confirmButton = { TextButton(onClick = { mostrarDialogoPermisos = false; val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }; context.startActivity(intent) }) { Text("Ir a Ajustes") } },
            dismissButton = { TextButton(onClick = { mostrarDialogoPermisos = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SeccionCuenta() {
    val scope = rememberCoroutineScope()
    val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsState()
    val isLogged = sessionStatus is SessionStatus.Authenticated
    val userEmail = SupabaseManager.client.auth.currentUserOrNull()?.email ?: ""
    var mostrarDialogoLogout by remember { mutableStateOf(false) }
    val action = SupabaseManager.client.composeAuth.rememberSignInWithGoogle(onResult = { }, fallback = { })

    // Animación de pulsación
    val escala = remember { Animatable(1f) }

    Column {
        Text("CUENTA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = escala.value; scaleY = escala.value }.efectoPulsacionSutil(escala),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (isLogged) Icons.Outlined.Person else Icons.Outlined.CloudSync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isLogged) "Conectado" else "Sincronización en la nube", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(if (isLogged) userEmail else "Inicia sesión para guardar frases", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (isLogged) {
                    Button(onClick = { mostrarDialogoLogout = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Salir") }
                } else {
                    Button(onClick = { action.startFlow() }) { Text("Ingresar") }
                }
            }
        }
    }
    if (mostrarDialogoLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Dejarás de sincronizar tus frases en la nube.") },
            confirmButton = { TextButton(onClick = { mostrarDialogoLogout = false; scope.launch { SupabaseManager.client.auth.signOut() } }) { Text("Sí, salir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { mostrarDialogoLogout = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SeccionAjustes(nombre: String, ajustes: List<ConfigAjuste>) {
    val escala = remember { Animatable(1f) }
    Column {
        Text(nombre.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = escala.value; scaleY = escala.value }.efectoPulsacionSutil(escala), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column {
                ajustes.forEachIndexed { idx, ajuste ->
                    ItemAjuste(ajuste = ajuste)
                    if (idx < ajustes.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun ItemAjuste(ajuste: ConfigAjuste) {
    val activo = ajuste.estadoActual
    val scope = rememberCoroutineScope()
    val escalaIcono = remember { Animatable(1f) }
    val desplazX by animateFloatAsState(targetValue = if (activo) 6f else 0f, animationSpec = SpringMuyRebotante, label = "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !ajuste.tieneSwitch) {
                ajuste.onClick?.invoke()
                scope.launch {
                    escalaIcono.animateTo(1.2f, tweenAccion())
                    escalaIcono.animateTo(1f, SpringMuyRebotante)
                }
            }
            .graphicsLayer { translationX = desplazX }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = ajuste.colorIcono().copy(alpha = 0.15f), modifier = Modifier.size(42.dp).graphicsLayer { scaleX = escalaIcono.value; scaleY = escalaIcono.value }) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(ajuste.icono, null, tint = ajuste.colorIcono(), modifier = Modifier.size(22.dp)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(ajuste.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(ajuste.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        if (ajuste.tieneSwitch) {
            Switch(checked = activo, onCheckedChange = { ajuste.onActivoChange?.invoke(it); scope.launch { escalaIcono.animateTo(1.3f, tweenAccion()); escalaIcono.animateTo(1f, SpringMuyRebotante) } })
        } else {
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}