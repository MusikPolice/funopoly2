package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Dice
import kotlin.reflect.KClass

sealed class TitleDeed(
    val colourGroup: ColourGroup,
    val price: Int,
    val mortgageValue: Int
) {
    companion object {
        // lazy modifier breaks a circular initialization dependency between TitleDeed and its child classes
        val values: Map<KClass<out TitleDeed>, TitleDeed> by lazy {
            Property.values + Railroad.values + Utility.values
        }
        fun <T : TitleDeed> of(kClass: KClass<T>): TitleDeed = values.getValue(kClass)

    }

    // true if a player can build houses on the corresponding Tile
    abstract val isBuildable: Boolean

    abstract fun calculateRent(owner: Player, diceRoll: Dice.Roll): Int

    // houses must be built evenly, leading to two legal cases where a house can be built:
    //  1. all properties in the group have the same number of houses; or
    //  2. the target property has one fewer house than one or both of the other properties in the group, and an
    //      equal number of houses to any remaining properties in the group
    fun addingHouseRespectsEvenBuildingRules(player: Player): Boolean {
        val houseCounts = player.deeds.filter { deedDevelopment -> deedDevelopment.key.colourGroup == colourGroup }
            .map { deedDevelopment -> deedDevelopment.key::class to deedDevelopment.value.numHouses }
            .groupBy { deedHouseCount -> deedHouseCount.second }
            .map { it.key to it.value.map { deedHouseCount -> deedHouseCount.first } }
            .toMap()
        val allDeedsHaveSameHouseCount = houseCounts.keys.size == 1
        val twoHouseCountGroups = houseCounts.keys.size == 2
        val minHouses = houseCounts.keys.min()
        val maxHouses = houseCounts.keys.max()
        val minHousesIsOneLessThanMaxHouses = minHouses == maxHouses - 1
        val targetDeedHasMinHouses = houseCounts[minHouses]?.contains(this::class) == true

        return allDeedsHaveSameHouseCount || (twoHouseCountGroups && minHousesIsOneLessThanMaxHouses && targetDeedHasMinHouses)
    }

    // hotels must be built evenly
    // all properties in the monopoly group must already have either four houses or a hotel
    fun addingHotelRespectsEvenBuildingRules(player: Player): Boolean {
        return player.deeds
            .filter { deedDevelopment ->
                deedDevelopment.key.colourGroup == colourGroup
            }.map { deedDevelopment ->
                deedDevelopment.value
            }
            .all { development ->
                development.numHouses == 4 || development.hasHotel
            }
    }
}