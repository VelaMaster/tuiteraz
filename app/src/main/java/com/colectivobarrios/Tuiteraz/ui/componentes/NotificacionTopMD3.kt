package com.colectivobarrios.Tuiteraz.ui.componentes

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.colectivobarrios.Tuiteraz.Evento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionTopMD3(
    visible: Boolean,
    evento: Evento,
    topPadding: Dp,
    onDismiss: () -> Unit,
    onAlternarAlarma: (Evento) -> Unit,
    onEliminar: (Evento) -> Unit,
    onActualizar: (Evento) -> Unit
) {
    val contexto = LocalContext.current
    val sharedPrefs = remember { contexto.getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE) }

    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val expandida by remember { derivedStateOf { swipeOffset > 60f } }

    var modoEdicion by remember { mutableStateOf(false) }
    var tituloEdit by remember { mutableStateOf(evento.titulo) }
    var prioridadEdit by remember { mutableStateOf(evento.prioridad) }
    var horaEdit by remember { mutableStateOf(evento.hora) }

    var recordatorioLocal by remember(evento) { mutableStateOf(evento.recordatorio) }
    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }
    var noVolverAMostrar by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(visible, expandida, modoEdicion, mostrarConfirmarEliminar) {
        if (visible && !expandida && !modoEdicion && !mostrarConfirmarEliminar) {
            delay(5000)
            if (!expandida && !modoEdicion && !mostrarConfirmarEliminar) onDismiss()
        }
    }

    // CORRECCIÓN: Reactividad completa. Si el ViewModel actualiza el evento,
    // se refleja inmediatamente en el modo visualización.
    LaunchedEffect(evento) {
        if (!modoEdicion) {
            tituloEdit = evento.titulo
            prioridadEdit = evento.prioridad
            horaEdit = evento.hora
            recordatorioLocal = evento.recordatorio
        }
    }

    val colorPrioridad = when (prioridadEdit) {
        "Alta" -> MaterialTheme.colorScheme.error
        "Baja" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(300)) { -it } + fadeOut(),
        modifier = Modifier.fillMaxWidth().zIndex(15f).padding(horizontal = 12.dp).padding(top = topPadding + 8.dp)
    ) {
        // CORRECCIÓN BORDES: Usamos Surface en lugar de ElevatedCard para evitar
        // el renderizado doble del fondo/elevación que causa "esquinas sucias"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, if (swipeOffset > 0) (swipeOffset * 0.4f).roundToInt() else swipeOffset.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = !modoEdicion,
                    state = rememberDraggableState { delta -> swipeOffset = (swipeOffset + delta).coerceIn(-200f, 500f) },
                    onDragStopped = { velocity ->
                        if (swipeOffset < -80f || velocity < -600f) onDismiss()
                        else if (swipeOffset > 80f) scope.launch { animate(swipeOffset, 220f) { v, _ -> swipeOffset = v } }
                        else scope.launch { animate(swipeOffset, 0f) { v, _ -> swipeOffset = v } }
                    }
                )
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 300f))
                // Borde y clip limpios
                .border(1.dp, colorPrioridad.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 8.dp // Da el efecto flotante sin ensuciar los bordes internos
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- CABECERA ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(colorPrioridad.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Notifications, null, tint = colorPrioridad)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${prioridadEdit} • ${horaEdit}", style = MaterialTheme.typography.labelMedium, color = colorPrioridad, fontWeight = FontWeight.Bold)
                        if (!modoEdicion) {
                            Text(evento.titulo, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = if (expandida) 2 else 1)
                        } else {
                            BasicTextField(
                                value = tituloEdit,
                                onValueChange = { tituloEdit = it },
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(colorPrioridad),
                                modifier = Modifier.fillMaxWidth().background(colorPrioridad.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(10.dp),
                                decorationBox = { inner -> if (tituloEdit.isEmpty()) Text("Nombre...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)); inner() }
                            )
                        }
                    }
                    IconButton(onClick = { recordatorioLocal = !recordatorioLocal; onAlternarAlarma(evento) }) {
                        Icon(if (recordatorioLocal) Icons.Rounded.AlarmOn else Icons.Rounded.AlarmOff, null, tint = if (recordatorioLocal) colorPrioridad else MaterialTheme.colorScheme.outline)
                    }
                }

                // --- CUERPO EXPANDIDO ---
                if (expandida) {
                    Spacer(Modifier.height(16.dp))

                    if (modoEdicion) {
                        Surface(
                            onClick = { mostrarTimePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, null, tint = colorPrioridad, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Hora programada", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                Text(horaEdit, fontWeight = FontWeight.Bold, color = colorPrioridad)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Column(Modifier.fillMaxWidth()) {
                            Text("Prioridad", style = MaterialTheme.typography.labelSmall, color = colorPrioridad, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Baja", "Media", "Alta").forEach { p ->
                                    FilterChip(
                                        selected = prioridadEdit == p,
                                        onClick = { prioridadEdit = p },
                                        label = { Text(p, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // --- BOTONES DE ACCIÓN ---
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!modoEdicion) {
                            FilledTonalButton(onClick = { modoEdicion = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Editar")
                            }
                            Button(
                                onClick = { if (sharedPrefs.getBoolean("omitir_dialogo_borrar_evento", false)) onEliminar(evento) else mostrarConfirmarEliminar = true },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Eliminar") }
                        } else {
                            TextButton(onClick = { modoEdicion = false; tituloEdit = evento.titulo; prioridadEdit = evento.prioridad }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    // Pasamos el evento actualizado de regreso a la pantalla principal
                                    onActualizar(evento.copy(titulo = tituloEdit, prioridad = prioridadEdit, hora = horaEdit))
                                    modoEdicion = false
                                },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colorPrioridad)
                            ) { Text("Guardar") }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                }
            }
        }
    }

    if (mostrarConfirmarEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarEliminar = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar evento?") },
            text = {
                Column {
                    Text("¿Estás seguro de borrar '${evento.titulo}'?")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { noVolverAMostrar = !noVolverAMostrar }) {
                        Checkbox(checked = noVolverAMostrar, onCheckedChange = { noVolverAMostrar = it })
                        Text("No volver a preguntar", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noVolverAMostrar) sharedPrefs.edit().putBoolean("omitir_dialogo_borrar_evento", true).apply()
                    onEliminar(evento); mostrarConfirmarEliminar = false; onDismiss()
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmarEliminar = false }) { Text("Cancelar") } }
        )
    }

    if (mostrarTimePicker) {
        val state = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { mostrarTimePicker = false },
            confirmButton = { TextButton(onClick = { horaEdit = String.format("%02d:%02d", state.hour, state.minute); mostrarTimePicker = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { mostrarTimePicker = false }) { Text("Cancelar") } },
            text = { TimePicker(state = state) }
        )
    }
}