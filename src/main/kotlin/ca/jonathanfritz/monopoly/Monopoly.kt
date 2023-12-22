package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Board
import kotlin.random.Random

class Monopoly(
    players: List<Player>,
    rng: Random = Random.Default,
    private val board: Board = Board(players, rng = rng),
    private val config: Config = Config()
) {
    fun executeGame() {
        // TODO: unit tests for each movement scenario that is logged
        (1 .. config.maxRounds).forEach { round ->
            println("\nRound $round:")
            board.executeRound()
        }
    }

    // TODO: add properties here that change gameplay to reflect deviations from the official rules that we want to simulate
    data class Config (
        val maxRounds: Int = 10
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