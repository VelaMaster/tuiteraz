package com.example.Tuiteraz.ui.componentes

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Tuiteraz.Frase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@Composable
fun TarjetaFrase(
    frase: Frase,
    esTablet: Boolean = false,
    esFavorita: Boolean = false,
    onToggleFavorito: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val escala = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Animaciones para el corazón
    val colorCorazon by animateColorAsState(
        targetValue = if (esFavorita) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "colorCorazon"
    )
    val escalaCorazon = remember { Animatable(1f) }

    // --- CAPTURAMOS LOS COLORES DEL TEMA ACTUAL MD3 ---
    val colorFondo = MaterialTheme.colorScheme.surfaceContainerLow.toArgb()
    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()
    val colorPrimario = MaterialTheme.colorScheme.primary.toArgb()
    val colorTerciario = MaterialTheme.colorScheme.tertiary.toArgb()

    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .efectoPulsacionSutil(escala),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = Color(colorFondo)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                top = if (esTablet) 44.dp else 28.dp,
                bottom = if (esTablet) 24.dp else 16.dp,
                start = if (esTablet) 48.dp else 32.dp,
                end = if (esTablet) 48.dp else 32.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\u201C",
                style      = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color      = Color(colorPrimario).copy(alpha = 0.2f),
                lineHeight = 32.sp,
                modifier   = Modifier.fillMaxWidth(),
                textAlign  = TextAlign.Start
            )
            Text(
                frase.texto,
                style      = if (esTablet) MaterialTheme.typography.headlineSmall
                else          MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                fontStyle  = FontStyle.Italic,
                textAlign  = TextAlign.Center,
                lineHeight = if (esTablet) 36.sp else 30.sp,
                color      = Color(colorTexto)
            )
            Spacer(Modifier.height(if (esTablet) 24.dp else 18.dp))
            Box(
                Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color(colorPrimario).copy(alpha = 0.35f))
            )
            Spacer(Modifier.height(if (esTablet) 18.dp else 14.dp))
            Text(
                frase.autor.uppercase(),
                style         = MaterialTheme.typography.labelLarge,
                fontWeight    = FontWeight.Bold,
                color         = Color(colorPrimario),
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(if (esTablet) 32.dp else 24.dp))

            // Fila de botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- BOTÓN COMPARTIR IMAGEN (MD3) ---
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val bitmap = generarBitmapCuadrado(
                                contexto = contexto,
                                texto = frase.texto,
                                autor = frase.autor,
                                colorFondo = colorFondo,
                                colorTexto = colorTexto,
                                colorPrimario = colorPrimario,
                                colorTerciario = colorTerciario
                            )
                            val uri = guardarBitmapEnCache(contexto, bitmap)
                            if (uri != null) {
                                compartirImagen(contexto, uri)
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(contexto, "Error al generar imagen", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir imagen",
                        tint = Color(colorTexto).copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Botón Favorito
                IconButton(
                    onClick = {
                        onToggleFavorito()
                        scope.launch {
                            escalaCorazon.animateTo(1.4f, tween(100))
                            escalaCorazon.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (esFavorita) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Añadir a favoritos",
                        tint = colorCorazon,
                        modifier = Modifier.scale(escalaCorazon.value)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FUNCIONES TÉCNICAS (EL PINTOR VIRTUAL Y EL ENVÍO)
// ─────────────────────────────────────────────────────────────────────────────

private fun generarBitmapCuadrado(
    contexto: Context,
    texto: String,
    autor: String,
    colorFondo: Int,
    colorTexto: Int,
    colorPrimario: Int,
    colorTerciario: Int
): Bitmap {
    // --- MODO ULTRA HD (4K Cuadrado) ---
    val tamaño = 2160
    val bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Fondo
    canvas.drawColor(colorFondo)

    val pad = tamaño * 0.1f
    val anchoUtil = tamaño - (pad * 2)

    // Pintura para las comillas primarias (Ahora con suavizado extremo)
    val paintComillas = TextPaint().apply {
        color = Color(colorPrimario).copy(alpha = 0.15f).toArgb()
        textSize = tamaño * 0.25f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true // Borde suave
        isDither = true    // Colores perfectos
    }
    canvas.drawText("\u201C", pad * 0.5f, pad * 1.5f, paintComillas)

    // Tipografía Serif para la frase
    val paintTexto = TextPaint().apply {
        color = colorTexto
        textSize = tamaño * 0.055f
        typeface = Typeface.create("serif", Typeface.NORMAL)
        isAntiAlias = true
        isDither = true
    }

    val builder = StaticLayout.Builder.obtain(texto, 0, texto.length, paintTexto, anchoUtil.toInt())
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, 1.2f)
        .setIncludePad(false)
    val textLayout = builder.build()

    canvas.save()
    val textHeight = textLayout.height
    val textY = (tamaño - textHeight) / 2f
    canvas.translate(pad, textY)
    textLayout.draw(canvas)
    canvas.restore()

    // Pintura para la línea divisoria
    val paintLinea = Paint().apply {
        color = Color(colorPrimario).copy(alpha = 0.3f).toArgb()
        strokeWidth = tamaño * 0.005f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    val lineaY = textY + textHeight + (tamaño * 0.05f)
    canvas.drawLine(tamaño / 2f - (tamaño * 0.05f), lineaY, tamaño / 2f + (tamaño * 0.05f), lineaY, paintLinea)

    // Pintura para el autor
    val paintAutor = TextPaint().apply {
        color = colorPrimario
        textSize = tamaño * 0.035f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isDither = true
        letterSpacing = 0.15f
    }
    val textoAutor = "— ${autor.uppercase()}"
    canvas.drawText(textoAutor, tamaño / 2f, lineaY + (tamaño * 0.07f), paintAutor)

    // Identidad SÚPER SUTIL abajo
    val paintApp = TextPaint().apply {
        color = Color(colorTexto).copy(alpha = 0.18f).toArgb()
        textSize = tamaño * 0.020f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isDither = true
    }
    canvas.drawText("Tuiteraz", tamaño / 2f, tamaño - (pad * 0.5f), paintApp)

    return bitmap
}

private fun guardarBitmapEnCache(contexto: Context, bitmap: Bitmap): Uri? {
    val nombreArchivo = "Tuiteraz_${System.currentTimeMillis()}.png"
    var uri: Uri? = null

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Tuiteraz")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = contexto.contentResolver
    uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        try {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.let { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(it, null, null)
            uri = null
        }
    }
    return uri
}

private fun compartirImagen(contexto: Context, uri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(shareIntent, "Compartir tarjeta con..."))
}