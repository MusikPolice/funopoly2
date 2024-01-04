package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import kotlin.random.Random

// TODO:
//  players purchase property when landing on a buyable
//  rent calculations
//  developing properties for fun and profit
//  Player.liquidateAssets(amount: Int) should be the only thrower of InsufficientFundsException
//  trading?
//  start collecting stats on landings, rounds, networth deltas, etc
class Monopoly(
    private val players: List<Player>,
    private val rng: Random = Random.Default,
    private val bank: Bank = Bank(),
    private val board: Board = Board(players, rng = rng),
    private val config: Config = Config()
) {
    init {
        players.forEach { player ->
            // the bank grants each player $1500 starting cash
            bank.pay(player, 1500, "starting salary")

            // each player starts on Go
            player.position = 0
        }
    }

    fun executeGame() {
        (1 .. config.maxRounds).forEach { round ->
            board.executeRound(round)

            // TODO: capture some kind of game state after each round
            //  could be used for testing, debugging, post-game analysis, or eventually for animating individual games
        }
    }

    // TODO: add properties here that change gameplay to reflect deviations from the official rules that we want to simulate
    data class Config (
        val maxRounds: Int = 50
    )
}

fun main(args: Array<String>) {
    Monopoly(
        listOf(
            Player("Elmo"),
            Player("Bert"),
            Player("Ernie"),
            Player("Cookie Monster")
        ),
        Random(1), // for now, play the same game over and over to verify functionality
    ).executeGame()
}