package com.colectivobarrios.Tuiteraz.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.colectivobarrios.Tuiteraz.Evento
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarjetaProximosEventos(
    esTablet: Boolean = false,
    eventos: List<Evento>,
    onVerTodoClick: () -> Unit,
    onEventoClick: (Evento) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (esTablet) 28.dp else 24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(if (esTablet) 24.dp else 16.dp)) {

            // ── HEADER ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Próximos eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onVerTodoClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Ver agenda")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── LISTA ──────────────────────────────────────────────────────
            if (eventos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Agenda despejada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val visibles = eventos.take(3)
                val restantes = (eventos.size - 3).coerceAtLeast(0)

                visibles.forEachIndexed { index, evento ->
                    ItemEventoInicio(
                        evento = evento,
                        onClick = { onEventoClick(evento) }
                    )
                    if (index < visibles.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Cuando hay más eventos de los 3 que mostramos, ofrecemos un acceso
                // sutil a la agenda completa. Tappeable, lleva a la pantalla de Eventos.
                if (restantes > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onVerTodoClick() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$restantes más en tu agenda",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemEventoInicio(
    evento: Evento,
    onClick: () -> Unit
) {
    val localDate = try { LocalDate.parse(evento.fecha) } catch (e: Exception) { LocalDate.now() }
    val dia = localDate.dayOfMonth.toString()
    val mes = localDate.month
        .getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "ES"))
        .uppercase()

    // Colores según prioridad — la píldora del día toma estos colores para
    // que el nivel de urgencia se "lea" de un vistazo, sin necesidad de barras
    // laterales ni elementos extra.
    val colores = coloresParaPrioridad(evento.prioridad, MaterialTheme.colorScheme)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Píldora de fecha — el color cambia con la prioridad (única señal
            // visual fuerte; el resto del item se mantiene neutro)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colores.contenedor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mes,
                    style = MaterialTheme.typography.labelSmall,
                    color = colores.onContenedor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dia,
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onContenedor,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Título + hora + etiqueta de prioridad
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = evento.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = evento.hora,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Solo mostramos el texto de prioridad cuando NO es Media (la default),
                    // así no saturamos la mayoría de eventos con la etiqueta "Media"
                    if (!evento.prioridad.equals("Media", ignoreCase = true)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = evento.prioridad,
                            style = MaterialTheme.typography.labelSmall,
                            color = colores.acento,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Campana cuando el evento tiene recordatorio activado — confirma
            // visualmente que va a sonar/notificar, lo cual ahora sí se cumple
            // gracias al sistema de scheduling con AlarmManager.
            if (evento.recordatorio) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colores.contenedor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Con recordatorio",
                        tint = colores.acento,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
