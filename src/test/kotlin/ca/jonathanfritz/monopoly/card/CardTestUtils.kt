package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import org.junit.jupiter.api.Assertions

fun assertBankPaysPlayer(card: Card, expectedAmount: Int) {
    val player = Player("Big Bird", money = 0)
    val bank = Bank(money = expectedAmount)
    val board = Board(listOf(player), bank)
    card.onDraw(player, bank, board)
    Assertions.assertEquals(expectedAmount, player.money)
    Assertions.assertEquals(0, bank.money)
}

fun assertPlayerPaysBank(card: Card, expectedAmount: Int) {
    val player = Player("Big Bird", money = expectedAmount)
    val bank = Bank(money = 0)
    val board = Board(listOf(player), bank)
    card.onDraw(player, bank, board)
    Assertions.assertEquals(0, player.money)
    Assertions.assertEquals(expectedAmount, bank.money)
}