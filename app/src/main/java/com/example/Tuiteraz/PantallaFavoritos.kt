package com.example.Tuiteraz

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Tuiteraz.ui.componentes.efectoPulsacionSutil
import com.example.Tuiteraz.ui.viewmodel.FavoritosViewModel
import kotlinx.coroutines.delay

@Composable
fun PantallaFavoritos(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: FavoritosViewModel
) {
    val context = LocalContext.current
    // 1. Instanciamos las preferencias compartidas para recordar la decisión
    val sharedPrefs = remember { context.getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE) }

    val listaFavoritos by viewModel.listaFavoritos.collectAsState()
    val visibles = remember { mutableStateListOf<Boolean>() }

    // 2. Estados para controlar el diálogo de confirmación
    var mostrarDialogo by remember { mutableStateOf(false) }
    var fraseAEliminar by remember { mutableStateOf<Frase?>(null) }
    var noVolverAMostrar by remember { mutableStateOf(false) }

    LaunchedEffect(listaFavoritos.size) {
        visibles.clear()
        visibles.addAll(List(listaFavoritos.size) { false })
        listaFavoritos.indices.forEach { i ->
            delay(DELAY_ITEM.toLong() * i)
            if (i < visibles.size) visibles[i] = true
        }
    }

    val infTransicion = rememberInfiniteTransition(label = "latido")
    val escalaCorazon by infTransicion.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.25f,
        animationSpec = infiniteRepeatable(tween(DUR_ENTRADA), repeatMode = RepeatMode.Reverse),
        label         = "latido"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                top    = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start  = 16.dp,
                end    = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Favorite, contentDescription = null,
                        tint     = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp).scale(escalaCorazon)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${listaFavoritos.size} frases guardadas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            itemsIndexed(items = listaFavoritos, key = { _, frase -> frase.id }) { index, frase ->
                AnimatedVisibility(
                    visible = visibles.getOrElse(index) { false },
                    enter   = fadeIn(tween(DUR_ENTRADA)) + slideInVertically(SpringMedioRebotanteIntOffset) { it / 3 }
                ) {
                    TarjetaFavorito(
                        frase = frase,
                        numero = index + 1,
                        onRemover = {
                            // 3. Verificamos si el usuario ya nos pidió no mostrar el aviso
                            val omitirDialogo = sharedPrefs.getBoolean("omitir_dialogo_borrar_fav", false)
                            if (omitirDialogo) {
                                viewModel.alternarFavorito(frase) // Borramos directo
                            } else {
                                fraseAEliminar = frase
                                noVolverAMostrar = false // Reseteamos el checkbox
                                mostrarDialogo = true
                            }
                        }
                    )
                }
            }

            if (listaFavoritos.isEmpty()) {
                item {
                    Text(
                        "Aún no tienes frases favoritas. \nVe al inicio y toca el corazón para agregar algunas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // --- 4. EL DIÁLOGO DE CONFIRMACIÓN ---
        if (mostrarDialogo && fraseAEliminar != null) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                icon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("¿Eliminar frase?") },
                text = {
                    Column {
                        Text(
                            "¿Estás seguro? Esta frase se eliminará de tus favoritos y no la podrás volver a ver aquí.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { noVolverAMostrar = !noVolverAMostrar }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = noVolverAMostrar,
                                onCheckedChange = { noVolverAMostrar = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "No volver a preguntar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Si marcó la casilla, lo guardamos para siempre
                            if (noVolverAMostrar) {
                                sharedPrefs.edit().putBoolean("omitir_dialogo_borrar_fav", true).apply()
                            }
                            // Borramos la frase y cerramos el diálogo
                            fraseAEliminar?.let { viewModel.alternarFavorito(it) }
                            mostrarDialogo = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sí, eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogo = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun TarjetaFavorito(frase: Frase, numero: Int, onRemover: () -> Unit) {
    val escala      = remember { Animatable(1f) }
    var anchoPx by remember { mutableStateOf(1f) }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .onSizeChanged { anchoPx = it.width.toFloat() }
            .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
            .efectoPulsacionSutil(escala),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "$numero",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                IconButton(onClick = onRemover, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Favorite, contentDescription = "Quitar de favoritos",
                        tint     = Color(0xFFE53935)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                frase.texto,
                style      = MaterialTheme.typography.bodyLarge,
                fontStyle  = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp,
                maxLines   = 4,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape    = RoundedCornerShape(1.dp),
                    modifier = Modifier.width(24.dp).height(2.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    "— ${frase.autor}",
                    style         = MaterialTheme.typography.labelLarge,
                    fontWeight    = FontWeight.Bold,
                    color         = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}