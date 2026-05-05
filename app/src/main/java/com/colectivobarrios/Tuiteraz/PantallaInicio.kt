package com.colectivobarrios.Tuiteraz

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.colectivobarrios.Tuiteraz.ui.componentes.*
import com.colectivobarrios.Tuiteraz.ui.viewmodel.ClimaViewModel
import com.colectivobarrios.Tuiteraz.ui.viewmodel.EstadoFraseDia
import com.colectivobarrios.Tuiteraz.ui.viewmodel.EventosViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PantallaInicio(
    estadoFrase   : EstadoFraseDia,
    paddingValues : PaddingValues = PaddingValues(),
    climaViewModel: ClimaViewModel = viewModel(),
    eventosViewModel: EventosViewModel = viewModel(),
    esFavorita    : Boolean = false,
    onToggleFavorito: () -> Unit = {},
    onIrAAgenda: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val clienteUbicacion = remember { LocationServices.getFusedLocationProviderClient(contexto) }
    val permisoUbicacion = rememberPermissionState(permission = Manifest.permission.ACCESS_COARSE_LOCATION)
    val sharedPrefs = remember { contexto.getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE) }

    var mostrarDialogoUbicacion by remember { mutableStateOf(false) }
    var noVolverAMostrarUbicacion by remember { mutableStateOf(false) }
    val estadoClima by climaViewModel.estadoClima.collectAsStateWithLifecycle()

    val todosLosEventos by eventosViewModel.eventos.collectAsStateWithLifecycle()

    val eventosProximos = remember(todosLosEventos) {
        val hoy = LocalDate.now()
        todosLosEventos.filter {
            try { !LocalDate.parse(it.fecha).isBefore(hoy) } catch (e: Exception) { false }
        }.sortedWith(compareBy({ it.fecha }, { it.hora }))
    }

    var visible by remember { mutableStateOf(false) }
    val offsetFrase      = remember { Animatable(0f) }
    val offsetCalendario = remember { Animatable(0f) }

    // --- ESTADOS SIMPLIFICADOS PARA LA NOTIFICACIÓN ---
    var mostrarNotificacion by remember { mutableStateOf(false) }
    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) } // Guardamos el evento tal cual

    LaunchedEffect(mostrarNotificacion) {
        if (!mostrarNotificacion) {
            delay(500)
            eventoSeleccionado = null
        }
    }

    var refrescandoClima by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var overscrollPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val maxOverscrollPx = remember(density) { with(density) { 180.dp.toPx() } }
    val umbralRefreshPx = remember(density) { with(density) { 110.dp.toPx() } }
    val haptic = LocalHapticFeedback.current

    val obtenerUbicacionYClima = { forzar: Boolean ->
        android.util.Log.d("TUITERAZ_DEBUG", "obtenerUbicacionYClima() llamado, forzar=$forzar")
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                clienteUbicacion.lastLocation.addOnSuccessListener { ubicacion: Location? ->
                    android.util.Log.d("TUITERAZ_DEBUG", "lastLocation success: ubicacion=${ubicacion != null}")
                    if (ubicacion != null) climaViewModel.cargarClima(ubicacion.latitude, ubicacion.longitude, forzar)
                    else climaViewModel.cargarClima(17.0654, -96.7236, forzar)
                }.addOnFailureListener { e ->
                    android.util.Log.w("TUITERAZ_DEBUG", "lastLocation failure: ${e.message}", e)
                    climaViewModel.cargarClima(17.0654, -96.7236, forzar)
                }
            } catch (e: Exception) {
                android.util.Log.e("TUITERAZ_DEBUG", "Excepción en lastLocation: ${e.message}", e)
                climaViewModel.cargarClima(17.0654, -96.7236, forzar)
            }
        } else {
            android.util.Log.d("TUITERAZ_DEBUG", "Sin permiso de ubicación, marcando bloqueada")
            climaViewModel.marcarUbicacionBloqueada()
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("TUITERAZ_DEBUG", "PantallaInicio LaunchedEffect(Unit) - solicitando permiso si falta")
        visible = true
        if (!permisoUbicacion.status.isGranted) permisoUbicacion.launchPermissionRequest()
        eventosViewModel.cargarEventos()
    }

    LaunchedEffect(permisoUbicacion.status) {
        android.util.Log.d("TUITERAZ_DEBUG", "PantallaInicio LaunchedEffect(permiso) - granted=${permisoUbicacion.status.isGranted}")
        if (permisoUbicacion.status.isGranted) obtenerUbicacionYClima(false)
        else climaViewModel.marcarUbicacionBloqueada()
    }

    val dispararActualizacionClima = {
        if (!refrescandoClima) {
            refrescandoClima = true
            scope.launch {
                if (permisoUbicacion.status.isGranted) obtenerUbicacionYClima(true)
                else {
                    climaViewModel.marcarUbicacionBloqueada()
                    if (permisoUbicacion.status.shouldShowRationale) permisoUbicacion.launchPermissionRequest()
                    else {
                        val omitirDialogo = sharedPrefs.getBoolean("omitir_dialogo_ubicacion", false)
                        if (!omitirDialogo) { noVolverAMostrarUbicacion = false; mostrarDialogoUbicacion = true }
                    }
                }
                eventosViewModel.cargarEventos()
                launch {
                    delay(200)
                    offsetFrase.animateTo(30f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow))
                    offsetFrase.animateTo(0f, SpringMuyRebotante)
                }
                launch {
                    delay(320)
                    offsetCalendario.animateTo(44f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow))
                    offsetCalendario.animateTo(0f, SpringMuyRebotante)
                }
                delay(1500)
                refrescandoClima = false
            }
        }
    }

    val conexionScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                if (overscrollPx > 0f && available.y < 0) {
                    val consume = available.y.coerceAtLeast(-overscrollPx)
                    overscrollPx += consume
                    return Offset(0f, consume)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && available.y > 0) {
                    val friccion = (1f - (overscrollPx / maxOverscrollPx)).coerceAtLeast(0.1f)
                    overscrollPx = (overscrollPx + available.y * (0.6f * friccion)).coerceAtMost(maxOverscrollPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollPx > 0f) {
                    if (overscrollPx > umbralRefreshPx) dispararActualizacionClima()
                    scope.launch {
                        animate(initialValue = overscrollPx, targetValue = 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        ) { value, _ -> overscrollPx = value }
                    }
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var anchoRaizPx by remember { mutableStateOf(0) }
            val anchoRaizDp: Dp by remember(anchoRaizPx) { derivedStateOf { with(density) { anchoRaizPx.toDp() } } }
            val esTablet = anchoRaizDp > 600.dp
            val padH = if (esTablet) 40.dp else 16.dp

            Box(modifier = Modifier.fillMaxSize().nestedScroll(conexionScroll)) {
                val progreso = (overscrollPx / umbralRefreshPx).coerceIn(0f, 1f)
                val metaAlcanzada = progreso >= 1f

                LaunchedEffect(metaAlcanzada) {
                    if (metaAlcanzada && !refrescandoClima) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = paddingValues.calculateTopPadding() + 8.dp)
                        .offset { IntOffset(0, (overscrollPx / 2).roundToInt()) },
                    contentAlignment = Alignment.TopCenter
                ) {
                    val alpha by animateFloatAsState(targetValue = if (overscrollPx > 10f) progreso else 0f, label = "alpha")
                    val scale by animateFloatAsState(
                        targetValue = if (metaAlcanzada) 1.15f else (progreso * 0.9f),
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f), label = "scale"
                    )
                    Box(
                        modifier = Modifier.size(40.dp)
                            .graphicsLayer { this.alpha = alpha; this.scaleX = scale; this.scaleY = scale; this.rotationZ = overscrollPx * 2f }
                            .clip(CircleShape)
                            .background(if (metaAlcanzada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null,
                            tint = if (metaAlcanzada) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                        .offset { IntOffset(0, overscrollPx.roundToInt()) }
                        .onSizeChanged { anchoRaizPx = it.width }
                        .verticalScroll(rememberScrollState())
                        .padding(top = paddingValues.calculateTopPadding() + 8.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))

                    Box(Modifier.widthIn(max = if (esTablet) 720.dp else 600.dp).fillMaxWidth().padding(horizontal = padH)) {
                        EntradaAnimada(visible, DELAY_SECCION_1) {
                            TarjetaClimaDinamica(
                                estaCargando = refrescandoClima || estadoClima.actualizando,
                                esTablet     = esTablet,
                                ciudad       = estadoClima.ciudad,
                                temperatura  = estadoClima.temperatura,
                                descripcion  = estadoClima.descripcion,
                                huboError    = estadoClima.huboErrorAlActualizar,
                                desdCache    = estadoClima.desdCache,   // <-- nuevo
                                onTap        = { dispararActualizacionClima() }
                            )
                        }
                    }

                    Spacer(Modifier.height(if (esTablet) 28.dp else 18.dp))

                    Box(Modifier.widthIn(max = if (esTablet) 720.dp else 600.dp).fillMaxWidth().padding(horizontal = padH)) {
                        EntradaAnimada(visible, DELAY_SECCION_2) {
                            Box(Modifier.offset { IntOffset(0, offsetFrase.value.roundToInt()) }) {
                                when (estadoFrase) {
                                    is EstadoFraseDia.CargandoSkeleton -> TarjetaFraseSkeleton(esTablet = esTablet)
                                    is EstadoFraseDia.MostrarFrase -> TarjetaFrase(
                                        frase = estadoFrase.frase, esTablet = esTablet,
                                        esFavorita = esFavorita, onToggleFavorito = onToggleFavorito
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(if (esTablet) 28.dp else 18.dp))

                    Box(Modifier.widthIn(max = if (esTablet) 720.dp else 600.dp).fillMaxWidth().padding(horizontal = padH)) {
                        EntradaAnimada(visible, DELAY_SECCION_3) {
                            Box(Modifier.offset { IntOffset(0, offsetCalendario.value.roundToInt()) }) {
                                TarjetaProximosEventos(
                                    esTablet = esTablet,
                                    eventos = eventosProximos,
                                    onVerTodoClick = onIrAAgenda,
                                    onEventoClick = { evento ->
                                        // AHORA SÍ: Guardamos el evento DIRECTAMENTE. Sin complicaciones de ID.
                                        eventoSeleccionado = evento
                                        mostrarNotificacion = true
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Recuperamos el evento para asegurarnos de que la UI tenga los datos más frescos
        val eventoAMostrar = eventoSeleccionado?.let { seleccionado ->
            if (seleccionado.id != null) {
                // Si tiene ID (Nube), lo busca actualizado
                todosLosEventos.find { it.id == seleccionado.id } ?: seleccionado
            } else {
                // Si es local, usa el que tenemos guardado en memoria
                seleccionado
            }
        }

        if (eventoAMostrar != null) {
            NotificacionTopMD3(
                visible = mostrarNotificacion,
                evento = eventoAMostrar,
                topPadding = paddingValues.calculateTopPadding(),
                onDismiss = { mostrarNotificacion = false },
                onAlternarAlarma = { eventoActual ->
                    val nuevoEvento = eventoActual.copy(
                        recordatorio = !eventoActual.recordatorio,
                        idLocal = eventoActual.idLocal
                    )
                    eventosViewModel.actualizarEvento(nuevoEvento, eventoActual)
                    eventoSeleccionado = nuevoEvento
                },
                onEliminar = { eventoAEliminar ->
                    eventosViewModel.eliminarEvento(eventoAEliminar)
                    mostrarNotificacion = false
                },
                onActualizar = { eventoEditado ->
                    eventosViewModel.actualizarEvento(eventoEditado, eventoAMostrar)
                    eventoSeleccionado = eventoEditado // Actualizamos la memoria para que no se pierda al ser local
                }
            )
        }

        if (mostrarDialogoUbicacion) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoUbicacion = false },
                icon = { Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Ubicación bloqueada") },
                text = {
                    Column {
                        Text("Has bloqueado el acceso a la ubicación. Para darte el clima de donde estás, debes prender el permiso manualmente.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { noVolverAMostrarUbicacion = !noVolverAMostrarUbicacion }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = noVolverAMostrarUbicacion, onCheckedChange = { noVolverAMostrarUbicacion = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("No volver a preguntar", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (noVolverAMostrarUbicacion) sharedPrefs.edit().putBoolean("omitir_dialogo_ubicacion", true).apply()
                        mostrarDialogoUbicacion = false
                        contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", contexto.packageName, null)
                        })
                    }) { Text("Ir a Ajustes") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        if (noVolverAMostrarUbicacion) sharedPrefs.edit().putBoolean("omitir_dialogo_ubicacion", true).apply()
                        mostrarDialogoUbicacion = false
                    }) { Text("Cancelar") }
                }
            )
        }
    }
}