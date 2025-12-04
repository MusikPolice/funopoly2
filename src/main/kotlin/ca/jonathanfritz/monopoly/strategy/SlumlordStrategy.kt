package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.random.Random

/**
 * Slumlord strategy (Oscar the Grouch persona) that focuses on cheap properties with high volume returns.
 *
 * Characteristics:
 * - Prefers Brown/Light Blue (cheap sets)
 * - Cash reserve of $200 (one GO salary)
 * - Only buys if money > price * 1.5 + reserve
 * - Rejects expensive properties (Green/Dark Blue) unless completing monopoly
 * - Bids conservatively: 80% for cheap properties, 50% for expensive
 * - Builds to exactly 4 houses (avoids hotels)
 * - Mortgages expensive properties first
 * - Sells hotels first, then expensive properties
 *
 * @param rng Random number generator for deterministic bid increments
 */
class SlumlordStrategy(
    private val rng: Random = Random.Default,
) : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Reject expensive properties unless completing monopoly
        if (isExpensiveProperty(deed) && !wouldCompleteMonopoly(deed, player)) {
            return false
        }

        // Only buy if we have 1.5x the price plus minimum reserve
        val minimumRequired = (deed.price * 1.5).toInt() + getMinimumCashReserve(player, board)
        return player.money >= minimumRequired
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        // Calculate internal max based on property type
        val baseMaxBid =
            if (isExpensiveProperty(deed)) {
                (deed.price * 0.5).toInt()
            } else {
                (deed.price * 0.8).toInt()
            }

        // Add 20% bonus if completing monopoly
        val maxBid =
            if (wouldCompleteMonopoly(deed, player)) {
                (baseMaxBid * 1.2).toInt()
            } else {
                baseMaxBid
            }

        // Drop out if current bid exceeds our max
        if (currentBid >= maxBid) {
            return null
        }

        // Increment by $10-20 (small increments to avoid overpaying)
        val increment = rng.nextInt(10, 21)
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
                isExpensiveProperty(deed) -> 0.5
                isCheapProperty(deed) -> 1.5
                else -> 1.0
            }

        // Add 50% bonus for monopoly completion
        val monopolyMultiplier =
            if (wouldCompleteMonopoly(deed, player)) {
                1.5
            } else {
                1.0
            }

        val finalValue = (baseValue.strategicValue * multiplier * monopolyMultiplier).toInt()

        val reasoning =
            buildString {
                append(baseValue.reasoning)
                if (multiplier != 1.0) {
                    append(", Property type: ${String.format("%.2fx", multiplier)}")
                }
                if (monopolyMultiplier != 1.0) {
                    append(", Monopoly completion: +50%")
                }
            }

        return PropertyValuation(deed, finalValue, reasoning)
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // Maintain one GO salary as reserve
        return 200
    }

    override fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Pay if we have cash above reserve
        return player.money >= getMinimumCashReserve(player, board) + feeAmount
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

        // Calculate highest rent on board for safety buffer
        val highestRent =
            board.players
                .flatMap { p -> p.deeds.entries.map { (deed, development) -> Triple(deed, development, p) } }
                .maxOfOrNull { (deed, _, owner) ->
                    if (deed is Property) {
                        deed.calculateRent(owner, board)
                    } else {
                        0
                    }
                } ?: 0

        // Only develop if we have cash above reserve + highest rent
        val requiredCash = getMinimumCashReserve(player, board) + highestRent
        if (player.money <= requiredCash) {
            return null
        }

        // Filter out properties with 4 houses (don't build hotels)
        val notAtMax =
            developableProperties.filter { property ->
                val development =
                    player.deeds.entries
                        .firstOrNull { it.key::class == property::class }
                        ?.value
                        ?: return@filter false
                development.numHouses < 4 && !development.hasHotel
            }

        if (notAtMax.isEmpty()) {
            return null
        }

        // Prioritize cheap color groups (Brown, Light Blue)
        val cheapProperties = notAtMax.filter { isCheapProperty(it) }
        val candidates = cheapProperties.ifEmpty { notAtMax }

        // Select cheapest property we can afford
        return candidates
            .filter { it.buildingCost <= player.money - requiredCash }
            .minByOrNull { it.buildingCost }
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Very conservative: only unmortgage if we have 3x the cost
        return player.money >= unmortgageCost * 3
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Mortgage expensive properties first (Green, Dark Blue, Red)
        return mortgageableProperties.sortedWith(
            compareByDescending { deed -> deed.price },
        )
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Sell hotels first (convert to 4 houses), then expensive properties
        return developedProperties.sortedWith(
            compareByDescending<Property> { property ->
                val development =
                    player.deeds.entries
                        .firstOrNull { it.key::class == property::class }
                        ?.value
                        ?: return@compareByDescending 0
                if (development.hasHotel) 1 else 0 // Hotels first
            }.thenByDescending { it.price }, // Then by price (expensive first)
        )
    }

    private fun isExpensiveProperty(deed: TitleDeed): Boolean =
        deed.colourGroup in listOf(ColourGroup.Red, ColourGroup.Yellow, ColourGroup.Green, ColourGroup.DarkBlue)

    private fun isCheapProperty(deed: TitleDeed): Boolean = deed.colourGroup in listOf(ColourGroup.Brown, ColourGroup.LightBlue)

    private fun wouldCompleteMonopoly(
        deed: TitleDeed,
        player: Player,
    ): Boolean {
        val colorGroup = deed.colourGroup
        val totalInGroup = colorGroup.titleDeeds().values.count()
        val ownedInGroup = player.deeds.keys.count { it.colourGroup == colorGroup }

        // Would complete monopoly if we own all but this one property
        return ownedInGroup == totalInGroup - 1 && !player.isOwner(deed::class)
    }
}
