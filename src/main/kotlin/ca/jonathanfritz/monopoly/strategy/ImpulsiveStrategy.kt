package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.random.Random

/**
 * Impulsive strategy (Elmo persona) that makes random, fun-over-strategy decisions.
 *
 * Characteristics:
 * - 90% chance to buy if affordable, 10% random decline
 * - Cash reserve of $50 (minimal)
 * - Random auction bidding: 50-150% max, $5-100 increments
 * - Random property valuation: 0.5x-2.0x base value (inconsistent)
 * - Random property development selection
 * - 50% chance to unmortgage if affordable
 * - Random order for mortgages and building sales
 *
 * @param rng Random number generator for all random decisions
 */
class ImpulsiveStrategy(
    private val rng: Random = Random.Default,
) : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Can't buy if can't afford
        if (player.money < deed.price) {
            return false
        }

        // 90% chance to buy, 10% random decline
        return rng.nextDouble() < 0.9
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        // Random max between 50% and 150% of deed price
        val maxMultiplier = rng.nextDouble(0.5, 1.5)
        val maxBid = (deed.price * maxMultiplier).toInt()

        // Drop out if current bid exceeds our max
        if (currentBid >= maxBid) {
            return null
        }

        // Random increments: $5-100 per round (wildly inconsistent)
        val increment = rng.nextInt(5, 101)
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

        // Random value between 0.5× and 2.0× base value (changes each time)
        val multiplier = rng.nextDouble(0.5, 2.0)
        val finalValue = (baseValue.strategicValue * multiplier).toInt()

        val reasoning =
            buildString {
                append(baseValue.reasoning)
                append(", Random multiplier: ${String.format("%.2fx", multiplier)}")
            }

        return PropertyValuation(deed, finalValue, reasoning)
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // Minimal cash buffer
        return 50
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

        // Filter to affordable properties
        val affordable = developableProperties.filter { it.buildingCost <= player.money }
        if (affordable.isEmpty()) {
            return null
        }

        // Random selection from affordable properties
        return affordable.random(rng)
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Can't unmortgage if can't afford
        if (player.money < unmortgageCost) {
            return false
        }

        // 50% chance if affordable
        return rng.nextBoolean()
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Random order
        return mortgageableProperties.shuffled(rng)
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Random order
        return developedProperties.shuffled(rng)
    }
}
