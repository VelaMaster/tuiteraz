package com.example.Tuiteraz.ui.componentes

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SeccionCalendarioOpenSource(onFeriadoSeleccionado: (String) -> Unit) {
    val mesActual  = remember { YearMonth.now() }
    val mesInicio  = remember { mesActual.minusMonths(12) }
    val mesFin     = remember { mesActual.plusMonths(12) }
    val diasSemana = remember { daysOfWeek(firstDayOfWeekFromLocale()) }

    val feriados = remember {
        mapOf(
            LocalDate.of(2026,  1,  1) to "Año Nuevo",
            LocalDate.of(2026,  2,  2) to "Día de la Candelaria",
            LocalDate.of(2026,  3, 16) to "Natalicio de B. Juárez",
            LocalDate.of(2026,  3, 21) to "Inicio de la Primavera",
            LocalDate.of(2026,  4,  2) to "Jueves Santo",
            LocalDate.of(2026,  4,  3) to "Viernes Santo",
            LocalDate.of(2026,  4, 30) to "Día del Niño",
            LocalDate.of(2026,  5,  1) to "Día del Trabajo",
            LocalDate.of(2026,  5,  5) to "Batalla de Puebla",
            LocalDate.of(2026,  9, 16) to "Día de la Independencia",
            LocalDate.of(2026, 11,  2) to "Día de Muertos",
            LocalDate.of(2026, 12, 25) to "Navidad"
        )
    }

    var fechaSeleccionada by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var mesVisible        by remember { mutableStateOf(mesActual) }

    val estadoCalendario = rememberCalendarState(
        startMonth        = mesInicio,
        endMonth          = mesFin,
        firstVisibleMonth = mesActual,
        firstDayOfWeek    = diasSemana.first()
    )

    LaunchedEffect(estadoCalendario.firstVisibleMonth) {
        mesVisible = estadoCalendario.firstVisibleMonth.yearMonth
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessVeryLow
                )
            ),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            val nombreMes = mesVisible.month.getDisplayName(TextStyle.FULL, Locale("es", "MX"))
            Text(
                text       = "${nombreMes.replaceFirstChar { it.uppercase() }} ${mesVisible.year}",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                for (dia in diasSemana) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text       = dia.getDisplayName(TextStyle.NARROW, Locale("es", "MX")).uppercase(),
                            color      = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalCalendar(
                modifier       = Modifier.clipToBounds(),
                state          = estadoCalendario,
                contentPadding = PaddingValues(horizontal = 0.dp),
                dayContent     = { day ->
                    DiaPersonalizado(
                        day            = day,
                        esSeleccionado = fechaSeleccionada == day.date,
                        tieneEvento    = feriados.containsKey(day.date),
                        onClick        = { fecha ->
                            fechaSeleccionada = fecha
                            feriados[fecha]?.let { onFeriadoSeleccionado(it) }
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun DiaPersonalizado(
    day           : CalendarDay,
    esSeleccionado: Boolean,
    tieneEvento   : Boolean,
    onClick       : (LocalDate) -> Unit
) {
    if (day.position == DayPosition.MonthDate) {
        val colorFondo = if (esSeleccionado) MaterialTheme.colorScheme.primary else Color.Transparent
        val colorTexto = if (esSeleccionado) MaterialTheme.colorScheme.onPrimary
        else               MaterialTheme.colorScheme.onSecondaryContainer

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(6.dp)
                .clip(CircleShape)
                .background(colorFondo)
                .clickable { onClick(day.date) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = day.date.dayOfMonth.toString(),
                color      = colorTexto,
                fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                style      = MaterialTheme.typography.bodyLarge
            )
            if (tieneEvento) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (esSeleccionado) MaterialTheme.colorScheme.onPrimary
                            else               MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    } else {
        Spacer(modifier = Modifier.aspectRatio(1f).padding(6.dp))
    }
}