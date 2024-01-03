package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Dice
import kotlin.reflect.KClass

sealed class TitleDeed(
    val colourGroup: ColourGroup,
    val price: Int,
    val mortgageValue: Int
) {
    // true if a player can build houses on the corresponding Tile
    abstract val isBuildable: Boolean

    companion object {
        // lazy modifier breaks a circular initialization dependency between TitleDeed and its child classes
        val values: Map<KClass<out TitleDeed>, TitleDeed> by lazy {
            Property.values + Railroad.values + Utility.values
        }

        fun <T : TitleDeed> of(kClass: KClass<T>): TitleDeed = values.getValue(kClass)
    }

    abstract fun calculateRent(owner: Player, diceRoll: Dice.Roll): Int
}