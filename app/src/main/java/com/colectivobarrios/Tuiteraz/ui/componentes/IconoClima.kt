package com.colectivobarrios.Tuiteraz.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icono moderno del clima dibujado con Canvas, estilo widget de Samsung.
 *
 * Iconografía estática (sin animaciones — más limpio y "pesa menos visualmente"
 * dentro de una tarjeta). Composición:
 *  - Cielo despejado     → sol amarillo con rayos cortos
 *  - Parcialmente nublado → sol arriba-der + nube blanca al frente (como el icono Samsung)
 *  - Nublado              → nube blanca grande
 *  - Niebla               → nube + 3 líneas horizontales por debajo
 *  - Llovizna             → nube + 3 gotitas pequeñas
 *  - Lluvia               → nube + 3 gotas grandes
 *  - Nieve                → nube + 3 copos (estrellas de 6 puntas)
 *  - Tormenta             → nube oscura + rayo amarillo
 *  - Default              → sol con nube (parcialmente nublado)
 */
/**
 * Composable principal. Si [esDeNoche] es true, los iconos que normalmente
 * muestran sol cambian a luna. La luna también puede combinarse con nubes.
 */
@Composable
fun IconoClima(
    descripcion: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 56.dp,
    esDeNoche: Boolean = false
) {
    Canvas(modifier = modifier.size(tamano)) {
        val texto = descripcion.lowercase()
        when {
            "despejado" in texto && esDeNoche             -> dibujarLuna()
            "despejado" in texto                          -> dibujarSol()
            "tormenta" in texto                            -> dibujarTormenta()
            "nieve" in texto                               -> dibujarNieve()
            "lluvia" in texto                              -> dibujarLluvia()
            "llovizna" in texto                            -> dibujarLlovizna()
            "niebla" in texto                              -> dibujarNiebla()
            "parcialmente" in texto && esDeNoche           -> dibujarLunaConNube()
            "parcialmente" in texto                        -> dibujarSolConNube()
            "nublado" in texto || "nubes" in texto         -> dibujarNubladoCompleto()
            else                                           -> if (esDeNoche) dibujarLunaConNube() else dibujarSolConNube()
        }
    }
}

// ─── Paleta plana, estilo Samsung Weather ─────────────────────────────────
private val ColorSol         = Color(0xFFFFC107)  // amarillo dorado lleno
private val ColorRayoSol     = Color(0xFFFFB300)  // un punto más oscuro para los rayos
private val ColorNube        = Color(0xFFFFFFFF)  // blanco puro
private val ColorNubeBorde   = Color(0xFFE0E7EE)  // borde casi imperceptible
private val ColorNubeOscura  = Color(0xFF90A4AE)  // gris azulado para tormenta
private val ColorAgua        = Color(0xFF42A5F5)  // azul cielo para lluvia
private val ColorNieve       = Color(0xFFE3F2FD)  // azul claro casi blanco
private val ColorRayo        = Color(0xFFFFD600)  // amarillo brillante para tormentas
private val ColorLuna        = Color(0xFFFFF3CC)  // crema cálida (más cálida que blanco para luna)
private val ColorLunaSombra  = Color(0xFFE8DBA8)  // borde dorado tenue
private val ColorEstrella    = Color(0xFFFFFFFF)  // blanco puro para estrellitas decorativas

// ─── DIBUJO: SOL (despejado) ──────────────────────────────────────────────
private fun DrawScope.dibujarSol() {
    val centro = Offset(size.width / 2f, size.height / 2f)
    val radio = size.minDimension * 0.26f

    dibujarSolCentrado(centro, radio, conRayos = true)
}

// ─── DIBUJO: LUNA (despejado de noche) ────────────────────────────────────
/**
 * Luna creciente moderna estilo Samsung. Se dibuja con dos círculos:
 * uno relleno y otro encima del color de fondo para "morder" un creciente.
 * Acompañada de 2 estrellitas pequeñas para que se vea de noche.
 */
