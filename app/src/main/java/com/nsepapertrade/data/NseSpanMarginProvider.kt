package com.nsepapertrade.data

class NseSpanMarginProvider(
    private val downloader: NseSpanFileDownloader =
        NseSpanFileDownloader(),
    private val dateResolver: NseSpanDateResolver =
        NseSpanDateResolver()
) : FnoMarginProvider {

    override suspend fun fetchMargins():
        List<FnoContractMargin> {

        var lastError: Exception? = null

        for (
            date in dateResolver.getRecentDates(7)
        ) {

            val reportTypes = listOf(
                NseSpanReportType.BEGIN_OF_DAY,
                NseSpanReportType.END_OF_DAY
            )

            for (reportType in reportTypes) {

                try {

                    val lines =
                        downloader.download(
                            NseSpanReportRequest(
                                date = date,
                                reportType = reportType
                            )
                        )

                    if (lines.isNotEmpty()) {
                        throw IllegalStateException(
                            "NSE SPAN file downloaded successfully, " +
                                "but its exact record format still needs " +
                                "to be parsed before margin values can be used."
                        )
                    }

                } catch (e: Exception) {
                    lastError = e
                }
            }
        }

        throw IllegalStateException(
            lastError?.message
                ?: "No NSE SPAN file could be downloaded."
        )
    }
}
