package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.function.Executable
import kotlin.reflect.KClass

internal class BoardTest {

    @Test
    fun `the bank grants each player $1500 when the board is initialized`() {
        val player = Player("Big Bird")
        val bank = Bank()
        val startingMoney = bank.money
        Board(listOf(player), bank)
        assertEquals(1500, player.money)
        assertEquals(startingMoney - 1500, bank.money)
    }

    @Test
    fun `each player starts the game on Go`() {
        // this player is initialized on some tile other than Go (something that would only be done in a test scenario)
        val player = Player("Count von Count", position = 5)
        Board(listOf(player))

        // initializing the board put the player on Go, as expected
        assertEquals(0, player.position)
    }

    @TestFactory
    fun `rolls from Go`(): List<DynamicTest> {
        return (3 until 12).map { amount ->
            DynamicTest.dynamicTest("Rolled $amount from Go", Executable {
                val player = Player("Oscar")
                val board = Board(listOf(player), dice = FakeDice(amount))
                board.executeRound()
                assertEquals(amount, player.position)

                if (player.position == 10) {
                    // player can be just visiting if they landed on jail without being sent there
                    assertFalse(player.inJail)
                }
            })
        }
    }

    @Test
    fun `rolling doubles grants another turn`() {
        val player = Player("Elmo")

        // the first two rolls are doubles, followed by a non-doubles roll
        val dice = FakeDice(2, 2, 3)
        val board = Board(listOf(player), dice = dice)
        board.executeRound()

        // player is on chance
        assertEquals(3, dice.rollCount)
        assertEquals(7, player.position)
    }

    @Test
    fun `three consecutive doubles sends player to jail`() {
        val player = Player("Abbi")

        // no matter how many times this player rolls, they will always get 2 (doubles)
        val dice = FakeDice(2, 2, 2)
        val board = Board(listOf(player), dice = dice)
        board.executeRound()

        // the player is in jail because they rolled three consecutive doubles
        // if they had not been sent to jail 2x3=6, and they would be on Oriental Avenue
        assertEquals(3, dice.rollCount)
        assertEquals(10, player.position)
        assertTrue(player.inJail)
    }

    @Test
    fun `advancePlayerBy moves the player the specified number of tiles`() {
        val player = Player("Oscar")
        val board = Board(listOf(player))

        // the player starts on Go, so advancing 12 positions should put them on Electric Company
        val (tile, passedGo) = board.advancePlayerBy(player, 12)
        assertEquals(Board.Tile.UtilityBuyable::class, tile::class)
        assertEquals(Utility.ElectricCompany::class, (tile as Board.Tile.Buyable).deedClass)
        assertFalse(passedGo)

        // moving another 28 tiles should put the player on Go
        val (tile2, passedGo2) = board.advancePlayerBy(player, 28)
        assertEquals(Board.Tile.Go::class, tile2::class)
        assertTrue(passedGo2)
    }

