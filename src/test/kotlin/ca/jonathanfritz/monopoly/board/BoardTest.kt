package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.*
import ca.jonathanfritz.monopoly.board.Dice.*
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BoardTest {

    @Test
    fun `player who lands on Jail is just visiting`() {
        val player = Player("Abbi", 0)
        val fakeDice = FakeDice(Roll(9, 1))
        val board = Board(listOf(player), dice = fakeDice)
        board.executeRound()

        board.assertPlayerOn(player, Tile.Jail::class)
        assertFalse(player.isInJail)
    }

    @Test
    fun `rolling doubles grants another turn`() {
        val player = Player("Elmo", 200)

        // first two rolls are doubles, followed by a non-doubles roll
        val fakeDice = FakeDice(Roll(2, 2), Roll(1, 1), Roll(2, 1))
        val board = Board(listOf(player), dice = fakeDice)
        board.executeRound()

        board.assertPlayerOnProperty(player, Property.ConnecticutAvenue::class)
        assertEquals(3, fakeDice.rollCount)
    }

    @Test
    fun `three consecutive doubles sends player to jail`() {
        val player = Player("Abbi", 500)

        // no matter how many times this player rolls, they will always get 2 (doubles)
        val fakeDice = FakeDice(Roll(1, 1), Roll(1, 1), Roll(1, 1))
        val board = Board(listOf(player), dice = fakeDice)
        board.executeRound()

        // the player is in jail because they rolled three consecutive doubles
        // if they had not been sent to jail 2x3=6, and they would be on Oriental Avenue
        assertEquals(3, fakeDice.rollCount)
        assertEquals(10, player.position)
        assertTrue(player.isInJail)
    }

    @Test
    fun `goToJail sets player state as expected`() {
        val player = Player("Elmo", 500)
        val board = Board(listOf(player))

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
        val bank = Bank(money = 0)
        val fakeDice = FakeDice(Roll(1, 1), Roll(2,1))
        val board = Board(listOf(player), bank = bank, dice = fakeDice)
        board.goToJail(player)

        // this player is in jail, and will opt to pay the fee to get out early
        board.executeRound()

        // because they paid the fee, they are no longer in jail and proceeded around the board
        assertFalse(player.isInJail)
        assertEquals(Railroad.PennsylvaniaRailroad::class, (board.playerTile(player) as Tile.RailroadBuyable).deedClass)
        assertEquals(startingPlayerBalance - config.getOutOfJailEarlyFeeAmount, player.money)
        assertEquals(config.getOutOfJailEarlyFeeAmount, bank.money)

        // the dice were rolled twice because doubles still grant another turn if the player pays to get out of jail early
        assertEquals(2, fakeDice.rollCount)
    }

    @Test
    fun `a player who is in jail and rolls doubles is released and proceeds with their turn as expected`() {
        val config = Config()
        val startingPlayerBalance = config.getOutOfJailEarlyFeeAmount - 10
        val player = Player("Bert", money = startingPlayerBalance)
        val bank = Bank()
        val fakeDice = FakeDice(Roll(1, 1))
        val board = Board(listOf(player), dice = fakeDice,)
        val startingBankBalance = bank.money
        board.goToJail(player)

        // this player is in jail and does not have enough money to pay the fee to pay the fee
        board.executeRound()

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
        val player = NotUsingGetOutOfJailFreeCardPlayer("Ernie", 50)
        val bank = Bank(money = 0)
        val fakeDice = FakeDice(Roll(2,1 ), Roll(2,1 ), Roll(2,1 ))
        val board = Board(listOf(player), bank = bank, dice = fakeDice)
        board.goToJail(player)

        // player takes two turns without leaving jail
        (1 .. 2).forEach {
            board.executeRound()
            assertTrue(player.isInJail)
            assertEquals(Tile.Jail::class, board.playerTile(player)::class)
            assertEquals(3 - it, player.remainingTurnsInJail)
            assertEquals(50, player.money)
        }

        // on the third turn, the player is charged a fee and released, proceeding with their turn as expected
        board.executeRound()
        assertFalse(player.isInJail)
        assertEquals(Property.StatesAvenue::class, (board.playerTile(player) as Tile.PropertyBuyable).deedClass)
        assertEquals(0, player.money)
        assertEquals(config.getOutOfJailEarlyFeeAmount, bank.money)
    }

    private class NotUsingGetOutOfJailFreeCardPlayer(name: String, money: Int): Player(name, money) {
        override fun isPayingGetOutOfJailEarlyFee(amount: Int) = false
    }

    @Test
    fun `a player who passes go is awarded $200 salary`() {
        val player = Player("Grover")
        val fakeDice = FakeDice(Roll(5, 1))
        val bank = Bank(money = 200)
        val board = Board(listOf(player), bank = bank, dice = fakeDice)

        // initialize the test by moving the player to Park Place
        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.ParkPlace::class),
            Property.ParkPlace::class
        )

        // advance to Baltic Avenue, passing go in the process
        board.executeRound()
        assertEquals(Property.BalticAvenue::class, (board.playerTile(player) as Tile.PropertyBuyable).deedClass)
        assertEquals(200, player.money)
        assertEquals(0, bank.money)
    }

    @Test
    fun `a player who lands on go is awarded $200 salary`() {
        val player = Player("Super Grover")
        val fakeDice = FakeDice(Roll(2, 1))
        val bank = Bank(money = 200)
        val board = Board(listOf(player), bank = bank, dice = fakeDice)

        // initialize the test by moving the player to Park Place
        assertLandedOnProperty(
            board.advancePlayerToProperty(player, Property.ParkPlace::class),
            Property.ParkPlace::class
        )

        // advance to Go; salary should be awarded for landing on the tile
        board.executeRound()
        assertEquals(Tile.Go::class, board.playerTile(player)::class)
        assertEquals(200, player.money)
        assertEquals(0, bank.money)
    }

    @Test
    fun `advancePlayerBy moves the player the specified number of tiles`() {
        val player = Player("Oscar")
        val board = Board(listOf(player))

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
        val board = Board(listOf(player))

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
        val board = Board(listOf(player))

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
        val board = Board(listOf(player))

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
        val board = Board(listOf(player))

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
        val board = Board(listOf(player))

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
        val board = Board(listOf(player))

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