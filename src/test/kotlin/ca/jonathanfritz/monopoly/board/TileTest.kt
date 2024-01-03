package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.card.CommunityChestCard
import ca.jonathanfritz.monopoly.card.Deck
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TileTest {

    @Test
    fun `income tax tile charges player upon landing`() {
        val player = Player("Big Bird", 100)
        val bank = Bank(money = 0)
        val incomeTax = Tile.IncomeTax

        incomeTax.onLanding(player, bank, Board(listOf(player)))

        assertEquals(90, player.money)
        assertEquals(10, bank.money)
    }

    @Test
    fun `luxury tax tile charges player upon landing`() {
        val player = Player("Snuffy", 200)
        val bank = Bank(money = 0)
        val luxuryTax = Tile.LuxuryTax

        luxuryTax.onLanding(player, bank, Board(listOf(player)))

        assertEquals(100, player.money)
        assertEquals(100, bank.money)
    }

    @Test
    fun `go to jail tile puts player in jail upon landing`() {
        val player = Player("Ernie")
        val bank = Bank()
        val board = Board(listOf(player))

        Tile.GoToJail.onLanding(player, bank, board)
        assertTrue(player.isInJail)
    }

    @Test
    fun `community chest draws and plays a card upon landing`() {
        val player = Player("Bert", money = 0)
        val bank = Bank(money = 100)
        val board = Board(listOf(player), communityChest = Deck(mutableListOf(CommunityChestCard.Inheritance)))

        // player draws the only available community chest card and is paid a $100 inheritance
        Tile.CommunityChest(1).onLanding(player, bank, board)
        assertEquals(100, player.money)
        assertEquals(0, bank.money)
    }

    @Test
    fun `chance draws and plays a card upon landing`() {
        val player = Player("Bert", money = 15)
        val bank = Bank(money = 0)
        val board = Board(listOf(player), chance = Deck(mutableListOf(ChanceCard.PoorTax)))

        // player draws the only available chance card and pays a $15 poor tax
        Tile.Chance(1).onLanding(player, bank, board)
        assertEquals(0, player.money)
        assertEquals(15, bank.money)
    }
}