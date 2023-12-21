package ca.jonathanfritz.monopoly

import kotlin.reflect.KClass

data class Player(
    val name: String,
    private var money: Int,
    private val titleDeeds: MutableMap<TitleDeed, Development> = mutableMapOf()
) {

    // a player has a monopoly on a property set if they own all properties that belong to that set
    val monopolies = titleDeeds.keys.groupBy { titleDeed -> titleDeed.colourGroup }
        .filter { (colourGroup, ownedTitleDeeds) ->
            // TODO: this doesn't properly support railroads or utilities - maybe move colourGroups up into the TitleDeed class?
            ownedTitleDeeds.containsAll(Property.of(colourGroup))
        }

    fun <T : TitleDeed> isOwner(titleDeed: KClass<T>): Boolean = titleDeeds.keys.map { it::class }.contains(titleDeed)

    fun hasMonopoly(propertySet: ColourGroup) = monopolies.containsKey(propertySet)

    // TODO: rent calculation and logic dictating whether a house or hotel can be purchased will live inside of this Development object
    //  figure out how to generalize it for all types of TitleDeed, possibly with a when over sealed class type
    data class Development(
        val numHouses: Int = 0,
        val hotel: Boolean = false,
        val mortgaged: Boolean = false
    )
}