    @Test
    fun `advance player to next railroad test`() {
        val player = Player("Gonger")
        val board = Board(listOf(player))

        // the player starts on go, so moving the player to the next railroad puts them on Reading Railroad
        assertLandedOnRailroad(
            board.advancePlayerToNext(player, Board.Tile.RailroadBuyable::class),
            Railroad.ReadingRailroad::class
        )

        // the next railroad is Pennsylvania Railroad
        assertLandedOnRailroad(
            board.advancePlayerToNext(player, Board.Tile.RailroadBuyable::class),
            Railroad.PennsylvaniaRailroad::class
        )

        // followed by B&O Railroad
        assertLandedOnRailroad(
            board.advancePlayerToNext(player, Board.Tile.RailroadBuyable::class),
            Railroad.BAndORailroad::class
        )

        // and then Short Line Railroad
        assertLandedOnRailroad(
            board.advancePlayerToNext(player, Board.Tile.RailroadBuyable::class),
            Railroad.ShortlineRailroad::class
        )

        // one more puts us back on Reading Railroad, this time having passed Go
        assertLandedOnRailroad(
            board.advancePlayerToNext(player, Board.Tile.RailroadBuyable::class),
            Railroad.ReadingRailroad::class,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next utility test`() {
        val player = Player("Cookie Monster")
        val board = Board(listOf(player))

        // the player starts on go, so moving the player to the next utility puts them on Electric Company
        assertLandedOnUtility(
            board.advancePlayerToNext(player, Board.Tile.UtilityBuyable::class),
            Utility.ElectricCompany::class
        )

        // the next utility is Water Works
        assertLandedOnUtility(
            board.advancePlayerToNext(player, Board.Tile.UtilityBuyable::class),
            Utility.WaterWorks::class
        )

        // one more puts us back on Electric Company, this time having passed Go
        assertLandedOnUtility(
            board.advancePlayerToNext(player, Board.Tile.UtilityBuyable::class),
            Utility.ElectricCompany::class,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next chance test`() {
        val player = Player("Bert")
        val board = Board(listOf(player))

        // the player starts on go, so moving the player to the next chance leaves them on the first side
        assertLandedOnChance(
            board.advancePlayerToNext(player, Board.Tile.Chance::class),
            1
        )

        // the next chance is on the third side
        assertLandedOnChance(
            board.advancePlayerToNext(player, Board.Tile.Chance::class),
            3
        )

        // there's another on the fourth side
        assertLandedOnChance(
            board.advancePlayerToNext(player, Board.Tile.Chance::class),
            4
        )

        // one more puts us back on the first side, this time having passed Go
        assertLandedOnChance(
            board.advancePlayerToNext(player, Board.Tile.Chance::class),
            1,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next community chest test`() {
        val player = Player("Ernie")
        val board = Board(listOf(player))

        // the player starts on go, so moving the player to the next community chest leaves them on the first side
        assertLandedOnCommunityChest(
            board.advancePlayerToNext(player, Board.Tile.CommunityChest::class),
            1
        )

        // the next chance is on the second side
        assertLandedOnCommunityChest(
            board.advancePlayerToNext(player, Board.Tile.CommunityChest::class),
            2
        )

        // there's another on the fourth side
        assertLandedOnCommunityChest(
            board.advancePlayerToNext(player, Board.Tile.CommunityChest::class),
            4
        )

        // one more puts us back on the first side, this time having passed Go
        assertLandedOnCommunityChest(
            board.advancePlayerToNext(player, Board.Tile.CommunityChest::class),
            1,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance to property test`() {
        val player = Player("Kermit the Frog")
        val board = Board(listOf(player))

        assertLandedOnProperty(
            board.advancePlayerTo(player, Property.StJamesPlace::class),
            Property.StJamesPlace::class
        )

        assertLandedOnProperty(
            board.advancePlayerTo(player, Property.AtlanticAvenue::class),
            Property.AtlanticAvenue::class
        )

        assertLandedOnProperty(
            board.advancePlayerTo(player, Property.Boardwalk::class),
            Property.Boardwalk::class
        )

        assertLandedOnProperty(
            board.advancePlayerTo(player, Property.OrientalAvenue::class),
            Property.OrientalAvenue::class,
            expectedPassedGo = true
        )
    }

    private fun assertLandedOnRailroad(actual: Pair<Board.Tile, Boolean>, expectedRailroad: KClass<out Railroad>, expectedPassedGo: Boolean = false) {
        val (tile, passedGo) = actual
        assertEquals(Board.Tile.RailroadBuyable::class, tile::class)
        assertEquals(expectedRailroad, (tile as Board.Tile.Buyable).deedClass)
        assertEquals(expectedPassedGo, passedGo)
    }

    private fun assertLandedOnUtility(actual: Pair<Board.Tile, Boolean>, expectedUtility: KClass<out Utility>, expectedPassedGo: Boolean = false) {
        val (tile, passedGo) = actual
        assertEquals(Board.Tile.UtilityBuyable::class, tile::class)
        assertEquals(expectedUtility, (tile as Board.Tile.Buyable).deedClass)
        assertEquals(expectedPassedGo, passedGo)
    }

    private fun assertLandedOnChance(actual: Pair<Board.Tile, Boolean>, expectedSide: Int, expectedPassedGo: Boolean = false) {
        val (tile, passedGo) = actual
        assertEquals(Board.Tile.Chance::class, tile::class)
        assertEquals(expectedSide, (tile as Board.Tile.Chance).side)
        assertEquals(expectedPassedGo, passedGo)
    }

    private fun assertLandedOnCommunityChest(actual: Pair<Board.Tile, Boolean>, expectedSide: Int, expectedPassedGo: Boolean = false) {
        val (tile, passedGo) = actual
        assertEquals(Board.Tile.CommunityChest::class, tile::class)
        assertEquals(expectedSide, (tile as Board.Tile.CommunityChest).side)
        assertEquals(expectedPassedGo, passedGo)
    }

    private fun assertLandedOnProperty(actual: Pair<Board.Tile, Boolean>, expectedProperty: KClass<out Property>, expectedPassedGo: Boolean = false) {
        val (tile, passedGo) = actual
        assertEquals(Board.Tile.PropertyBuyable::class, tile::class)
        assertEquals(expectedProperty, (tile as Board.Tile.Buyable).deedClass)
        assertEquals(expectedPassedGo, passedGo)
    }

    private class FakeDice(vararg val rolls: Int): Dice() {
        var rollCount = 0;
        override fun roll(): Roll {
            return Roll(rolls[rollCount++] - 1, 1)
        }
    }
}