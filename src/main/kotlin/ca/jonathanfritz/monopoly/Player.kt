package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.card.Card
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.exception.InsufficientFundsException
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
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
    val deeds: MutableMap<TitleDeed, Development> = mutableMapOf(),

    // any Get out of Jail Free cards that the player has in their inventory
    private val getOutOfJailFreeCards: MutableList<Card.GetOutOfJailFreeCard> = mutableListOf()
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

    fun getDevelopment(deedClass: KClass<out TitleDeed>): Development {
        return deeds.entries.firstOrNull { it.key::class == deedClass }?.value
            ?: throw PropertyOwnershipException("$name does not own ${deedClass.simpleName}")
    }

    // a player has a monopoly on a property set if they own all properties that belong to that set
    fun hasMonopoly(colourGroup: ColourGroup) = deeds.keys
        .filter { deed ->
            deed.colourGroup == colourGroup
        }.map {
            it::class
        }.containsAll(
            colourGroup.titleDeeds().values.map { it::class }
        )

    // net worth is all cash on hand, plus the price of all owned properties, plus the price of all developed buildings
    fun netWorth(): Int {
        return money +
                deeds.keys.sumOf { it.price } +
                deeds.filter { it.key.isBuildable }.map { (deed, development) ->
                    val buildingCost = (deed as Property).buildingCost
                    val developments = development.numHouses + if (development.hasHotel) 1 else 0
                    buildingCost * developments
                }.sum()
    }

    // income tax is the lesser of $200 or 10% of net worth
    fun incomeTaxAmount(): Int = ceil(min(200.0, netWorth() * 0.10)).toInt()

    // give the player a get out of jail free card for later use
    fun grantGetOutOfJailFreeCard(card: Card.GetOutOfJailFreeCard) = getOutOfJailFreeCards.add(card)

    // returns an instance of a Get out of Jail Free card if the player intends to use one, else null
    fun useGetOutOfJailFreeCard(): Card.GetOutOfJailFreeCard? {
        if (isInJail && getOutOfJailFreeCards.isNotEmpty()) {
            println("\t\t$name uses a Get out of Jail Free card")
            return getOutOfJailFreeCards.removeAt(0)
        }
        return null
    }

    // returns true if the player intends to pay a fine to get out of jail on this turn
    // TODO: there are some cases in which the player should stay in jail rather than paying the fine
    open fun isPayingGetOutOfJailEarlyFee(amount: Int) = isInJail && getOutOfJailFreeCards.isEmpty() && remainingTurnsInJail > 0 && money > amount

    // returns a Pair<num houses, num hotels> that includes developments on all owned properties
    fun countDevelopments(): Pair<Int, Int> =
        deeds.values.sumOf { it.numHouses } to deeds.values.sumOf { (if (it.hasHotel) 1 else 0).toInt() }

    // TODO: rather than throw InsufficientFundsException here, attempt to liquidate assets or mortgage properties to
    //  to cover the amount due. Pipe bank charges through that same logic!
    fun pay(other: Player, amount: Int, reason: String = "") {
        if (amount < 0) throw IllegalArgumentException("Amount to pay must be greater than $0")
        if (money < amount) throw InsufficientFundsException("$name does not have $amount")

        println("\t\t$name pays ${other.name} \$$amount $reason")
        money -= amount
        other.money += amount
    }

    // for now, every player buys up every property they can
    // TODO: in the future, consider the amount of money on hand, maybe liquidate to raise money to complete a monopoly, etc
    fun isBuying(deed: TitleDeed): Boolean = money > deed.price

    // TODO: rent calculation and logic dictating whether a house or hotel can be purchased will live inside of this Development object
    //  figure out how to generalize it for all types of TitleDeed, possibly with a when over sealed class type
    data class Development(
        var numHouses: Int = 0,
        var hasHotel: Boolean = false,
        var isMortgaged: Boolean = false
    )
}
