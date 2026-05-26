package com.example.pocketnotev20.ui.ctmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold
import java.util.Locale

@Composable
fun CtMarksCalculatorScreen(
    onBackClick: () -> Unit
) {
    var bestOfCount by remember { mutableIntStateOf(2) }

    // State for CT marks, default obtained marks are "0"
    var ct1Obt by remember { mutableStateOf("0") }
    var ct1Tot by remember { mutableStateOf("15") }
    var ct2Obt by remember { mutableStateOf("0") }
    var ct2Tot by remember { mutableStateOf("15") }
    var ct3Obt by remember { mutableStateOf("0") }
    var ct3Tot by remember { mutableStateOf("15") }
    var ct4Obt by remember { mutableStateOf("0") }
    var ct4Tot by remember { mutableStateOf("15") }

    // Calculation logic: (45 / N) * Sum of best N ratios (Obtained / Total)
    // For N=2, this is 22.5 * (R1 + R2) as requested.
    val ctData = listOf(
        ct1Obt to ct1Tot,
        ct2Obt to ct2Tot,
        ct3Obt to ct3Tot,
        ct4Obt to ct4Tot
    )

    val ratios = ctData.map { (obt, tot) ->
        val o = obt.toDoubleOrNull() ?: 0.0
        val t = tot.toDoubleOrNull() ?: 1.0
        if (t == 0.0) 0.0 else (o / t)
    }

    val sortedRatios = ratios.sortedDescending()
    val bestOfSum = sortedRatios.take(bestOfCount).sum()
    val finalScore = (45.0 / bestOfCount.toDouble()) * bestOfSum

    ProfessionalPageScaffold(
        title = "CT Marks Calculator",
        subtitle = "Calculate your score based on best of N count.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppInfoStrip(label = "BEST OF", value = bestOfCount.toString())
            }
            Box(modifier = Modifier.weight(1f)) {
                AppInfoStrip(label = "FINAL SCORE", value = String.format(Locale.US, "%.2f", finalScore))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Select Best Of Count Section
        AppPanelCard {
            AppSectionTitle(title = "Select Best Of Count")
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..4).forEach { count ->
                    val isSelected = bestOfCount == count
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { bestOfCount = count },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFE8DEF8) else Color.Transparent,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFE8DEF8) else Color.LightGray.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "Best of $count",
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF21005D) else Color.Gray,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enter Marks Section
        AppPanelCard {
            AppSectionTitle(title = "Enter Marks")
            Spacer(modifier = Modifier.height(16.dp))
            
            fun validateAndSet(newValue: String, total: String, setter: (String) -> Unit) {
                val n = newValue.toDoubleOrNull() ?: 0.0
                val t = total.toDoubleOrNull() ?: 0.0
                if (n <= t) setter(newValue)
            }

            fun onTotalChange(newTotal: String, obt: String, obtSetter: (String) -> Unit, totSetter: (String) -> Unit) {
                totSetter(newTotal)
                val n = obt.toDoubleOrNull() ?: 0.0
                val t = newTotal.toDoubleOrNull() ?: 0.0
                if (n > t) {
                    obtSetter(if (newTotal.isEmpty()) "0" else newTotal)
                }
            }

            CtInputRow("CT 1", ct1Obt, { validateAndSet(it, ct1Tot) { ct1Obt = it } }, ct1Tot, { onTotalChange(it, ct1Obt, { ct1Obt = it }, { ct1Tot = it }) })
            Spacer(modifier = Modifier.height(16.dp))
            CtInputRow("CT 2", ct2Obt, { validateAndSet(it, ct2Tot) { ct2Obt = it } }, ct2Tot, { onTotalChange(it, ct2Obt, { ct2Obt = it }, { ct2Tot = it }) })
            Spacer(modifier = Modifier.height(16.dp))
            CtInputRow("CT 3", ct3Obt, { validateAndSet(it, ct3Tot) { ct3Obt = it } }, ct3Tot, { onTotalChange(it, ct3Obt, { ct3Obt = it }, { ct3Tot = it }) })
            Spacer(modifier = Modifier.height(16.dp))
            CtInputRow("CT 4", ct4Obt, { validateAndSet(it, ct4Tot) { ct4Obt = it } }, ct4Tot, { onTotalChange(it, ct4Obt, { ct4Obt = it }, { ct4Tot = it }) })
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        AppPrimaryButton(
            text = "Clear All",
            onClick = {
                ct1Obt = "0"; ct1Tot = "15"
                ct2Obt = "0"; ct2Tot = "15"
                ct3Obt = "0"; ct3Tot = "15"
                ct4Obt = "0"; ct4Tot = "15"
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CtInputRow(
    label: String,
    obtValue: String,
    onObtChange: (String) -> Unit,
    totValue: String,
    onTotChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(50.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        
        OutlinedTextField(
            value = obtValue,
            onValueChange = onObtChange,
            modifier = Modifier.weight(1f),
            label = { Text("Obtained", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        OutlinedTextField(
            value = totValue,
            onValueChange = onTotChange,
            modifier = Modifier.weight(1f),
            label = { Text("Total", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}
