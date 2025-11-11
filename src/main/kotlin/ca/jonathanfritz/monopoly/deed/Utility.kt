package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Board
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
    override fun calculateRent(owner: Player, board: Board): Int {
        if (owner.getDevelopment(this::class).isMortgaged) return 0

        // mortgaged utilities are not included in the count
        val numUnmortgagedUtilities = owner.deeds.filter {
            it.key is Utility
        }.filterNot {
            it.value.isMortgaged
        }.count()

        return board.dice.previousRoll().amount * if (numUnmortgagedUtilities == 2) 10 else 4
    }
}