package ca.jonathanfritz.monopoly

import kotlin.reflect.KClass

sealed class Utility(): TitleDeed(ColourGroup.Utilities, 150, 75) {

    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    class ElectricCompany(): Utility()
    class WaterWorks(): Utility()

    companion object {
        val values = Utility::class.sealedSubclasses.associateWith { it.constructors.first().call() }
        fun <U: Utility> of(kClass: KClass<U>) = values.getValue(kClass)
    }

    // TODO: if one utility is owned, rent is 4x dice roll; if both are owned, rent is 10x dice roll
}