package com.example.balance
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class DestNav(
    val etiqueta : String,
    val iconoOn  : ImageVector,
    val iconoOff : ImageVector
)

private val destinos = listOf(
    DestNav("Inicio",    Icons.Filled.Home,     Icons.Outlined.Home),
    DestNav("Favoritos", Icons.Filled.Favorite,  Icons.Outlined.FavoriteBorder),
    DestNav("Ajustes",   Icons.Filled.Settings,  Icons.Outlined.Settings)
)

// ─────────────────────────────────────────────────────────────────────────────
// COMPORTAMIENTO DE SCROLL:
//
// enterAlwaysScrollBehavior + TopAppBar (NO LargeTopAppBar):
//   • Al bajar → la barra se desliza completamente hacia arriba y DESAPARECE
//   • Al subir → la barra reaparece deslizándose hacia abajo
//   → Comportamiento idéntico a Instagram / YouTube
//
// LargeTopAppBar con enterAlways solo COLAPSA el título grande a pequeño
// pero la barra NUNCA desaparece → eso es el comportamiento de WhatsApp
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipalConNavegacion(fraseActual: Frase) {
    var itemSeleccionado by remember { mutableIntStateOf(0) }

    // enterAlwaysScrollBehavior = desaparece al bajar, reaparece al subir
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            // TopAppBar simple (no Large) → se va COMPLETAMENTE con el scroll
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
        },

        bottomBar = {
            NavigationBar {
                destinos.forEachIndexed { index, destino ->
                    val seleccionado = itemSeleccionado == index

                    val escala by animateFloatAsState(
                        targetValue   = if (seleccionado) 1.20f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "escala_nav_$index"
                    )

                    NavigationBarItem(
                        selected = seleccionado,
                        onClick  = { itemSeleccionado = index },
                        icon     = {
                            Icon(
                                imageVector        = if (seleccionado) destino.iconoOn else destino.iconoOff,
                                contentDescription = destino.etiqueta,
                                modifier           = Modifier.size(24.dp).scale(escala)
                            )
                        },
                        label = { Text(destino.etiqueta) }
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState   = itemSeleccionado,
            modifier      = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(350)) { it / 2 } + fadeIn(tween(350)))
                        .togetherWith(slideOutHorizontally(tween(350)) { -it / 2 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(350)) { -it / 2 } + fadeIn(tween(350)))
                        .togetherWith(slideOutHorizontally(tween(350)) { it / 2 } + fadeOut(tween(200)))
                }
            },
            label = "nav_pantallas"
        ) { pantalla ->
            when (pantalla) {
                0 -> PantallaInicio(frase = fraseActual, paddingValues = paddingValues)
                1 -> PantallaFavoritos(paddingValues = paddingValues)
                2 -> PantallaAjustes(paddingValues = paddingValues)
            }
        }
    }
}