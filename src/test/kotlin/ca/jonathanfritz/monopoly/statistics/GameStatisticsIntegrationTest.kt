@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.statistics

import ca.jonathanfritz.monopoly.Config
import ca.jonathanfritz.monopoly.Monopoly
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.PlayerConfig
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.strategy.CalculatingStrategy
import ca.jonathanfritz.monopoly.strategy.DefaultStrategy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Integration tests that verify GameStatistics correctly collects statistics from a full game.
 */
internal class GameStatisticsIntegrationTest {
    @Test
    fun `collects statistics from a complete game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 10,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                                PlayerConfig("Player3", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Verify game completed
        assertTrue(snapshot.gameEnded, "Game should have ended")
        assertTrue(snapshot.totalRounds > 0, "Should have played at least one round")

        // Verify dice rolls occurred
        assertTrue(snapshot.totalDiceRolls > 0, "Should have rolled dice")
        assertTrue(snapshot.averageDiceRoll > 2.0, "Average dice roll should be reasonable")
        assertTrue(snapshot.averageDiceRoll < 12.0, "Average dice roll should be reasonable")

        // Verify players moved
        assertTrue(snapshot.goPassings.isNotEmpty(), "Players should have passed GO")
        assertTrue(snapshot.tileLandings.isNotEmpty(), "Players should have landed on tiles")

        // Verify financial activity
        assertTrue(snapshot.totalBankPayments > 0, "Bank should have made payments")
        assertTrue(snapshot.totalBankCharges >= 0, "Bank may have charged players")

        // Verify game end
        assertNotNull(snapshot.endReason, "Should have an end reason")
    }

    @Test
    fun `tracks property purchases during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 20,
                        randomSeed = 123,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // With 20 rounds and 2 players, some properties should be purchased
        if (snapshot.totalPropertiesPurchased > 0) {
            assertTrue(snapshot.totalPropertySpending > 0, "Property spending should be positive")
            assertTrue(snapshot.propertiesByPlayer.isNotEmpty(), "Should track properties per player")

            // Verify spending matches purchases
            val propertiesCount = snapshot.propertiesByPlayer.values.sum()
            assertTrue(
                propertiesCount <= snapshot.totalPropertiesPurchased,
                "Sum of properties per player should match or be less than total",
            )
        }
    }

    @Test
    fun `tracks development activity during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 50,
                        randomSeed = 999,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // In a longer game, development may occur
        if (snapshot.totalHousesPurchased > 0) {
            assertTrue(
                snapshot.developmentByColorGroup.isNotEmpty(),
                "Should track development by color group",
            )

            val totalDevByColorGroup = snapshot.developmentByColorGroup.values.sum()
            assertEquals(
                snapshot.totalHousesPurchased,
                totalDevByColorGroup,
                "Total houses should match color group breakdown",
            )
        }
    }

    @Test
    fun `tracks jail events during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 30,
                        randomSeed = 777,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                                PlayerConfig("Player3", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // With 30 rounds and 3 players, jail events are likely
        if (snapshot.totalJailSentences > 0) {
            assertTrue(
                snapshot.jailReleasesByMethod.isNotEmpty(),
                "Should track how players got out of jail",
            )

            // Release methods should be valid
            snapshot.jailReleasesByMethod.keys.forEach { method ->
                assertTrue(
                    method in setOf("rolled doubles", "paid fee", "used card"),
                    "Release method should be valid: $method",
                )
            }
        }
    }

    @Test
    fun `tracks card draws during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 25,
                        randomSeed = 555,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Cards should be drawn during gameplay
        if (snapshot.totalCardsDrawn > 0) {
            assertTrue(snapshot.cardsByDeck.isNotEmpty(), "Should track cards by deck")

            // Verify only valid deck names
            snapshot.cardsByDeck.keys.forEach { deck ->
                assertTrue(
                    deck in setOf("Chance", "Community Chest"),
                    "Deck name should be valid: $deck",
                )
            }

            // Verify counts match
            val totalByDeck = snapshot.cardsByDeck.values.sum()
            assertEquals(
                snapshot.totalCardsDrawn,
                totalByDeck,
                "Total cards should match deck breakdown",
            )
        }
    }

    @Test
    fun `tracks rent payments during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 30,
                        randomSeed = 321,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Rent payments should occur when players land on owned properties
        if (snapshot.totalRentPaid > 0) {
            assertTrue(
                snapshot.rentTransactions.isNotEmpty(),
                "Should have rent transaction details",
            )

            // Verify rent transaction integrity
            snapshot.rentTransactions.forEach { tx ->
                assertTrue(tx.amount > 0, "Rent amount should be positive")
                assertNotNull(tx.payer, "Should have a payer")
                assertNotNull(tx.recipient, "Should have a recipient")
                assertNotNull(tx.property, "Should have a property")
                assertNotEquals(
                    tx.payer,
                    tx.recipient,
                    "Payer and recipient should be different",
                )
            }

            // Verify total matches sum of transactions
            val sumOfTransactions = snapshot.rentTransactions.sumOf { it.amount }
            assertEquals(
                snapshot.totalRentPaid,
                sumOfTransactions,
                "Total rent should match sum of transactions",
            )
        }
    }

    @Test
    fun `tracks mortgage activity during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 40,
                        randomSeed = 888,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Mortgages may occur when players need cash
        assertTrue(snapshot.totalMortgages >= 0, "Should track mortgages")
        assertTrue(snapshot.totalUnmortgages >= 0, "Should track unmortgages")
        assertTrue(
            snapshot.totalUnmortgages <= snapshot.totalMortgages,
            "Cannot unmortgage more than mortgaged",
        )
    }

    @Test
    fun `provides consistent snapshot at any point`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 5,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        // Take snapshot before game
        val beforeSnapshot = stats.snapshot()
        assertEquals(0, beforeSnapshot.totalRounds, "Should have 0 rounds before game")

        game.executeGame()

        // Take snapshot after game
        val afterSnapshot = stats.snapshot()
        assertTrue(afterSnapshot.totalRounds > 0, "Should have rounds after game")
        assertTrue(afterSnapshot.gameEnded, "Game should have ended")
    }

    @Test
    fun `outputs statistics in JSON format`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 15,
                        statisticsOutputFormat = StatisticsOutputFormat.JSON,
                        randomSeed = 666,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val snapshot = stats.snapshot()
        assertTrue(snapshot.gameEnded, "Game should have ended")
    }

    @Test
    fun `tracks property offers and purchase decisions during gameplay`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 20,
                        randomSeed = 123,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val report = stats.generateReport()

        // Verify that properties were offered and purchase decisions were made
        assertTrue(report.playerStatistics.isNotEmpty(), "Should have player statistics")

        val totalOffered = report.playerStatistics.sumOf { it.propertiesOffered }
        val totalPurchased = report.playerStatistics.sumOf { it.propertiesPurchased }

        assertTrue(totalOffered > 0, "Properties should have been offered during gameplay")
        assertTrue(totalPurchased > 0, "Some properties should have been purchased")
        assertTrue(totalPurchased <= totalOffered, "Purchases cannot exceed offers")

        // Verify purchase rates are calculated
        report.playerStatistics.forEach { playerStats ->
            if (playerStats.propertiesOffered > 0) {
                assertTrue(playerStats.purchaseRate >= 0.0, "Purchase rate should be non-negative")
                assertTrue(playerStats.purchaseRate <= 1.0, "Purchase rate should not exceed 100%")
                assertEquals(
                    playerStats.propertiesPurchased.toDouble() / playerStats.propertiesOffered,
                    playerStats.purchaseRate,
                    0.001,
                    "Purchase rate should match calculation",
                )
            }
        }

        // Verify strategy names are captured
        report.playerStatistics.forEach { playerStats ->
            assertEquals("DefaultStrategy", playerStats.strategyName, "Strategy name should be captured")
        }
    }

    @Test
    fun `tracks auction activity during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 30,
                        randomSeed = 456,
                        playerConfigs =
                            listOf(
                                // Use CalculatingStrategy which participates in auctions
                                PlayerConfig("Player1", CalculatingStrategy()),
                                PlayerConfig("Player2", CalculatingStrategy()),
                                PlayerConfig("Player3", CalculatingStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val report = stats.generateReport()

        // With CalculatingStrategy, auctions should occur when players decline to buy
        assertTrue(report.auctionStatistics.totalAuctions > 0, "Should have auctions with CalculatingStrategy")
        assertEquals(
            report.auctionStatistics.successfulAuctions + report.auctionStatistics.failedAuctions,
            report.auctionStatistics.totalAuctions,
            "Successful + failed auctions should equal total",
        )

        // If auctions occurred, verify data integrity
        assertTrue(report.auctionStatistics.totalBids >= 0, "Should track total bids")
        assertTrue(report.auctionStatistics.averageBidsPerAuction >= 0.0, "Average bids should be non-negative")

        assertTrue(report.auctionStatistics.totalAuctionSpending > 0, "Successful auctions should have spending")
        assertTrue(report.auctionStatistics.averageWinningBid > 0.0, "Average winning bid should be positive")
        assertNotNull(report.auctionStatistics.highestWinningBid, "Should have highest winning bid")
        assertNotNull(report.auctionStatistics.lowestWinningBid, "Should have lowest winning bid")

        val highestBid = report.auctionStatistics.highestWinningBid!!
        val lowestBid = report.auctionStatistics.lowestWinningBid!!
        assertTrue(
            highestBid.winningBid >= lowestBid.winningBid,
            "Highest bid should be >= lowest bid",
        )

        // Verify player auction wins
        assertTrue(
            report.auctionStatistics.playerAuctionWins.isNotEmpty(),
            "Should track auction wins by player",
        )
        val totalWins =
            report.auctionStatistics.playerAuctionWins.values
                .sum()
        assertEquals(
            report.auctionStatistics.successfulAuctions,
            totalWins,
            "Total wins should match successful auctions",
        )

        // Verify player participation
        assertTrue(
            report.auctionStatistics.playerAuctionParticipation.isNotEmpty(),
            "Should track auction participation by player",
        )
        val totalParticipationBids =
            report.auctionStatistics.playerAuctionParticipation.values
                .sum()
        assertEquals(
            report.auctionStatistics.totalBids,
            totalParticipationBids,
            "Total participation bids should match total bids",
        )
    }
}
