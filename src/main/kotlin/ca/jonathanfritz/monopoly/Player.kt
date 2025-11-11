package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.card.Card
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.exception.BankruptcyException
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KClass

@Suppress("ktlint:standard:no-blank-line-in-list")
open class Player(
    // this player's name, for display purposes only
    val name: String,

    // the amount of money that this player has
    var money: Int = 0,

    // the player's position on the board
    var position: Int = 0,

    // the properties that this player owns, along with their development state
    val deeds: MutableMap<TitleDeed, Development> = mutableMapOf(),

    // true if the player has fully liquidated all assets and does not have enough money to cover a debt
    private var isBankrupt: Boolean = false,

    // any Get out of Jail Free cards that the player has in their inventory
    private val getOutOfJailFreeCards: MutableList<Card.GetOutOfJailFreeCard> = mutableListOf(),
) {
    // true if the player is in jail (as opposed to just visiting)
    var remainingTurnsInJail = 0
        private set
    var isInJail: Boolean = false
        set(value) {
            remainingTurnsInJail =
                if (value) {
                    3
                } else {
                    0
                }
            field = value
        }

    // expose isBankrupt as read-only
    fun isBankrupt() = isBankrupt

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

    fun getDevelopment(deedClass: KClass<out TitleDeed>): Development =
        deeds.entries.firstOrNull { it.key::class == deedClass }?.value
            ?: throw PropertyOwnershipException("$name does not own ${deedClass.simpleName}")

    // a player has a monopoly on a property set if they own all properties that belong to that set
    fun hasMonopoly(colourGroup: ColourGroup) =
        deeds.keys
            .filter { deed ->
                deed.colourGroup == colourGroup
            }.map {
                it::class
            }.containsAll(
                colourGroup.titleDeeds().values.map { it::class },
            )

    // net worth is all cash on hand, plus the price of all owned properties, plus the price of all developed buildings
    fun netWorth(): Int =
        money +
            deeds.keys.sumOf { it.price } +
            deeds
                .filter { it.key.isBuildable }
                .map { (deed, development) ->
                    val buildingCost = (deed as Property).buildingCost
                    val developments = development.numHouses + if (development.hasHotel) 1 else 0
                    buildingCost * developments
                }.sum()

    // income tax is the lesser of $200 or 10% of net worth
    fun incomeTaxAmount(): Int = ceil(min(200.0, netWorth() * 0.10)).toInt()

    // give the player a get out of jail free card for later use
    fun grantGetOutOfJailFreeCard(card: Card.GetOutOfJailFreeCard) = getOutOfJailFreeCards.add(card)

    fun hasGetOutOfJailFreeCard(): Boolean = getOutOfJailFreeCards.isNotEmpty()

    // returns an instance of a Get out of Jail Free card if the player intends to use one, else null
    fun useGetOutOfJailFreeCard(): Card.GetOutOfJailFreeCard? {
        if (isInJail && hasGetOutOfJailFreeCard()) {
            println("\t\t$name uses a Get out of Jail Free card")
            return getOutOfJailFreeCards.removeAt(0)
        }
        return null
    }

    // returns true if the player intends to pay a fine to get out of jail on this turn
    // TODO: there are some cases in which the player should stay in jail rather than paying the fine
    //  consider only paying if money > highest rent on the board > $50
    open fun isPayingGetOutOfJailEarlyFee(amount: Int) =
        isInJail && !hasGetOutOfJailFreeCard() && remainingTurnsInJail > 0 && money > amount

    // returns a Pair<num houses, num hotels> that includes developments on all owned properties
    fun countDevelopments(): Pair<Int, Int> = deeds.values.sumOf { it.numHouses } to deeds.values.sumOf { (if (it.hasHotel) 1 else 0) }

    fun pay(
        amount: Int,
        other: Player,
        bank: Bank,
        board: Board,
        reason: String = "",
    ) {
        if (amount < 0) throw IllegalArgumentException("Amount to pay must be greater than $0")
        if (money < amount) {
            try {
                liquidateAssets(amount, bank, board)
            } catch (_: BankruptcyException) {
                declareBankruptcy(other)
                return
            }
        }

        println($$"\t\t$$name pays $${other.name} $$$amount $$reason")
        money -= amount
        other.money += amount
    }

    // for now, every player buys up every property they can
    // TODO: in the future, consider the amount of money on hand, maybe liquidate to raise money to complete a monopoly, etc
    fun isBuying(deed: TitleDeed): Boolean = money > deed.price

    fun developProperties(
        bank: Bank,
        board: Board,
    ) {
        // railroads, utilities, and properties that already have a hotel cannot be developed
        val developableDeeds =
            deeds
                .filterNot { it.value.hasHotel }
                .map { it.key }
                .filterIsInstance<Property>()

        // attempt to determine which of the developable deeds we should build on
        developableDeeds
            .map { titleDeed ->
                titleDeed.colourGroup
            }.distinct()
            .filter { ownedColourGroup ->
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
                candidateProperty.calculateRent(this, board)
            }.firstOrNull { candidateProperty ->
                // even building rules may limit the properties that can be developed at this time
                // choose the first one that we are currently allowed to build on
                when (getDevelopment(candidateProperty::class).numHouses) {
                    4 -> candidateProperty.addingOrRemovingHotelRespectsEvenBuildingRules(this)
                    else -> candidateProperty.addingHouseRespectsEvenBuildingRules(this)
                }
            }?.let { property ->
                // if we found a property that can be developed, build a house or hotel on it as appropriate
                when (getDevelopment(property::class).numHouses) {
                    4 -> bank.sellHotelToPlayer(property::class, this, board)
                    else -> bank.sellHouseToPlayer(property::class, this, board)
                }
            }
    }

    // when this is called, the player will attempt to mortgage or sell enough assets to cover the specified amount
    // TODO: this code can be tidied up
    fun liquidateAssets(
        requiredAmount: Int,
        bank: Bank,
        board: Board,
    ) {
        // Step 1: attempt to cover the required amount by mortgaging properties that are not a part of a monopoly
        // we earn double rent from and can develop monopolies, so it's best not to break them up if possible
        deeds
            .filterNot { titleDevelopment ->
                hasMonopoly(titleDevelopment.key.colourGroup)
            }.selectDeedsToMortgage()
            .takeWhile { deed ->
                bank.mortgageDeed(deed::class, this)
                money < requiredAmount
            }
        if (money >= requiredAmount) return

        do {
            // Step 2: attempt to cover the remaining amount by selling houses and hotels
            // selling houses/hotels nets half of the building's purchase value and must adhere to even building rules
            deeds
                .filter { it.value.hasHotel || it.value.numHouses > 0 }
                .filter { it.key is Property }
                .map { (it.key as Property) to it.value.hasHotel }
                .sortedBy {
                    // sell off buildings that net the lowest rent first to minimize income loss
                    it.first.calculateRent(this, board)
                }.filter { (deed, hasHotel) ->
                    if (hasHotel) {
                        deed.addingOrRemovingHotelRespectsEvenBuildingRules(this)
                    } else {
                        deed.removingHouseRespectsEvenBuildingRules(this)
                    }
                }.takeWhile { (deed, hasHotel) ->
                    if (hasHotel) {
                        bank.buyHotelFromPlayer(deed::class, this)
                    } else {
                        bank.buyHouseFromPlayer(deed::class, this)
                    }
                    money < requiredAmount
                }
            if (money >= requiredAmount) return

            // Step 3: second pass at attempting to mortgage properties
            // this time, only consider properties that are a part of a monopoly and have been newly undeveloped
            deeds
                .filter { titleDevelopment ->
                    hasMonopoly(titleDevelopment.key.colourGroup)
                }.selectDeedsToMortgage()
                .takeWhile { deed ->
                    bank.mortgageDeed(deed::class, this)
                    money < requiredAmount
                }
            if (money >= requiredAmount) return
        } while (!hasFullyLiquidatedAssets())

        // if all else fails, this player is bankrupt

        // TODO: the target player and/or bank needs to get all of our assets

        println($$"\t\t$$name owes $$$requiredAmount but has liquidated all assets and only has $$$money remaining")
        throw BankruptcyException($$"$$name has insufficient funds ($$$money < $$$requiredAmount)")
    }

    private fun hasFullyLiquidatedAssets(): Boolean =
        deeds.isEmpty() ||
            deeds.values.all { development ->
                development.numHouses == 0 && !development.hasHotel && development.isMortgaged
            }

    fun declareBankruptcy(
        bank: Bank,
        board: Board,
    ) {
        if (!hasFullyLiquidatedAssets()) {
            throw IllegalStateException("$name has declared bankruptcy without first liquidating their assets")
        }

        // money
        bank.charge(money, this, board, "in the bankruptcy settlement")

        // cards
        while (hasGetOutOfJailFreeCard()) board.returnGetOutOfJailFreeCard(this.getOutOfJailFreeCards.removeAt(0))

        // deeds - this is meant to trigger an auction
        bank.transferMortgagedDeeds(this.deeds.keys)
        this.deeds.clear()

        isBankrupt = true
        println("\t\t$name is bankrupt!")
    }

    private fun declareBankruptcy(player: Player) {
        if (!hasFullyLiquidatedAssets()) {
            throw IllegalStateException("$name has declared bankruptcy without first liquidating their assets")
        }

        // TODO: transfer this player's assets to other; other must immediately pay a penalty on mortgaged properties
        //  ideally, other player immediately unmortgages all newly acquired properties, but can elect to pay a 10% fee
        //  to the bank to assume the mortgage

        isBankrupt = true
        println("\t\t$name is bankrupt!")
    }

    private fun Map<TitleDeed, Development>.selectDeedsToMortgage(): List<TitleDeed> =
        this
            .filterNot { titleDevelopment ->
                // we can't mortgage a property that has already been mortgaged
                titleDevelopment.value.isMortgaged
            }.filterNot { titleDevelopment ->
                // we can't mortgage a property that has been developed
                titleDevelopment.value.hasHotel || titleDevelopment.value.numHouses > 0
            }.map { titleDevelopment ->
                titleDevelopment.key
            }.sortedWith(
                compareByDescending<TitleDeed> { deed ->
                    // the number of properties that need to be purchased before we have a monopoly on this colour group
                    // intent here is to avoid mortgaging properties that are most likely to form a monopoly in future turns
                    val numDeedsInColourGroup =
                        deed.colourGroup
                            .titleDeeds()
                            .values
                            .count()
                    val numOwnedDeedsInColourGroup = deeds.keys.count { it.colourGroup == deed.colourGroup }
                    numDeedsInColourGroup - numOwnedDeedsInColourGroup
                }.thenBy { deed ->
                    // we want to mortgage the property with the smallest value first to make it easier to pay off the debt
                    // this also means that cheaper properties that yield smaller rents are most likely to be mortgaged
                    deed.mortgageValue
                },
            )

    data class Development(
        var numHouses: Int = 0,
        var hasHotel: Boolean = false,
        var isMortgaged: Boolean = false,
    )
}
