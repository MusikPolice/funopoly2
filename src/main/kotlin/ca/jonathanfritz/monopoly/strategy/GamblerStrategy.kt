package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.random.Random

/**
 * Gambler strategy (Cookie Monster persona) that spends cash fast to grow assets.
 *
 * Characteristics:
 * - Buys nearly everything affordable
 * - Loves railroads (always buy)
 * - No cash reserve ($0)
 * - Aggressive bidding: 150-200% normally, 200-250% for railroads/monopoly
 * - Builds aggressively on everything
 * - Rushes to hotels
 * - Unmortgages whenever possible
 * - Mortgages everything as last resort
 * - Sells buildings only as absolute last resort
 *
 * @param rng Random number generator for deterministic bid increments and max calculations
 */
class GamblerStrategy(
    private val rng: Random = Random.Default,
) : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Buy everything affordable
        return player.money >= deed.price
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        // Extra aggressive on railroads and monopoly completion
        val maxMultiplier =
            if (isRailroad(deed) || wouldCompleteMonopoly(deed, player)) {
                rng.nextDouble(2.0, 2.5)
            } else {
                rng.nextDouble(1.5, 2.0)
            }

        val maxBid = (deed.price * maxMultiplier).toInt()

        // Drop out if current bid exceeds our max
        if (currentBid >= maxBid) {
            return null
        }

        // Aggressive increments: $50-100 per round (go big or go home)
        val increment = rng.nextInt(50, 101)
        val nextBid = minOf(currentBid + increment, maxBid)

        return if (nextBid > currentBid) nextBid else null
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
                isRailroad(deed) -> 2.0
                wouldCompleteMonopoly(deed, player) -> 2.5
                else -> 1.2
            }

        val finalValue = (baseValue.strategicValue * multiplier).toInt()

        val reasoning =
            buildString {
                append(baseValue.reasoning)
                when {
                    isRailroad(deed) -> append(", Railroad: ${String.format("%.2fx", multiplier)}")
                    wouldCompleteMonopoly(deed, player) -> append(", Monopoly completion: ${String.format("%.2fx", multiplier)}")
                    else -> append(", Property type: ${String.format("%.2fx", multiplier)}")
                }
            }

        return PropertyValuation(deed, finalValue, reasoning)
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // No cash reserve - spend it all
        return 0
    }

    override fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Pay if we have more than the fee (keep at least $1)
        return player.money > feeAmount
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

        // Build on everything possible - no cash reserve consideration
        // Rush to hotels by selecting most expensive affordable property
        return developableProperties
            .filter { it.buildingCost <= player.money }
            .maxByOrNull { it.price }
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Always unmortgage if we can afford it
        return player.money >= unmortgageCost
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Mortgage everything - no preference (only as last resort before selling buildings)
        return mortgageableProperties
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Sell buildings only as absolute last resort - no preference
        return developedProperties
    }

    private fun isRailroad(deed: TitleDeed): Boolean = deed.colourGroup == ColourGroup.Railroads
}
