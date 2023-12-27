package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TileTest {

    @Test
    fun `income tax tile charges player on landing`() {
        val player = Player("Big Bird", 100)
        val bank = Bank()
        val incomeTax = Tile.IncomeTax()

        incomeTax.onLanding(player, bank)

        assertEquals(90, player.money)
    }
}