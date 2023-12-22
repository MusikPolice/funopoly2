package ca.jonathanfritz.monopoly

import kotlin.reflect.KClass

data class Player(
    val name: String,
    private var money: Int,
    private val deeds: MutableMap<TitleDeed, Development> = mutableMapOf()
) {

    fun <T : TitleDeed> isOwner(titleDeed: KClass<T>): Boolean = deeds.keys.map { it::class }.contains(titleDeed)

    // a player has a monopoly on a property set if they own all properties that belong to that set
    fun hasMonopoly(colourGroup: ColourGroup) = deeds.keys.filter { deed ->
        deed.colourGroup == colourGroup
    }.containsAll(colourGroup.titleDeeds().values)

    // TODO: rent calculation and logic dictating whether a house or hotel can be purchased will live inside of this Development object
    //  figure out how to generalize it for all types of TitleDeed, possibly with a when over sealed class type
    data class Development(
        val numHouses: Int = 0,
        val hotel: Boolean = false,
        val mortgaged: Boolean = false
    )
}
