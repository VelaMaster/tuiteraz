package com.example.Tuiteraz

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAcercaDe(onBack: () -> Unit) {
    // Manejo del gesto de atrás del sistema
    BackHandler { onBack() }

    // Contexto para lanzar el intent de correo
    val context = LocalContext.current
    // Estado para guardar el texto de la sugerencia
    var sugerenciaTexto by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO ELEGANTE ---
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "T",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Tu i teraz",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Versión 1.0 - México",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(32.dp))

            // --- SECCIÓN TEXTO INFORMATIVO ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Acerca de la app",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tuiteraz es una herramienta diseñada para ayudarte a ver detalles de tu día y conectar con un pensamiento día a día. El nombre de la aplicación viene del polaco, que significa \"aquí y ahora\". Espero les pueda ayudar y les guste.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Justify,
                    lineHeight = 24.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- LISTA VERTICAL DE NOVEDADES ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Lo nuevo en la v1.0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TarjetaPreviewV1(
                    "Frase Diaria",
                    "Recibe una sola frase poderosa cada 24 horas para mantener el enfoque.",
                    Icons.Outlined.FormatQuote,
                    MaterialTheme.colorScheme.primaryContainer
                )
                TarjetaPreviewV1(
                    "Widget de Motivación",
                    "Un widget que te da la motivación con tu frase diaria directamente en tu pantalla de inicio.",
                    Icons.Outlined.Widgets,
                    MaterialTheme.colorScheme.secondaryContainer
                )
                TarjetaPreviewV1(
                    "Diseño Visual",
                    "Interfaz adaptable con Material You, icono dinámico y soporte total para tema oscuro.",
                    Icons.Outlined.Palette,
                    MaterialTheme.colorScheme.tertiaryContainer
                )
                TarjetaPreviewV1(
                    "Copia Cloud",
                    "Sincronización segura con Supabase para no perder nunca tus frases favoritas.",
                    Icons.Outlined.CloudDone,
                    MaterialTheme.colorScheme.surfaceVariant
                )
                TarjetaPreviewV1(
                    "Exportación HD",
                    "Comparte pensamientos en alta resolución con diseños minimalistas y elegantes.",
                    Icons.Outlined.Hd,
                    MaterialTheme.colorScheme.primaryContainer // <- Aquí quitamos el ".copy(alpha = 0.7f)" feo
                )
            }

            Spacer(Modifier.height(48.dp))

            // --- SECCIÓN DE CONTACTO FINAL ---
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))

            Text(
                "¿Dudas o sugerencias?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = sugerenciaTexto,
                onValueChange = { sugerenciaTexto = it },
                label = { Text("Insertar duda o sugerencia") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (sugerenciaTexto.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:pdiegovela@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Duda o sugerencia - Tuiteraz")
                            putExtra(Intent.EXTRA_TEXT, sugerenciaTexto)
                        }
                        context.startActivity(intent)
                        sugerenciaTexto = "" // Opcional: limpiar el campo después de enviar
                    }
                },
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "Enviar", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TarjetaPreviewV1(titulo: String, desc: String, icono: ImageVector, color: Color) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}