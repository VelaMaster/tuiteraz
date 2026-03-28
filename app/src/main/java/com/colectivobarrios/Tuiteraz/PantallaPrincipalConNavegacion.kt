package com.colectivobarrios.Tuiteraz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import com.colectivobarrios.Tuiteraz.ui.viewmodel.EstadoFraseDia
import com.colectivobarrios.Tuiteraz.ui.viewmodel.FavoritosViewModel
import com.colectivobarrios.Tuiteraz.ui.viewmodel.FraseDelDiaViewModel
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

private data class DestNav(val etiqueta: String, val iconoOn: ImageVector, val iconoOff: ImageVector)
private val destinos = listOf(
    DestNav("Inicio",    Icons.Filled.Home,     Icons.Outlined.Home),
    DestNav("Favoritos", Icons.Filled.Favorite,  Icons.Outlined.FavoriteBorder),
    DestNav("Ajustes",   Icons.Filled.Settings,  Icons.Outlined.Settings)
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipalConNavegacion(
    fraseViewModel: FraseDelDiaViewModel, // <--- CAMBIO AQUÍ: Recibimos el ViewModel en lugar de la frase estática
    isNotificacionesActivas: Boolean,
    onNotificacionesChange: (Boolean) -> Unit,
    favViewModel: FavoritosViewModel = viewModel()
) {
    var itemSeleccionado by remember { mutableIntStateOf(0) }
    var verAcercaDe by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsStateWithLifecycle()
    val isLogged = sessionStatus is SessionStatus.Authenticated
    val listaFavoritos by favViewModel.listaFavoritos.collectAsStateWithLifecycle()
    val estadoFrase by fraseViewModel.estadoUi.collectAsStateWithLifecycle()
    val esFavoritaActual = if (estadoFrase is EstadoFraseDia.MostrarFrase) {
        listaFavoritos.any { it.id == (estadoFrase as EstadoFraseDia.MostrarFrase).frase.id }
    } else false

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!(itemSeleccionado == 2 && verAcercaDe)) {
                TopAppBar(
                    title = {
                        Text(
                            text       = "Tuiteraz",
                            fontWeight = FontWeight.ExtraBold,
                            style      = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor      = MaterialTheme.colorScheme.onBackground
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            if (!(itemSeleccionado == 2 && verAcercaDe)) {
                NavigationBar {
                    destinos.forEachIndexed { index, destino ->
                        val seleccionado = itemSeleccionado == index
                        val escala by animateFloatAsState(
                            targetValue = if (seleccionado) 1.20f else 1f,
                            animationSpec = SpringMuyRebotante,
                            label = "escala_nav"
                        )

                        NavigationBarItem(
                            selected = seleccionado,
                            onClick  = {
                                itemSeleccionado = index
                                verAcercaDe = false
                            },
                            icon = {
                                Icon(
                                    imageVector = if (seleccionado) destino.iconoOn else destino.iconoOff,
                                    contentDescription = destino.etiqueta,
                                    modifier = Modifier.size(24.dp).scale(escala)
                                )
                            },
                            label = { Text(destino.etiqueta) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = itemSeleccionado,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(DUR_EFECTO)) { it / 2 } + fadeIn(tween(DUR_EFECTO)))
                        .togetherWith(slideOutHorizontally(tween(DUR_EFECTO)) { -it / 2 } + fadeOut(tween(DUR_RAPIDO)))
                } else {
                    (slideInHorizontally(tween(DUR_EFECTO)) { -it / 2 } + fadeIn(tween(DUR_EFECTO)))
                        .togetherWith(slideOutHorizontally(tween(DUR_EFECTO)) { it / 2 } + fadeOut(tween(DUR_RAPIDO)))
                }
            },
            label = "nav_pantallas"
        ) { pantalla ->
            when (pantalla) {
                0 -> PantallaInicio(
                    estadoFrase = estadoFrase,
                    paddingValues = paddingValues,
                    esFavorita = esFavoritaActual,
                    onToggleFavorito = {
                        if (estadoFrase is EstadoFraseDia.MostrarFrase) {
                            val frase = (estadoFrase as EstadoFraseDia.MostrarFrase).frase
                            if (isLogged) {
                                favViewModel.alternarFavorito(frase)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Inicia sesión en ajustes para poder sincronizar tus favoritas.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                )
                1 -> PantallaFavoritos(
                    paddingValues = paddingValues,
                    viewModel = favViewModel
                )
                2 -> {
                    AnimatedContent(
                        targetState = verAcercaDe,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally(spring(Spring.DampingRatioLowBouncy)) { it } + fadeIn())
                                    .togetherWith(slideOutHorizontally(tween(DUR_RAPIDO)) { -it / 3 } + fadeOut())
                            } else {
                                (slideInHorizontally(spring(Spring.DampingRatioLowBouncy)) { -it / 3 } + fadeIn())
                                    .togetherWith(slideOutHorizontally(tween(DUR_RAPIDO)) { it } + fadeOut())
                            }
                        },
                        label = "sub_nav_ajustes"
                    ) { mostrarAcercaDe ->
                        if (mostrarAcercaDe) {
                            PantallaAcercaDe(onBack = { verAcercaDe = false })
                        } else {
                            PantallaAjustes(
                                paddingValues = paddingValues,
                                isNotificacionesActivas = isNotificacionesActivas,
                                onNotificacionesChange = onNotificacionesChange,
                                onAcercaDeClick = { verAcercaDe = true }
                            )
                        }
                    }
                }
            }
        }
    }
}