package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun Board.assertPlayerOnProperty(
    player: Player,
    expectedProperty: KClass<out Property>,
) {
    val tile = this.playerTile(player)
    assertEquals(Tile.PropertyBuyable::class, tile::class)
    assertEquals(expectedProperty, (tile as Tile.Buyable).deedClass)
}

fun Board.assertPlayerOnRailroad(
    player: Player,
    expectedRailroad: KClass<out Railroad>,
) {
    val tile = this.playerTile(player)
    assertEquals(Tile.RailroadBuyable::class, tile::class)
    assertEquals(expectedRailroad, (tile as Tile.Buyable).deedClass)
}

fun Board.assertPlayerOnUtility(
    player: Player,
    expectedUtility: KClass<out Utility>,
) {
    val tile = this.playerTile(player)
    assertEquals(Tile.UtilityBuyable::class, tile::class)
    assertEquals(expectedUtility, (tile as Tile.Buyable).deedClass)
}

fun Board.assertPlayerOn(
    player: Player,
    expectedTile: KClass<out Tile>,
) {
    val tile = this.playerTile(player)
    assertEquals(expectedTile, tile::class)
}

fun Board.assertPlayerOnChance(
    player: Player,
    expectedSide: Int,
) {
    val tile = this.playerTile(player)
    assertEquals(Tile.Chance::class, tile::class)
    assertEquals(expectedSide, (tile as Tile.Chance).side)
}

fun Board.assertPlayerOnCommunityChest(
    player: Player,
    expectedSide: Int,
) {
    val tile = this.playerTile(player)
    assertEquals(Tile.CommunityChest::class, tile::class)
    assertEquals(expectedSide, (tile as Tile.CommunityChest).side)
}

class FakeDice(
    private vararg val rolls: Roll,
) : Dice() {
    var rollCount = 0

    override fun roll(): Roll {
        previous = rolls[rollCount++]
        return previous
    }
}
