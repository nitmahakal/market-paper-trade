package com.nsepapertrade.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NseSpanDateResolver {

    fun getRecentDates(
        days: Int = 7
    ): List<String> {

        require(days > 0) {
            "Days must be greater than zero."
        }

        val formatter =
            SimpleDateFormat(
                "yyyyMMdd",
                Locale.US
            )

        val calendar =
            Calendar.getInstance()

        return buildList {

            repeat(days) {

                add(
                    formatter.format(
                        calendar.time
                    )
                )

                calendar.add(
                    Calendar.DAY_OF_MONTH,
                    -1
                )
            }
        }.distinct()
    }
}
