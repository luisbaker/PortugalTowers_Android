package com.bakertelekom.portugaltowers.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bakertelekom.portugaltowers.R
import com.bakertelekom.portugaltowers.domain.Tower
import com.bakertelekom.portugaltowers.domain.formatDistance
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TowerDetailDialog(
    tower: Tower,
    distanceMeters: Double?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coords = "%.5f, %.5f".format(Locale.US, tower.latitude, tower.longitude)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OperatorGrid(tower.operators, modifier = Modifier.size(56.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Torre ${tower.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(tower.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            InfoBlock(title = "Localizacao", icon = Icons.Default.Place) {
                InfoLine(
                    label = "Morada",
                    value = tower.address,
                    onCopy = { copyText(context, "Morada", tower.address) },
                )
                Spacer(Modifier.height(8.dp))
                InfoLine(
                    label = "GPS",
                    value = coords,
                    onCopy = { copyText(context, "Coordenadas GPS", coords) },
                )
                distanceMeters?.let {
                    Spacer(Modifier.height(8.dp))
                    InfoLine(label = "Distancia", value = formatDistance(it))
                }
            }

            InfoBlock(title = "Operadoras", icon = Icons.Default.SignalCellularAlt) {
                OperatorChips(tower.operators)
            }

            if (tower.bands4g.isNotEmpty() || tower.bands5g.isNotEmpty()) {
                InfoBlock(title = "Frequencias", icon = Icons.Default.SignalCellularAlt) {
                    if (tower.bands4g.isNotEmpty()) InfoLine("4G", tower.bands4g.joinToString(", "))
                    if (tower.bands4g.isNotEmpty() && tower.bands5g.isNotEmpty()) Spacer(Modifier.height(8.dp))
                    if (tower.bands5g.isNotEmpty()) InfoLine("5G", tower.bands5g.joinToString(", "))
                }
            }

            Button(
                onClick = {
                    val uri = Uri.parse("geo:${tower.latitude},${tower.longitude}?q=${tower.latitude},${tower.longitude}(Portugal Towers)")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.open_maps), fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Fechar")
            }
        }
    }
}

@Composable
private fun InfoBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        if (onCopy != null) {
            TextButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun copyText(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
}
