package com.example.balance.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.balance.Frase

@Composable
fun TarjetaFrase(frase: Frase, esTablet: Boolean = false) {
    val escala = remember { Animatable(1f) }

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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (esTablet) 48.dp else 32.dp,
                vertical   = if (esTablet) 44.dp else 28.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\u201C",
                style      = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
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
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(if (esTablet) 24.dp else 18.dp))
            Box(
                Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
            Spacer(Modifier.height(if (esTablet) 18.dp else 14.dp))
            Text(
                frase.autor.uppercase(),
                style         = MaterialTheme.typography.labelLarge,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }
    }
}