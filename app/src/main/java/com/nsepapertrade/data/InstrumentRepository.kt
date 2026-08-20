package com.nsepapertrade.data

import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.InstrumentType

class InstrumentRepository {

    private val equityInstruments = listOf(
        Instrument("RELIANCE", "Reliance Industries Ltd"),
        Instrument("HDFCBANK", "HDFC Bank Ltd"),
        Instrument("ICICIBANK", "ICICI Bank Ltd"),
        Instrument("INFY", "Infosys Ltd"),
        Instrument("TCS", "Tata Consultancy Services Ltd"),
        Instrument("SBIN", "State Bank of India"),
        Instrument("ITC", "ITC Ltd"),
        Instrument("BHARTIARTL", "Bharti Airtel Ltd"),
        Instrument("LT", "Larsen & Toubro Ltd"),
        Instrument("AXISBANK", "Axis Bank Ltd")
    )

    fun getEquities(): List<Instrument> {
        return equityInstruments
    }

    fun searchEquities(query: String): List<Instrument> {
        val text = query.trim()

        if (text.isEmpty()) {
            return equityInstruments
        }

        return equityInstruments.filter {
            it.symbol.contains(text, ignoreCase = true) ||
            it.name.contains(text, ignoreCase = true)
        }
    }

    fun findBySymbol(symbol: String): Instrument? {
        return equityInstruments.firstOrNull {
            it.symbol.equals(symbol.trim(), ignoreCase = true)
        }
    }

    fun getByType(type: InstrumentType): List<Instrument> {
        return equityInstruments.filter {
            it.type == type
        }
    }
}
