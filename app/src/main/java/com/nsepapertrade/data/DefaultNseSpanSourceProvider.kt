package com.nsepapertrade.data

class DefaultNseSpanSourceProvider :
    NseSpanSourceProvider {

    override fun getSources():
        List<NseSpanSource> {

        return listOf(
            NseSpanSource(
                name = "NSE F&O SPAN BOD",
                url = "",
                isActive = false
            ),
            NseSpanSource(
                name = "NSE F&O SPAN EOD",
                url = "",
                isActive = false
            )
        )
    }
}
