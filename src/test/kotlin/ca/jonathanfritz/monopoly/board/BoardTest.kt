package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.*
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BoardTest {

    @TestFactory
    fun `rolls from Go`(): List<DynamicTest> {
        return (3 until 12).map { amount ->
            DynamicTest.dynamicTest("Rolled $amount from Go") {
                val player = Player("Oscar", money = 500)
                val board = Board(Bank(), dice = FakeDice(amount))
                board.executeRound(listOf(player))
                assertEquals(amount, player.position)

                if (player.position == 10) {
                    // player can be just visiting if they landed on jail without being sent there
                    assertFalse(player.isInJail)
                }
            }
        }
    }

    @Test
    fun `rolling doubles grants another turn`() {
        val player = Player("Elmo", 500)

        // the first two rolls are doubles, followed by a non-doubles roll
        val dice = FakeDice(2, 2, 3)
        val board = Board(Bank(), dice = dice)
        board.executeRound(listOf(player))

        // player is on chance
        assertEquals(3, dice.rollCount)
        assertEquals(7, player.position)
    }

    @Test
    fun `three consecutive doubles sends player to jail`() {
        val player = Player("Abbi", 500)

        // no matter how many times this player rolls, they will always get 2 (doubles)
        val dice = FakeDice(2, 2, 2)
        val board = Board(Bank(), dice = dice)
        board.executeRound(listOf(player))

        // the player is in jail because they rolled three consecutive doubles
        // if they had not been sent to jail 2x3=6, and they would be on Oriental Avenue
        assertEquals(3, dice.rollCount)
        assertEquals(10, player.position)
        assertTrue(player.isInJail)
    }

    @Test
    fun `goToJail sets player state as expected`() {
        val player = Player("Elmo", 500)
        val board = Board(Bank())

        board.goToJail(player)

        assertTrue(player.isInJail)
        assertEquals(10, player.position)
        assertEquals(Tile.Jail::class, board.playerTile(player)::class)
    }

    @Test
    fun `a player who is in jail but pays a get out of jail early fee proceeds with their turn as expected`() {
        val config = Config()
        val startingPlayerBalance = config.getOutOfJailEarlyFeeAmount * 2
        val player = Player("Gordon", money = startingPlayerBalance)
        val bank = Bank()
        val fakeDice = FakeDice(2, 3)
        val board = Board(bank, dice = fakeDice)
        val startingBankBalance = bank.money
        board.goToJail(player)

        // this player is in jail, and will opt to pay the fee to get out early
        board.executeRound(listOf(player))

        // because they paid the fee, they are no longer in jail and proceeded around the board
        assertFalse(player.isInJail)
        assertEquals(Railroad.PennsylvaniaRailroad::class, (board.playerTile(player) as Tile.RailroadBuyable).deedClass)
        assertEquals(startingPlayerBalance - config.getOutOfJailEarlyFeeAmount, player.money)
        assertEquals(startingBankBalance + config.getOutOfJailEarlyFeeAmount, bank.money)

        // the dice were rolled twice because doubles still grant another turn if the player pays to get out of jail early
        assertEquals(2, fakeDice.rollCount)
    }

    @Test
    fun `a player who is in jail and rolls doubles is released and proceeds with their turn as expected`() {
        val config = Config()
        val startingPlayerBalance = config.getOutOfJailEarlyFeeAmount - 10
        val player = Player("Bert", money = startingPlayerBalance)
        val bank = Bank()
        val fakeDice = FakeDice(2)
        val board = Board(bank, dice = fakeDice)
        val startingBankBalance = bank.money
        board.goToJail(player)

        // this player is in jail and does not have enough money to pay the fee to pay the fee
        board.executeRound(listOf(player))

        // however they did roll doubles, so they are no longer in jail and can proceed around the board
        assertFalse(player.isInJail)
        assertEquals(Utility.ElectricCompany::class, (board.playerTile(player) as Tile.UtilityBuyable).deedClass)

        // no fee was paid
        assertEquals(startingPlayerBalance, player.money)
        assertEquals(startingBankBalance, bank.money)

        // the dice were only rolled once, because players don't get another turn if doubles are used to get out of jail
        assertEquals(1, fakeDice.rollCount)
    }

    @Test
    fun `a player who is in jail for three turns without rolling doubles, playing a card, or paying a fee is charged the fee and proceeds with their turn as expected`() {
        val config = Config()
        val player = NotUsingGetOutOfJailFreeCardPlayer("Ernie")
        val startingPlayerBalance = player.money
        val bank = Bank()
        val fakeDice = FakeDice(3, 3, 3)
        val board = Board(bank, dice = fakeDice)
        val startingBankBalance = bank.money
        board.goToJail(player)

        // player takes two turns without leaving jail
        (1 .. 2).forEach {
            board.executeRound(listOf(player))
            assertTrue(player.isInJail)
            assertEquals(Tile.Jail::class, board.playerTile(player)::class)
            assertEquals(3 - it, player.remainingTurnsInJail)
            assertEquals(startingPlayerBalance, player.money)
        }

        // on the third turn, the player is charged a fee and released, proceeding with their turn as expected
        board.executeRound(listOf(player))
        assertFalse(player.isInJail)
        assertEquals(Property.StatesAvenue::class, (board.playerTile(player) as Tile.PropertyBuyable).deedClass)
        assertEquals(startingPlayerBalance - config.getOutOfJailEarlyFeeAmount, player.money)
        assertEquals(startingBankBalance + config.getOutOfJailEarlyFeeAmount, bank.money)
    }

    private class NotUsingGetOutOfJailFreeCardPlayer(name: String): Player(name, 100) {
        override fun isPayingGetOutOfJailEarlyFee(amount: Int) = false
    }

    @Test
    fun `a player who passes go is awarded $200 salary`() {
        val player = Player("Grover")
        val playerStartingBalance = player.money
        val fakeDice = FakeDice(6)
        val bank = Bank()
        val bankStartingBalance = bank.money
        val board = Board(bank, dice = fakeDice)

        // initialize the test by moving the player to Park Place
        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.ParkPlace::class),
            Property.ParkPlace::class
        )

        // advance to Baltic Avenue, passing go in the process
        board.executeRound(listOf(player))
        assertEquals(Property.BalticAvenue::class, (board.playerTile(player) as Tile.PropertyBuyable).deedClass)
        assertEquals(playerStartingBalance + 200, player.money)
        assertEquals(bankStartingBalance - 200, bank.money)
    }

    @Test
    fun `a player who lands on go is awarded $200 salary`() {
        val player = Player("Super Grover")
        val playerStartingBalance = player.money
        val fakeDice = FakeDice(3)
        val bank = Bank()
        val bankStartingBalance = bank.money
        val board = Board(bank, dice = fakeDice)

        // initialize the test by moving the player to Park Place
        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.ParkPlace::class),
            Property.ParkPlace::class
        )

        // advance to Go; salary should be awarded for landing on the tile
        board.executeRound(listOf(player))
        assertEquals(Tile.Go::class, board.playerTile(player)::class)
        assertEquals(playerStartingBalance + 200, player.money)
        assertEquals(bankStartingBalance - 200, bank.money)
    }

    @Test
    fun `advancePlayerBy moves the player the specified number of tiles`() {
        val player = Player("Oscar")
        val board = Board(Bank())

        // the player starts on Go, so advancing 12 positions should put them on Electric Company
        val (electricCompany, passedGo) = board.advancePlayerBy(player, 12)
        assertEquals(Tile.UtilityBuyable::class, electricCompany::class)
        assertEquals(Utility.ElectricCompany::class, (electricCompany as Tile.Buyable).deedClass)
        assertFalse(passedGo)

        // moving another 25 tiles should put the player on Park Place
        assertLandedOnProperty(
            board.advancePlayerBy(player, 25),
            Property.ParkPlace::class
        )

        // moving another 4 tiles passes go and puts the player on Mediterranean Avenuue
        assertLandedOnProperty(
            board.advancePlayerBy(player, 4),
            Property.MediterraneanAvenue::class,
            expectedPassedGo = true
        )

        // moving back 2 tiles puts the player on Boardwalk without passing go (because they're going in the wrong direction)
        assertLandedOnProperty(
            board.advancePlayerBy(player, -2),
            Property.Boardwalk::class
        )

        // finally, moving forward 1 tile puts the player on Go, which counts as having passed it
        assertLandedOnTile(
            board.advancePlayerBy(player, 1),
            Tile.Go::class,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next railroad test`() {
        val player = Player("Gonger")
        val board = Board(Bank())

        // the player starts on go, so moving the player to the next railroad puts them on Reading Railroad
        assertLandedOnRailroad(
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class),
            Railroad.ReadingRailroad::class
        )

        // the next railroad is Pennsylvania Railroad
        assertLandedOnRailroad(
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class),
            Railroad.PennsylvaniaRailroad::class
        )

        // followed by B&O Railroad
        assertLandedOnRailroad(
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class),
            Railroad.BAndORailroad::class
        )

        // and then Short Line Railroad
        assertLandedOnRailroad(
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class),
            Railroad.ShortlineRailroad::class
        )

        // one more puts us back on Reading Railroad, this time having passed Go
        assertLandedOnRailroad(
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class),
            Railroad.ReadingRailroad::class,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next utility test`() {
        val player = Player("Cookie Monster")
        val board = Board(Bank())

        // the player starts on go, so moving the player to the next utility puts them on Electric Company
        assertLandedOnUtility(
            board.advancePlayerToTile(player, Tile.UtilityBuyable::class),
            Utility.ElectricCompany::class
        )

        // the next utility is Water Works
        assertLandedOnUtility(
            board.advancePlayerToTile(player, Tile.UtilityBuyable::class),
            Utility.WaterWorks::class
        )

        // one more puts us back on Electric Company, this time having passed Go
        assertLandedOnUtility(
            board.advancePlayerToTile(player, Tile.UtilityBuyable::class),
            Utility.ElectricCompany::class,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next chance test`() {
        val player = Player("Bert")
        val board = Board(Bank())

        // the player starts on go, so moving the player to the next chance leaves them on the first side
        assertLandedOnChance(
            board.advancePlayerToTile(player, Tile.Chance::class),
            1
        )

        // the next chance is on the third side
        assertLandedOnChance(
            board.advancePlayerToTile(player, Tile.Chance::class),
            3
        )

        // there's another on the fourth side
        assertLandedOnChance(
            board.advancePlayerToTile(player, Tile.Chance::class),
            4
        )

        // one more puts us back on the first side, this time having passed Go
        assertLandedOnChance(
            board.advancePlayerToTile(player, Tile.Chance::class),
            1,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to next community chest test`() {
        val player = Player("Ernie")
        val board = Board(Bank())

        // the player starts on go, so moving the player to the next community chest leaves them on the first side
        assertLandedOnCommunityChest(
            board.advancePlayerToTile(player, Tile.CommunityChest::class),
            1
        )

        // the next chance is on the second side
        assertLandedOnCommunityChest(
            board.advancePlayerToTile(player, Tile.CommunityChest::class),
            2
        )

        // there's another on the fourth side
        assertLandedOnCommunityChest(
            board.advancePlayerToTile(player, Tile.CommunityChest::class),
            4
        )

        // one more puts us back on the first side, this time having passed Go
        assertLandedOnCommunityChest(
            board.advancePlayerToTile(player, Tile.CommunityChest::class),
            1,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to property test`() {
        val player = Player("Kermit the Frog")
        val board = Board(Bank())

        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.StJamesPlace::class),
            Property.StJamesPlace::class
        )

        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.AtlanticAvenue::class),
            Property.AtlanticAvenue::class
        )

        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.Boardwalk::class),
            Property.Boardwalk::class
        )

        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.OrientalAvenue::class),
            Property.OrientalAvenue::class,
            expectedPassedGo = true
        )
    }

    @Test
    fun `advance player to railroad test`() {
        val player = Player("Oscar the Grouch")
        val board = Board(Bank())

        assertLandedOnRailroad(
            board.advancePlayerToRailroad(player, Railroad.ShortlineRailroad::class),
            Railroad.ShortlineRailroad::class
        )

        assertLandedOnRailroad(
            board.advancePlayerToRailroad(player, Railroad.ReadingRailroad::class),
            Railroad.ReadingRailroad::class,
            expectedPassedGo = true
        )
    }
}