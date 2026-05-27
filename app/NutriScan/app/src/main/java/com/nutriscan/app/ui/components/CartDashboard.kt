package com.nutriscan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutriscan.app.ui.theme.SurfaceDarkElevated

@Composable
fun CartDashboard(
    averageNutriScore: String?,
    totalItems: Int,
    totalSugar: Double,
    totalSodium: Double,
    totalSaturatedFat: Double,
    alertCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            NutriScoreGauge(
                score = averageNutriScore,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Sua Média: ${averageNutriScore ?: "-"}",
                color = Color(0xFF81C784), // Light Green as mockup
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Total: $totalItems itens",
                color = Color.LightGray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroStat(
                    label = "Açúcar",
                    value = "${String.format("%.1f", totalSugar)}g"
                )
                MacroStat(
                    label = "Sódio",
                    value = "${String.format("%.1f", totalSodium)}g"
                )
                MacroStat(
                    label = "Gordura Sat.",
                    value = "${String.format("%.1f", totalSaturatedFat)}g"
                )
            }
            
            if (alertCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF004D40), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Aviso",
                        tint = Color(0xFF00BFA5),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Atenção: $alertCount produtos têm alto teor de açúcar.",
                        color = Color(0xFF00BFA5),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MacroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
