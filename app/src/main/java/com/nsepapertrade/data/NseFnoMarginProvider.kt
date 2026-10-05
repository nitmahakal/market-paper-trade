package com.nsepapertrade.data

class NseFnoMarginProvider : FnoMarginProvider {

    override suspend fun fetchMargins():
        List<FnoContractMargin> {

        throw IllegalStateException(
            "NSE F&O margin data source is not configured yet."
        )
    }
}
