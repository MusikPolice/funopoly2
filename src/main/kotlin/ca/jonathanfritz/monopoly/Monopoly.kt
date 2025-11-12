package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import kotlin.random.Random

// TODO:
//  implement asset transfer on bankruptcy
//  update rules to match 2023 box edition
//  start collecting stats on landings, rounds, net worth deltas, etc
//  property auctions on decline to buy?
//  trading between players?
//  house rules
class Monopoly(
    private val players: List<Player>,
    private val rng: Random = Random.Default,
    private val bank: Bank = Bank(),
    private val board: Board = Board(players, rng = rng),
    private val config: Config = Config(),
) {
    init {
        println("Starting a new game with ${players.size} players:")
        players.forEach { player ->
            // the bank grants each player $1500 starting cash
            bank.pay(1500, player, "in starting salary")

            // each player starts on Go
            player.position = 0
        }
    }

    fun executeGame() {
        (1..config.maxRounds).forEach { round ->
            board.executeRound(round)

            // if all but one player has been bankrupted, the game is over
            if (players.count { it.isBankrupt() } == players.size - 1) {
                println("\nGame over!")
                return
            }
        }
    }

    // TODO: add properties here that change gameplay to reflect deviations from the official rules that we want to simulate
    data class Config(
        val maxRounds: Int = 100,
    )
}

fun main() {
    Monopoly(
        listOf(
            Player("Elmo"),
            Player("Bert"),
            Player("Ernie"),
            Player("Cookie Monster"),
        ),
        Random(1), // for now, play the same game over and over to verify functionality
    ).executeGame()
}
