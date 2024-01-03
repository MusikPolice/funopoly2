package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import kotlin.reflect.KClass

sealed class Utility(): TitleDeed(ColourGroup.Utilities, 150, 75) {

    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    class ElectricCompany : Utility()
    class WaterWorks : Utility()

    override val isBuildable: Boolean = false

    companion object {
        val values = Utility::class.sealedSubclasses.associateWith { it.constructors.first().call() }
        fun <U: Utility> of(kClass: KClass<U>) = values.getValue(kClass)
    }

    // if one utility is owned, rent is 4x dice roll; if both are owned, rent is 10x dice roll
    override fun calculateRent(owner: Player, diceRoll: Dice.Roll): Int {
        if (owner.getDevelopment(this::class).isMortgaged) return 0
        return diceRoll.amount * if (owner.hasMonopoly(ColourGroup.Utilities)) 10 else 4
    }
}