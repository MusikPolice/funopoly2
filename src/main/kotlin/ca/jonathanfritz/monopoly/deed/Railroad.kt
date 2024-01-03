package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import kotlin.reflect.KClass

sealed class Railroad(): TitleDeed(ColourGroup.Railroads, 200, 100) {
    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    class ReadingRailroad : Railroad()
    class PennsylvaniaRailroad : Railroad()
    class BAndORailroad : Railroad()
    class ShortlineRailroad : Railroad()

    override val isBuildable: Boolean = false

    companion object {
        val values: Map<KClass<out Railroad>, Railroad> = Railroad::class.sealedSubclasses.associateWith {
            it.constructors.first().call()
        }
        fun <R: Railroad> of(kClass: KClass<R>): Railroad = values.getValue(kClass)
    }

    // rent is based on number of railroads owned - $25 if one, $50 if 2, $100 if three, $200 if four
    override fun calculateRent(owner: Player, diceRoll: Dice.Roll): Int {
        if (owner.getDevelopment(this::class).isMortgaged) return 0

        return when(owner.deeds.keys.count { it is Railroad }) {
            1 -> 25
            2 -> 50
            3 -> 100
            4 -> 200
            else -> throw PropertyOwnershipException("Rent can only be calculated if ${owner.name} owns 1, 2, 3, or 4 railroads")
        }
    }
}