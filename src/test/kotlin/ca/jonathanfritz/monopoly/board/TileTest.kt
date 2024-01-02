package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
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
        val goToJail = Tile.GoToJail

        goToJail.onLanding(player, bank, board)
        assertTrue(player.isInJail)
    }
}