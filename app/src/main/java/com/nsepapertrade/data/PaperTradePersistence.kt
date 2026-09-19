package com.nsepapertrade.data

import com.nsepapertrade.model.InstrumentType
import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade
import com.nsepapertrade.model.TradeSide
import org.json.JSONArray
import org.json.JSONObject

class PaperTradePersistence(
    private val storage: PaperTradeStorage
) {

    companion object {
        private const val POSITIONS_KEY = "positions"
        private const val TRADES_KEY = "trades"
    }

    fun savePositions(
        positions: List<Position>
    ) {
        val array = JSONArray()

        positions.forEach { position ->
            array.put(
                JSONObject().apply {
                    put("symbol", position.symbol)
                    put("quantity", position.quantity)
                    put("averagePrice", position.averagePrice)
                    put("instrumentType", position.instrumentType.name)
                    put("lastPrice", position.lastPrice)
                }
            )
        }

        storage.putString(
            POSITIONS_KEY,
            array.toString()
        )
    }

    fun loadPositions(): MutableList<Position> {
        val result = mutableListOf<Position>()

        val raw = storage.getString(POSITIONS_KEY)

        if (raw.isBlank()) {
            return result
        }

        try {
            val array = JSONArray(raw)

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                val instrumentType =
                    try {
                        InstrumentType.valueOf(
                            item.optString(
                                "instrumentType",
                                InstrumentType.EQUITY.name
                            )
                        )
                    } catch (_: Exception) {
                        InstrumentType.EQUITY
                    }

                result.add(
                    Position(
                        symbol = item.optString("symbol"),
                        quantity = item.optInt("quantity"),
                        averagePrice = item.optDouble("averagePrice"),
                        instrumentType = instrumentType,
                        lastPrice = item.optDouble("lastPrice")
                    )
                )
            }
        } catch (_: Exception) {
            return mutableListOf()
        }

        return result
    }

    fun saveTrades(
        trades: List<Trade>
    ) {
        val array = JSONArray()

        trades.forEach { trade ->
            array.put(
                JSONObject().apply {
                    put("id", trade.id)
                    put("symbol", trade.symbol)
                    put("side", trade.side.name)
                    put("quantity", trade.quantity)
                    put("price", trade.price)
                    put("charge", trade.charge)
                    put("instrumentType", trade.instrumentType.name)
                    put("timestamp", trade.timestamp)
                }
            )
        }

        storage.putString(
            TRADES_KEY,
            array.toString()
        )
    }

    fun loadTrades(): MutableList<Trade> {
        val result = mutableListOf<Trade>()

        val raw = storage.getString(TRADES_KEY)

        if (raw.isBlank()) {
            return result
        }

        try {
            val array = JSONArray(raw)

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                val side =
                    try {
                        TradeSide.valueOf(
                            item.optString(
                                "side",
                                TradeSide.BUY.name
                            )
                        )
                    } catch (_: Exception) {
                        TradeSide.BUY
                    }

                val instrumentType =
                    try {
                        InstrumentType.valueOf(
                            item.optString(
                                "instrumentType",
                                InstrumentType.EQUITY.name
                            )
                        )
                    } catch (_: Exception) {
                        InstrumentType.EQUITY
                    }

                result.add(
                    Trade(
                        id = item.optLong("id"),
                        symbol = item.optString("symbol"),
                        side = side,
                        quantity = item.optInt("quantity"),
                        price = item.optDouble("price"),
                        charge = item.optDouble("charge"),
                        instrumentType = instrumentType,
                        timestamp = item.optLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {
            return mutableListOf()
        }

        return result
    }
}
