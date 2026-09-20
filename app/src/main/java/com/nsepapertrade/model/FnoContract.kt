package com.nsepapertrade.model

enum class FnoUnderlyingType {
    STOCK,
    INDEX
}

enum class FnoContractType {
    FUTURE,
    OPTION
}

enum class OptionType {
    NONE,
    CE,
    PE
}

data class FnoContract(
    val underlying: String,
    val underlyingType: FnoUnderlyingType,
    val contractType: FnoContractType,
    val expiry: String,
    val strikePrice: Double = 0.0,
    val optionType: OptionType = OptionType.NONE,
    val lotSize: Int
) {
    val symbol: String
        get() = buildString {
            append(underlying.uppercase())
            append("_")
            append(expiry)

            when (contractType) {
                FnoContractType.FUTURE -> append("_FUT")

                FnoContractType.OPTION -> {
                    append("_")
                    append(strikePrice)
                    append("_")
                    append(optionType.name)
                }
            }
        }
}
