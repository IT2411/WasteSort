package org.wastesort.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wastesort.model.DisposalRecommendation
import org.wastesort.model.WasteStream

@Composable
fun ResultOverlay(
    recommendation: DisposalRecommendation,
    onRetake: () -> Unit,
    onManualFallback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streamColor = when (recommendation.stream) {
        WasteStream.WET -> Color(0xFF2E7D32)
        WasteStream.DRY -> Color(0xFF1565C0)
        WasteStream.OTHER -> Color(0xFFD84315)
        WasteStream.NOT_SURE -> Color(0xFF616161)
    }

    // Surface anchors to the screen edge and draws the dark background all the way down
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF1E1E1E),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Navigation padding inside: keeps buttons above gesture pill, but sheet touches bottom
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Category Badge
            Box(
                modifier = Modifier
                    .background(streamColor, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = recommendation.stream.displayName.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Identified Item
            Text(
                text = recommendation.identifiedItem,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Plain-Language Guidance
            Text(
                text = recommendation.instruction,
                color = Color(0xFFE0E0E0),
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onManualFallback,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Manual Select")
                }

                Button(
                    onClick = onRetake,
                    colors = ButtonDefaults.buttonColors(containerColor = streamColor),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Scan Again")
                }
            }
        }
    }
}
