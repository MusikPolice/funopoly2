package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.exception.*
import kotlin.math.ceil
import kotlin.reflect.KClass

class Bank (
    // https://www.hasbro.com/common/instruct/00009.pdf
    private var availableHouses: Int = 32,
    private var availableHotels: Int = 12,

    // https://www.monopolyland.com/how-much-money-in-monopoly-set/
    // TODO: apparently old sets (pre-2008) shipped with $15,140 - could be a difference that we can test
    // TODO: per the rules, the bank can never run out of money, so maybe this doesn't matter?
    var money: Int = 20580,

    // a list of all title deeds that the bank can sell to players
    private val titleDeeds: MutableList<TitleDeed> = mutableListOf(*TitleDeed.values.values.toTypedArray())
) {
    fun pay(amount: Int, player: Player, reason: String = "") {
        if (amount < 0) throw IllegalArgumentException("Amount to pay must be greater than $0")

        println("\t\t${player.name} receives \$$amount from the bank $reason")
        money -= amount
        player.money += amount
    }

    fun charge(amount: Int, player: Player, board: Board, reason: String = "") {
        if (amount < 0) throw IllegalArgumentException("Amount to charge must be greater than $0")
        if (player.money < amount) try {
            println("\t\t${player.name} needs \$$amount $reason but only has \$${player.money}. Attempting to liquidate assets")
            player.liquidateAssets(amount, this, board)
        } catch (ex: BankruptcyException) {
            player.declareBankruptcy(this, board)
            return
        }

        println("\t\t${player.name} pays \$$amount $reason")
        money += amount
        player.money -= amount
    }

    fun deed(deedClass: KClass<out TitleDeed>): TitleDeed? = titleDeeds.firstOrNull { it::class == deedClass }

    fun mortgageDeed(deedClass: KClass<out TitleDeed>, player: Player) {
        val deed = player.deeds.keys.firstOrNull { it::class == deedClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${deedClass.simpleName}")

        val development = player.deeds.getValue(deed)
        if (development.isMortgaged || development.hasHotel || development.numHouses > 0) {
            throw PropertyDevelopmentException("Cannot mortgage ${deedClass.simpleName}. The deed has already been developed or mortgaged")
        }

        pay(deed.mortgageValue, player, "for mortgaging ${deedClass.simpleName}")
        player.deeds.getValue(deed).isMortgaged = true
    }

    fun sellDeedToPlayer(deedClass: KClass<out TitleDeed>, player: Player, board: Board) {
        val deed = titleDeeds.firstOrNull { it::class == deedClass }
            ?: throw PropertyOwnershipException("Bank does not have ${deedClass.simpleName}")
        val purchasePrice = validatePlayerCanAffordDeed(player, deed)
        charge(purchasePrice, player, board, "to buy ${deedClass.simpleName}")
        titleDeeds.remove(deed)
        player.deeds[deed] = Player.Development()
    }

    // TODO: houses can be built out of turn, potentially triggering an auction for
    //  remaining houses if two or more players express intent to build them at the
    //  same time https://boardgames.stackexchange.com/questions/25411/monopoly-houses-bidding
    fun sellHouseToPlayer(propertyClass: KClass<out Property>, player: Player, board: Board) {
        // player must own the property
        val deed = player.deeds.keys.firstOrNull { it::class == propertyClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${propertyClass.simpleName}")

        // and have a monopoly on that property's colour group
        if (!player.hasMonopoly(deed.colourGroup))
            throw MonopolyOwnershipException("${player.name} does not have monopoly on ${deed.colourGroup}")

        if (!deed.addingHouseRespectsEvenBuildingRules(player))
            throw PropertyDevelopmentException("House placement on ${deed::class.simpleName} is illegal")

        // a maximum of four houses can be built on a property
        val development = player.getDevelopment(propertyClass)
        if (development.numHouses == 4)
            throw PropertyDevelopmentException("${player.name} can build at most 4 houses on ${propertyClass.simpleName}")

        // the bank has a limited number of houses to sell
        if (availableHouses <= 0) throw InsufficientTokenException("The bank does not have any houses to sell")

        // player must have the funds - for simplicity's sake, we don't allow implicit liquidation of assets to fund development
        val buildingCost = validatePlayerCanAffordDevelopment(player, deed)

        // actually build the house
        charge(buildingCost, player, board, "to build a house on ${propertyClass.simpleName}")
        availableHouses -= 1
        player.deeds[deed]?.let {
            it.numHouses += 1
        }
    }

    fun buyHouseFromPlayer(propertyClass: KClass<out Property>, player: Player) {
        // player must own the property
        val deed = player.deeds.keys.firstOrNull { it::class == propertyClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${propertyClass.simpleName}")

        // there must be a house to sell
        val development = player.getDevelopment(propertyClass)
        if (development.hasHotel)
            throw PropertyDevelopmentException("${player.name} must sell the hotel on ${propertyClass.simpleName} before selling any houses")
        if (development.numHouses == 0)
            throw PropertyDevelopmentException("${player.name} cannot sell a house that does not exist on ${propertyClass.simpleName}")

        // even building rules apply
        if (!deed.removingHouseRespectsEvenBuildingRules(player))
            throw PropertyDevelopmentException("House sale from ${deed::class.simpleName} is illegal")

        // the player returns the house to the bank and is paid half the building's value
        pay(
            ceil((deed as Property).buildingCost / 2f).toInt(),
            player,
            "for selling a house on ${propertyClass.simpleName}"
        )
        availableHouses += 1
        player.deeds[deed]?.let {
            it.numHouses -= 1
        }
    }

    // TODO: hotels can be built out of turn, potentially triggering an auction for
    //  remaining hotels if two or more players express intent to build them at the
    //  same time https://boardgames.stackexchange.com/questions/25411/monopoly-houses-bidding
    fun sellHotelToPlayer(propertyClass: KClass<out Property>, player: Player, board: Board) {
        // player must own the property
        val deed = player.deeds.keys.firstOrNull { it::class == propertyClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${propertyClass.simpleName}")

        // and have a monopoly on that property's colour group
        if (!player.hasMonopoly(deed.colourGroup))
            throw MonopolyOwnershipException("${player.name} does not have monopoly on ${deed.colourGroup}")

        // the target property must already have four houses on it
        if (player.deeds[deed]?.hasHotel == true)
            throw PropertyDevelopmentException("${player.name} has already built a hotel on ${propertyClass.simpleName}")
        if (player.deeds[deed]?.numHouses != 4)
            throw PropertyDevelopmentException("${player.name} must build 4 houses on ${propertyClass.simpleName} before building a hotel")

        // even building rules apply
        if (!deed.addingOrRemovingHotelRespectsEvenBuildingRules(player))
            throw PropertyDevelopmentException("Properties in ${deed.colourGroup} are not sufficiently developed to allow building a hotel on ${propertyClass.simpleName}")

        // the bank must have available hotels
        if (availableHotels <= 0) throw InsufficientTokenException("The bank does not have any hotels to sell")

        // player must have the funds - for simplicity's sake, we don't allow implicit liquidation of assets to fund development
        val buildingCost = validatePlayerCanAffordDevelopment(player, deed)

        // actually build the hotel
        charge(buildingCost, player, board, "to build a hotel on ${propertyClass.simpleName}")
        availableHotels -= 1
        availableHouses += 4
        player.deeds[deed]?.let {
            it.numHouses = 0
            it.hasHotel = true
        }
    }

    fun buyHotelFromPlayer(propertyClass: KClass<out Property>, player: Player) {
        // player must own the property
        val deed = player.deeds.keys.firstOrNull { it::class == propertyClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${propertyClass.simpleName}")

        // the target property must have a hotel on it
        if (player.deeds[deed]?.hasHotel == false)
            throw PropertyDevelopmentException("${player.name} has not built a hotel on ${propertyClass.simpleName}")

        // even building rules apply
        if (!deed.addingOrRemovingHotelRespectsEvenBuildingRules(player))
            throw PropertyDevelopmentException("Properties in ${deed.colourGroup} are not sufficiently developed to allow removing a hotel from ${propertyClass.simpleName}")

        // the bank must have four available houses to replace the hotel
        if (availableHouses < 4) throw InsufficientTokenException("The bank does not have any houses available to replace the sold hotel")

        // the player returns the hotel to the bank and is paid half the building's value
        pay(
            ceil((deed as Property).buildingCost / 2f).toInt(),
            player,
            "for selling the hotel on ${propertyClass.simpleName}"
        )
        availableHotels += 1
        availableHouses -= 4
        player.deeds[deed]?.let {
            it.numHouses = 4
            it.hasHotel = false
        }
    }

    fun transferMortgagedDeeds(deeds: Set<TitleDeed>) {
        println("\t\t${deeds.joinToString(", ") { it::class.simpleName.toString() }} were returned to the bank")
        titleDeeds.addAll(deeds)

        // TODO: upon declaring bankruptcy to the bank, players return their mortgaged deeds
        //  this is meant to trigger an immediate auction wherein remaining players can bid on those (newly unmortgaged) deeds
    }

    private fun validatePlayerCanAffordDeed(player: Player, deed: TitleDeed): Int {
        if (player.money < deed.price)
            throw InsufficientFundsException("${player.name} does not have enough money for this purchase (\$${player.money} < \$${deed.price})")
        return deed.price
    }

    private fun validatePlayerCanAffordDevelopment(player: Player, deed: TitleDeed): Int {
        val buildingCost = (deed as Property).buildingCost
        if (player.money < buildingCost)
            throw InsufficientFundsException("${player.name} does not have enough money for this purchase (\$${player.money} < \$$buildingCost)")
        return buildingCost
    }
}