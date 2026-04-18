package com.bakertelekom.portugaltowers.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bakertelekom.portugaltowers.R
import com.bakertelekom.portugaltowers.domain.Tower
import com.bakertelekom.portugaltowers.domain.formatDistance
import java.util.Locale

@Composable
fun TowerDetailDialog(
    tower: Tower,
    distanceMeters: Double?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Torre ${tower.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tower.address, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "%.6f, %.6f".format(Locale.US, tower.latitude, tower.longitude),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                distanceMeters?.let {
                    Text("Distancia: ${formatDistance(it)}")
                }
                OperatorChips(tower.operators)
                if (tower.bands4g.isNotEmpty()) {
                    Text("4G: ${tower.bands4g.joinToString(", ")}")
                }
                if (tower.bands5g.isNotEmpty()) {
                    Text("5G: ${tower.bands5g.joinToString(", ")}")
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        val uri = Uri.parse("geo:${tower.latitude},${tower.longitude}?q=${tower.latitude},${tower.longitude}(Portugal Towers)")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                ) {
                    Text(stringResource(R.string.open_maps))
                }
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        },
    )
}