private fun DrawScope.dibujarLuna() {
    val centro = Offset(size.width * 0.50f, size.height * 0.52f)
    val radio = size.minDimension * 0.28f
    dibujarLunaCentrada(centro, radio)

    // Estrellitas decorativas alrededor
    drawCircle(color = ColorEstrella, radius = size.minDimension * 0.022f, center = Offset(size.width * 0.18f, size.height * 0.20f))
    drawCircle(color = ColorEstrella, radius = size.minDimension * 0.018f, center = Offset(size.width * 0.85f, size.height * 0.30f))
    drawCircle(color = ColorEstrella, radius = size.minDimension * 0.014f, center = Offset(size.width * 0.82f, size.height * 0.78f))
}

// ─── DIBUJO: LUNA + NUBE (parcialmente nublado de noche) ──────────────────
private fun DrawScope.dibujarLunaConNube() {
    // Luna pequeña esquina sup-derecha
    val lunaCentro = Offset(size.width * 0.72f, size.height * 0.30f)
    val lunaRadio = size.minDimension * 0.18f
    dibujarLunaCentrada(lunaCentro, lunaRadio)

    // Una estrellita arriba a la izquierda
    drawCircle(color = ColorEstrella, radius = size.minDimension * 0.018f, center = Offset(size.width * 0.20f, size.height * 0.18f))

    // Nube blanca al frente
    dibujarSiluetaNube(
        centroX = size.width * 0.42f,
        centroY = size.height * 0.62f,
        ancho = size.width * 0.78f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
}

// ─── HELPER: LUNA CRECIENTE ───────────────────────────────────────────────
/**
 * Dibuja una luna creciente. Técnica: círculo lleno + círculo del color de
 * fondo desplazado encima para "comer" un trozo y formar la curva creciente.
 *
 * Como no podemos saber el color exacto del fondo (depende del tema/Material You),
 * usamos blendMode con clear para sustraer un círculo. Más portable: dibujar el
 * "mordisco" con el color del card container — pero como va dentro de un Card de
 * tertiaryContainer, mejor usar approach más seguro: una forma personalizada.
 *
 * Aquí usamos la técnica del círculo desplazado con un color que se mezcla bien
 * con cualquier fondo (transparente). Para eso aprovechamos que dibujamos en un
 * Canvas vacío — el círculo "mordisco" lo hacemos con `BlendMode.Clear`.
 */
private fun DrawScope.dibujarLunaCentrada(centro: Offset, radio: Float) {
    // Cuerpo lleno de la luna
    drawCircle(color = ColorLuna, radius = radio, center = centro)

    // Borde sutil dorado para profundidad
    drawCircle(
        color = ColorLunaSombra,
        radius = radio,
        center = centro,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = radio * 0.08f)
    )

    // Detalles de cráteres (3 círculos pequeños sutiles) — le da personalidad
    // tipo emoji de luna sin ser caricaturesco.
    val sombra = ColorLunaSombra.copy(alpha = 0.55f)
    drawCircle(color = sombra, radius = radio * 0.18f, center = centro.copy(x = centro.x - radio * 0.30f, y = centro.y - radio * 0.20f))
    drawCircle(color = sombra, radius = radio * 0.12f, center = centro.copy(x = centro.x + radio * 0.25f, y = centro.y + radio * 0.10f))
    drawCircle(color = sombra, radius = radio * 0.10f, center = centro.copy(x = centro.x - radio * 0.10f, y = centro.y + radio * 0.30f))
}

