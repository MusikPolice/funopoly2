package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.statistics.StatisticsOutputFormat
import ca.jonathanfritz.monopoly.strategy.PlayerStrategy

// captures all configurable aspects of the game, including options that deviate from the official ruleset
@Suppress("ktlint:standard:no-blank-line-in-list")
data class Config(
    // the amount that a player who is in jail must pay to be released if they do not have a Get Out of Jail Free card
    // to play, if they have failed to roll doubles for three consecutive turns, or if they wish to leave jail early
    val getOutOfJailEarlyFeeAmount: Int = 50,

    // maximum number of rounds before the game ends
    val maxRounds: Int = 100,

    // whether to collect statistics during gameplay
    val collectStatistics: Boolean = true,

    // output format for statistics reports (CONSOLE or JSON)
    val statisticsOutputFormat: StatisticsOutputFormat = StatisticsOutputFormat.CONSOLE,

    // player configurations (name and strategy for each player)
    val playerConfigs: List<PlayerConfig> = emptyList(),

    // random seed for deterministic gameplay (null = non-deterministic)
    val randomSeed: Long? = null,

    // whether to conduct auctions when players decline to purchase properties
    val enableAuctions: Boolean = true,

    // starting bid for property auctions
    val auctionStartingBid: Int = 10,

    // minimum bid increment for property auctions
    val auctionMinimumIncrement: Int = 1,

    // maximum number of auction rounds before terminating
    val auctionMaxRounds: Int = 100,
)

/**
 * Configuration for a single player.
 *
 * @param name The player's display name
 * @param strategy The strategy this player will use for decision-making
 */
data class PlayerConfig(
    val name: String,
    val strategy: PlayerStrategy,
)
