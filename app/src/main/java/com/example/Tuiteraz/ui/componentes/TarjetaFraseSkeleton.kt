package com.example.Tuiteraz.ui.componentes

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun TarjetaFraseSkeleton(esTablet: Boolean = false) {
    val transicionInfinita = rememberInfiniteTransition(label = "animacionSkeleton")
    val alphaAnimado by transicionInfinita.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaSkeleton"
    )

    val colorSkeleton = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaAnimado)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
            Box(
                modifier = Modifier
                    .size(if (esTablet) 48.dp else 36.dp)
                    .clip(CircleShape)
                    .background(colorSkeleton)
                    .align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(0.9f).height(if (esTablet) 30.dp else 24.dp).clip(RoundedCornerShape(8.dp)).background(colorSkeleton))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(if (esTablet) 30.dp else 24.dp).clip(RoundedCornerShape(8.dp)).background(colorSkeleton))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(if (esTablet) 30.dp else 24.dp).clip(RoundedCornerShape(8.dp)).background(colorSkeleton))

            Spacer(Modifier.height(if (esTablet) 24.dp else 18.dp))

            Box(Modifier.width(48.dp).height(2.dp).clip(CircleShape).background(colorSkeleton))

            Spacer(Modifier.height(if (esTablet) 18.dp else 14.dp))

            Box(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(colorSkeleton))

            Spacer(Modifier.height(if (esTablet) 32.dp else 24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(colorSkeleton))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(colorSkeleton))
            }
        }
    }
}