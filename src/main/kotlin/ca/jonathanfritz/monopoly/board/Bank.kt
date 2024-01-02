package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.exception.*
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
    fun pay(player: Player, amount: Int, reason: String = "") {
        if (amount < 0) throw IllegalArgumentException("Amount to pay must be greater than $0")

        println("${player.name} receives \$$amount $reason")
        money -= amount
        player.money += amount
    }

    // TODO: rather than throwing InsufficientFundsException, give player the chance to liquidate assets or mortgage
    //  properties to cover the amount due
    fun charge(player: Player, amount: Int, reason: String = "") {
        if (amount < 0) throw IllegalArgumentException("Amount to charge must be greater than $0")
        if (player.money < amount) throw InsufficientFundsException("${player.name} has insufficient funds (${player.money} < $amount) $reason")

        println("\t\t${player.name} pays \$$amount $reason")
        money += amount
        player.money -= amount
    }

    fun useGetOutOfJailFreeCard(player: Player) {
        TODO("Not yet implemented - need chest/community chest first!")
    }

    fun sellPropertyToPlayer(deedClass: KClass<out TitleDeed>, player: Player) {
        val deed = titleDeeds.firstOrNull { it::class == deedClass }
            ?: throw PropertyOwnershipException("Bank does not have ${deedClass.simpleName}")
        charge(player, deed.price, "to buy ${deedClass.simpleName}")
        titleDeeds.remove(deed)
        player.deeds[deed] = Player.Development()
    }

    // TODO: houses can be built out of turn, potentially triggering an auction for
    //  remaining houses if two or more players express intent to build them at the
    //  same time https://boardgames.stackexchange.com/questions/25411/monopoly-houses-bidding
    fun buildHouse(propertyClass: KClass<out Property>, player: Player) {
        // player must own the property
        val deed = player.deeds.keys.firstOrNull { it::class == propertyClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${propertyClass.simpleName}")

        // and have a monopoly on that property's colour group
        if (!player.hasMonopoly(deed.colourGroup))
            throw MonopolyOwnershipException("${player.name} does not have monopoly on ${deed.colourGroup}")

        // houses must be built evenly, leading to two legal cases where a house can be built:
        //  1. all properties in the group have the same number of houses; or
        //  2. the target property has one fewer house than one or both of the other properties in the group, and an
        //      equal number of houses to any remaining properties in the group
        val houseCounts = player.deeds.filter { deedDevelopment -> deedDevelopment.key.colourGroup == deed.colourGroup }
            .map { deedDevelopment -> deedDevelopment.key::class to deedDevelopment.value.numHouses }
            .groupBy { deedHouseCount -> deedHouseCount.second }
            .map { it.key to it.value.map { deedHouseCount -> deedHouseCount.first } }
            .toMap()
        val allDeedsHaveSameHouseCount = houseCounts.keys.size == 1
        val twoHouseCountGroups = houseCounts.keys.size == 2
        val minHouses = houseCounts.keys.min()
        val maxHouses = houseCounts.keys.max()
        val minHousesIsOneLessThanMaxHouses = minHouses == maxHouses - 1
        val targetDeedHasMinHouses = houseCounts[minHouses]?.contains(deed::class) == true
        if (!allDeedsHaveSameHouseCount && !(twoHouseCountGroups && minHousesIsOneLessThanMaxHouses && targetDeedHasMinHouses)) {
            throw PropertyDevelopmentException("House placement on ${propertyClass.simpleName} is illegal")
        }

        // a maximum of four houses can be built on a property
        if (player.deeds[deed]?.numHouses == 4)
            throw PropertyDevelopmentException("${player.name} can build at most 4 houses on ${propertyClass.simpleName}")

        // the bank has a limited number of houses to sell
        if (availableHouses <= 0) throw InsufficientTokenException("The bank does not have any houses to sell")

        // actually build the house
        charge(player, (deed as Property).buildingCost, "to build a house on ${propertyClass.simpleName}")
        availableHouses -= 1
        player.deeds[deed]?.let {
            it.numHouses += 1
        }
    }

    // TODO: hotels can be built out of turn, potentially triggering an auction for
    //  remaining hotels if two or more players express intent to build them at the
    //  same time https://boardgames.stackexchange.com/questions/25411/monopoly-houses-bidding
    fun buildHotel(propertyClass: KClass<out Property>, player: Player) {
        // player must own the property
        val deed = player.deeds.keys.firstOrNull { it::class == propertyClass }
            ?: throw PropertyOwnershipException("${player.name} does not own ${propertyClass.simpleName}")

        // and have a monopoly on that property's colour group
        if (!player.hasMonopoly(deed.colourGroup)) throw MonopolyOwnershipException("${player.name} does not have monopoly on ${deed.colourGroup}")

        // the target property must already have four houses on it
        if (player.deeds[deed]?.hotel == true) throw PropertyDevelopmentException("${player.name} has already built a hotel on ${propertyClass.simpleName}")
        if (player.deeds[deed]?.numHouses != 4) throw PropertyDevelopmentException("${player.name} must build 4 houses on ${propertyClass.simpleName} before building a hotel")

        // hotels must be built evenly, so all properties in the monopoly group must have either four houses or a hotel
        if (!player.deeds.filter { deedDevelopment -> deedDevelopment.key.colourGroup == deed.colourGroup }
            .all { deedDevelopment -> deedDevelopment.value.numHouses == 4 || deedDevelopment.value.hotel })
            throw PropertyDevelopmentException("Properties in ${deed.colourGroup} are not sufficiently developed to allow building a hotel on ${propertyClass.simpleName}")

        // the bank must have available hotels
        if (availableHotels <= 0) throw InsufficientTokenException("The bank does not have any hotels to sell")

        // actually build the hotel
        charge(player, (deed as Property).buildingCost, "to build a hotel on ${propertyClass.simpleName}")
        availableHotels -= 1
        player.deeds[deed]?.let {
            it.numHouses = 0
            it.hotel = true
        }
    }
}