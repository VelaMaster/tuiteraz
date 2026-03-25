package com.example.Tuiteraz

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.Tuiteraz.data.network.SupabaseManager
import com.example.Tuiteraz.ui.componentes.efectoPulsacionSutil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PantallaAjustes(
    paddingValues: PaddingValues = PaddingValues(),
    isNotificacionesActivas: Boolean = false,
    onNotificacionesChange: (Boolean) -> Unit = {},
    onAcercaDeClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Controlador de permisos de Accompanist
    val permisoNotificaciones = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val sharedPrefs = remember { context.getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE) }

    var mostrarDialogoPermisos by remember { mutableStateOf(false) }
    var noVolverAMostrarNotif by remember { mutableStateOf(false) }
    var vieneDeAjustes by remember { mutableStateOf(false) } // <--- EL SENSOR NUEVO

    // Launcher nativo: Atrapa la respuesta silenciosamente
    val launcherPermisoNotif = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onNotificacionesChange(true)
            } else {
                // Solo apagamos el switch. Dejamos que Android haga su proceso sin interrumpir.
                onNotificacionesChange(false)
            }
        }
    )

    // SINCRONIZACIÓN AUTOMÁTICA AL VOLVER A LA APP
    LifecycleResumeEffect(vieneDeAjustes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

            // Si fuimos a Ajustes y el usuario le dio permiso, prendemos el switch automáticamente
            if (vieneDeAjustes && isGranted) {
                onNotificacionesChange(true)
                vieneDeAjustes = false
            }
            // Si el permiso está denegado en el sistema, aseguramos que el switch se apague
            else if (!isGranted && isNotificacionesActivas) {
                onNotificacionesChange(false)
            }
        }
        onPauseOrDispose { }
    }

    val handleNotificacionesCambio = { activar: Boolean ->
        if (activar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permisoNotificaciones != null) {
                if (permisoNotificaciones.status.isGranted) {
                    onNotificacionesChange(true)
                } else {
                    val yaPreguntamosAlgunaVez = sharedPrefs.getBoolean("ya_preguntamos_notif", false)

                    if (permisoNotificaciones.status.shouldShowRationale || !yaPreguntamosAlgunaVez) {
                        // Android aún nos deja usar su alerta nativa
                        sharedPrefs.edit().putBoolean("ya_preguntamos_notif", true).apply()
                        launcherPermisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Bloqueo permanente de Android. Revisamos si el usuario nos silenció.
                        val omitirDialogo = sharedPrefs.getBoolean("omitir_dialogo_notif", false)
                        if (!omitirDialogo) {
                            noVolverAMostrarNotif = false
                            mostrarDialogoPermisos = true
                        }
                        onNotificacionesChange(false)
                    }
                }
            } else {
                onNotificacionesChange(true)
            }
        } else {
            onNotificacionesChange(false)
        }
    }

    val secciones = listOf(
        "Notificaciones" to listOf(
            ConfigAjuste(
                icono = Icons.Outlined.Notifications,
                titulo = "Notificaciones diarias",
                descripcion = "Recibe un recordatorio cada mañana",
                colorIcono = { MaterialTheme.colorScheme.secondary },
                tieneSwitch = true,
                estadoActual = isNotificacionesActivas,
                onActivoChange = handleNotificacionesCambio
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

    // --- DIÁLOGO DE AJUSTES ---
    if (mostrarDialogoPermisos) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPermisos = false },
            icon = { Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Permiso bloqueado") },
            text = {
                Column {
                    Text("Las notificaciones están bloqueadas en tu dispositivo. Para recibir tu frase diaria, actívalas manualmente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { noVolverAMostrarNotif = !noVolverAMostrarNotif }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = noVolverAMostrarNotif, onCheckedChange = { noVolverAMostrarNotif = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No volver a preguntar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noVolverAMostrarNotif) sharedPrefs.edit().putBoolean("omitir_dialogo_notif", true).apply()
                    mostrarDialogoPermisos = false
                    vieneDeAjustes = true // <--- ACTIVAMOS EL SENSOR
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Ir a Ajustes") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (noVolverAMostrarNotif) sharedPrefs.edit().putBoolean("omitir_dialogo_notif", true).apply()
                    mostrarDialogoPermisos = false
                }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SeccionCuenta() {
    val scope = rememberCoroutineScope()
    val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsStateWithLifecycle()
    val isLogged = sessionStatus is SessionStatus.Authenticated
    val userEmail = SupabaseManager.client.auth.currentUserOrNull()?.email ?: ""
    var mostrarDialogoLogout by remember { mutableStateOf(false) }
    val action = SupabaseManager.client.composeAuth.rememberSignInWithGoogle(onResult = { }, fallback = { })

    val escala = remember { Animatable(1f) }

    Column {
        Text("CUENTA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
                .efectoPulsacionSutil(escala),
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
    Column {
        Text(nombre.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        ajustes.forEachIndexed { idx, ajuste ->
            ItemAjusteCard(ajuste = ajuste)
            if (idx < ajustes.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ItemAjusteCard(ajuste: ConfigAjuste) {
    val activo = ajuste.estadoActual
    val escala = remember { Animatable(1f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
            .efectoPulsacionSutil(escala, onTap = {
                if (!ajuste.tieneSwitch) {
                    ajuste.onClick?.invoke()
                }
            }),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = ajuste.colorIcono().copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(ajuste.icono, null, tint = ajuste.colorIcono(), modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ajuste.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(ajuste.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (ajuste.tieneSwitch) {
                Switch(
                    checked = activo,
                    onCheckedChange = { nuevoEstado ->
                        ajuste.onActivoChange?.invoke(nuevoEstado)
                    }
                )
            } else {
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
            }
        }
    }
}