// ─── DIBUJO: SOL + NUBE (parcialmente nublado, como Samsung) ──────────────
private fun DrawScope.dibujarSolConNube() {
    // Sol pequeño en la esquina superior derecha
    val solCentro = Offset(size.width * 0.72f, size.height * 0.30f)
    val solRadio = size.minDimension * 0.16f
    dibujarSolCentrado(solCentro, solRadio, conRayos = true, escalaRayos = 0.85f)

    // Nube blanca al frente, ocupa la parte central-inferior izquierda
    dibujarSiluetaNube(
        centroX = size.width * 0.42f,
        centroY = size.height * 0.62f,
        ancho = size.width * 0.78f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
}

// ─── DIBUJO: NUBLADO (sin sol) ────────────────────────────────────────────
private fun DrawScope.dibujarNubladoCompleto() {
    // Una nube grande centrada, ocupando casi todo el espacio
    dibujarSiluetaNube(
        centroX = size.width / 2f,
        centroY = size.height * 0.55f,
        ancho = size.width * 0.85f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
}

// ─── DIBUJO: NIEBLA ───────────────────────────────────────────────────────
private fun DrawScope.dibujarNiebla() {
    // Nube apagada arriba
    dibujarSiluetaNube(
        centroX = size.width / 2f,
        centroY = size.height * 0.33f,
        ancho = size.width * 0.75f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
    // 3 líneas horizontales = niebla
    val grosor = size.minDimension * 0.06f
    val xs = floatArrayOf(0.20f, 0.12f, 0.25f)  // ligeramente desalineadas para vida
    val ys = floatArrayOf(0.66f, 0.78f, 0.90f)
    val anchos = floatArrayOf(0.65f, 0.75f, 0.55f)
    ys.forEachIndexed { i, y ->
        drawLine(
            color = ColorNubeOscura.copy(alpha = 0.65f),
            start = Offset(size.width * xs[i], size.height * y),
            end = Offset(size.width * (xs[i] + anchos[i]), size.height * y),
            strokeWidth = grosor,
            cap = StrokeCap.Round
        )
    }
}

// ─── DIBUJO: LLOVIZNA ─────────────────────────────────────────────────────
private fun DrawScope.dibujarLlovizna() {
    dibujarSiluetaNube(
        centroX = size.width / 2f,
        centroY = size.height * 0.40f,
        ancho = size.width * 0.78f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
    // 3 gotitas pequeñas estáticas debajo
    val xs = floatArrayOf(0.32f, 0.50f, 0.68f)
    val y = size.height * 0.82f
    xs.forEach { x ->
        drawCircle(
            color = ColorAgua,
            radius = size.minDimension * 0.035f,
            center = Offset(size.width * x, y)
        )
    }
}

// ─── DIBUJO: LLUVIA ───────────────────────────────────────────────────────
private fun DrawScope.dibujarLluvia() {
    dibujarSiluetaNube(
        centroX = size.width / 2f,
        centroY = size.height * 0.38f,
        ancho = size.width * 0.80f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
    // 3 gotas alargadas estáticas — forma de lágrima vertical
    val xs = floatArrayOf(0.28f, 0.50f, 0.72f)
    val yArriba = size.height * 0.70f
    val largo = size.minDimension * 0.18f
    xs.forEach { x ->
        drawLine(
            color = ColorAgua,
            start = Offset(size.width * x, yArriba),
            end = Offset(size.width * x, yArriba + largo),
            strokeWidth = size.minDimension * 0.06f,
            cap = StrokeCap.Round
        )
    }
}

// ─── DIBUJO: NIEVE ────────────────────────────────────────────────────────
private fun DrawScope.dibujarNieve() {
    dibujarSiluetaNube(
        centroX = size.width / 2f,
        centroY = size.height * 0.38f,
        ancho = size.width * 0.78f,
        color = ColorNube,
        colorBorde = ColorNubeBorde
    )
    // 3 copos: estrellitas de 3 ejes
    val xs = floatArrayOf(0.30f, 0.50f, 0.70f)
    val y = size.height * 0.80f
    val brazo = size.minDimension * 0.07f
    xs.forEach { x ->
        val centroCopo = Offset(size.width * x, y)
        // 3 líneas a 0°, 60°, 120°
        listOf(0.0, 60.0, 120.0).forEach { ang ->
            val rad = Math.toRadians(ang)
            val dx = (kotlin.math.cos(rad) * brazo).toFloat()
            val dy = (kotlin.math.sin(rad) * brazo).toFloat()
            drawLine(
                color = ColorNieve,
                start = Offset(centroCopo.x - dx, centroCopo.y - dy),
                end = Offset(centroCopo.x + dx, centroCopo.y + dy),
                strokeWidth = size.minDimension * 0.03f,
                cap = StrokeCap.Round
            )
        }
    }
}

// ─── DIBUJO: TORMENTA ─────────────────────────────────────────────────────
private fun DrawScope.dibujarTormenta() {
    // Nube más oscura para sugerir tempestad
    dibujarSiluetaNube(
        centroX = size.width / 2f,
        centroY = size.height * 0.36f,
        ancho = size.width * 0.82f,
        color = ColorNubeOscura,
        colorBorde = Color(0xFF607D8B)
    )
    // Rayo zigzag amarillo en el centro
    val cx = size.width * 0.45f
    val cy = size.height * 0.62f
    val rayoPath = Path().apply {
        moveTo(cx, cy)
        lineTo(cx - size.minDimension * 0.07f, cy + size.minDimension * 0.10f)
        lineTo(cx, cy + size.minDimension * 0.13f)
        lineTo(cx - size.minDimension * 0.04f, cy + size.minDimension * 0.28f)
        lineTo(cx + size.minDimension * 0.12f, cy + size.minDimension * 0.10f)
        lineTo(cx + size.minDimension * 0.03f, cy + size.minDimension * 0.07f)
        lineTo(cx + size.minDimension * 0.10f, cy)
        close()
    }
    drawPath(path = rayoPath, color = ColorRayo)
}

// ─── HELPER: SOL CON RAYOS ────────────────────────────────────────────────
/**
 * Dibuja un sol redondo lleno con (opcionalmente) 8 rayos cortos alrededor.
 * Los rayos son segmentos cortos discretos, estilo Samsung — no líneas largas
 * tipo emoji clásico.
 */
private fun DrawScope.dibujarSolCentrado(
    centro: Offset,
    radio: Float,
    conRayos: Boolean,
    escalaRayos: Float = 1f
) {
    if (conRayos) {
        val largoRayo = radio * 0.55f * escalaRayos
        val gapRayo = radio * 0.30f
        val grosor = radio * 0.20f * escalaRayos
        repeat(8) { i ->
            val angulo = i * 45.0
            val rad = Math.toRadians(angulo)
            val cos = kotlin.math.cos(rad).toFloat()
            val sin = kotlin.math.sin(rad).toFloat()
            val inicio = Offset(
                centro.x + (radio + gapRayo) * cos,
                centro.y + (radio + gapRayo) * sin
            )
            val fin = Offset(
                centro.x + (radio + gapRayo + largoRayo) * cos,
                centro.y + (radio + gapRayo + largoRayo) * sin
            )
            drawLine(
                color = ColorRayoSol,
                start = inicio,
                end = fin,
                strokeWidth = grosor,
                cap = StrokeCap.Round
            )
        }
    }
    // Cuerpo del sol
    drawCircle(color = ColorSol, radius = radio, center = centro)
}

// ─── HELPER: SILUETA DE NUBE PLANA ────────────────────────────────────────
/**
 * Nube formada por 4 burbujas circulares solapadas. Compone una silueta
 * blanca estilo "cumulus simplificado" como el icono de Samsung Weather.
 *
 * El parámetro [ancho] define el ancho total horizontal aproximado;
 * la altura se deriva proporcionalmente.
 */
private fun DrawScope.dibujarSiluetaNube(
    centroX: Float,
    centroY: Float,
    ancho: Float,
    color: Color,
    colorBorde: Color
) {
    val rGrande = ancho * 0.30f   // burbuja superior izquierda (la más prominente)
    val rMedio  = ancho * 0.26f   // burbuja superior derecha
    val rBajoIzq = ancho * 0.22f  // burbuja inferior izquierda
    val rBajoDer = ancho * 0.22f  // burbuja inferior derecha

    val centroSupIzq = Offset(centroX - ancho * 0.10f, centroY - ancho * 0.10f)
    val centroSupDer = Offset(centroX + ancho * 0.18f, centroY - ancho * 0.04f)
    val izq = Offset(centroX - ancho * 0.30f, centroY + ancho * 0.06f)
    val der = Offset(centroX + ancho * 0.32f, centroY + ancho * 0.08f)

    drawCircle(color = color, radius = rGrande, center = centroSupIzq)
    drawCircle(color = color, radius = rMedio,  center = centroSupDer)
    drawCircle(color = color, radius = rBajoIzq, center = izq)
    drawCircle(color = color, radius = rBajoDer, center = der)

    // Borde sutil unificado para que las 4 burbujas se vean como una sola
    // silueta y no como 4 círculos separados.
    val grosorBorde = ancho * 0.012f
    drawCircle(color = colorBorde, radius = rGrande,  center = centroSupIzq, style = Stroke(grosorBorde))
    drawCircle(color = colorBorde, radius = rMedio,   center = centroSupDer, style = Stroke(grosorBorde))
    drawCircle(color = colorBorde, radius = rBajoIzq, center = izq,         style = Stroke(grosorBorde))
    drawCircle(color = colorBorde, radius = rBajoDer, center = der,         style = Stroke(grosorBorde))
}
