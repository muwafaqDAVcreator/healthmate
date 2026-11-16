package com.healthmate.app.features.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.utils.AppDates
import com.healthmate.app.core.utils.formatted
import com.healthmate.app.features.records.data.HealthRecord

/** One record in the list: the date and the three values in their colours. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordTile(record: HealthRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateBadge(record)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    AppDates.shortDate(record.date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HealthMetric.entries.forEach { metric ->
                        MetricValue(metric, metric.valueIn(record))
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Day number and month in a teal rounded square, like a calendar page. */
@Composable
private fun DateBadge(record: HealthRecord) {
    Column(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            record.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            AppDates.monthShort(record.date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun MetricValue(metric: HealthMetric, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(metric.icon, contentDescription = null, tint = metric.color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            "${value.formatted()} ${metric.unit}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
