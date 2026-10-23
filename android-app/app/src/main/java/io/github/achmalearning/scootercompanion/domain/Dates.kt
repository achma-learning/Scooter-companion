package io.github.achmalearning.scootercompanion.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Owner-entered dates are calendar days stored as UTC-midnight epoch millis — the same
 * representation Material3's DatePicker returns — and exchanged with the website as
 * ISO "yyyy-MM-dd" strings. Always format/parse in UTC so a day never shifts.
 */
object Dates {
    const val DAY_MS = 24L * 60 * 60 * 1000
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /** Today's local calendar date, as UTC midnight. */
    fun today(): Long {
        val local = Calendar.getInstance()
        return utcMidnight(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }

    fun utcMidnight(year: Int, month0: Int, day: Int): Long =
        Calendar.getInstance(UTC).apply { clear(); set(year, month0, day) }.timeInMillis

    /** Normalises any instant (e.g. a legacy System.currentTimeMillis()) to its UTC day. */
    fun startOfDay(millis: Long): Long = Math.floorDiv(millis, DAY_MS) * DAY_MS

    fun addMonths(dayMillis: Long, months: Int): Long =
        Calendar.getInstance(UTC).apply { timeInMillis = dayMillis; add(Calendar.MONTH, months) }.timeInMillis

    fun addDays(dayMillis: Long, days: Int): Long = dayMillis + days * DAY_MS

    fun daysBetween(fromDay: Long, toDay: Long): Int = ((startOfDay(toDay) - startOfDay(fromDay)) / DAY_MS).toInt()

    fun month(dayMillis: Long): Int = Calendar.getInstance(UTC).apply { timeInMillis = dayMillis }.get(Calendar.MONTH) + 1

    fun year(dayMillis: Long): Int = Calendar.getInstance(UTC).apply { timeInMillis = dayMillis }.get(Calendar.YEAR)

    fun dayOfMonth(dayMillis: Long): Int = Calendar.getInstance(UTC).apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_MONTH)

    /** A calendar month as one comparable number: year × 12 + month (0-based). */
    fun monthKey(dayMillis: Long): Int = year(dayMillis) * 12 + month(dayMillis) - 1

    /** First day of the month [key] (see [monthKey]), as UTC midnight. */
    fun monthStart(key: Int): Long = utcMidnight(Math.floorDiv(key, 12), Math.floorMod(key, 12), 1)

    fun daysInMonth(key: Int): Int = daysBetween(monthStart(key), monthStart(key + 1))

    /** "2026-10" for the month [key]. */
    fun monthIso(key: Int): String = "%04d-%02d".format(Locale.US, Math.floorDiv(key, 12), Math.floorMod(key, 12) + 1)

    fun toIso(dayMillis: Long): String = formatter("yyyy-MM-dd").format(dayMillis)

    fun fromIso(iso: String?): Long? = iso?.takeIf { it.length >= 10 }?.let {
        runCatching { formatter("yyyy-MM-dd").parse(it.substring(0, 10))?.time }.getOrNull()
    }

    fun display(dayMillis: Long): String = formatter("d MMM yyyy").format(dayMillis)

    fun short(dayMillis: Long): String = formatter("dd/MM/yy").format(dayMillis)

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.UK).apply { timeZone = UTC }
}
