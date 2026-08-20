package com.nsepapertrade.data

import com.nsepapertrade.model.InstrumentType

object InstrumentRules {

    fun allowsShortSelling(
        instrumentType: InstrumentType
    ): Boolean {
        return when (instrumentType) {
            InstrumentType.EQUITY -> false
            InstrumentType.FUTURE -> true
            InstrumentType.OPTION -> true
        }
    }

    fun shortSellWarning(
        instrumentType: InstrumentType
    ): String? {
        return if (allowsShortSelling(instrumentType)) {
            null
        } else {
            "Short selling is not allowed in NSE Equity Paper Trade."
        }
    }
}
