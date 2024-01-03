package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Tile

sealed class Card {

    // executed immediately after the card is drawn by the player
    abstract fun onDraw(player: Player, bank: Bank, board: Board)

    // Advance to "Go". (Collect $200)
    object AdvanceToGo : Card() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.advancePlayerToTile(player, Tile.Go::class)
            bank.pay(player, 200, "for passing go")
        }
    }

    // Go to Jail. Go directly to Jail. Do not pass GO, do not collect $200.
    object GoToJail: Card() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.goToJail(player)
        }
    }

    // parent class of the Get Out of Jail Free cards from the Chance and Community Chest decks respectively
    sealed class GetOutOfJailFreeCard: Card()

    open class BankPaysYou(private val amount: Int, private val message: String) : Card() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            bank.pay(player, amount, message)
        }
    }

    open class YouPayBank(private val amount: Int, private val message: String) : Card() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            bank.charge(player, amount, message)
        }
    }
}