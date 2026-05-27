package com.nutriscan.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nutriscan.app.data.diabetic.DiabetesRiskLevel
import com.nutriscan.app.data.diabetic.DiabetesRiskResult

/**
 * Card de alerta para diabéticos exibido na tela de detalhe do produto.
 *
 * Mostra o nível de risco glicêmico com cores e ícones distintos:
 * - SAFE: verde com ícone de check
 * - MODERATE: amarelo/laranja com ícone de aviso
 * - HIGH: vermelho com ícone de perigo
 */
@Composable
fun DiabeticAlertCard(
    result: DiabetesRiskResult,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, icon) = when (result.level) {
        DiabetesRiskLevel.SAFE -> Triple(
            Color(0xFF1B5E20),      // verde escuro
            Color(0xFF81C784),      // verde claro
            Icons.Default.CheckCircle
        )
        DiabetesRiskLevel.MODERATE -> Triple(
            Color(0xFF7C4D00),      // âmbar escuro
            Color(0xFFFFD54F),      // âmbar claro
            Icons.Default.Warning
        )
        DiabetesRiskLevel.HIGH -> Triple(
            Color(0xFF7F0000),      // vermelho escuro
            Color(0xFFFF8A80),      // vermelho claro
            Icons.Default.Warning
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when (result.level) {
                        DiabetesRiskLevel.SAFE -> "Adequado para diabéticos"
                        DiabetesRiskLevel.MODERATE -> "Atenção — Moderado"
                        DiabetesRiskLevel.HIGH -> "Alto Risco — Evitar"
                    },
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.message,
                    color = contentColor.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
