@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.statistics

import ca.jonathanfritz.monopoly.Monopoly
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.event.EventBus
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

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
            Player("Player3"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(42),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 10),
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

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(123),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 20),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // With 20 rounds and 2 players, some properties should be purchased
        if (snapshot.totalPropertiesPurchased > 0) {
            assertTrue(snapshot.totalPropertySpending > 0, "Property spending should be positive")
            assertTrue(snapshot.propertiesByPlayer.isNotEmpty(), "Should track properties per player")

            // Verify spending matches purchases
            val propertiesCount = snapshot.propertiesByPlayer.values.sum()
            assertTrue(propertiesCount <= snapshot.totalPropertiesPurchased,
                "Sum of properties per player should match or be less than total")
        }
    }

    @Test
    fun `tracks development activity during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(999),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 50),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // In a longer game, development may occur
        if (snapshot.totalHousesPurchased > 0) {
            assertTrue(snapshot.developmentByColorGroup.isNotEmpty(),
                "Should track development by color group")
            
            val totalDevByColorGroup = snapshot.developmentByColorGroup.values.sum()
            assertEquals(snapshot.totalHousesPurchased, totalDevByColorGroup,
                "Total houses should match color group breakdown")
        }
    }

    @Test
    fun `tracks jail events during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
            Player("Player3"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(777),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 30),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // With 30 rounds and 3 players, jail events are likely
        if (snapshot.totalJailSentences > 0) {
            assertTrue(snapshot.jailReleasesByMethod.isNotEmpty(),
                "Should track how players got out of jail")
            
            // Release methods should be valid
            snapshot.jailReleasesByMethod.keys.forEach { method ->
                assertTrue(method in setOf("rolled doubles", "paid fee", "used card"),
                    "Release method should be valid: $method")
            }
        }
    }

    @Test
    fun `tracks card draws during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(555),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 25),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Cards should be drawn during gameplay
        if (snapshot.totalCardsDrawn > 0) {
            assertTrue(snapshot.cardsByDeck.isNotEmpty(), "Should track cards by deck")
            
            // Verify only valid deck names
            snapshot.cardsByDeck.keys.forEach { deck ->
                assertTrue(deck in setOf("Chance", "Community Chest"),
                    "Deck name should be valid: $deck")
            }
            
            // Verify counts match
            val totalByDeck = snapshot.cardsByDeck.values.sum()
            assertEquals(snapshot.totalCardsDrawn, totalByDeck,
                "Total cards should match deck breakdown")
        }
    }

    @Test
    fun `tracks rent payments during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(321),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 30),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Rent payments should occur when players land on owned properties
        if (snapshot.totalRentPaid > 0) {
            assertTrue(snapshot.rentTransactions.isNotEmpty(),
                "Should have rent transaction details")
            
            // Verify rent transaction integrity
            snapshot.rentTransactions.forEach { tx ->
                assertTrue(tx.amount > 0, "Rent amount should be positive")
                assertNotNull(tx.payer, "Should have a payer")
                assertNotNull(tx.recipient, "Should have a recipient")
                assertNotNull(tx.property, "Should have a property")
                assertNotEquals(tx.payer, tx.recipient,
                    "Payer and recipient should be different")
            }
            
            // Verify total matches sum of transactions
            val sumOfTransactions = snapshot.rentTransactions.sumOf { it.amount }
            assertEquals(snapshot.totalRentPaid, sumOfTransactions,
                "Total rent should match sum of transactions")
        }
    }

    @Test
    fun `tracks mortgage activity during game`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(888),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 40),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Mortgages may occur when players need cash
        assertTrue(snapshot.totalMortgages >= 0, "Should track mortgages")
        assertTrue(snapshot.totalUnmortgages >= 0, "Should track unmortgages")
        assertTrue(snapshot.totalUnmortgages <= snapshot.totalMortgages,
            "Cannot unmortgage more than mortgaged")
    }

    @Test
    fun `provides consistent snapshot at any point`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(42),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 5),
        )

        // Take snapshot before game starts
        val snapshotBefore = stats.snapshot()
        assertEquals(0, snapshotBefore.totalRounds)
        assertFalse(snapshotBefore.gameEnded)

        // Run game
        game.executeGame()

        // Take snapshot after game ends
        val snapshotAfter = stats.snapshot()
        assertTrue(snapshotAfter.totalRounds > 0)
        assertTrue(snapshotAfter.gameEnded)

        // Snapshots should be independently valid
        assertNotEquals(snapshotBefore.totalRounds, snapshotAfter.totalRounds)
    }

    @Test
    fun `tracks doubles leading to different outcomes`() {
        val eventBus = EventBus()
        val stats = GameStatistics()
        eventBus.register(stats)

        val players = listOf(
            Player("Player1"),
            Player("Player2"),
        )

        val game = Monopoly(
            players = players,
            rng = Random(666),
            eventBus = eventBus,
            config = Monopoly.Config(maxRounds = 20),
        )

        game.executeGame()

        val snapshot = stats.snapshot()

        // Verify doubles are being tracked
        if (snapshot.doublesCount.isNotEmpty()) {
            snapshot.doublesCount.forEach { (player, count) ->
                assertTrue(count > 0, "Doubles count should be positive for $player")
            }
        }
    }
}
