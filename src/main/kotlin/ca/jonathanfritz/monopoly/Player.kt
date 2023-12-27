package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KClass

data class Player(
    // this player's name, for display purposes only
    val name: String,

    // the amount of money that this player has
    var money: Int = 0,

    // the player's position on the board
    var position: Int = 0,

    // true if the player is in jail (as opposed to just visiting)
    var inJail: Boolean = false,

    // the properties that this player owns, along with their development state
    val deeds: MutableMap<TitleDeed, Development> = mutableMapOf()
) {

    fun <T : TitleDeed> isOwner(titleDeed: KClass<T>): Boolean = deeds.keys.map { it::class }.contains(titleDeed)

    // a player has a monopoly on a property set if they own all properties that belong to that set
    fun hasMonopoly(colourGroup: ColourGroup) = deeds.keys.filter { deed ->
        deed.colourGroup == colourGroup
    }.containsAll(colourGroup.titleDeeds().values)

    // net worth is all cash on hand, plus the price of all owned properties, plus the price of all developed buildings
    fun networth(): Int {
        return money +
                deeds.keys.sumOf { it.price } +
                deeds.filter { it.key.isBuildable }.map { (deed, development) ->
                    val buildingCost = (deed as Property).buildingCost
                    val developments = development.numHouses + if (development.hotel) 1 else 0
                    buildingCost * developments
                }.sum()
    }

    // income tax is the lesser of $200 or 10% of net worth
    fun incomeTaxAmount(): Int = ceil(min(200.0, networth() * 0.10)).toInt()

    // TODO: rent calculation and logic dictating whether a house or hotel can be purchased will live inside of this Development object
    //  figure out how to generalize it for all types of TitleDeed, possibly with a when over sealed class type
    data class Development(
        val numHouses: Int = 0,
        val hotel: Boolean = false,
        val mortgaged: Boolean = false
    )
}
