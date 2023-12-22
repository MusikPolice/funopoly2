package ca.jonathanfritz.monopoly

import kotlin.reflect.KClass

sealed class TitleDeed(
    val colourGroup: ColourGroup,
    val price: Int,
    val mortgageValue: Int,
    val isUtility: Boolean = false,
    val isRailroad: Boolean = false,
    val isBuildable: Boolean = !(isUtility || isRailroad)
) {
    companion object {
        // lazy modifier breaks a circular initialization dependency between TitleDeed and its child classes
        val values: Map<KClass<out TitleDeed>, TitleDeed> by lazy {
            Property.values + Railroad.values + Utility.values
        }

        fun <T : TitleDeed> of(kClass: KClass<T>): TitleDeed = values.getValue(kClass)
    }
}