package com.nsepapertrade.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MarketClock {

    // Refresh intervals
    const val LIVE_INTERVAL_MS = 10_000L
    const val QUIET_INTERVAL_MS = 60_000L
    const val AFTER_HOURS_INTERVAL_MS = 5 * 60_000L
    const val WEEKEND_INTERVAL_MS = 15 * 60_000L

    // Feed lag above this is not treated as a live-but-delayed feed
    const val MAX_FEED_DELAY_MS = 20 * 60_000L

    // Until measured, assume the feed may lag by this much
    const val DEFAULT_FEED_DELAY_MS = 15 * 60_000L

    private const val OPEN_MS = (9 * 60 + 15) * 60_000L
    private const val CLOSE_MS = (15 * 60 + 30) * 60_000L
    private const val DAY_MS = 24 * 3_600_000L

    private val IST: TimeZone =
        TimeZone.getTimeZone("Asia/Kolkata")

    private fun istCalendar(nowMs: Long): Calendar {
        val calendar = Calendar.getInstance(IST)
        calendar.timeInMillis = nowMs
        return calendar
    }

    fun msOfDay(
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val c = istCalendar(nowMs)

        return c.get(Calendar.HOUR_OF_DAY) * 3_600_000L +
            c.get(Calendar.MINUTE) * 60_000L +
            c.get(Calendar.SECOND) * 1_000L +
            c.get(Calendar.MILLISECOND)
    }

    fun isWeekend(
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val day = istCalendar(nowMs).get(Calendar.DAY_OF_WEEK)

        return day == Calendar.SATURDAY ||
            day == Calendar.SUNDAY
    }

    // Mon-Fri, 09:15 to 15:30 IST.
    // (Weekday exchange holidays are not known here.)
    fun isOpen(
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return !isWeekend(nowMs) &&
            msOfDay(nowMs) in OPEN_MS until CLOSE_MS
    }

    // Today's 09:15 IST as epoch milliseconds.
    fun todaySessionStartMs(
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        return nowMs - msOfDay(nowMs) + OPEN_MS
    }

    // True if this quote time belongs to today's trading session.
    // A weekday holiday has no such quotes, so this also detects it.
    fun isFromCurrentSession(
        quoteTimeMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return !isWeekend(nowMs) &&
            msOfDay(nowMs) >= OPEN_MS &&
            quoteTimeMs >= todaySessionStartMs(nowMs)
    }

    fun nextOpenMs(
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        var candidate = todaySessionStartMs(nowMs)

        if (isWeekend(candidate) || candidate <= nowMs) {
            do {
                candidate += DAY_MS
            } while (isWeekend(candidate))
        }

        return candidate
    }

    fun nextOpenLabel(
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val format = SimpleDateFormat(
            "EEE d MMM, HH:mm",
            Locale.US
        )

        format.timeZone = IST

        return format.format(Date(nextOpenMs(nowMs))) + " IST"
    }

    // Warning shown near BUY / SELL while the market is closed.
    fun closedNotice(
        nowMs: Long = System.currentTimeMillis()
    ): String {
        return "Market is closed. Your order will be queued and " +
            "executed at the market open price " +
            "(next open: ${nextOpenLabel(nowMs)})."
    }

    // How long to wait before the next price refresh.
    // Market hours (and the feed's own delay after close): fast.
    // Otherwise: slow. Before the open: wake up just in time.
    fun refreshDelayMs(
        feedDelayMs: Long,
        freshestLagMs: Long?,
        nowMs: Long = System.currentTimeMillis()
    ): Long {

        if (isWeekend(nowMs)) {
            return WEEKEND_INTERVAL_MS
        }

        val now = msOfDay(nowMs)

        if (now < OPEN_MS) {
            return (OPEN_MS - now).coerceIn(
                LIVE_INTERVAL_MS,
                AFTER_HOURS_INTERVAL_MS
            )
        }

        val windowEnd = CLOSE_MS + feedDelayMs + 60_000L

        if (now < windowEnd) {

            val feedIsFresh =
                freshestLagMs != null &&
                    freshestLagMs <= MAX_FEED_DELAY_MS

            val justOpened =
                now - OPEN_MS < MAX_FEED_DELAY_MS

            return if (feedIsFresh || justOpened) {
                LIVE_INTERVAL_MS
            } else {
                QUIET_INTERVAL_MS
            }
        }

        return AFTER_HOURS_INTERVAL_MS
    }
}
