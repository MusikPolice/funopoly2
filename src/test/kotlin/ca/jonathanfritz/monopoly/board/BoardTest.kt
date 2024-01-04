package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.*
import ca.jonathanfritz.monopoly.board.Dice.*
import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.card.CommunityChestCard
import ca.jonathanfritz.monopoly.card.Deck
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
        board.executeRound(1)

        board.assertPlayerOn(player, Tile.Jail::class)
        assertFalse(player.isInJail)
    }

    @Test
    fun `rolling doubles grants another turn`() {
        val player = Player("Elmo", 200)

        // first two rolls are doubles, followed by a non-doubles roll
        val fakeDice = FakeDice(Roll(2, 2), Roll(1, 1), Roll(2, 1))
        val board = Board(listOf(player), dice = fakeDice)
        board.executeRound(1)

        board.assertPlayerOnProperty(player, Property.ConnecticutAvenue::class)
        assertEquals(3, fakeDice.rollCount)
    }

    @Test
    fun `three consecutive doubles sends player to jail`() {
        val player = Player("Abbi", 500)

        // no matter how many times this player rolls, they will always get 2 (doubles)
        val fakeDice = FakeDice(Roll(1, 1), Roll(1, 1), Roll(1, 1))
        val board = Board(listOf(player), dice = fakeDice)
        board.executeRound(1)

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
    fun `a player who is in jail and opts to use a get out of jail free card proceeds with their turn as expected`() {
        val player = Player("Oscar the Grouch", getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        val board = Board(listOf(player), dice = FakeDice(Roll(1, 2)), chance = Deck(mutableListOf()))
        board.goToJail(player)

        // this player is in jail and will opt to use their get out of jail free card
        board.executeRound(1)

        // player is no longer in jail and their get out of jail free card has been returned to the chance deck
        assertFalse(player.isInJail)
        assertTrue(board.chance.contains(ChanceCard.GetOutOfJailFree))

        // player has proceeded with their turn by rolling the dice and moving three spaces
        board.assertPlayerOnProperty(player, Property.StatesAvenue::class)
        assertEquals(0, player.money)
    }

    @Test
    fun `a player who is in jail and opts to use a get out of jail free card can still roll doubles`() {
        val player = Player("Oscar the Grouch", getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        val board = Board(listOf(player), dice = FakeDice(Roll(1, 1), Roll(2, 1)))
        board.goToJail(player)

        // this player is in jail and will opt to use their get out of jail free card
        board.executeRound(1)

        // player is no longer in jail and their get out of jail free card has been returned to the chance deck
        assertFalse(player.isInJail)
        assertTrue(board.chance.contains(ChanceCard.GetOutOfJailFree))

        // player has proceeded with their turn by rolling a two followed by a three, moving a total of five spaces
        board.assertPlayerOnRailroad(player, Railroad.PennsylvaniaRailroad::class)
        assertEquals(0, player.money)
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
        board.executeRound(1)

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
        board.executeRound(1)

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
            board.executeRound(1)
            assertTrue(player.isInJail)
            assertEquals(Tile.Jail::class, board.playerTile(player)::class)
            assertEquals(3 - it, player.remainingTurnsInJail)
            assertEquals(50, player.money)
        }

        // on the third turn, the player is charged a fee and released, proceeding with their turn as expected
        board.executeRound(1)
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
        val player = Player(
            "Grover",
            deeds = mutableMapOf(Property.BalticAvenue() to Player.Development())
        )
        val fakeDice = FakeDice(Roll(5, 1))
        val bank = Bank(money = 200)
        val board = Board(listOf(player), bank = bank, dice = fakeDice)

        // initialize the test by moving the player to Park Place
        board.advancePlayerToProperty(player, Property.ParkPlace::class)
        board.assertPlayerOnProperty(player, Property.ParkPlace::class)

        // advance to Baltic Avenue, passing go in the process
        board.executeRound(1)
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
        board.advancePlayerToProperty(player, Property.ParkPlace::class)
        board.assertPlayerOnProperty(player, Property.ParkPlace::class)

        // advance to Go; salary should be awarded for landing on the tile
        board.executeRound(1)
        assertEquals(Tile.Go::class, board.playerTile(player)::class)
        assertEquals(200, player.money)
        assertEquals(0, bank.money)
    }

    @Test
    fun `advance player to next railroad test`() {
        // this player has no money, so they won't buy any of the railroads that they land on
        val player = Player("Gonger")
        val board = Board(listOf(player))

        // the player starts on go, so moving the player to the next railroad puts them on Reading Railroad
        board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
        board.assertPlayerOnRailroad(player, Railroad.ReadingRailroad::class)

        // the next railroad is Pennsylvania Railroad
        board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
        board.assertPlayerOnRailroad(player, Railroad.PennsylvaniaRailroad::class)

        // followed by B&O Railroad
        board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
        board.assertPlayerOnRailroad(player, Railroad.BAndORailroad::class)

        // and then Short Line Railroad
        board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
        board.assertPlayerOnRailroad(player, Railroad.ShortlineRailroad::class)

        // one more puts us back on Reading Railroad, this time having passed Go
        board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
        board.assertPlayerOnRailroad(player, Railroad.ReadingRailroad::class)

        // because the player passed go, they were paid a salary
        assertEquals(200, player.money)
    }

    @Test
    fun `advance player to next utility test`() {
        // this player has no money, so they won't buy any of the utilities that they land on
        val player = Player("Cookie Monster")
        val board = Board(listOf(player))

        // the player starts on go, so moving the player to the next utility puts them on Electric Company
        board.advancePlayerToTile(player, Tile.UtilityBuyable::class)
        board.assertPlayerOnUtility(player, Utility.ElectricCompany::class)

        // the next utility is Water Works
        board.advancePlayerToTile(player, Tile.UtilityBuyable::class)
        board.assertPlayerOnUtility(player, Utility.WaterWorks::class)

        // one more puts us back on Electric Company, this time having passed Go
        board.advancePlayerToTile(player, Tile.UtilityBuyable::class)
        board.assertPlayerOnUtility(player, Utility.ElectricCompany::class)

        // the player passed go and was paid a $200 salary, but immediately used $150 to buy Electric Company
        assertEquals(50, player.money)
    }

    @Test
    fun `advance player to next chance test`() {
        val player = Player("Bert")
        val board = Board(
            listOf(player),

            // this board has a rigged chance deck that doesn't affect the player's board position when drawn
            chance = Deck(mutableListOf(
                ChanceCard.BankPaysYouDividend,
            ))
        )

        // the player starts on go, so moving the player to the next chance leaves them on the first side
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 1)
        assertEquals(50, player.money)

        // the next chance is on the third side
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 3)
        assertEquals(100, player.money)

        // there's another on the fourth side
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 4)
        assertEquals(150, player.money)

        // one more puts us back on the first side, this time having passed Go
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 1)

        // passing go bumped the player's cash reserves by an extra $200 over and above what they got from the card
        assertEquals(400, player.money)
    }

    @Test
    fun `advance player to next community chest test`() {
        val player = Player("Ernie")
        val board = Board(
            listOf(player),

            // this board has a rigged community chest deck that doesn't affect the player's board position when drawn
            communityChest = Deck(mutableListOf(
                CommunityChestCard.Inheritance
            ))
        )

        // the player starts on go, so moving the player to the next community chest leaves them on the first side
        board.advancePlayerToTile(player, Tile.CommunityChest::class)
        board.assertPlayerOnCommunityChest(player, 1)
        assertEquals(100, player.money)

        // the next chance is on the second side
        board.advancePlayerToTile(player, Tile.CommunityChest::class)
        board.assertPlayerOnCommunityChest(player, 2)
        assertEquals(200, player.money)

        // there's another on the fourth side
        board.advancePlayerToTile(player, Tile.CommunityChest::class)
        board.assertPlayerOnCommunityChest(player, 4)
        assertEquals(300, player.money)

        // one more puts us back on the first side, this time having passed Go
        board.advancePlayerToTile(player, Tile.CommunityChest::class)
        board.assertPlayerOnCommunityChest(player, 1)

        // passing go bumped the player's cash reserves by an extra $200 over and above what they got from the card
        assertEquals(600, player.money)
    }

    @Test
    fun `advance player to property test`() {
        // this player has no money, so they won't buy any of the properties that they land on
        val player = Player("Kermit the Frog")
        val board = Board(listOf(player))

        board.advancePlayerToProperty(player, Property.StJamesPlace::class)
        board.assertPlayerOnProperty(player, Property.StJamesPlace::class)

        board.advancePlayerToProperty(player, Property.AtlanticAvenue::class)
        board.assertPlayerOnProperty(player, Property.AtlanticAvenue::class)

        board.advancePlayerToProperty(player, Property.Boardwalk::class)
        board.assertPlayerOnProperty(player, Property.Boardwalk::class)

        board.advancePlayerToProperty(player, Property.OrientalAvenue::class)
        board.assertPlayerOnProperty(player, Property.OrientalAvenue::class)

        // having passed go, the player received a $200 salary, then immediately spent $100 of it to buy Oriental Avenue
        assertEquals(100, player.money)
    }

    @Test
    fun `advance player to specific railroad test`() {
        // this player has no money, so they won't buy any of the railroads that they land on
        val player = Player("Oscar the Grouch")
        val board = Board(listOf(player))

        board.advancePlayerToRailroad(player, Railroad.ShortlineRailroad::class)
        board.assertPlayerOnRailroad(player, Railroad.ShortlineRailroad::class)

        board.advancePlayerToRailroad(player, Railroad.ReadingRailroad::class)
        board.assertPlayerOnRailroad(player, Railroad.ReadingRailroad::class)

        // having passed go, the player received a $200 salary
        assertEquals(200, player.money)
    }

    @Test
    fun `go back three spaces test`() {
        // this player already owns Mediterranean Avenue, so they won't attempt to buy it again
        val player = Player(
            "Oscar the Grouch",
            101,
            deeds = mutableMapOf(
                Property.MediterraneanAvenue() to Player.Development()
            )
        )
        val board = Board(listOf(player))

        // this player starts on Mediterranean Avenue
        board.advancePlayerToProperty(player, Property.MediterraneanAvenue::class)
        board.assertPlayerOnProperty(player, Property.MediterraneanAvenue::class)

        // moving back three spaces puts them on Luxury Tax
        board.goBackThreeSpaces(player)
        board.assertPlayerOn(player, Tile.LuxuryTax::class)

        // having paid the tax, they have $1 left
        // this proves that they did not receive a salary for passing Go the wrong way
        assertEquals(1, player.money)
    }

    @Test
    fun `landing on two or more tiles within the space of a single turn applies the onLanding action for each`() {
        val player = Player("Grover", 160)
        val fakeDice = FakeDice(Roll(6,1))
        val board = Board(
            listOf(player),
            dice = fakeDice,

            // this rigged Chance deck will always send the player to either Electric Company or Water Works
            chance = Deck(mutableListOf(
                ChanceCard.AdvanceToNearestUtility
            ))
        )

        // the player rolls a seven, lands on Chance (side 1), then draws a card that sends them to Electric Company
        board.executeRound(1)

        // the player purchased Electric Company
        assertTrue(player.isOwner(Utility.ElectricCompany::class))
        assertEquals(10, player.money)
    }

    @Test
    fun `a player that completes a monopoly can develop the cheapest property in the group`() {
        val player = Player(
            "Big Bird",
            money = 151,
            deeds = mutableMapOf(
                Property.VermontAvenue() to Player.Development(),
                Property.ConnecticutAvenue() to Player.Development()
            )
        )
        val fakeDice = FakeDice(Roll(5, 1))
        val board = Board(listOf(player), dice = fakeDice)

        // big bird will land on Oriental Avenue, buy it, and build a house on Connecticut Avenue (it has the highest
        // rent of the three light blue properties), having completed a monopoly on light blue
        board.executeRound(1)
        board.assertPlayerOnProperty(player, Property.OrientalAvenue::class)
        assertEquals(0, player.getDevelopment(Property.OrientalAvenue::class).numHouses)
        assertEquals(0, player.getDevelopment(Property.VermontAvenue::class).numHouses)
        assertEquals(1, player.getDevelopment(Property.ConnecticutAvenue::class).numHouses)
        assertEquals(1, player.money)
    }
}