package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.board.Tile
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
    //  consider only paying if money > highest rent on the board > $50
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

    fun developProperties(bank: Bank) {
        // railroads, utilities, and properties that already have a hotel cannot be developed
        val developableDeeds = deeds.filterNot { it.value.hasHotel }
            .map { it.key }
            .filterIsInstance<Property>()

        // attempt to determine which of the developable deeds we should build on
        developableDeeds.map { titleDeed ->
            titleDeed.colourGroup
        }.distinct().filter { ownedColourGroup ->
            // can only build if we have a monopoly on the colour group
            hasMonopoly(ownedColourGroup)
        }.flatMap { colourGroup ->
            // convert the colour group back into its constituent properties, limiting to properties we can afford to develop
            developableDeeds.filter { titleDeed ->
                // TODO: this is pretty aggressive - On round 27, Elmo spends all but $44 to build a house
                //  consider holding at least highest rent on the board in escrow
                titleDeed.colourGroup == colourGroup && titleDeed.buildingCost < money
            }
        }.sortedByDescending { candidateProperty ->
            // this is a bit inelegant - the idea here is to develop the property that yields the highest return on
            // investment. A reasonable proxy for this is the property's current rent
            candidateProperty.calculateRent(this, Dice.Roll(1, 1))
        }.firstOrNull { candidateProperty ->
            // even building rules may limit the properties that can be developed at this time
            // choose the first one that we are currently allowed to build on
            when (getDevelopment(candidateProperty::class).numHouses) {
                4 -> candidateProperty.addingHotelRespectsEvenBuildingRules(this)
                else -> candidateProperty.addingHouseRespectsEvenBuildingRules(this)
            }
        }?.let { property ->
            // if we found a property that can be developed, build a house or hotel on it as appropriate
            when(getDevelopment(property::class).numHouses) {
                4 -> bank.buildHotel(property::class, this)
                else -> bank.buildHouse(property::class, this)
            }
        }
    }

    data class Development(
        var numHouses: Int = 0,
        var hasHotel: Boolean = false,
        var isMortgaged: Boolean = false
    )
}
