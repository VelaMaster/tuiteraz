// RUTA: app/src/main/java/com/example/balance/AnimConstantes.kt
package com.colectivobarrios.Tuiteraz

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTES DE ANIMACIÓN — compartidas en TODA la app
// Si cambias un valor aquí cambia en todas las pantallas simultáneamente.
//
// Filosofía MD3:
//   • Las animaciones espaciales (movimiento, tamaño, forma) usan SPRING
//   • Las animaciones de efectos (alpha, color) usan TWEEN
//   • Entradas: más lentas  (~500ms) para dar peso visual
//   • Salidas:  más rápidas (~200ms) para no bloquear al usuario
//   • Acciones (shake, bounce): muy rápidas (~80-120ms por ciclo)
// ─────────────────────────────────────────────────────────────────────────────

// ── Duraciones tween ──────────────────────────────────────────────────────────
const val DUR_ENTRADA  = 500   // ms — entrada de pantallas / secciones
const val DUR_SALIDA   = 200   // ms — salida de pantallas
const val DUR_EFECTO   = 380   // ms — fades, crossfades
const val DUR_RAPIDO   = 180   // ms — salidas rápidas
const val DUR_ACCION   = 80    // ms — respuesta a toque (press scale)

// ── Retrasos de cascada ───────────────────────────────────────────────────────
const val DELAY_SECCION_1 = 0    // ms
const val DELAY_SECCION_2 = 80   // ms
const val DELAY_SECCION_3 = 160  // ms
const val DELAY_ITEM      = 60   // ms entre ítems de lista

// ── Springs ───────────────────────────────────────────────────────────────────

// MUY rebotante — para transformaciones de shape, círculos, acciones llamativas
val SpringMuyRebotante   = spring<Float>(Spring.DampingRatioHighBouncy,   Spring.StiffnessMediumLow)
val SpringMuyRebotanteDp = spring<Dp>(Spring.DampingRatioHighBouncy,      Spring.StiffnessMediumLow)

// Medio rebotante — para entradas de cards, ítems de lista
val SpringMedioRebotante          = spring<Float>(Spring.DampingRatioMediumBouncy,     Spring.StiffnessMedium)
val SpringMedioRebotanteDp        = spring<Dp>(Spring.DampingRatioMediumBouncy,        Spring.StiffnessMedium)
val SpringMedioRebotanteIntOffset = spring<IntOffset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)

// Sin rebote — para elevación, opacidad suave
val SpringSuave          = spring<Float>(Spring.DampingRatioNoBouncy,     Spring.StiffnessMediumLow)

// ── Tweens reutilizables ──────────────────────────────────────────────────────
fun tweenEntrada(retraso: Int = 0) = tween<Float>(DUR_ENTRADA, delayMillis = retraso)
fun tweenSalida()                  = tween<Float>(DUR_SALIDA)
fun tweenEfecto(retraso: Int = 0)  = tween<Float>(DUR_EFECTO, delayMillis = retraso)
fun tweenRapido()                  = tween<Float>(DUR_RAPIDO)
fun tweenAccion()                  = tween<Float>(DUR_ACCION)