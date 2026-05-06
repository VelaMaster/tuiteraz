package com.colectivobarrios.Tuiteraz

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import com.colectivobarrios.Tuiteraz.ui.componentes.ModalAgregarEvento
import com.colectivobarrios.Tuiteraz.ui.viewmodel.EstadoFraseDia
import com.colectivobarrios.Tuiteraz.ui.viewmodel.EventosViewModel
import com.colectivobarrios.Tuiteraz.ui.viewmodel.FavoritosViewModel
import com.colectivobarrios.Tuiteraz.ui.viewmodel.FraseDelDiaViewModel
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private data class DestNav(val etiqueta: String, val iconoOn: ImageVector, val iconoOff: ImageVector)
private val destinos = listOf(
    DestNav("Inicio",    Icons.Filled.Home,     Icons.Outlined.Home),
    DestNav("Calendario",Icons.Filled.DateRange,Icons.Outlined.DateRange),
    DestNav("Favoritos", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    DestNav("Ajustes",   Icons.Filled.Settings, Icons.Outlined.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipalConNavegacion(
    fraseViewModel: FraseDelDiaViewModel,
    isNotificacionesActivas: Boolean,
    onNotificacionesChange: (Boolean) -> Unit,
    favViewModel: FavoritosViewModel = viewModel(),
    eventosViewModel: EventosViewModel = viewModel()
) {
    var itemSeleccionado by remember { mutableIntStateOf(0) }
    var verAcercaDe by remember { mutableStateOf(false) }
    var mostrarCrearEvento by remember { mutableStateOf(false) }

    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Envolvemos sessionStatus con .catch para que un fallo de red al refrescar
    // el token (sin internet → DNS error) no llegue al hilo main como crash.
    val sessionStatusFlow = remember {
        SupabaseManager.client.auth.sessionStatus
            .catch { e ->
                android.util.Log.w("TUITERAZ_DEBUG", "sessionStatus flow excepción atrapada: ${e.message}", e)
            }
    }
    val sessionStatus by sessionStatusFlow.collectAsStateWithLifecycle(
        initialValue = SupabaseManager.client.auth.sessionStatus.value
    )
    val isLogged = sessionStatus is SessionStatus.Authenticated
    LaunchedEffect(isLogged) {
        android.util.Log.d("TUITERAZ_DEBUG", "Nav LaunchedEffect(isLogged=$isLogged) sessionStatus=${sessionStatus::class.simpleName}")
        if (isLogged) {
            eventosViewModel.migrarEventosLocales()
        }
    }
    val listaFavoritos by favViewModel.listaFavoritos.collectAsStateWithLifecycle()
    val estadoFrase by fraseViewModel.estadoUi.collectAsStateWithLifecycle()

    val esFavoritaActual = if (estadoFrase is EstadoFraseDia.MostrarFrase) {
        listaFavoritos.any { it.id == (estadoFrase as EstadoFraseDia.MostrarFrase).frase.id }
    } else false

    val errorSupabase by eventosViewModel.error.collectAsStateWithLifecycle()
    val fechaGlobal by eventosViewModel.fechaSeleccionada.collectAsStateWithLifecycle()
    val problemaPermiso by eventosViewModel.problemaPermiso.collectAsStateWithLifecycle()

    LaunchedEffect(errorSupabase) {
        errorSupabase?.let {
            Toast.makeText(contexto, it, Toast.LENGTH_LONG).show()
            eventosViewModel.limpiarError()
        }
    }

    // Cuando programar una alarma falla por falta de permiso, mostramos un
    // diálogo que lleva al usuario directo a los ajustes correctos.
    problemaPermiso?.let { problema ->
        com.colectivobarrios.Tuiteraz.notificaciones.DialogoPermisoNotificaciones(
            problema = problema,
            onDismiss = { eventosViewModel.limpiarProblemaPermiso() }
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!(itemSeleccionado == 3 && verAcercaDe)) {
                TopAppBar(
                    title = { Text(text = "Tuiteraz", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onBackground),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            if (!(itemSeleccionado == 3 && verAcercaDe)) {
                NavigationBar {
                    destinos.forEachIndexed { index, destino ->
                        val seleccionado = itemSeleccionado == index
                        val escala by animateFloatAsState(targetValue = if (seleccionado) 1.20f else 1f, animationSpec = SpringMuyRebotante, label = "escala_nav")

                        NavigationBarItem(
                            selected = seleccionado,
                            onClick  = { itemSeleccionado = index; verAcercaDe = false },
                            icon = { Icon(imageVector = if (seleccionado) destino.iconoOn else destino.iconoOff, contentDescription = destino.etiqueta, modifier = Modifier.size(24.dp).scale(escala)) },
                            label = { Text(destino.etiqueta) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = itemSeleccionado == 0 || itemSeleccionado == 1,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { mostrarCrearEvento = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar Evento") }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = itemSeleccionado,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(DUR_EFECTO)) { it / 2 } + fadeIn(tween(DUR_EFECTO))).togetherWith(slideOutHorizontally(tween(DUR_EFECTO)) { -it / 2 } + fadeOut(tween(DUR_RAPIDO)))
                } else {
                    (slideInHorizontally(tween(DUR_EFECTO)) { -it / 2 } + fadeIn(tween(DUR_EFECTO))).togetherWith(slideOutHorizontally(tween(DUR_EFECTO)) { it / 2 } + fadeOut(tween(DUR_RAPIDO)))
                }
            },
            label = "nav_pantallas"
        ) { pantalla ->
            when (pantalla) {
                0 -> PantallaInicio(
                    estadoFrase = estadoFrase,
                    paddingValues = paddingValues,
                    esFavorita = esFavoritaActual,
                    eventosViewModel = eventosViewModel, // Pasamos el viewModel para ver eventos reales
                    onIrAAgenda = { itemSeleccionado = 1 }, // Hacemos que cambie de pestaña
                    onToggleFavorito = {
                        if (estadoFrase is EstadoFraseDia.MostrarFrase) {
                            val frase = (estadoFrase as EstadoFraseDia.MostrarFrase).frase
                            if (isLogged) favViewModel.alternarFavorito(frase) else scope.launch { snackbarHostState.showSnackbar("Inicia sesión en ajustes para poder sincronizar tus favoritas.", duration = SnackbarDuration.Short) }
                        }
                    }
                )
                1 -> PantallaEventos(paddingValues = paddingValues, viewModel = eventosViewModel)
                2 -> PantallaFavoritos(paddingValues = paddingValues, viewModel = favViewModel)
                3 -> {
                    AnimatedContent(
                        targetState = verAcercaDe,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally(spring(Spring.DampingRatioLowBouncy)) { it } + fadeIn()).togetherWith(slideOutHorizontally(tween(DUR_RAPIDO)) { -it / 3 } + fadeOut())
                            } else {
                                (slideInHorizontally(spring(Spring.DampingRatioLowBouncy)) { -it / 3 } + fadeIn()).togetherWith(slideOutHorizontally(tween(DUR_RAPIDO)) { it } + fadeOut())
                            }
                        },
                        label = "sub_nav_ajustes"
                    ) { mostrarAcercaDe ->
                        if (mostrarAcercaDe) {
                            PantallaAcercaDe(onBack = { verAcercaDe = false })
                        } else {
                            PantallaAjustes(paddingValues = paddingValues, isNotificacionesActivas = isNotificacionesActivas, onNotificacionesChange = onNotificacionesChange, onAcercaDeClick = { verAcercaDe = true })
                        }
                    }
                }
            }
        }

        if (mostrarCrearEvento) {
            ModalAgregarEvento(
                fechaPredefinida = fechaGlobal,
                permiteCambiarFecha = (itemSeleccionado == 0),
                onDismiss = { mostrarCrearEvento = false },
                onGuardar = { titulo, fecha, hora, recordatorio, prioridad ->
                    eventosViewModel.agregarEvento(titulo, fecha, hora, recordatorio, prioridad)
                    mostrarCrearEvento = false
                }
            )
        }
    }
}