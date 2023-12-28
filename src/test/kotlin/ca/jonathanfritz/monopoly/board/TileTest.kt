package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TileTest {

    @Test
    fun `income tax tile charges player upon landing`() {
        val player = Player("Big Bird", 100)
        val bank = Bank()
        val incomeTax = Tile.IncomeTax()
        val bankStartingBalance = bank.money

        incomeTax.onLanding(player, bank, Board(bank))

        assertEquals(90, player.money)
        assertEquals(bankStartingBalance + 10, bank.money)
    }

    @Test
    fun `luxury tax tile charges player upon landing`() {
        val player = Player("Snuffy", 200)
        val bank = Bank()
        val luxuryTax = Tile.LuxuryTax()
        val bankStartingBalance = bank.money

        luxuryTax.onLanding(player, bank, Board(bank))

        assertEquals(100, player.money)
        assertEquals(bankStartingBalance + 100, bank.money)
    }

    @Test
    fun `go to jail tile puts player in jail upon landing`() {
        val player = Player("Ernie")
        val bank = Bank()
        val board = Board(bank)
        val goToJail = Tile.GoToJail()

        goToJail.onLanding(player, bank, board)
        assertTrue(player.isInJail)
    }
}