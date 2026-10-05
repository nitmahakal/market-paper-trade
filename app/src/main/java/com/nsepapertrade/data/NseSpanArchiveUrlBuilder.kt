package com.nsepapertrade.data

class NseSpanArchiveUrlBuilder {

    companion object {
        private const val NSE_ARCHIVE_BASE =
            "https://archives.nseindia.com/content/fo/"
    }

    fun build(
        date: String,
        reportType: NseSpanReportType
    ): String {

        val fileName =
            NseSpanArchiveNameBuilder().build(
                date = date,
                reportType = reportType
            )

        return NSE_ARCHIVE_BASE + fileName
    }
}
