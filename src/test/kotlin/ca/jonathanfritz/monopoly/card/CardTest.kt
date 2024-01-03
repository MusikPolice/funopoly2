package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.assertLandedOnChance
import ca.jonathanfritz.monopoly.assertPlayerOn
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Tile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CardTest {

    @Test
    fun `advance to go test`() {
        val player = Player("Elmo")
        val bank = Bank()
        val board = Board(listOf(player))
        val advanceToGo = Card.AdvanceToGo

        // our player draws a Chance card
        assertLandedOnChance(
            board.advancePlayerToTile(player, Tile.Chance::class),
            1
        )

        // the card advances the player to Go, where they receive a salary
        advanceToGo.onDraw(player, bank, board)
        board.assertPlayerOn(player, Tile.Go::class)
        assertEquals(200, player.money)
    }

    @Test
    fun `go to jail test`() {
        val player = Player("Bert")
        val bank = Bank()
        val board = Board(listOf(player))

        Card.GoToJail.onDraw(player, bank, board)

        // player went directly to jail with no salary and is not "just visiting"
        assertTrue(player.isInJail)
        board.assertPlayerOn(
            player,
            Tile.Jail::class
        )
        assertEquals(0, player.money)
    }
}