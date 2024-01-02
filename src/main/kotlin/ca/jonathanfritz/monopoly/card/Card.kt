package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board

sealed class Card {

    // executed immediately after the card is drawn by the player
    abstract fun onDraw(player: Player, bank: Bank, board: Board)
}