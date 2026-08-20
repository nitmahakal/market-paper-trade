package com.nsepapertrade.model

data class Instrument(
    val symbol: String,
    val name: String,
    val type: InstrumentType = InstrumentType.EQUITY,
    val exchange: String = "NSE"
)
