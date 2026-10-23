package io.github.achmalearning.scootercompanion.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.achmalearning.scootercompanion.domain.ChartGroup
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Chart colours. The six series use the first six slots of the dataviz reference palette in
 * order, validated (scripts/validate_palette.js) against this app's light surface #FFFFFF and
 * dark surface #171A20: every adjacent pair clears the colour-blind and normal-vision
 * separation checks. Three light-mode slots sit below 3:1 on white, so every chart shows its
 * values as text too (tooltips, labels and the tables under the charts).
 */
data class ChartColors(
    val series: List<Color>,
    val grid: Color,
    val axis: Color,
    val ink: Color,
    val muted: Color,
    val surface: Color,
    /** Sequential blue ramp for the heatmap, light to dark. */
    val ramp: List<Color>
) {
    fun of(group: ChartGroup): Color = series[group.ordinal]
}

private val LightColors = ChartColors(
    series = listOf(Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100), Color(0xFFE87BA4), Color(0xFF008300)),
    grid = Color(0xFFE1E0D9), axis = Color(0xFFC3C2B7), ink = Color(0xFF0B0B0B), muted = Color(0xFF52514E), surface = Color(0xFFFFFFFF),
    ramp = listOf(Color(0xFFCDE2FB), Color(0xFF9EC5F4), Color(0xFF6DA7EC), Color(0xFF3987E5), Color(0xFF256ABF), Color(0xFF184F95))
)

private val DarkColors = ChartColors(
    series = listOf(Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500), Color(0xFFD55181), Color(0xFF008300)),
    grid = Color(0xFF2C2C2A), axis = Color(0xFF383835), ink = Color(0xFFF0EFEC), muted = Color(0xFFC3C2B7), surface = Color(0xFF171A20),
    ramp = listOf(Color(0xFF104281), Color(0xFF184F95), Color(0xFF256ABF), Color(0xFF3987E5), Color(0xFF6DA7EC), Color(0xFF9EC5F4))
)

@Composable
fun chartColors(): ChartColors = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) DarkColors else LightColors

/** Axis ticks at 1-2-5 × 10ⁿ steps, like matplotlib's MaxNLocator. */
fun niceTicks(max: Double, count: Int = 4): List<Double> {
    if (max <= 0) return listOf(0.0, 1.0)
    val raw = max / count
    val mag = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= raw }
    val top = ceil(max / step) * step
    return generateSequence(0.0) { it + step }.takeWhile { it <= top + step / 2 }.toList()
}

/**
 * Ticks that fit [min]..[max] without starting at zero, for measures whose variation matters
 * more than their size (price per litre, consumption), like matplotlib's autoscale.
 */
fun fittedTicks(min: Double, max: Double, count: Int = 4): List<Double> {
    if (max <= min) return niceTicks(max, count)
    val raw = (max - min) / count
    val mag = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= raw }
    val lo = floor(min / step) * step
    val hi = ceil(max / step) * step
    return generateSequence(lo) { it + step }.takeWhile { it <= hi + step / 2 }.toList()
}

/** "1 250", "15.5", "1.2k" for axis labels. */
fun compact(v: Double): String = when {
    v >= 10_000 -> "%.0fk".format(java.util.Locale.US, v / 1000)
    v >= 1000 -> "%.1fk".format(java.util.Locale.US, v / 1000).replace(".0k", "k")
    v == floor(v) -> "%.0f".format(java.util.Locale.US, v)
    else -> "%.1f".format(java.util.Locale.US, v)
}

// Plot margins in dp, so the axes keep their room on every screen density.
private val Density.LEFT_AXIS: Float get() = 40.dp.toPx()
private val Density.BOTTOM_AXIS: Float get() = 22.dp.toPx()
private val Density.TOP_PAD: Float get() = 24.dp.toPx()

/** Text centred vertically on [y]; kept inside the canvas horizontally. */
private fun DrawScope.label(tm: TextMeasurer, text: String, x: Float, y: Float, color: Color, fontSize: Float = 10f, bold: Boolean = false, alignEnd: Boolean = false, center: Boolean = false) {
    val layout = tm.measure(text, TextStyle(color = color, fontSize = fontSize.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal))
    val dx = when { alignEnd -> -layout.size.width.toFloat(); center -> -layout.size.width / 2f; else -> 0f }
    val left = (x + dx).coerceIn(0f, max(0f, size.width - layout.size.width))
    drawText(layout, topLeft = Offset(left, y - layout.size.height / 2f))
}

/** Gridlines and y labels; returns the y of a value. */
private fun DrawScope.yAxis(tm: TextMeasurer, ticks: List<Double>, c: ChartColors): (Double) -> Float {
    val top = TOP_PAD
    val bottom = size.height - BOTTOM_AXIS
    val lo = ticks.first()
    val span = (ticks.last() - lo).takeIf { it > 0 } ?: 1.0
    val y = { v: Double -> (bottom - ((v - lo) / span) * (bottom - top)).toFloat() }
    ticks.forEach { t ->
        val yy = y(t)
        drawLine(if (t == lo) c.axis else c.grid, Offset(LEFT_AXIS, yy), Offset(size.width, yy), strokeWidth = 1f)
        label(tm, compact(t), LEFT_AXIS - 6f, yy, c.muted, alignEnd = true)
    }
    return y
}

