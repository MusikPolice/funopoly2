package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.event.GameEvent
import kotlin.random.Random

// TODO:
//  update rules to match 2023 box edition
//  property auctions on decline to buy?
//  trading between players?
//  house rules
@Suppress("ktlint:standard:no-blank-line-in-list")
class Monopoly(
    private val players: List<Player>,

    private val rng: Random = Random.Default,

    // optional event bus for statistics collection
    private val eventBus: EventBus? = null,

    private val bank: Bank = Bank(eventBus = eventBus),

    private val board: Board = Board(players, bank, rng, eventBus = eventBus),

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
                val winner = players.firstOrNull { !it.isBankrupt() }
                println("\nGame over!")
                eventBus?.emit(GameEvent.GameEnded(winner, round, "bankruptcy"))
                return
            }
        }

        // game ended by reaching max rounds
        val winner = players.filterNot { it.isBankrupt() }.maxByOrNull { it.netWorth() }
        eventBus?.emit(GameEvent.GameEnded(winner, config.maxRounds, "max rounds reached"))
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
