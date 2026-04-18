package com.bakertelekom.portugaltowers.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bakertelekom.portugaltowers.R
import com.bakertelekom.portugaltowers.domain.Operator

@DrawableRes
fun Operator.logoRes(): Int? = when (this) {
    Operator.Meo -> R.drawable.logo_meo
    Operator.Nos -> R.drawable.logo_nos
    Operator.Vodafone -> R.drawable.logo_vodafone
    Operator.Digi -> R.drawable.logo_digi
    Operator.Unknown -> null
}

fun Operator.composeColor(): Color = Color(brandColor)

@Composable
fun OperatorGrid(
    operators: Set<Operator>,
    modifier: Modifier = Modifier,
) {
    val cells = listOf(Operator.Meo, Operator.Nos, Operator.Vodafone, Operator.Digi)
    Column(
        modifier = modifier.size(56.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { operator ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.White, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (operator in operators) {
                            val logo = operator.logoRes()
                            if (logo != null) {
                                Image(
                                    painter = painterResource(logo),
                                    contentDescription = operator.displayName,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(operator.displayName.take(1))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperatorChips(
    operators: Set<Operator>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        operators.take(4).forEach { operator ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(operator.displayName) },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = operator.composeColor(),
                    disabledLabelColor = Color.White,
                ),
            )
        }
    }
}