/** Charts read left to right (time axis) in every language. */
@Composable
private fun Ltr(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)

/**
 * Monthly spending as stacked bars, one colour per group, with the monthly target as a dashed
 * line. Tap a bar to select it; the screen shows that month's breakdown as a table.
 */
@Composable
fun StackedBarChart(
    labels: List<String>,
    stacks: List<List<Pair<ChartGroup, Double>>>,
    target: Double?,
    targetLabel: String,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    height: Dp = 220.dp
) = Ltr {
    val c = chartColors()
    val tm = rememberTextMeasurer()
    val totals = stacks.map { s -> s.sumOf { it.second } }
    val ticks = niceTicks(max(totals.maxOrNull() ?: 0.0, target ?: 0.0))
    Canvas(
        Modifier.fillMaxWidth().height(height).pointerInput(stacks.size) {
            detectTapGestures { pos ->
                val slot = (size.width - LEFT_AXIS) / stacks.size.coerceAtLeast(1)
                val i = ((pos.x - LEFT_AXIS) / slot).toInt()
                onSelect(if (i in stacks.indices && i != selected) i else null)
            }
        }
    ) {
        val y = yAxis(tm, ticks, c)
        val n = stacks.size.coerceAtLeast(1)
        val slot = (size.width - LEFT_AXIS) / n
        val barW = (slot * 0.62f).coerceAtMost(36.dp.toPx())
        val gap = 2.dp.toPx()
        val labelEvery = ceil(n / 7.0).toInt().coerceAtLeast(1)
        stacks.forEachIndexed { i, stack ->
            val x = LEFT_AXIS + slot * i + (slot - barW) / 2
            val dim = selected != null && selected != i
            var acc = 0.0
            val visible = stack.filter { it.second > 0 }
            visible.forEachIndexed { k, (group, value) ->
                val y0 = y(acc)
                val y1 = y(acc + value)
                val top = k == visible.lastIndex
                val h = (y0 - y1 - if (k > 0) gap else 0f).coerceAtLeast(1f)
                val rect = RoundRect(
                    left = x, top = y0 - (if (k > 0) gap else 0f) - h, right = x + barW, bottom = y0 - if (k > 0) gap else 0f,
                    topLeftCornerRadius = if (top) CornerRadius(4.dp.toPx()) else CornerRadius.Zero,
                    topRightCornerRadius = if (top) CornerRadius(4.dp.toPx()) else CornerRadius.Zero
                )
                drawPath(Path().apply { addRoundRect(rect) }, c.of(group).copy(alpha = if (dim) 0.3f else 1f))
                acc += value
            }
            if (i % labelEvery == 0 || i == n - 1) label(tm, labels[i], x + barW / 2, size.height - BOTTOM_AXIS / 2, if (selected == i) c.ink else c.muted, center = true, bold = selected == i)
            if (selected == i && totals[i] > 0) label(tm, compact(totals[i]), x + barW / 2, y(totals[i]) - 10f, c.ink, fontSize = 11f, bold = true, center = true)
        }
        target?.takeIf { it > 0 }?.let { t ->
            val ty = y(t)
            drawLine(c.ink, Offset(LEFT_AXIS, ty), Offset(size.width, ty), strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            label(tm, targetLabel, size.width - 4f, ty - 12f, c.ink, alignEnd = true, bold = true)
        }
    }
}

/**
 * A single series over time, with a crosshair and tooltip that follow a tap or drag. [xs] are
 * days (epoch millis), [format] writes a value for the tooltip and [dateFormat] its date.
 */
@Composable
fun LineChart(
    xs: List<Long>,
    ys: List<Double>,
    color: Color,
    format: (Double) -> String,
    dateFormat: (Long) -> String,
    height: Dp = 180.dp,
    step: Boolean = false,
    showMean: String? = null,
    /** False for measures read for their variation: the axis fits the data instead of starting at 0. */
    zeroBased: Boolean = true
) = Ltr {
    val c = chartColors()
    val tm = rememberTextMeasurer()
    var picked by remember(xs, ys) { mutableStateOf(if (xs.isNotEmpty()) xs.lastIndex else -1) }
    val minX = xs.minOrNull() ?: 0L
    val maxX = (xs.maxOrNull() ?: 1L).let { if (it == minX) minX + 1 else it }
    val ticks = if (zeroBased) niceTicks(ys.maxOrNull() ?: 1.0) else fittedTicks(ys.minOrNull() ?: 0.0, ys.maxOrNull() ?: 1.0)
    fun pick(px: Float, width: Float, left: Float) {
        if (xs.isEmpty()) return
        val t = minX + ((px - left) / (width - left)).coerceIn(0f, 1f) * (maxX - minX)
        picked = xs.indices.minByOrNull { kotlin.math.abs(xs[it] - t) } ?: -1
    }
    Canvas(
        Modifier.fillMaxWidth().height(height)
            .pointerInput(xs) { detectTapGestures { pick(it.x, size.width.toFloat(), LEFT_AXIS) } }
            .pointerInput(xs) { detectDragGestures { change, _ -> pick(change.position.x, size.width.toFloat(), LEFT_AXIS) } }
    ) {
        val y = yAxis(tm, ticks, c)
        // Inset so the end points clear the y labels and the right edge.
        val inset = 10.dp.toPx()
        val x = { d: Long -> LEFT_AXIS + inset + ((d - minX).toFloat() / (maxX - minX)) * (size.width - LEFT_AXIS - 2 * inset) }
        if (xs.isEmpty()) return@Canvas
        // Date labels at both ends of the axis.
        label(tm, dateFormat(minX), LEFT_AXIS, size.height - BOTTOM_AXIS / 2, c.muted)
        label(tm, dateFormat(maxX), size.width - 4f, size.height - BOTTOM_AXIS / 2, c.muted, alignEnd = true)
        showMean?.let { meanLabel ->
            val mean = ys.average()
            val my = y(mean)
            drawLine(c.muted, Offset(LEFT_AXIS, my), Offset(size.width, my), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            // At the start of the line, away from the latest point and its tooltip.
            label(tm, "$meanLabel ${format(mean)}", LEFT_AXIS + 6f, my - 10.dp.toPx(), c.muted)
        }
        val path = Path()
        xs.indices.forEach { i ->
            val px = x(xs[i]); val py = y(ys[i])
            if (i == 0) path.moveTo(px, py)
            else if (step) { path.lineTo(px, y(ys[i - 1])); path.lineTo(px, py) } else path.lineTo(px, py)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (xs.size <= 40) xs.indices.forEach { i ->
            drawCircle(c.surface, 4.dp.toPx(), Offset(x(xs[i]), y(ys[i])))
            drawCircle(color, 3.dp.toPx(), Offset(x(xs[i]), y(ys[i])))
        }
        if (picked in xs.indices) {
            val px = x(xs[picked]); val py = y(ys[picked])
            drawLine(c.axis, Offset(px, TOP_PAD), Offset(px, size.height - BOTTOM_AXIS), strokeWidth = 1f)
            drawCircle(c.surface, 6.dp.toPx(), Offset(px, py))
            drawCircle(color, 5.dp.toPx(), Offset(px, py))
            val text = "${format(ys[picked])} · ${dateFormat(xs[picked])}"
            val layout = tm.measure(text, TextStyle(color = c.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
            val w = layout.size.width + 16f
            val h = layout.size.height + 8f
            val bx = (px - w / 2).coerceIn(LEFT_AXIS, size.width - w)
            drawRoundRect(c.surface, Offset(bx, 0f), Size(w, h), CornerRadius(8f))
            drawRoundRect(c.axis, Offset(bx, 0f), Size(w, h), CornerRadius(8f), style = Stroke(1f))
            drawText(layout, topLeft = Offset(bx + 8f, 4f))
        }
    }
}

/**
 * GitHub-style calendar: one column per week, one row per weekday, darker = more spent that
 * day. Tap a day to see it. [values] maps a day (UTC midnight) to its total.
 */
@Composable
fun CalendarHeatmap(
    startDay: Long,
    weeks: Int,
    values: Map<Long, Double>,
    onPick: (Long) -> Unit,
    picked: Long?
) = Ltr {
    val c = chartColors()
    val max = values.values.maxOrNull() ?: 0.0
    Canvas(
        Modifier.fillMaxWidth().height(110.dp).pointerInput(startDay, weeks) {
            detectTapGestures { pos ->
                val cell = size.width / weeks.toFloat()
                val col = (pos.x / cell).toInt()
                val row = (pos.y / (size.height / 7f)).toInt()
                if (col in 0 until weeks && row in 0..6) onPick(startDay + (col * 7L + row) * 86_400_000L)
            }
        }
    ) {
        val cell = size.width / weeks
        val rowH = size.height / 7f
        val s = minOf(cell, rowH) - 3f
        for (col in 0 until weeks) for (row in 0..6) {
            val day = startDay + (col * 7L + row) * 86_400_000L
            val v = values[day] ?: 0.0
            val color = if (v <= 0 || max <= 0) c.grid else c.ramp[((v / max) * (c.ramp.size - 1)).toInt().coerceIn(0, c.ramp.lastIndex)]
            val o = Offset(col * cell + (cell - s) / 2, row * rowH + (rowH - s) / 2)
            drawRoundRect(color, o, Size(s, s), CornerRadius(3f))
            if (picked == day) drawRoundRect(c.ink, o, Size(s, s), CornerRadius(3f), style = Stroke(2f))
        }
    }
}
