@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.*
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ChanceCardTest {
    @Test
    fun `advance to property test`() {
        val player = Player("Big Bird", 30)
        val bank = Bank()
        val board =
            Board(
                listOf(player),
                // this chance deck is rigged to avoid moving the player
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.PoorTax,
                        ),
                    ),
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
        val board =
            Board(
                listOf(player),
                // this deck is rigged to avoid moving the player
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.PoorTax,
                        ),
                    ),
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

    @Test
    fun `advance to nearest utility while not owned`() {
        val player = Player("Grover", 151)
        val fakeDice = FakeDice(Dice.Roll(6, 1))
        val board =
            Board(
                listOf(player),
                dice = fakeDice,
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.AdvanceToNearestUtility,
                        ),
                    ),
            )

        // on his first turn, Grover will land on Chance and draw the Advance to Nearest Utility Card
        // this will advance him to Electric Company, which he will buy since it is unowned
        board.executeRound(1)
        board.assertPlayerOnUtility(player, Utility.ElectricCompany::class)
        assertEquals(1, player.money)
        assertTrue(player.isOwner(Utility.ElectricCompany::class))
    }

    @Test
    fun `advance to nearest utility while owned and pay 10x the dice roll test`() {
        val grover = Player("Grover", 100)
        val cookie = Player("Cookie Monster", 150)
        val bank = Bank()
        val fakeDice = FakeDice(Dice.Roll(6, 1), Dice.Roll(5, 5), Dice.Roll(2, 1))
        val board =
            Board(
                listOf(grover, cookie),
                bank,
                dice = fakeDice,
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.AdvanceToNearestUtility,
                        ),
                    ),
            )

        // Cookie Monster owns Electric Company
        bank.sellDeedToPlayer(Utility.ElectricCompany::class, cookie, board)
        assertTrue(cookie.isOwner(Utility.ElectricCompany::class))
        assertEquals(0, cookie.money)

        // on his first turn, Grover will land on Chance and draw the Advance to Nearest Utility Card
        // this will advance him to Electric Company, where he will pay 10x the dice roll to Cookie ($100), who owns it
        board.executeRound(1)
        board.assertPlayerOnUtility(grover, Utility.ElectricCompany::class)
        assertEquals(0, grover.money)

        // on his first turn, Cookie will roll a 3, putting him on Baltic Avenue, which he will buy for $60
        // $10 x 10 == $100 - $60 == $40
        assertEquals(40, cookie.money)
    }

    @Test
    fun `advance to nearest railroad while not owned`() {
        val player = Player("Grover", 201)
        val fakeDice = FakeDice(Dice.Roll(6, 1))
        val board =
            Board(
                listOf(player),
                dice = fakeDice,
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.AdvanceToNearestRailroad,
                        ),
                    ),
            )

        // on his first turn, Grover will land on Chance and draw the Advance to Nearest Railroad Card
        // this will advance him to Pennsylvania Railroad, which he will buy since it is unowned
        board.executeRound(1)
        board.assertPlayerOnRailroad(player, Railroad.PennsylvaniaRailroad::class)
        assertEquals(1, player.money)
        assertTrue(player.isOwner(Railroad.PennsylvaniaRailroad::class))
    }

    @Test
    fun `advance to nearest railroad while owned and pay 2x normal rent test`() {
        val grover = Player("Grover", 50)
        val cookie = Player("Cookie Monster", 200)
        val fakeDice = FakeDice(Dice.Roll(6, 1), Dice.Roll(2, 1))
        val bank = Bank()
        val board =
            Board(
                listOf(grover, cookie),
                bank,
                dice = fakeDice,
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.AdvanceToNearestRailroad,
                        ),
                    ),
            )

        // Cookie Monster owns Pennsylvania Railroad
        bank.sellDeedToPlayer(Railroad.PennsylvaniaRailroad::class, cookie, board)
        assertTrue(cookie.isOwner(Railroad.PennsylvaniaRailroad::class))
        assertEquals(0, cookie.money)

        // on his first turn, Grover will land on Chance and draw the Advance to Nearest Railroad Card
        // this will advance him to Pennsylvania Railroad, where he will pay 2x the normal rent to Cookie ($50), who owns it
        board.executeRound(1)
        board.assertPlayerOnRailroad(grover, Railroad.PennsylvaniaRailroad::class)
        assertEquals(0, grover.money)

        // on his first turn, Cookie will roll a 3, putting him on Baltic Avenue, which he will not buy for lack of funds
        // $25 x 2 == $50
        assertEquals(50, cookie.money)
    }

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
            Property.ParkPlace::class,
        )
    }

    @Test
    fun `general repairs test`() {
        val player = Player("Ernie", 5000)
        val bank = Bank()
        val board = Board(listOf(player))

        // player has partially developed the light blue properties
        bank.sellDeedToPlayer(Property.OrientalAvenue::class, player, board)
        bank.sellDeedToPlayer(Property.VermontAvenue::class, player, board)
        bank.sellDeedToPlayer(Property.ConnecticutAvenue::class, player, board)

        (1..4).forEach { _ ->
            bank.sellHouseToPlayer(Property.OrientalAvenue::class, player, board)
            bank.sellHouseToPlayer(Property.VermontAvenue::class, player, board)
            bank.sellHouseToPlayer(Property.ConnecticutAvenue::class, player, board)
        }
        bank.sellHotelToPlayer(Property.ConnecticutAvenue::class, player, board)

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
