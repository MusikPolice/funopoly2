package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.*
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChanceCardTest {

    @Test
    fun `advance to property test`() {
        val player = Player("Big Bird", 30)
        val bank = Bank()
        val board = Board(
            listOf(player),

            // this chance deck is rigged to avoid moving the player
            chance = Deck(mutableListOf(
                ChanceCard.PoorTax
            ))
        )

        // our player draws a Chance card
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 1)
        assertEquals(15, player.money)

        // the card advances the player to Illinois Ave.
        val advanceToIllinoisAve = ChanceCard.AdvanceToProperty(Property.IllinoisAvenue::class)
        advanceToIllinoisAve.onDraw(player, bank, board)
        board.assertPlayerOnProperty(player, Property.IllinoisAvenue::class)
        assertEquals(15, player.money)

        // our player draws another Chance card
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 4)
        assertEquals(0, player.money)

        // the card advances the player to St. Charles Place
        val advanceToStCharlesPlace = ChanceCard.AdvanceToProperty(Property.StCharlesPlace::class)
        advanceToStCharlesPlace.onDraw(player, bank, board)
        board.assertPlayerOnProperty(player, Property.StCharlesPlace::class)

        // the player collects a $200 salary and immediately spends $140 of it to buy St. Charles Place
        assertEquals(60, player.money)
    }

    @Test
    fun `advance to railroad test`() {
        val player = Player("Grover", 15)
        val bank = Bank()
        val board = Board(
            listOf(player),

            // this deck is rigged to avoid moving the player
            chance = Deck(mutableListOf(
                ChanceCard.PoorTax
            ))
        )

        // our player draws a Chance card
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 1)

        // the card advances the player to Reading Railroad, passing go on the way
        val advanceToReadingRailroad = ChanceCard.AdvanceToRailroad(Railroad.ReadingRailroad::class)
        advanceToReadingRailroad.onDraw(player, bank, board)
        board.assertPlayerOnRailroad(player, Railroad.ReadingRailroad::class)
        assertEquals(200, player.money)

        // but if the player is already on Mediterranean Avenue...
        board.advancePlayerToProperty(player, Property.MediterraneanAvenue::class)
        board.assertPlayerOnProperty(player, Property.MediterraneanAvenue::class)

        // ...they get a $200 salary, immediately spend $60 of it to buy Mediterranean Avenue...
        assertEquals(340, player.money)

        // ...then advancing to Reading Railroad does not pay out a salary because the player doesn't pass go
        advanceToReadingRailroad.onDraw(player, bank, board)
        board.assertPlayerOnRailroad(player, Railroad.ReadingRailroad::class)

        // but they do buy the railroad for $200
        assertEquals(140, player.money)
    }

    // TODO: AdvanceToNearestUtility and AdvanceToNearestRailroad tests

    @Test
    fun `bank pays you dividend test`() {
        assertBankPaysPlayer(ChanceCard.BankPaysYouDividend, 50)
    }

    @Test
    fun `get out of jail free card test`() {
        val player = Player("Abbi")
        val bank = Bank()
        val board = Board(listOf(player))

        // grant the player a get out of jail free card
        ChanceCard.GetOutOfJailFree.onDraw(player, bank, board)
        assertFalse(board.chance.contains(ChanceCard.GetOutOfJailFree))

        // if the player is in jail, they will play the newly granted card
        player.isInJail = true
        assertEquals(ChanceCard.GetOutOfJailFree, player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `go back three spaces test`() {
        val player = Player("Snuffy")
        val bank = Bank()
        val board = Board(listOf(player))

        ChanceCard.GoBackThreeSpaces.onDraw(player, bank, board)
        board.assertPlayerOnProperty(
            player,
            Property.ParkPlace::class
        )
    }

    @Test
    fun `general repairs test`() {
        val player = Player("Ernie", 5000)
        val bank = Bank()
        val board = Board(listOf(player))

        // player has partially developed the light blue properties
        bank.sellPropertyToPlayer(Property.OrientalAvenue::class, player)
        bank.sellPropertyToPlayer(Property.VermontAvenue::class, player)
        bank.sellPropertyToPlayer(Property.ConnecticutAvenue::class, player)

        (1 .. 4).forEach { _ ->
            bank.buildHouse(Property.OrientalAvenue::class, player)
            bank.buildHouse(Property.VermontAvenue::class, player)
            bank.buildHouse(Property.ConnecticutAvenue::class, player)
        }
        bank.buildHotel(Property.ConnecticutAvenue::class, player)

        val playerBalance = player.money

        // 8 houses and a hotel costs the player a total of 300
        ChanceCard.GeneralRepairs.onDraw(player, bank, board)
        assertEquals(playerBalance - 300, player.money)
    }

    @Test
    fun `poor tax test`() {
        assertPlayerPaysBank(ChanceCard.PoorTax, 15)
    }

    @Test
    fun `chairman of the board test`() {
        val source = Player("Bert", 100)
        val target1 = Player("Ernie")
        val target2 = Player("Big Bird")
        val board = Board(listOf(source, target1, target2))

        ChanceCard.ChairmanOfTheBoard.onDraw(source, Bank(), board)
        assertEquals(0, source.money)
        assertEquals(50, target1.money)
        assertEquals(50, target2.money)
    }

    @Test
    fun `building and loan test`() {
        assertBankPaysPlayer(ChanceCard.BuildingAndLoan, 150)
    }

    @Test
    fun `crossword competition test`() {
        assertBankPaysPlayer(ChanceCard.CrosswordCompetition, 100)
    }
}