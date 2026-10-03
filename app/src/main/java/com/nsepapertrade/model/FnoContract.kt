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
    val lotSize: Int,
    val isActive: Boolean = true
) {

    val symbol: String
        get() = buildString {
            append(underlying.uppercase())
            append("_")
            append(expiry)

            when (contractType) {
                FnoContractType.FUTURE -> {
                    append("_FUT")
                }

                FnoContractType.OPTION -> {
                    append("_")
                    append(formatStrike(strikePrice))
                    append("_")
                    append(optionType.name)
                }
            }
        }

    val displayName: String
        get() = buildString {
            append(underlying.uppercase())
            append(" ")
            append(expiry)

            when (contractType) {
                FnoContractType.FUTURE -> {
                    append(" FUT")
                }

                FnoContractType.OPTION -> {
                    append(" ")
                    append(formatStrike(strikePrice))
                    append(" ")
                    append(optionType.name)
                }
            }
        }

    private fun formatStrike(
        value: Double
    ): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}
