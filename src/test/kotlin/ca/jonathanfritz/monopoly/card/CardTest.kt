@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.assertPlayerOn
import ca.jonathanfritz.monopoly.assertPlayerOnChance
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Railroad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CardTest {
    @Test
    fun `advance to go test`() {
        val player = Player("Elmo")
        val bank = Bank()
        val board =
            Board(
                listOf(player),
                // the chance deck is rigged to avoid moving the player when they land on that tile
                chance =
                    Deck(
                        mutableListOf(
                            ChanceCard.GetOutOfJailFree,
                        ),
                    ),
            )

        // our player draws a Chance card
        board.advancePlayerToTile(player, Tile.Chance::class)
        board.assertPlayerOnChance(player, 1)

        // the card advances the player to Go, where they receive a salary
        Card.AdvanceToGo.onDraw(player, bank, board)
        board.assertPlayerOn(player, Tile.Go::class)
        assertEquals(200, player.money)
    }

    @Test
    fun `go to jail test`() {
        val player = Player("Bert")
        val board =
            Board(
                listOf(player),
                chance =
                    Deck(
                        mutableListOf(
                            Card.GoToJail,
                        ),
                    ),
            )

        // get the player past Jail so they have to pass Go if sent there
        board.advancePlayerToRailroad(player, Railroad.PennsylvaniaRailroad::class)

        // advance the player to the next Chance tile where they will draw a Go to Jail card
        board.advancePlayerToTile(player, Tile.Chance::class)

        // player went directly to jail without collecting a salary
        assertTrue(player.isInJail)
        board.assertPlayerOn(player, Tile.Jail::class)
        assertEquals(0, player.money)
    }
}
