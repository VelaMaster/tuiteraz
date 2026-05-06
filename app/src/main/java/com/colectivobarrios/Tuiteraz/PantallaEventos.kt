package com.colectivobarrios.Tuiteraz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import com.colectivobarrios.Tuiteraz.Evento
import com.colectivobarrios.Tuiteraz.ui.componentes.coloresParaPrioridad
import com.colectivobarrios.Tuiteraz.ui.viewmodel.EventosViewModel

@Composable
fun PantallaEventos(
    paddingValues: PaddingValues,
    viewModel: EventosViewModel
) {
    var mesActual by remember { mutableStateOf(YearMonth.now()) }

    // CORRECCIÓN CLAVE: Ya no es una variable local, ahora leemos y escribimos directo en el ViewModel
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val eventos by viewModel.eventos.collectAsStateWithLifecycle()
    val estaCargando by viewModel.estaCargando.collectAsStateWithLifecycle()

    val eventosDelDia = eventos.filter {
        try {
            LocalDate.parse(it.fecha) == fechaSeleccionada
        } catch (e: Exception) {
            false
        }
    }

    val alturaSemana = 170.dp
    val alturaMes = 380.dp
    val alturaExpandida = 560.dp

    val alturaActual = remember { Animatable(initialValue = 380f) }

    val progresoSemana by remember {
        derivedStateOf { ((alturaMes.value - alturaActual.value) / (alturaMes.value - alturaSemana.value)).coerceIn(0f, 1f) }
    }

    val calendarioExpandido by remember {
        derivedStateOf { alturaActual.value > (alturaMes.value + alturaExpandida.value) / 2f }
    }

    val esVistaSemana by remember {
        derivedStateOf { alturaActual.value < (alturaSemana.value + alturaMes.value) / 2f }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tu Agenda",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (estaCargando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                FilledTonalButton(
                    onClick = {
                        mesActual = YearMonth.now()
                        // Avisamos al ViewModel que volvimos a "Hoy"
                        viewModel.actualizarFechaSeleccionada(LocalDate.now())
                        scope.launch {
                            alturaActual.animateTo(
                                targetValue = alturaMes.value,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hoy")
                }
            }
        }

        Card(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(alturaActual.value.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            val actual = alturaActual.value
                            val distSemana = Math.abs(actual - alturaSemana.value)
                            val distMes = Math.abs(actual - alturaMes.value)
                            val distExpandida = Math.abs(actual - alturaExpandida.value)

                            val destino = when {
                                distSemana < distMes && distSemana < distExpandida -> alturaSemana.value
                                distExpandida < distMes && distExpandida < distSemana -> alturaExpandida.value
                                else -> alturaMes.value
                            }

                            scope.launch {
                                alturaActual.animateTo(
                                    targetValue = destino,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            scope.launch { alturaActual.animateTo(alturaMes.value) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val deltaDp = with(density) { dragAmount.toDp().value }
                        val nueva = (alturaActual.value + deltaDp).coerceIn(alturaSemana.value, alturaExpandida.value)
                        scope.launch { alturaActual.snapTo(nueva) }
                    }
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                CabeceraCalendario(
                    mesActual = mesActual,
                    onAnterior = {
                        if (esVistaSemana) {
                            val nuevaFecha = fechaSeleccionada.minusWeeks(1)
                            viewModel.actualizarFechaSeleccionada(nuevaFecha)
                            mesActual = YearMonth.from(nuevaFecha)
                        } else {
                            mesActual = mesActual.minusMonths(1)
                        }
                    },
                    onSiguiente = {
                        if (esVistaSemana) {
                            val nuevaFecha = fechaSeleccionada.plusWeeks(1)
                            viewModel.actualizarFechaSeleccionada(nuevaFecha)
                            mesActual = YearMonth.from(nuevaFecha)
                        } else {
                            mesActual = mesActual.plusMonths(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                FilaDiasSemana()
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    CuadriculaDias(
                        mesActual = mesActual,
                        fechaSeleccionada = fechaSeleccionada,
                        eventos = eventos,
                        progresoSemana = progresoSemana,
                        onDiaSeleccionado = {
                            // AQUÍ ESTÁ LA MAGIA: Al tocar un día, le decimos al ViewModel que cambió
                            viewModel.actualizarFechaSeleccionada(it)
                        }
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.width(48.dp).height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
            }
        }

        AnimatedVisibility(
            visible = !calendarioExpandido,
            enter = fadeIn() + expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Text(
                    text = "Eventos del ${fechaSeleccionada.dayOfMonth} de ${fechaSeleccionada.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (eventosDelDia.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Día libre",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(eventosDelDia) { evento ->
                            TarjetaEventoDiario(evento)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun CabeceraCalendario(mesActual: YearMonth, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    val mesNombre = mesActual.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAnterior) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Anterior") }
        Text(text = "$mesNombre ${mesActual.year}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onSiguiente) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Siguiente") }
    }
}

@Composable
fun FilaDiasSemana() {
    val dias = listOf("L", "M", "M", "J", "V", "S", "D")
    Row(modifier = Modifier.fillMaxWidth()) {
        dias.forEach { dia ->
            Text(text = dia, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CuadriculaDias(
    mesActual: YearMonth,
    fechaSeleccionada: LocalDate,
    eventos: List<Evento>,
    progresoSemana: Float,
    onDiaSeleccionado: (LocalDate) -> Unit
) {
    val primerDiaDelMes = mesActual.atDay(1)
    val diasEnElMes = mesActual.lengthOfMonth()
    val offsetPrimerDia = (primerDiaDelMes.dayOfWeek.value - 1) % 7

    val totalCeldas = offsetPrimerDia + diasEnElMes
    val filas = Math.ceil(totalCeldas / 7.0).toInt()

    var diaActual = 1

    val filaSeleccionada = if (fechaSeleccionada.month == mesActual.month && fechaSeleccionada.year == mesActual.year) {
        (offsetPrimerDia + fechaSeleccionada.dayOfMonth - 1) / 7
    } else { 0 }

    Column(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until filas) {
            val rowWeight = if (i == filaSeleccionada) 1f else Math.max(0.001f, 1f - progresoSemana)
            val rowAlpha = if (i == filaSeleccionada) 1f else (1f - progresoSemana)

            Row(
                modifier = Modifier
                    .weight(rowWeight)
                    .fillMaxWidth()
                    .alpha(rowAlpha)
            ) {
                for (j in 0..6) {
                    if ((i == 0 && j < offsetPrimerDia) || diaActual > diasEnElMes) {
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                    } else {
                        val fechaCelda = mesActual.atDay(diaActual)
                        val esSeleccionado = fechaCelda == fechaSeleccionada
                        val esHoy = fechaCelda == LocalDate.now()

                        val tieneEvento = eventos.any {
                            try {
                                LocalDate.parse(it.fecha) == fechaCelda
                            } catch (e: Exception) { false }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        esSeleccionado -> MaterialTheme.colorScheme.primary
                                        esHoy -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDiaSeleccionado(fechaCelda) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = diaActual.toString(),
                                    color = when {
                                        esSeleccionado -> MaterialTheme.colorScheme.onPrimary
                                        esHoy -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (esHoy || esSeleccionado) FontWeight.ExtraBold else FontWeight.Normal
                                )
                                if (tieneEvento) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (esSeleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                        diaActual++
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaEventoDiario(evento: Evento) {
    // Colores derivados de la prioridad — mismo helper que usa la tarjeta de
    // próximos eventos en la pantalla de inicio, así el lenguaje visual es
    // idéntico en ambas pantallas.
    val colores = coloresParaPrioridad(evento.prioridad, MaterialTheme.colorScheme)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Píldora de hora — coloreada por prioridad para que el nivel de
            // urgencia salte a la vista. El "split(" ")[0]" se mantiene por si
            // viene "10:30 AM" — extraemos solo la hora.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colores.contenedor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = evento.hora.split(" ")[0],
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onContenedor,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Título + etiqueta sutil de prioridad
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = evento.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Etiqueta de prioridad (solo si no es la default Media)
                if (!evento.prioridad.equals("Media", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Prioridad ${evento.prioridad.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colores.acento,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Campana de recordatorio — visualmente confirma que está armado
            if (evento.recordatorio) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colores.contenedor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Con recordatorio",
                        tint = colores.acento,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}