package com.ryxan.recipescaler.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** Quantity display with tabular (fixed-width) numerals so columns of amounts align. */
@Composable
fun QuantityText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.merge(fontFeatureSettings = "tnum"),
    )
}
