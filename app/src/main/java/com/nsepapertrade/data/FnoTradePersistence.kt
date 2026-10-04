package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoPosition
import com.nsepapertrade.model.FnoPositionSide
import com.nsepapertrade.model.FnoTrade
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType
import com.nsepapertrade.model.TradeSide
import org.json.JSONArray
import org.json.JSONObject

class FnoTradePersistence(
    private val storage: PaperTradeStorage
) {

    companion object {
        private const val POSITIONS_KEY =
            "fno_positions"

        private const val TRADES_KEY =
            "fno_trades"
    }

    fun savePositions(
        positions: List<FnoPosition>
    ) {
        val array = JSONArray()

        positions.forEach { position ->
            array.put(
                JSONObject().apply {
                    put(
                        "underlying",
                        position.contract.underlying
                    )
                    put(
                        "underlyingType",
                        position.contract.underlyingType.name
                    )
                    put(
                        "contractType",
                        position.contract.contractType.name
                    )
                    put(
                        "expiry",
                        position.contract.expiry
                    )
                    put(
                        "strikePrice",
                        position.contract.strikePrice
                    )
                    put(
                        "optionType",
                        position.contract.optionType.name
                    )
                    put(
                        "lotSize",
                        position.contract.lotSize
                    )
                    put(
                        "isActive",
                        position.contract.isActive
                    )
                    put(
                        "side",
                        position.side.name
                    )
                    put(
                        "lots",
                        position.lots
                    )
                    put(
                        "averagePrice",
                        position.averagePrice
                    )
                    put(
                        "lastPrice",
                        position.lastPrice
                    )
                }
            )
        }

        storage.putString(
            POSITIONS_KEY,
            array.toString()
        )
    }

    fun loadPositions(): MutableList<FnoPosition> {
        val result =
            mutableListOf<FnoPosition>()

        val raw =
            storage.getString(POSITIONS_KEY)

        if (raw.isBlank()) {
            return result
        }

        try {
            val array = JSONArray(raw)

            for (index in 0 until array.length()) {
                val item =
                    array.getJSONObject(index)

                val contract =
                    readContract(item)

                val side =
                    FnoPositionSide.valueOf(
                        item.optString(
                            "side",
                            FnoPositionSide.LONG.name
                        )
                    )

                result.add(
                    FnoPosition(
                        contract = contract,
                        side = side,
                        lots = item.optInt("lots"),
                        averagePrice =
                            item.optDouble(
                                "averagePrice"
                            ),
                        lastPrice =
                            item.optDouble(
                                "lastPrice"
                            )
                    )
                )
            }
        } catch (_: Exception) {
            return mutableListOf()
        }

        return result
    }

    fun saveTrades(
        trades: List<FnoTrade>
    ) {
        val array = JSONArray()

        trades.forEach { trade ->
            array.put(
                JSONObject().apply {
                    put("id", trade.id)
                    put(
                        "underlying",
                        trade.contract.underlying
                    )
                    put(
                        "underlyingType",
                        trade.contract.underlyingType.name
                    )
                    put(
                        "contractType",
                        trade.contract.contractType.name
                    )
                    put(
                        "expiry",
                        trade.contract.expiry
                    )
                    put(
                        "strikePrice",
                        trade.contract.strikePrice
                    )
                    put(
                        "optionType",
                        trade.contract.optionType.name
                    )
                    put(
                        "lotSize",
                        trade.contract.lotSize
                    )
                    put(
                        "isActive",
                        trade.contract.isActive
                    )
                    put(
                        "side",
                        trade.side.name
                    )
                    put(
                        "positionSide",
                        trade.positionSide.name
                    )
                    put(
                        "lots",
                        trade.lots
                    )
                    put(
                        "price",
                        trade.price
                    )
                    put(
                        "charge",
                        trade.charge
                    )
                    put(
                        "timestamp",
                        trade.timestamp
                    )
                }
            )
        }

        storage.putString(
            TRADES_KEY,
            array.toString()
        )
    }

    fun loadTrades(): MutableList<FnoTrade> {
        val result =
            mutableListOf<FnoTrade>()

        val raw =
            storage.getString(TRADES_KEY)

        if (raw.isBlank()) {
            return result
        }

        try {
            val array = JSONArray(raw)

            for (index in 0 until array.length()) {
                val item =
                    array.getJSONObject(index)

                val side =
                    TradeSide.valueOf(
                        item.optString(
                            "side",
                            TradeSide.BUY.name
                        )
                    )

                val positionSide =
                    FnoPositionSide.valueOf(
                        item.optString(
                            "positionSide",
                            FnoPositionSide.LONG.name
                        )
                    )

                result.add(
                    FnoTrade(
                        id = item.optLong("id"),
                        contract =
                            readContract(item),
                        side = side,
                        positionSide =
                            positionSide,
                        lots =
                            item.optInt("lots"),
                        price =
                            item.optDouble("price"),
                        charge =
                            item.optDouble("charge"),
                        timestamp =
                            item.optLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {
            return mutableListOf()
        }

        return result
    }

    private fun readContract(
        item: JSONObject
    ): FnoContract {

        return FnoContract(
            underlying =
                item.optString("underlying"),

            underlyingType =
                FnoUnderlyingType.valueOf(
                    item.optString(
                        "underlyingType",
                        FnoUnderlyingType.STOCK.name
                    )
                ),

            contractType =
                FnoContractType.valueOf(
                    item.optString(
                        "contractType",
                        FnoContractType.FUTURE.name
                    )
                ),

            expiry =
                item.optString("expiry"),

            strikePrice =
                item.optDouble(
                    "strikePrice",
                    0.0
                ),

            optionType =
                OptionType.valueOf(
                    item.optString(
                        "optionType",
                        OptionType.NONE.name
                    )
                ),

            lotSize =
                item.optInt("lotSize"),

            isActive =
                item.optBoolean(
                    "isActive",
                    true
                )
        )
    }
}
