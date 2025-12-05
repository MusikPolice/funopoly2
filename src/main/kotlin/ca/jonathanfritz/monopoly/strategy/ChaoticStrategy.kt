package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.random.Random

/**
 * Ernie - The Chaotic Opportunist
 *
 * Focuses on disruption and unpredictability:
 * - Blocks opponent monopolies aggressively
 * - Random cash reserves ($0-$500)
 * - Chaotic bidding (sometimes $5, sometimes $100)
 * - Builds unevenly for intimidation (prioritizes hotels)
 * - Random unmortgaging (40% chance)
 * - Random liquidation priorities
 */
class ChaoticStrategy(
    private val rng: Random = Random.Default,
) : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Can't afford
        if (player.money < deed.price) {
            return false
        }

        // Always buy to block opponent monopoly
        if (wouldBlockOpponentMonopoly(deed, player, board)) {
            return true
        }

        // Random otherwise (60% buy rate)
        return rng.nextDouble() < 0.6
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        // Chaotic increments based on property importance
        val blocksOpponent = wouldBlockOpponentMonopoly(deed, player, board)
        val increment =
            if (blocksOpponent) {
                rng.nextInt(30, 81) // $30-$80 when blocking
            } else {
                // Sometimes $5, sometimes $100
                if (rng.nextDouble() < 0.3) {
                    rng.nextInt(5, 21) // $5-$20
                } else {
                    rng.nextInt(20, 101) // $20-$100
                }
            }
        val nextBid = currentBid + increment

        // Use valuateProperty to determine internal max bid
        val internalMax = valuateProperty(deed, player, bank, board).strategicValue
        if (currentBid >= internalMax) {
            return null
        }

        // Don't exceed internal max or player's money
        return if (nextBid <= internalMax && nextBid <= player.money) {
            nextBid
        } else {
            null
        }
    }

    override fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): PropertyValuation {
        val baseValue = PropertyValuation.calculateBaseValue(deed, player)

        // Check if blocks opponent monopoly
        val blocksOpponent = wouldBlockOpponentMonopoly(deed, player, board)

        // Check if completes own monopoly
        val completesOwnMonopoly = wouldCompleteMonopoly(deed, player)

        val (multiplier, reasoning) =
            when {
                blocksOpponent -> {
                    3.0 to "Blocks opponent monopoly"
                }

                completesOwnMonopoly -> {
                    2.0 to "Completes monopoly"
                }

                else -> {
                    // Random chaos multiplier
                    val chaosMultiplier = rng.nextDouble(0.5, 2.0)
                    chaosMultiplier to "Chaos factor: ${String.format("%.2fx", chaosMultiplier)}"
                }
            }

        val finalValue = (baseValue.strategicValue * multiplier).toInt()

        return PropertyValuation(
            deed,
            finalValue,
            buildString {
                append(baseValue.reasoning)
                append(", ")
                append(reasoning)
            },
        )
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // Random $0-$500 (chaotic)
        return rng.nextInt(0, 501)
    }

    override fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Can't afford
        if (player.money < feeAmount) {
            return false
        }

        // Chaotic: 70% chance to pay
        return rng.nextDouble() < 0.7
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

        // Prioritize intimidation: properties with 4 houses (can build hotel)
        val canBuildHotel =
            affordable.filter { property ->
                val development = player.deeds[property]
                development != null && development.numHouses == 4 && !development.hasHotel
            }

        if (canBuildHotel.isNotEmpty()) {
            // Random selection from hotel candidates
            return canBuildHotel.random(rng)
        }

        // Otherwise, random selection (chaotic)
        return affordable.random(rng)
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Can't unmortgage if unaffordable
        if (player.money < unmortgageCost) {
            return false
        }

        // Random 40% chance (chaotic)
        return rng.nextDouble() < 0.4
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Random order (chaotic)
        return mortgageableProperties.shuffled(rng)
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Random order (chaotic/emotional)
        return developedProperties.shuffled(rng)
    }

    /**
     * Check if purchasing this deed would block an opponent from completing a monopoly.
     */
    private fun wouldBlockOpponentMonopoly(
        deed: TitleDeed,
        player: Player,
        board: Board,
    ): Boolean {
        val colorGroup = deed.colourGroup

        // Check each opponent
        return board.players
            .filter { it != player }
            .any { opponent ->
                // Count how many properties in this color group the opponent owns
                val opponentOwnsInGroup = opponent.deeds.keys.count { it.colourGroup == colorGroup }
                val totalInGroup = colorGroup.titleDeeds().values.count()

                // Would block if opponent owns all but this one property
                opponentOwnsInGroup == totalInGroup - 1 && !opponent.isOwner(deed::class)
            }
    }

    override fun toString(): String = "ChaoticStrategy (Ernie)"
}
