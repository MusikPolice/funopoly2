package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.statistics.StatisticsOutputFormat
import ca.jonathanfritz.monopoly.strategy.DefaultStrategy
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
            config = Config(
                maxRounds = 20, // Short game for testing
                collectStatistics = true,
                statisticsOutputFormat = StatisticsOutputFormat.CONSOLE,
                randomSeed = 123, // Seeded for reproducibility
                playerConfigs = listOf(
                    PlayerConfig("Alice", DefaultStrategy()),
                    PlayerConfig("Bob", DefaultStrategy()),
                ),
            ),
        )
        
        // This should run without errors and output statistics at the end
        game.executeGame()
    }
    
    @Test
    fun `game with JSON statistics output completes successfully`() {
        val game = Monopoly(
            config = Config(
                maxRounds = 15,
                collectStatistics = true,
                statisticsOutputFormat = StatisticsOutputFormat.JSON,
                randomSeed = 456,
                playerConfigs = listOf(
                    PlayerConfig("Charlie", DefaultStrategy()),
                    PlayerConfig("Diana", DefaultStrategy()),
                ),
            ),
        )
        
        game.executeGame()
    }
    
    @Test
    fun `game without statistics collection still works`() {
        val game = Monopoly(
            config = Config(
                maxRounds = 10,
                collectStatistics = false, // No statistics
                randomSeed = 999,
                playerConfigs = listOf(
                    PlayerConfig("Grace", DefaultStrategy()),
                    PlayerConfig("Henry", DefaultStrategy()),
                ),
            ),
        )
        
        game.executeGame()
    }
}
