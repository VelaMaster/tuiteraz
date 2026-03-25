package com.example.Tuiteraz

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
import com.example.Tuiteraz.data.network.SupabaseManager
import com.example.Tuiteraz.ui.viewmodel.FavoritosViewModel
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
    fraseActual: Frase,
    isNotificacionesActivas: Boolean,
    onNotificacionesChange: (Boolean) -> Unit,
    favViewModel: FavoritosViewModel = viewModel()
) {
    var itemSeleccionado by remember { mutableIntStateOf(0) }
    // Estado para saber si estamos dentro de "Acerca de"
    var verAcercaDe by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsStateWithLifecycle()
    val isLogged = sessionStatus is SessionStatus.Authenticated

    val listaFavoritos by favViewModel.listaFavoritos.collectAsStateWithLifecycle()
    val esFavoritaActual = listaFavoritos.any { it.id == fraseActual.id }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Ocultamos la barra principal si estamos en "Acerca de", ya que ella tiene su propia flecha de volver
            if (!(itemSeleccionado == 2 && verAcercaDe)) {
                TopAppBar(
                    title = {
                        Text(
                            text       = "Tu i teraz",
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
            // Ocultamos la barra inferior en "Acerca de" para dar más espacio y enfoque
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
                                verAcercaDe = false // Reseteamos el sub-estado al cambiar de pestaña
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
        // Animación principal entre pestañas (Inicio, Favoritos, Ajustes)
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
                    frase = fraseActual,
                    paddingValues = paddingValues,
                    esFavorita = esFavoritaActual,
                    onToggleFavorito = {
                        if (isLogged) {
                            favViewModel.alternarFavorito(fraseActual)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Inicia sesión en ajustes para poder sincronizar tus favoritas.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )
                1 -> PantallaFavoritos(
                    paddingValues = paddingValues,
                    viewModel = favViewModel
                )
                2 -> {
                    // --- TRANSICIÓN INTERNA DE LA PESTAÑA AJUSTES ---
                    AnimatedContent(
                        targetState = verAcercaDe,
                        transitionSpec = {
                            if (targetState) { // Entrando a "Acerca de"
                                (slideInHorizontally(spring(Spring.DampingRatioLowBouncy)) { it } + fadeIn())
                                    .togetherWith(slideOutHorizontally(tween(DUR_RAPIDO)) { -it / 3 } + fadeOut())
                            } else { // Regresando a "Ajustes"
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