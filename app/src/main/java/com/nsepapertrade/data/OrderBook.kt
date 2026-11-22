package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoPositionSide
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType
import com.nsepapertrade.model.TradeSide
import org.json.JSONArray
import org.json.JSONObject

enum class OrderKind {
    EQUITY,
    FNO
}

// An order placed while the market was closed.
// quantity = shares for EQUITY, lots for FNO.
data class QueuedOrder(
    val id: Long,
    val kind: OrderKind,
    val symbol: String,
    val contract: FnoContract?,
    val side: TradeSide,
    val quantity: Int,
    val createdAt: Long
)

enum class ExitType {
    TARGET,
    STOP_LOSS
}

// A target or stop-loss for part (or all) of an F&O position.
data class ExitOrder(
    val id: Long,
    val contract: FnoContract,
    val positionSide: FnoPositionSide,
    val type: ExitType,
    val lots: Int,
    val triggerPrice: Double,
    val createdAt: Long
)

// Keeps queued orders (per kind) and F&O exit orders,
// in memory and on the phone. Use from the main thread only.
class OrderBook(
    context: Context,
    private val kind: OrderKind
) {

    companion object {
        private const val EXIT_KEY = "fno_exit_orders"
    }

    private val storage = PaperTradeStorage(context)

    private val queuedKey =
        "queued_orders_" + kind.name.lowercase()

    private val queued = mutableListOf<QueuedOrder>()
    private val exits = mutableListOf<ExitOrder>()

    private var lastId = 0L

    init {
        queued.addAll(loadQueued())

        if (kind == OrderKind.FNO) {
            exits.addAll(loadExits())
        }

        lastId = maxOf(
            queued.maxOfOrNull { it.id } ?: 0L,
            exits.maxOfOrNull { it.id } ?: 0L
        )
    }

    fun nextId(): Long {
        lastId = maxOf(lastId + 1, System.currentTimeMillis())
        return lastId
    }

    // -----------------------------------------------------
    // Queued orders
    // -----------------------------------------------------

    fun getQueuedOrders(): List<QueuedOrder> =
        queued.sortedBy { it.createdAt }

    fun addQueuedOrder(order: QueuedOrder) {
        queued.add(order)
        saveQueued()
    }

    fun removeQueuedOrder(id: Long) {
        if (queued.removeAll { it.id == id }) {
            saveQueued()
        }
    }

    // -----------------------------------------------------
    // Exit orders (F&O only)
    // -----------------------------------------------------

    fun getExitOrders(): List<ExitOrder> =
        exits.sortedBy { it.createdAt }

    fun addExitOrder(order: ExitOrder) {
        exits.add(order)
        saveExits()
    }

    fun removeExitOrder(id: Long) {
        if (exits.removeAll { it.id == id }) {
            saveExits()
        }
    }

    fun replaceExitOrders(orders: List<ExitOrder>) {
        exits.clear()
        exits.addAll(orders)
        saveExits()
    }

    // -----------------------------------------------------
    // Save / load
    // -----------------------------------------------------

    private fun saveQueued() {
        val array = JSONArray()

        queued.forEach { order ->
            val json = JSONObject()
                .put("id", order.id)
                .put("kind", order.kind.name)
                .put("symbol", order.symbol)
                .put("side", order.side.name)
                .put("quantity", order.quantity)
                .put("createdAt", order.createdAt)

            if (order.contract != null) {
                json.put("contract", contractToJson(order.contract))
            }

            array.put(json)
        }

        storage.putString(queuedKey, array.toString())
    }

    private fun loadQueued(): List<QueuedOrder> {
        val text = storage.getString(queuedKey, "")

        if (text.isBlank()) {
            return emptyList()
        }

        val result = mutableListOf<QueuedOrder>()

        try {
            val array = JSONArray(text)

            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue

                try {
                    result.add(
                        QueuedOrder(
                            id = json.getLong("id"),
                            kind = OrderKind.valueOf(
                                json.getString("kind")
                            ),
                            symbol = json.getString("symbol"),
                            contract = json
                                .optJSONObject("contract")
                                ?.let { contractFromJson(it) },
                            side = TradeSide.valueOf(
                                json.getString("side")
                            ),
                            quantity = json.getInt("quantity"),
                            createdAt = json.getLong("createdAt")
                        )
                    )
                } catch (e: Exception) {
                    // skip a damaged entry
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return result.filter { it.kind == kind }
    }

    private fun saveExits() {
        val array = JSONArray()

        exits.forEach { order ->
            array.put(
                JSONObject()
                    .put("id", order.id)
                    .put("contract", contractToJson(order.contract))
                    .put("positionSide", order.positionSide.name)
                    .put("type", order.type.name)
                    .put("lots", order.lots)
                    .put("triggerPrice", order.triggerPrice)
                    .put("createdAt", order.createdAt)
            )
        }

        storage.putString(EXIT_KEY, array.toString())
    }

    private fun loadExits(): List<ExitOrder> {
        val text = storage.getString(EXIT_KEY, "")

        if (text.isBlank()) {
            return emptyList()
        }

        val result = mutableListOf<ExitOrder>()

        try {
            val array = JSONArray(text)

            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue

                try {
                    val contract = contractFromJson(
                        json.getJSONObject("contract")
                    ) ?: continue

                    result.add(
                        ExitOrder(
                            id = json.getLong("id"),
                            contract = contract,
                            positionSide = FnoPositionSide.valueOf(
                                json.getString("positionSide")
                            ),
                            type = ExitType.valueOf(
                                json.getString("type")
                            ),
                            lots = json.getInt("lots"),
                            triggerPrice =
                                json.getDouble("triggerPrice"),
                            createdAt = json.getLong("createdAt")
                        )
                    )
                } catch (e: Exception) {
                    // skip a damaged entry
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return result
    }

    private fun contractToJson(
        contract: FnoContract
    ): JSONObject {
        return JSONObject()
            .put("underlying", contract.underlying)
            .put("underlyingType", contract.underlyingType.name)
            .put("contractType", contract.contractType.name)
            .put("expiry", contract.expiry)
            .put("strikePrice", contract.strikePrice)
            .put("optionType", contract.optionType.name)
            .put("lotSize", contract.lotSize)
            .put("isActive", contract.isActive)
    }

    private fun contractFromJson(
        json: JSONObject
    ): FnoContract? {
        return try {
            FnoContract(
                underlying = json.getString("underlying"),
                underlyingType = FnoUnderlyingType.valueOf(
                    json.getString("underlyingType")
                ),
                contractType = FnoContractType.valueOf(
                    json.getString("contractType")
                ),
                expiry = json.getString("expiry"),
                strikePrice = json.optDouble("strikePrice", 0.0),
                optionType = OptionType.valueOf(
                    json.optString("optionType", "NONE")
                ),
                lotSize = json.getInt("lotSize"),
                isActive = json.optBoolean("isActive", true)
            )
        } catch (e: Exception) {
            null
        }
    }
}
