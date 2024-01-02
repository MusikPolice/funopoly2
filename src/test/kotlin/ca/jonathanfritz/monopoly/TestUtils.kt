package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun assertLandedOnProperty(actual: Pair<Tile, Boolean>, expectedProperty: KClass<out Property>, expectedPassedGo: Boolean = false) {
    val (tile, passedGo) = actual
    assertEquals(Tile.PropertyBuyable::class, tile::class)
    assertEquals(expectedProperty, (tile as Tile.Buyable).deedClass)
    assertEquals(expectedPassedGo, passedGo)
}

fun Board.assertPlayerOnProperty(player: Player, expectedProperty: KClass<out Property>) {
    val tile = this.playerTile(player)
    assertEquals(Tile.PropertyBuyable::class, tile::class)
    assertEquals(expectedProperty, (tile as Tile.Buyable).deedClass)
}

fun assertLandedOnRailroad(actual: Pair<Tile, Boolean>, expectedRailroad: KClass<out Railroad>, expectedPassedGo: Boolean = false) {
    val (tile, passedGo) = actual
    assertEquals(Tile.RailroadBuyable::class, tile::class)
    assertEquals(expectedRailroad, (tile as Tile.Buyable).deedClass)
    assertEquals(expectedPassedGo, passedGo)
}

fun Board.assertPlayerOnRailroad(player: Player, expectedRailroad: KClass<out Railroad>) {
    val tile = this.playerTile(player)
    assertEquals(Tile.RailroadBuyable::class, tile::class)
    assertEquals(expectedRailroad, (tile as Tile.Buyable).deedClass)
}

fun assertLandedOnUtility(actual: Pair<Tile, Boolean>, expectedUtility: KClass<out Utility>, expectedPassedGo: Boolean = false) {
    val (tile, passedGo) = actual
    assertEquals(Tile.UtilityBuyable::class, tile::class)
    assertEquals(expectedUtility, (tile as Tile.Buyable).deedClass)
    assertEquals(expectedPassedGo, passedGo)
}

fun Board.assertPlayerOnUtility(player: Player, expectedUtility: KClass<out Utility>) {
    val tile = this.playerTile(player)
    assertEquals(Tile.UtilityBuyable::class, tile::class)
    assertEquals(expectedUtility, (tile as Tile.Buyable).deedClass)
}

fun assertLandedOnTile(
    actual: Pair<Tile, Boolean>,
    expectedTile: KClass<out Tile>,
    expectedPassedGo: Boolean = false
) {
    assertEquals(expectedTile, actual.first::class)
    assertEquals(expectedPassedGo, actual.second)
}

fun Board.assertPlayerOn(player: Player, expectedTile: KClass<out Tile>) {
    val tile = this.playerTile(player)
    assertEquals(expectedTile, tile::class)
}

fun assertLandedOnChance(actual: Pair<Tile, Boolean>, expectedSide: Int, expectedPassedGo: Boolean = false) {
    val (tile, passedGo) = actual
    assertEquals(Tile.Chance::class, tile::class)
    assertEquals(expectedSide, (tile as Tile.Chance).side)
    assertEquals(expectedPassedGo, passedGo)
}

fun Board.assertPlayerOnChance(player: Player, expectedSide: Int) {
    val tile = this.playerTile(player)
    assertEquals(Tile.Chance::class, tile::class)
    assertEquals(expectedSide, (tile as Tile.Chance).side)
}

fun assertLandedOnCommunityChest(actual: Pair<Tile, Boolean>, expectedSide: Int, expectedPassedGo: Boolean = false) {
    val (tile, passedGo) = actual
    assertEquals(Tile.CommunityChest::class, tile::class)
    assertEquals(expectedSide, (tile as Tile.CommunityChest).side)
    assertEquals(expectedPassedGo, passedGo)
}

fun Board.assertPlayerOnCommunityChest(player: Player, expectedSide: Int) {
    val tile = this.playerTile(player)
    assertEquals(Tile.CommunityChest::class, tile::class)
    assertEquals(expectedSide, (tile as Tile.CommunityChest).side)
}

class FakeDice(private vararg val amounts: Int): Dice() {
    var rollCount = 0
    override fun roll(): Roll {
        return Roll(amounts[rollCount++] - 1, 1)
    }
}