package org.wastesort.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.wastesort.model.ManualWasteCatalog
import org.wastesort.model.ManualWasteOption
import org.wastesort.model.WasteStream

@Composable
fun ManualSelectionDialog(
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
            ) {
                Text(
                    text = "Select Waste Item",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Choose the closest match for guidance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA),
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF333333))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ManualWasteCatalog.items) { option ->
                        ManualOptionRow(option = option, onClick = { onItemSelected(option.classId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualOptionRow(
    option: ManualWasteOption,
    onClick: () -> Unit,
) {
    val badgeColor = when (option.expectedStream) {
        WasteStream.WET -> Color(0xFF2E7D32)
        WasteStream.DRY -> Color(0xFF1565C0)
        WasteStream.OTHER -> Color(0xFFD84315)
        WasteStream.NOT_SURE -> Color(0xFF757575)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFF282828), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(
                text = option.examples,
                color = Color(0xFFB0B0B0),
                fontSize = 12.sp,
            )
        }

        Box(
            modifier = Modifier
                .background(badgeColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = option.expectedStream.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
