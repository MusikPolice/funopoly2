@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.TitleDeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for default helper methods in PlayerStrategy interface.
 * Uses DefaultStrategy as a concrete implementation for testing.
 */
internal class PlayerStrategyTest {

    private val strategy = DefaultStrategy()

    @Test
    fun `wouldCompleteMonopoly returns true when player owns all but one property in color group`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // NewYorkAvenue would complete Orange monopoly
        val wouldComplete = strategy.wouldCompleteMonopoly(NewYorkAvenue(), player)

        assertTrue(wouldComplete)
    }

    @Test
    fun `wouldCompleteMonopoly returns false when player owns no properties in color group`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val wouldComplete = strategy.wouldCompleteMonopoly(StJamesPlace(), player)

        assertFalse(wouldComplete)
    }

    @Test
    fun `wouldCompleteMonopoly returns false when player owns only one property in color group`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val wouldComplete = strategy.wouldCompleteMonopoly(TennesseeAvenue(), player)

        assertFalse(wouldComplete)
    }

    @Test
    fun `wouldCompleteMonopoly returns false when player already owns the property`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        player.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // Player already owns NewYorkAvenue
        val wouldComplete = strategy.wouldCompleteMonopoly(NewYorkAvenue(), player)

        assertFalse(wouldComplete)
    }

    @Test
    fun `wouldCompleteMonopoly works for two-property color groups`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // BalticAvenue would complete Brown monopoly (2-property group)
        val wouldComplete = strategy.wouldCompleteMonopoly(BalticAvenue(), player)

        assertTrue(wouldComplete)
    }

    @Test
    fun `calculateHighestRentOnBoard returns 0 when only current player has properties`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        // Only one property, owned by current player
        val board = Board(listOf(player), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, player)

        // Should be 0 because player's own properties are excluded
        assertEquals(0, highestRent)
    }

    @Test
    fun `calculateHighestRentOnBoard returns 0 when no properties exist`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, player)

        assertEquals(0, highestRent)
    }

    @Test
    fun `calculateHighestRentOnBoard returns highest rent from opponent's developed property`() {
        val player = Player("Test", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500, strategy = strategy)
        opponent.deeds[StJamesPlace()] = Player.Development(numHouses = 1)
        opponent.deeds[TennesseeAvenue()] = Player.Development()
        opponent.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player, opponent), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, player)

        // StJamesPlace with 1 house has rent of $70
        assertEquals(70, highestRent)
    }

    @Test
    fun `calculateHighestRentOnBoard returns highest rent across multiple opponents`() {
        val currentPlayer = Player("CurrentPlayer", money = 1500, strategy = strategy)
        
        val opponent1 = Player("Opponent1", money = 1500, strategy = strategy)
        opponent1.deeds[StJamesPlace()] = Player.Development(numHouses = 1) // Rent $70
        opponent1.deeds[TennesseeAvenue()] = Player.Development()
        opponent1.deeds[NewYorkAvenue()] = Player.Development()

        val opponent2 = Player("Opponent2", money = 1500, strategy = strategy)
        opponent2.deeds[ParkPlace()] = Player.Development(numHouses = 2) // Rent $500
        opponent2.deeds[Boardwalk()] = Player.Development()

        val board = Board(listOf(currentPlayer, opponent1, opponent2), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, currentPlayer)

        // ParkPlace with 2 houses has rent of $500
        assertEquals(500, highestRent)
    }

    @Test
    fun `calculateHighestRentOnBoard returns hotel rent when highest`() {
        val currentPlayer = Player("CurrentPlayer", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500, strategy = strategy)
        opponent.deeds[ParkPlace()] = Player.Development(hasHotel = true)
        opponent.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(currentPlayer, opponent), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, currentPlayer)

        // ParkPlace with hotel has rent of $1500
        assertEquals(1500, highestRent)
    }

    @Test
    fun `calculateHighestRentOnBoard considers monopoly rent doubling`() {
        val currentPlayer = Player("CurrentPlayer", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500, strategy = strategy)
        opponent.deeds[StJamesPlace()] = Player.Development() // No houses
        opponent.deeds[TennesseeAvenue()] = Player.Development()
        opponent.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(currentPlayer, opponent), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, currentPlayer)

        // NewYorkAvenue with monopoly (no houses) has rent of $32 (base $16 * 2)
        assertEquals(32, highestRent)
    }

    @Test
    fun `calculateHighestRentOnBoard ignores railroads and utilities`() {
        val currentPlayer = Player("CurrentPlayer", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500, strategy = strategy)
        opponent.deeds[StJamesPlace()] = Player.Development(numHouses = 1) // Rent $70
        val board = Board(listOf(currentPlayer, opponent), Bank())

        val highestRent = strategy.calculateHighestRentOnBoard(board, currentPlayer)

        // Should return property rent, not railroad/utility rent
        assertEquals(70, highestRent)
    }
}
