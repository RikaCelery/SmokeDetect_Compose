package org.celery.smokedetect_compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OptionSlider(
    hint: String,
    value: Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f,
    default: Float=0f,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(hint)
        Spacer(modifier = Modifier.width(5.dp))
        Slider(value, onValueChange, valueRange = valueRange, modifier = Modifier.weight(1f))
        Button(onClick = { onValueChange(default) }) {
            Icon(Icons.Rounded.Undo, null)
        }
    }
    Divider(modifier)
}