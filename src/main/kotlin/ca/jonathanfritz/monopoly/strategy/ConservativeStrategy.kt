package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Conservative strategy (Big Bird persona) that prioritizes financial safety.
 *
 * Characteristics:
 * - High cash reserve ($500)
 * - Only buys if money > price * 2.0 (very cautious)
 * - Bids conservatively at 70% of property value
 * - Develops slowly, prefers 2-3 houses over hotels
 * - Liquidates early to maintain cash reserves
 */
class ConservativeStrategy : PlayerStrategy {
    override fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): Boolean {
        // Only buy if we have twice the price plus minimum reserve
        val minimumRequired = (deed.price * 2.0).toInt() + getMinimumCashReserve(player, board)
        return player.money > minimumRequired
    }

    override fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        minimumBid: Int,
        player: Player,
        bank: Bank,
        board: Board,
    ): Int? {
        val valuation = valuateProperty(deed, player, bank, board)

        // Bid up to 70% of strategic value
        val maxBid = (valuation.strategicValue * 0.7).toInt()

        // Don't bid if we can't maintain cash reserve
        val availableCash = player.money - getMinimumCashReserve(player, board)

        if (minimumBid > maxBid || minimumBid > availableCash) {
            return null // Drop out
        }

        // Increment by $10 or to maxBid, whichever is smaller, but respect minimum
        val nextBid = maxOf(minimumBid, minOf(currentBid + 10, maxBid, availableCash))
        return if (nextBid >= minimumBid) nextBid else null
    }

    override fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board,
    ): PropertyValuation {
        // Use base valuation - conservative players don't inflate values
        return PropertyValuation.calculateBaseValue(deed, player)
    }

    override fun getMinimumCashReserve(
        player: Player,
        board: Board,
    ): Int {
        // Conservative players maintain high cash reserves
        return 500
    }

    override fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Conservative strategy: stay in jail in late game to avoid rent
        
        // Count total owned properties across all players
        val totalOwnedProperties = board.players.sumOf { it.deeds.size }
        
        // Get total buyable properties from the board
        val totalProperties = board.totalBuyableProperties()
        val unownedProperties = totalProperties - totalOwnedProperties
        
        // If there are 5+ unowned properties, get out to buy them (if we have cash)
        if (unownedProperties >= 5) {
            return player.money >= getMinimumCashReserve(player, board) + feeAmount + 200
        }
        
        // In late game (few unowned properties), calculate risk of leaving jail
        // Count opponent properties and their development
        val opponentDevelopments = board.players
            .filter { it != player && !it.isBankrupt() }
            .flatMap { opponent -> opponent.deeds.entries }
            .sumOf { (_, development) ->
                // Weight hotels heavily, houses moderately
                if (development.hasHotel) 5 else development.numHouses
            }
        
        // If opponents have significant development (10+ houses/hotels), stay in jail
        // This avoids expensive rents
        if (opponentDevelopments >= 10) {
            return false
        }
        
        // Otherwise, only pay if we have plenty of cash
        return player.money >= getMinimumCashReserve(player, board) + feeAmount + 200
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

        // Only develop if we have cash well above reserve
        val availableCash = player.money - getMinimumCashReserve(player, board)
        if (availableCash < 100) {
            return null // Not enough cushion
        }

        // Find properties with 0-2 houses (avoid hotels, prefer slow development)
        val lowDevelopment =
            developableProperties.filter { property ->
                // Find the development for this property by class (since instances may differ)
                val development = player.deeds.entries.firstOrNull { it.key::class == property::class }?.value ?: return@filter false
                development.numHouses in 0..2 && !development.hasHotel
            }

        if (lowDevelopment.isEmpty()) {
            return null // Don't build beyond 3 houses
        }

        // Select property with highest rent that we can afford
        return lowDevelopment
            .filter { it.buildingCost <= availableCash }
            .maxByOrNull { it.calculateRent(player, board) }
    }

    override fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board,
    ): Boolean {
        // Only unmortgage if we have significant cash above reserve
        val availableCash = player.money - getMinimumCashReserve(player, board)
        return availableCash >= unmortgageCost * 2
    }

    override fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board,
    ): List<TitleDeed> {
        // Mortgage early to preserve cash - prioritize non-monopoly properties first,
        // then expensive properties (to get more cash quickly)
        return mortgageableProperties.sortedWith(
            compareBy(
                { deed -> if (player.hasMonopoly(deed.colourGroup)) 1 else 0 }, // Non-monopolies first
                { deed -> -deed.mortgageValue }, // Then by highest mortgage value
            ),
        )
    }

    override fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board,
    ): List<Property> {
        // Sell buildings early to preserve cash - start with most developed properties
        // to get cash quickly
        return developedProperties.sortedWith(
            compareByDescending<Property> { property ->
                // Find the development for this property by class (since instances may differ)
                val development = player.deeds.entries.firstOrNull { it.key::class == property::class }?.value
                    ?: return@compareByDescending 0
                (if (development.hasHotel) 5 else 0) + development.numHouses // Hotels worth 5 houses
            }.thenByDescending { it.buildingCost }, // Then by building cost (more cash back)
        )
    }

    override fun toString(): String = "ConservativeStrategy (Count von Count)"
}
