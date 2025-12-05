package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.random.Random

/**
 * High-rent strategy (Count von Count persona) that focuses on expensive properties with maximum rents.
 *
 * Characteristics:
 * - Prioritizes Green/Dark Blue (expensive sets)
 * - Cash reserve of $300 (medium buffer)
 * - Buys if money > price * 1.2 + reserve
 * - Always buys if completing monopoly
 * - Aggressive bidding: 120% for normal, 150% for monopoly completion
 * - Rushes to hotels (builds 4 houses then hotel ASAP)
 * - Mortgages cheap properties first
 * - Sells cheap properties first to preserve expensive developments
 *
 * @param rng Random number generator for deterministic bid increments
 */
class HighRentStrategy(
    private val rng: Random = Random.Default,
) : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Always buy if completing monopoly AND we can afford it
        if (wouldCompleteMonopoly(deed, player) && player.money >= deed.price) {
            return true
        }

        // Buy if we have 1.2x the price plus minimum reserve
        val minimumRequired = (deed.price * 1.2).toInt() + getMinimumCashReserve(player, board)
        return player.money >= minimumRequired
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        minimumBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        // Calculate internal max based on monopoly completion
        val maxBidByValue =
            if (wouldCompleteMonopoly(deed, player)) {
                (deed.price * 1.5).toInt()
            } else {
                (deed.price * 1.2).toInt()
            }
        
        // Can't bid more than we can afford (keep minimum reserve)
        val reserve = getMinimumCashReserve(player, board)
        val maxBid = minOf(maxBidByValue, player.money - reserve)

        // Drop out if minimum bid exceeds our max
        if (minimumBid > maxBid) {
            return null
        }

        // Aggressive increments: $20-50 per round (intimidate opponents), but respect minimum
        val increment = rng.nextInt(20, 51)
        val nextBid = maxOf(minimumBid, minOf(currentBid + increment, maxBid))

        return if (nextBid >= minimumBid) nextBid else null
    }

    override fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): PropertyValuation {
        val baseValue = PropertyValuation.calculateBaseValue(deed, player)

        // Apply multipliers based on property type
        val multiplier =
            when {
                isExpensiveProperty(deed) -> 1.5
                isCheapProperty(deed) -> 0.7
                else -> 1.0
            }

        val finalValue = (baseValue.strategicValue * multiplier).toInt()

        val reasoning =
            buildString {
                append(baseValue.reasoning)
                if (multiplier != 1.0) {
                    append(", Property type: ${String.format("%.2fx", multiplier)}")
                }
            }

        return PropertyValuation(deed, finalValue, reasoning)
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // Medium cash buffer
        return 300
    }

    override fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Pay if we have cash above reserve
        return player.money > getMinimumCashReserve(player, board) + feeAmount
    }

    override fun selectPropertyToDevelop(
        developableProperties: List<Property>,
        player: Player,
        bank: Bank,
        board: Board,
    ): Property? {
        if (developableProperties.isEmpty()) {
            return null
        }

        // Only develop if we have cash above reserve
        val availableCash = player.money - getMinimumCashReserve(player, board)
        if (availableCash <= 0) {
            return null
        }

        // Prioritize expensive color groups (Green, Dark Blue, Red, Yellow)
        val expensiveProperties = developableProperties.filter { isExpensiveProperty(it) }
        val candidates = expensiveProperties.ifEmpty { developableProperties }

        // Select most expensive property we can afford (rush to hotels)
        return candidates
            .filter { it.buildingCost <= availableCash }
            .maxByOrNull { it.price }
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Unmortgage if we have more than 1.8x the cost
        return player.money > unmortgageCost * 1.8
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Mortgage cheap properties first (Brown, Light Blue)
        return mortgageableProperties.sortedWith(
            compareBy { deed -> deed.price },
        )
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Sell cheap properties first (preserve expensive developments)
        return developedProperties.sortedWith(
            compareBy { it.price },
        )
    }

    private fun isExpensiveProperty(deed: TitleDeed): Boolean =
        deed.colourGroup in listOf(ColourGroup.Red, ColourGroup.Yellow, ColourGroup.Green, ColourGroup.DarkBlue)

    private fun isCheapProperty(deed: TitleDeed): Boolean = deed.colourGroup in listOf(ColourGroup.Brown, ColourGroup.LightBlue)

    override fun toString(): String = "HighRentStrategy (Big Bird)"
}
