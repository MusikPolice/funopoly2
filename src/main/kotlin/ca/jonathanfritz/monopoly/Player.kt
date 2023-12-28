package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KClass

open class Player(
    // this player's name, for display purposes only
    val name: String,

    // the amount of money that this player has
    var money: Int = 0,

    // the player's position on the board
    var position: Int = 0,

    // the properties that this player owns, along with their development state
    val deeds: MutableMap<TitleDeed, Development> = mutableMapOf()
) {

    // true if the player is in jail (as opposed to just visiting)
    var remainingTurnsInJail = 0
        private set
    var isInJail: Boolean = false
        set(value) {
            remainingTurnsInJail = if (value) { 3 } else { 0 }
            field = value
        }
    fun decrementRemainingTurnsInJail(): Int {
        if (isInJail && remainingTurnsInJail > 0) {
            if (remainingTurnsInJail == 1) {
                isInJail = false
            } else {
                return --remainingTurnsInJail
            }
        }
        return 0
    }

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

    // returns true if the player has a get out of jail free card and intends to use it
    fun isUsingGetOutOfJailFreeCard(): Boolean {
        // TODO: update this once the player can draw and hold a get out of jail free card from chance/community chest
        return false
    }

    // returns true if the player intends to pay a fine to get out of jail on this turn
    // TODO: there are some cases in which the player should stay in jail rather than paying the fine
    open fun isPayingGetOutOfJailEarlyFee(amount: Int) = isInJail && remainingTurnsInJail > 0 && money > amount

    // TODO: rent calculation and logic dictating whether a house or hotel can be purchased will live inside of this Development object
    //  figure out how to generalize it for all types of TitleDeed, possibly with a when over sealed class type
    data class Development(
        val numHouses: Int = 0,
        val hotel: Boolean = false,
        val mortgaged: Boolean = false
    )
}
