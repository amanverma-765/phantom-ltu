package com.navi.phantom.features.places.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navi.phantom.core.ui.CoordinateChip
import com.navi.phantom.core.ui.SelectionIndicator
import com.navi.phantom.domain.model.Place

@Composable
internal fun SelectablePlaceCard(
    place: Place,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
            val iconBackgroundBrush = remember(primaryColor, primaryContainerColor) {
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.15f),
                        primaryContainerColor
                    )
                )
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = primaryContainerColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = iconBackgroundBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoordinateChip(text = "%.4f".format(place.latitude))
                    Text(
                        text = ",",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    CoordinateChip(text = "%.4f".format(place.longitude))
                    if (place.accuracy != null) {
                        CoordinateChip(text = "\u00B1${place.accuracy.toInt()}m")
                    }
                }

                place.address?.let { address ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                SelectionIndicator()
            }
        }
    }
}
