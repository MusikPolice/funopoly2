package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Simple greedy strategy that always does whatever the player can afford without considering board state.
 *
 * This strategy buys any property it can afford, develops properties with the highest current rent,
 * and liquidates assets starting with properties farthest from completing monopolies. It maintains
 * no cash reserve and makes no strategic decisions based on opponent positions or game state.
 *
 * Serves as the baseline for regression testing and comparison with more sophisticated strategies.
 */
open class DefaultStrategy : PlayerStrategy {

    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board
    ): Boolean {
        // Buy any property if we can afford it
        return player.money > deed.price
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        minimumBid: Int,
        player: Player,
        bank: Bank,
        board: Board
    ): Int? {
        // Never participate in auctions - this keeps the baseline strategy simple and predictable
        // for regression testing. Other strategies implement more sophisticated auction logic.
        return null
    }

    override fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board
    ): PropertyValuation {
        // Simple valuation: property is worth exactly its purchase price
        return PropertyValuation(
            deed = deed,
            strategicValue = deed.price,
            reasoning = "Base value equals deed price"
        )
    }

    override fun getMinimumCashReserve(player: Player, board: Board): Int {
        // No cash reserve - spend everything
        return 0
    }

    override fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board
    ): Boolean {
        // Pay to get out of jail if no card is available and we have the money
        return player.isInJail &&
            !player.hasGetOutOfJailFreeCard() &&
            player.remainingTurnsInJail > 0 &&
            player.money > feeAmount
    }

    override fun selectPropertyToDevelop(
        developableProperties: List<Property>,
        player: Player,
        bank: Bank,
        board: Board
    ): Property? {
        // Develop the property with the highest current rent that we can afford and that respects even-building rules
        return developableProperties
            .filter { property ->
                property.buildingCost < player.money
            }
            .sortedByDescending { property ->
                property.calculateRent(player, board)
            }
            .firstOrNull { property ->
                val development = player.getDevelopment(property::class)
                when (development.numHouses) {
                    4 -> property.addingOrRemovingHotelRespectsEvenBuildingRules(player)
                    else -> property.addingHouseRespectsEvenBuildingRules(player)
                }
            }
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board
    ): Boolean {
        // Unmortgage if we have comfortable cash reserves (2x the cost)
        return player.money >= unmortgageCost * 2.0
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board
    ): List<TitleDeed> {
        // Mortgage properties farthest from completing monopolies first, then by lowest mortgage value
        return mortgageableProperties.sortedWith(
            compareByDescending<TitleDeed> { deed ->
                val numDeedsInColourGroup = deed.colourGroup.titleDeeds().values.count()
                val numOwnedDeedsInColourGroup = player.deeds.keys.count { it.colourGroup == deed.colourGroup }
                numDeedsInColourGroup - numOwnedDeedsInColourGroup
            }.thenBy { deed ->
                deed.mortgageValue
            }
        )
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board
    ): List<Property> {
        // Sell buildings from properties with the lowest rent first to minimize income loss
        return developedProperties.sortedBy { property ->
            property.calculateRent(player, board)
        }
    }

    override fun toString(): String = "DefaultStrategy"
}
