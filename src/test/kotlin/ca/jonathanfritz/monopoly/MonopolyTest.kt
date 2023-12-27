package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MonopolyTest {

    @Test
    fun `the bank grants each player $1500 when the board is initialized`() {
        val player = Player("Big Bird")
        val bank = Bank()
        val startingMoney = bank.money
        Monopoly(listOf(player), bank = bank)
        assertEquals(1500, player.money)
        assertEquals(startingMoney - 1500, bank.money)
    }

    @Test
    fun `each player starts the game on Go`() {
        // this player is initialized on some tile other than Go (something that would only be done in a test scenario)
        val player = Player("Count von Count", position = 5)
        Monopoly(listOf(player))

        // initializing the board put the player on Go, as expected
        assertEquals(0, player.position)
    }
}