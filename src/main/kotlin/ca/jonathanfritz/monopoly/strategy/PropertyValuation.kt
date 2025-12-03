package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Represents a player's assessment of a property's strategic value.
 *
 * @param deed The property being valued
 * @param strategicValue The calculated value in dollars
 * @param reasoning Human-readable explanation of the valuation
 */
data class PropertyValuation(
    val deed: TitleDeed,
    val strategicValue: Int,
    val reasoning: String
)
