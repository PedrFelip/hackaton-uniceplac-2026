package com.nutriscan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NutriScoreGauge(
    score: String?,
    modifier: Modifier = Modifier
) {
    val scoreMap = mapOf(
        "A" to 1.0f, // Verde (Direita)
        "B" to 0.75f,
        "C" to 0.5f,
        "D" to 0.25f,
        "E" to 0.0f  // Vermelho (Esquerda)
    )
    
    // Default to middle if null
    val pointerPosition = scoreMap[score?.uppercase()] ?: 0.5f

    Box(modifier = modifier.aspectRatio(2f)) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(2f).padding(16.dp)) {
            val strokeWidth = 30.dp.toPx()
            
            // O SweepGradient fecha a volta no mesmo ponto em que ele começa.
            // Como o arco termina na direita, colocamos a emenda do gradiente ali e repetimos o verde
            // nas posições 0.0f e 1.0f para evitar que a ponta volte para vermelho.
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color(0xFF2E7D32),  // Green A (seam / Direita)
                    0.5f to Color(0xFFD32F2F),  // Red E (180 deg / Esquerda)
                    0.625f to Color(0xFFFF9800),// Orange D (225 deg)
                    0.75f to Color(0xFFFFEB3B), // Yellow C (270 deg / Topo)
                    0.875f to Color(0xFF8BC34A),// Light Green B (315 deg)
                    1.0f to Color(0xFF2E7D32),  // Green A (360 deg / Direita)
                    center = Offset(size.width / 2, size.height)
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height * 2)
            )

            // Draw pointer
            val angleInDegrees = 180f + (180f * pointerPosition)
            val angleInRadians = Math.toRadians(angleInDegrees.toDouble())
            
            val centerX = size.width / 2
            val centerY = size.height
            
            val lineLength = size.width / 2.5f
            
            val endX = centerX + (cos(angleInRadians) * lineLength).toFloat()
            val endY = centerY + (sin(angleInRadians) * lineLength).toFloat()
            
            // Pointer line
            drawLine(
                color = Color.White,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Pointer base circle
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}
