package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Calculating strategy (Bert persona) that makes mathematically optimal decisions.
 *
 * Characteristics:
 * - Targets Orange/Red properties (best ROI)
 * - Dynamic cash reserve: 2x highest rent on board, minimum $300
 * - Buys if ROI > 15% and money > price * 1.5 + reserve
 * - Strict bidding: exactly $10 increments, never exceeds calculated value
 * - Builds to 3 houses first (rent efficiency sweet spot)
 * - Unmortgages based on payback period (< 10 turns)
 * - Liquidates lowest ROI properties first
 */
class CalculatingStrategy : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Must have 1.5x the price plus reserve
        val minimumRequired = (deed.price * 1.5).toInt() + getMinimumCashReserve(player, board)
        if (player.money < minimumRequired) {
            return false
        }

        // Calculate ROI - simplified: rent-to-price ratio, and buy iff > 15%
        return calculateROI(deed, player) > 0.15
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        // Calculate strategic value
        val valuation = valuateProperty(deed, player, bank, board)

        // Allow up to 110% of strategic value, or higher for monopoly completion
        val maxBid =
            if (wouldCompleteMonopoly(deed, player)) {
                (valuation.strategicValue * 1.5).toInt()
            } else {
                (valuation.strategicValue * 1.1).toInt()
            }

        // Drop out if current bid exceeds our max
        if (currentBid >= maxBid) {
            return null
        }

        // Efficient increments: exactly $10 per round
        val nextBid = minOf(currentBid + 10, maxBid)

        return if (nextBid > currentBid) nextBid else null
    }

    override fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): PropertyValuation {
        val baseValue = PropertyValuation.calculateBaseValue(deed, player)

        // Orange/Red get +20% (optimal ROI from analysis)
        val multiplier =
            if (isOptimalProperty(deed)) {
                1.2
            } else {
                1.0
            }

        val finalValue = (baseValue.strategicValue * multiplier).toInt()

        val reasoning =
            buildString {
                append(baseValue.reasoning)
                if (multiplier != 1.0) {
                    append(", Optimal ROI: ${String.format("%.2fx", multiplier)}")
                }
            }

        return PropertyValuation(deed, finalValue, reasoning)
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // Dynamic: highest rent × 2, minimum $300
        val highestRent = calculateHighestRentOnBoard(board, player)
        return maxOf(300, highestRent * 2)
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

        // Filter to affordable properties
        val affordable = developableProperties.filter { it.buildingCost <= availableCash }
        if (affordable.isEmpty()) {
            return null
        }

        // Prioritize Orange/Red properties
        val optimalProperties = affordable.filter { isOptimalProperty(it) }
        val candidates = optimalProperties.ifEmpty { affordable }

        // Build to 3 houses first (rent efficiency sweet spot)
        // Filter to properties with < 3 houses, then select the one with fewest houses
        val propertiesNeedingThreeHouses =
            candidates.filter { property ->
                val development = player.deeds[property]
                development != null && development.numHouses < 3 && !development.hasHotel
            }

        if (propertiesNeedingThreeHouses.isNotEmpty()) {
            // Select the one with fewest houses
            return propertiesNeedingThreeHouses.minByOrNull { property ->
                val development = player.deeds[property]
                development?.numHouses ?: Int.MAX_VALUE
            }
        }

        // Otherwise, select property with highest ROI
        return candidates.maxByOrNull { calculateROI(it, player) }
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Can't unmortgage if it would drop below reserve
        if (player.money < unmortgageCost + getMinimumCashReserve(player, board)) {
            return false
        }

        // Unmortgage if ROI > 10% (payback period < 10 turns)
        return calculateROI(deed, player) > 0.1
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Lowest ROI properties first
        return mortgageableProperties.sortedBy { calculateROI(it, player) }
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Lowest ROI developments first
        return developedProperties.sortedBy { calculateROI(it, player) }
    }

    private fun calculateROI(
        deed: TitleDeed,
        player: Player,
    ): Double {
        // Simplified ROI calculation
        if (deed !is Property) {
            // Railroads and utilities have lower ROI
            return 0.05
        }

        // Base ROI varies by property type
        // Orange/Red have best ROI (around 20%), others around 10-15%
        val baseROI =
            when {
                isOptimalProperty(deed) -> 0.20
                deed.colourGroup in listOf(ColourGroup.Yellow, ColourGroup.LightBlue) -> 0.15
                else -> 0.12
            }

        // If we have monopoly, ROI doubles
        return if (player.hasMonopoly(deed.colourGroup)) {
            baseROI * 2
        } else {
            baseROI
        }
    }

    private fun isOptimalProperty(deed: TitleDeed): Boolean = deed.colourGroup in listOf(ColourGroup.Orange, ColourGroup.Red)

    override fun toString(): String = "CalculatingStrategy (Bert)"
}
