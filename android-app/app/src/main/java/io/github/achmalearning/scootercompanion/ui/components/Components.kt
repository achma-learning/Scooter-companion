package io.github.achmalearning.scootercompanion.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.DueStatus
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors
import kotlin.math.min

@Composable
fun statusColor(status: DueStatus): Color = when (status) {
    DueStatus.OVERDUE -> LocalStatusColors.current.due
    DueStatus.SOON -> LocalStatusColors.current.soon
    DueStatus.OK -> LocalStatusColors.current.ok
}

fun statusLabel(status: DueStatus) = when (status) {
    DueStatus.OVERDUE -> "Due"
    DueStatus.SOON -> "Soon"
    DueStatus.OK -> "OK"
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        modifier = modifier.fillMaxWidth().height(6.dp)
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Rounded surface card used for most list rows. */
@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val shape = RoundedCornerShape(18.dp)
    val border = CardDefaults.outlinedCardBorder()
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) { content() }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) { content() }
    }
}

@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String? = null, tint: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Panel(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.onSurface) {
    Panel(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Callout(title: String, text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(color = color.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
        actions = { actions() }
    )
}

val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)

/** Health ring on the dashboard: 0-100. */
@Composable
fun HealthRing(score: Int, modifier: Modifier = Modifier, size: Dp = 84.dp) {
    val colors = LocalStatusColors.current
    val color = when { score >= 80 -> colors.ok; score >= 50 -> colors.soon; else -> colors.due }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.11f
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * score / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text("health", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Stylised Agility side view — same path data as web/img/scooter.svg. */
@Composable
fun ScooterArt(modifier: Modifier = Modifier, body: Color = MaterialTheme.colorScheme.primary) {
    val paths = remember {
        object {
            val rear = PathParser().parsePathString("M16 90 C16 72 32 62 54 60 L122 58 L128 72 L124 96 L90 96 C86 84 76 78 61 78 C46 78 36 86 33 96 L20 96 C17 95 16 93 16 90 Z").toPath()
            val front = PathParser().parsePathString("M120 96 L123 88 L146 88 C152 70 156 50 158 36 L161 24 L172 22 C175 36 179 52 187 67 C196 77 207 85 208 95 C208 98 206 100 204 98 C200 91 194 88 186 88 C176 87 167 93 164 101 L148 101 L144 96 Z").toPath()
            val seat = PathParser().parsePathString("M46 60 C48 48 62 43 84 43 L116 45 C125 46 127 55 120 58 L50 61 Z").toPath()
            val bars = PathParser().parsePathString("M148 24 L178 15 M163 19 L158 7").toPath()
            val light = PathParser().parsePathString("M171 22 C178 22 185 27 185 33 L180 37 C176 33 172 31 170 30 Z").toPath()
            val exhaust = PathParser().parsePathString("M66 106 L112 102 C116 102 118 105 118 108 L117 111 L68 114 C64 114 62 108 66 106 Z").toPath()
        }
    }
    val tyre = Color(0xFF1D1F25)
    val metal = Color(0xFF9AA0AB)
    Canvas(modifier) {
        val vbX = 6f; val vbW = 214f; val vbH = 140f
        val s = min(size.width / vbW, size.height / vbH)
        val dx = (size.width - vbW * s) / 2 - vbX * s
        val dy = (size.height - vbH * s) / 2
        translate(dx, dy) {
            scale(s, s, pivot = Offset.Zero) {
                drawPath(paths.rear, body)
                drawPath(paths.front, body)
                drawPath(paths.seat, Color(0xFF2A2D35))
                drawPath(paths.bars, Color(0xFFC9CCD3), style = Stroke(3f, cap = StrokeCap.Round))
                drawOval(Color(0xFFC9CCD3), Offset(150f, 2f), androidx.compose.ui.geometry.Size(12f, 6f))
                drawPath(paths.light, Color(0xFFFFF4C2))
                drawRoundRect(Color(0xFFFF8A80), Offset(13f, 82f), androidx.compose.ui.geometry.Size(6f, 6f), androidx.compose.ui.geometry.CornerRadius(2f))
                drawPath(paths.exhaust, Color(0xFF3A3E48))
                for (cx in listOf(61f, 186f)) {
                    drawCircle(tyre, 20f, Offset(cx, 116f), style = Stroke(9f))
                    drawCircle(metal, 11f, Offset(cx, 116f), style = Stroke(3f))
                    drawCircle(metal, 3.5f, Offset(cx, 116f))
                }
            }
        }
    }
}

@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, decimal: Boolean = false, suffix: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.replace(',', '.')) },
        label = { Text(label) },
        singleLine = true,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}

/** Read-only field that opens a Material date picker; [value] is UTC-midnight millis. */
@Composable
fun DateField(value: Long?, onChange: (Long?) -> Unit, label: String, modifier: Modifier = Modifier, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.let(Dates::display) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value ?: Dates.today())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(state.selectedDateMillis); open = false }) { Text("OK") } },
            dismissButton = {
                Row {
                    if (clearable) TextButton(onClick = { onChange(null); open = false }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, MaterialTheme.colorScheme.primary, 56.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

fun formatKm(km: Int): String = "%,d".format(km).replace(',', ' ') + " km"

fun formatMad(v: Double): String = if (v >= 100) "%,.0f MAD".format(v).replace(',', ' ') else "%.2f MAD".format(v)

fun Context.openUrl(url: String) = runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

fun Context.dial(number: String) = runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

/** Human "due" line, e.g. "in 420 km · 3 weeks" or "1,200 km overdue". */
fun dueText(kmLeft: Int?, daysLeft: Int?): String {
    val parts = mutableListOf<String>()
    if (kmLeft != null) parts += if (kmLeft <= 0) "${formatKm(-kmLeft)} overdue" else "in ${formatKm(kmLeft)}"
    if (daysLeft != null) parts += when {
        daysLeft < 0 -> "${-daysLeft} d overdue"
        daysLeft == 0 -> "today"
        daysLeft < 14 -> "in $daysLeft d"
        daysLeft < 60 -> "in ${daysLeft / 7} wk"
        else -> "in ${daysLeft / 30} mo"
    }
    return parts.joinToString(" · ").ifEmpty { "Not logged yet — do it once to start the clock" }
}
