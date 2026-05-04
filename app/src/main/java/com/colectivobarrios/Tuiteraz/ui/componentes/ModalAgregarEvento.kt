package com.colectivobarrios.Tuiteraz.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalAgregarEvento(
    fechaPredefinida: LocalDate,
    permiteCambiarFecha: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (titulo: String, fecha: String, hora: String, recordatorio: Boolean, prioridad: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tituloEvento by remember { mutableStateOf("") }

    var recordatorio by remember { mutableStateOf(false) }
    var prioridad by remember { mutableStateOf("Media") }
    val opcionesPrioridad = listOf("Baja", "Media", "Alta")

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }

    // SOLUCIÓN DEFINITIVA A LA ZONA HORARIA:
    // El DatePicker de Compose siempre trabaja en UTC. Si le pasamos la zona de México (systemDefault),
    // le restará 6 horas y te atrasará un día. Usamos ZoneOffset.UTC para mantener la fecha intacta.
    var fechaSeleccionadaMillis by remember(fechaPredefinida) {
        mutableStateOf(fechaPredefinida.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    }

    val timePickerState = rememberTimePickerState()
    val horaTexto = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)

    val fechaFormatVista = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))
    val fechaFormatDB = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Recuperamos la fecha también en UTC para que cuadre exacto con lo que elegiste
    val fechaLocal = Instant.ofEpochMilli(fechaSeleccionadaMillis).atZone(ZoneOffset.UTC).toLocalDate()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Text("Nuevo Evento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tituloEvento,
                onValueChange = { tituloEvento = it },
                label = { Text("¿Qué tienes planeado?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = fechaLocal.format(fechaFormatVista),
                    onValueChange = { },
                    label = { Text(if (permiteCambiarFecha) "Fecha" else "Día Seleccionado") },
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable(enabled = permiteCambiarFecha) { mostrarDatePicker = true },
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (permiteCambiarFecha) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        disabledBorderColor = if (permiteCambiarFecha) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = { Icon(Icons.Outlined.DateRange, null) }
                )

                OutlinedTextField(
                    value = horaTexto,
                    onValueChange = { },
                    label = { Text("Hora") },
                    modifier = Modifier.weight(0.8f).clickable { mostrarTimePicker = true },
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = { Icon(Icons.Outlined.Info, null) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Nivel de prioridad:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    opcionesPrioridad.forEach { op ->
                        FilterChip(
                            selected = prioridad == op,
                            onClick = { prioridad = op },
                            label = { Text(op) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Activar recordatorio", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = recordatorio, onCheckedChange = { recordatorio = it })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onGuardar(tituloEvento, fechaLocal.format(fechaFormatDB), horaTexto, recordatorio, prioridad)
                    },
                    enabled = tituloEvento.isNotBlank()
                ) {
                    Text("Guardar Evento")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (mostrarDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaSeleccionadaMillis)
            DatePickerDialog(
                onDismissRequest = { mostrarDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fechaSeleccionadaMillis = datePickerState.selectedDateMillis ?: fechaSeleccionadaMillis
                        mostrarDatePicker = false
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }
            ) { DatePicker(state = datePickerState) }
        }

        if (mostrarTimePicker) {
            AlertDialog(
                onDismissRequest = { mostrarTimePicker = false },
                confirmButton = { TextButton(onClick = { mostrarTimePicker = false }) { Text("Aceptar") } },
                dismissButton = { TextButton(onClick = { mostrarTimePicker = false }) { Text("Cancelar") } },
                text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { TimePicker(state = timePickerState) } }
            )
        }
    }
}