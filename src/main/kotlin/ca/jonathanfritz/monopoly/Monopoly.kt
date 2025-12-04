package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.event.GameEvent
import ca.jonathanfritz.monopoly.statistics.GameStatistics
import ca.jonathanfritz.monopoly.statistics.StatisticsFormatter
import ca.jonathanfritz.monopoly.statistics.StatisticsOutputFormat
import ca.jonathanfritz.monopoly.strategy.ConservativeStrategy
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

    private val config: Config = Config(),

    // optional event bus for statistics collection (auto-created if config.collectStatistics is true)
    private val eventBus: EventBus? = if (config.collectStatistics) EventBus() else null,

    private val bank: Bank = Bank(eventBus = eventBus),

    private val board: Board = Board(players, bank, rng, eventBus = eventBus),
) {
    private val gameStatistics: GameStatistics? =
        if (config.collectStatistics && eventBus != null) {
            GameStatistics().also { eventBus.register(it) }
        } else {
            null
        }

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
                outputStatistics()
                return
            }
        }

        // game ended by reaching max rounds
        val winner = players.filterNot { it.isBankrupt() }.maxByOrNull { it.netWorth() }
        eventBus?.emit(GameEvent.GameEnded(winner, config.maxRounds, "max rounds reached"))
        outputStatistics()
    }

    private fun outputStatistics() {
        if (gameStatistics == null) return

        val report = gameStatistics.generateReport()

        when (config.statisticsOutputFormat) {
            StatisticsOutputFormat.CONSOLE -> {
                println(StatisticsFormatter.formatConsole(report))
            }

            StatisticsOutputFormat.JSON -> {
                println(StatisticsFormatter.formatJson(report))
            }
        }
    }
}

fun main() {
    val eventBus = EventBus()
    Monopoly(
        players =
            listOf(
                Player("Elmo", eventBus = eventBus),
                Player("Bert", eventBus = eventBus, strategy = ConservativeStrategy()),
                Player("Ernie", eventBus = eventBus),
                Player("Cookie Monster", eventBus = eventBus),
            ),
        rng = Random(1), // for now, play the same game over and over to verify functionality
        eventBus = eventBus,
    ).executeGame()
}
