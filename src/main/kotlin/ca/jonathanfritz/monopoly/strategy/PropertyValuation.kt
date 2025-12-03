package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
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
    val reasoning: String,
) {
    companion object {
        /**
         * Calculates the base strategic value of a property using simple heuristics.
         *
         * The calculation considers:
         * - Deed price as baseline
         * - Traffic multipliers for high-traffic positions (post-GO, post-Jail, near Railroads)
         * - Monopoly proximity bonus (how close to completing the color group)
         * - Development potential value (for properties that can be developed)
         *
         * @param deed The property to value
         * @param player The player making the assessment
         * @return PropertyValuation with calculated strategic value
         */
        fun calculateBaseValue(
            deed: TitleDeed,
            player: Player,
        ): PropertyValuation {
            val basePrice = deed.price.toDouble()

            // Apply traffic multiplier for high-traffic positions
            val trafficMultiplier = getTrafficMultiplier(deed)
            val trafficAdjustedValue = basePrice * trafficMultiplier

            // Add monopoly proximity bonus
            val monopolyBonus = calculateMonopolyBonus(deed, player)

            // Add development potential value
            val developmentValue = calculateDevelopmentValue(deed, player)

            val totalValue = (trafficAdjustedValue + monopolyBonus + developmentValue).toInt()

            val reasoning =
                buildString {
                    append("Base: $${deed.price}")
                    if (trafficMultiplier != 1.0) {
                        append(", Traffic: ${String.format("%.2fx", trafficMultiplier)}")
                    }
                    if (monopolyBonus > 0) {
                        append(", Monopoly bonus: +$$monopolyBonus")
                    }
                    if (developmentValue > 0) {
                        append(", Development potential: +$$developmentValue")
                    }
                }

            return PropertyValuation(deed, totalValue, reasoning)
        }

        /**
         * Returns a traffic multiplier based on color group position.
         *
         * High-traffic color groups get higher multipliers:
         * - Brown/LightBlue (post-GO): 1.2x (players just passed GO with cash)
         * - Pink/Orange (post-Jail): 1.15x (Jail is most-landed-on space)
         * - Railroads: 1.1x (cards send players to railroads)
         * - Other groups: 1.0x
         */
        private fun getTrafficMultiplier(deed: TitleDeed): Double =
            when (deed.colourGroup) {
                // Post-GO
                ColourGroup.Brown, ColourGroup.LightBlue -> 1.2

                // Post-Jail
                ColourGroup.Pink, ColourGroup.Orange -> 1.15

                // Railroads
                ColourGroup.Railroads -> 1.1

                // Cards send to railroads
                else -> 1.0
            }

        /**
         * Calculates a bonus based on how close the player is to completing a monopoly.
         *
         * Returns a percentage of the deed price based on ownership:
         * - Own 0 properties in group: 0% bonus
         * - Own 1 property in 3-property group: 10% bonus
         * - Own 2 properties in 3-property group: 25% bonus
         * - Own 1 property in 2-property group: 20% bonus
         * - Already have monopoly: 0% bonus (already counted in base value)
         */
        private fun calculateMonopolyBonus(
            deed: TitleDeed,
            player: Player,
        ): Int {
            val colorGroup = deed.colourGroup
            val totalInGroup = colorGroup.titleDeeds().values.count()
            val ownedInGroup = player.deeds.keys.count { it.colourGroup == colorGroup }

            // If we already own this deed or have the monopoly, no bonus
            if (player.isOwner(deed::class) || player.hasMonopoly(colorGroup)) {
                return 0
            }

            val bonusPercentage =
                when {
                    totalInGroup == 3 && ownedInGroup == 2 -> 0.25

                    // One away from monopoly
                    totalInGroup == 3 && ownedInGroup == 1 -> 0.10

                    // Two away from monopoly
                    totalInGroup == 2 && ownedInGroup == 1 -> 0.20

                    // One away from monopoly (utilities/dark blue)
                    else -> 0.0
                }

            return (deed.price * bonusPercentage).toInt()
        }

        /**
         * Calculates the development potential value for properties that can be developed.
         *
         * For properties (not railroads/utilities), estimates value based on:
         * - Building cost as a proxy for development value
         * - Properties with lower building costs relative to price are more valuable
         *
         * Returns 0 for railroads and utilities.
         */
        private fun calculateDevelopmentValue(
            deed: TitleDeed,
            player: Player,
        ): Int {
            if (deed !is Property) {
                return 0 // Railroads and utilities can't be developed
            }

            // If we don't have or can't get a monopoly, no development value
            val colorGroup = deed.colourGroup
            val ownedInGroup = player.deeds.keys.count { it.colourGroup == colorGroup }
            val totalInGroup = colorGroup.titleDeeds().values.count()

            if (ownedInGroup == 0 || (ownedInGroup < totalInGroup - 1)) {
                return 0 // Too far from monopoly
            }

            // Estimate development value as a percentage of building cost
            // Lower building costs relative to property price indicate better ROI
            val buildingCostRatio = deed.buildingCost.toDouble() / deed.price
            val developmentValue = deed.buildingCost * (1.0 - buildingCostRatio) * 0.5

            return developmentValue.toInt()
        }
    }
}
