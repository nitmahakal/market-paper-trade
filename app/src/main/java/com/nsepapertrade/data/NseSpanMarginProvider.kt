package com.nsepapertrade.data

class NseSpanMarginProvider(
    private val downloader: NseSpanFileDownloader =
        NseSpanFileDownloader(),
    private val dateResolver: NseSpanDateResolver =
        NseSpanDateResolver(),
    private val parser: NseSpanMarginParser =
        NseSpanMarginParser()
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

                    if (lines.isEmpty()) {
                        continue
                    }

                    val records =
                        parser.parse(lines)

                    if (records.isEmpty()) {
                        continue
                    }

                    val margins =
                        NseSpanMarginMapper()
                            .map(records)

                    if (margins.isNotEmpty()) {
                        return margins
                    }

                } catch (e: Exception) {
                    lastError = e
                }
            }
        }

        throw IllegalStateException(
            lastError?.message
                ?: "No valid NSE SPAN margin records found."
        )
    }
}
