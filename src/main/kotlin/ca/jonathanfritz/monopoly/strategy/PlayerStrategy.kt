package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Defines decision-making behavior for a Monopoly player.
 *
 * All methods receive full game state (player, bank, board) to enable
 * sophisticated strategies. The Board contains all players, allowing strategies
 * to examine opponent states (cash, properties, developments) for competitive
 * decisions like blocking monopolies or strategic bidding.
 *
 * Implementations MUST be stateless - all decisions based purely on method parameters.
 * This ensures strategies are reusable across players and games, and simplifies testing.
 *
 * Strategies can implement "dynamic" behavior by evaluating relative game position
 * on each decision (e.g., comparing player.netWorth() to opponents' net worth to
 * determine if "winning" or "losing" and adjusting risk tolerance accordingly).
 *
 * Random strategies (e.g., ImpulsiveStrategy, ChaoticStrategy) should accept a Random
 * instance in their constructor for deterministic testing, but should not maintain
 * mutable state based on previous decisions.
 */
interface PlayerStrategy {

    /**
     * Decides whether to purchase an unowned property at full price.
     *
     * @param deed The property being offered
     * @param player The player making the decision
     * @param bank Current bank state
     * @param board Current board state
     * @return true to purchase at full price, false to decline (triggers auction)
     */
    fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board
    ): Boolean

    /**
     * Calculates the next bid amount for a property at auction.
     *
     * Called iteratively during auction as bidding progresses. Allows strategies
     * to incrementally increase bids without revealing their maximum upfront.
     *
     * @param deed The property being auctioned
     * @param currentBid The current highest bid (or starting price if first round)
     * @param player The player making the decision
     * @param bank Current bank state
     * @param board Current board state
     * @return Next bid amount (must be > currentBid), or null to drop out of auction
     */
    fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board
    ): Int?

    /**
     * Assesses the value of a property to this player.
     *
     * Used for purchase decisions, auction bidding, and future trading.
     *
     * @param deed The property to value
     * @param player The player making the assessment
     * @param bank Current bank state
     * @param board Current board state
     * @return PropertyValuation with strategic value and reasoning
     */
    fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board
    ): PropertyValuation

    /**
     * Determines the minimum cash reserve this player wants to maintain.
     *
     * Used to avoid over-developing or over-bidding.
     *
     * @param player The player
     * @param board Current board state
     * @return Minimum cash to keep on hand
     */
    fun getMinimumCashReserve(
        player: Player,
        board: Board
    ): Int

    /**
     * Decides whether to pay the early jail release fee.
     *
     * @param feeAmount The fee to pay (typically $50)
     * @param player The player in jail
     * @param board Current board state
     * @return true to pay and leave jail, false to stay and roll for doubles
     */
    fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board
    ): Boolean

    /**
     * Selects which property to develop next, if any.
     *
     * Called after each dice roll. Can return null to skip development this turn.
     *
     * @param developableProperties Properties eligible for development (monopolies, affordable, even-building legal)
     * @param player The player making the decision
     * @param bank Current bank state (house/hotel availability)
     * @param board Current board state
     * @return The property to develop, or null to skip
     */
    fun selectPropertyToDevelop(
        developableProperties: List<Property>,
        player: Player,
        bank: Bank,
        board: Board
    ): Property?

    /**
     * Decides whether to unmortgage a property when cash is available.
     *
     * @param deed The mortgaged property
     * @param unmortgageCost The cost to unmortgage (110% of mortgage value)
     * @param player The player making the decision
     * @param board Current board state
     * @return true to unmortgage, false to keep mortgaged
     */
    fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board
    ): Boolean

    /**
     * Prioritizes which properties to mortgage first when liquidating assets.
     *
     * @param mortgageableProperties Properties eligible for mortgaging (owned, unmortgaged, undeveloped)
     * @param player The player liquidating
     * @param board Current board state
     * @return Properties in order of preference to mortgage (first = mortgage first)
     */
    fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board
    ): List<TitleDeed>

    /**
     * Prioritizes which buildings to sell first when liquidating assets.
     *
     * @param developedProperties Properties with houses/hotels that can be sold
     * @param player The player liquidating
     * @param board Current board state
     * @return Properties in order of preference to sell from (first = sell first)
     */
    fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board
    ): List<Property>
}
