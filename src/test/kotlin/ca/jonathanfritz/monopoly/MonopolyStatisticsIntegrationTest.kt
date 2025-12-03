package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.statistics.StatisticsOutputFormat
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Integration test that runs a full game with statistics collection enabled.
 * This test verifies the end-to-end statistics pipeline works correctly.
 */
internal class MonopolyStatisticsIntegrationTest {
    
    @Test
    fun `game with statistics collection enabled completes successfully`() {
        val game = Monopoly(
            players = listOf(
                Player("Alice"),
                Player("Bob"),
            ),
            rng = Random(123), // Seeded for reproducibility
            config = Config(
                maxRounds = 20, // Short game for testing
                collectStatistics = true,
                statisticsOutputFormat = StatisticsOutputFormat.CONSOLE,
            ),
        )
        
        // This should run without errors and output statistics at the end
        game.executeGame()
    }
    
    @Test
    fun `game with JSON statistics output completes successfully`() {
        val game = Monopoly(
            players = listOf(
                Player("Charlie"),
                Player("Diana"),
            ),
            rng = Random(456),
            config = Config(
                maxRounds = 15,
                collectStatistics = true,
                statisticsOutputFormat = StatisticsOutputFormat.JSON,
            ),
        )
        
        game.executeGame()
    }
    
    @Test
    fun `game without statistics collection still works`() {
        val game = Monopoly(
            players = listOf(
                Player("Grace"),
                Player("Henry"),
            ),
            rng = Random(999),
            config = Config(
                maxRounds = 10,
                collectStatistics = false, // No statistics
            ),
        )
        
        game.executeGame()
    }
}
