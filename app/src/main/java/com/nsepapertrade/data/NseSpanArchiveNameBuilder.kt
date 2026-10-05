package com.nsepapertrade.data

class NseSpanArchiveNameBuilder {

    fun build(
        date: String,
        reportType: NseSpanReportType
    ): String {

        require(
            date.matches(
                Regex("\\d{8}")
            )
        ) {
            "Date must be in yyyyMMdd format."
        }

        val suffix =
            when (reportType) {
                NseSpanReportType.BEGIN_OF_DAY ->
                    "bod"

                NseSpanReportType.INTRADAY_1 ->
                    "i1"

                NseSpanReportType.INTRADAY_2 ->
                    "i2"

                NseSpanReportType.INTRADAY_3 ->
                    "i3"

                NseSpanReportType.INTRADAY_4 ->
                    "i4"

                NseSpanReportType.END_OF_DAY ->
                    "eod"
            }

        return "nsccl_o.$date.$suffix.zip"
    }
}
