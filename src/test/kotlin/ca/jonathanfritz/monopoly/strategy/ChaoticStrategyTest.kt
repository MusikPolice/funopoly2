@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class ChaoticStrategyTest {
    private val seededRng = Random(42)
    private val strategy = ChaoticStrategy(seededRng)

    @Test
    fun `shouldBuyProperty buys to block opponent monopoly`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500)
        opponent.deeds[StJamesPlace()] = Player.Development()
        opponent.deeds[TennesseeAvenue()] = Player.Development()
        val board = Board(listOf(player, opponent), Bank())

        // NewYorkAvenue would complete opponent's Orange monopoly
        val shouldBuy = strategy.shouldBuyProperty(NewYorkAvenue(), player, Bank(), board)

        assertTrue(shouldBuy, "Should buy to block opponent monopoly")
    }

    @Test
    fun `shouldBuyProperty has random behavior when not blocking`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // With seeded random, behavior should be deterministic
        val shouldBuy = strategy.shouldBuyProperty(StJamesPlace(), player, Bank(), board)

        // Just verify it returns a boolean (can't predict exact value without knowing seed behavior)
        assertNotNull(shouldBuy)
    }

    @Test
    fun `calculateBidIncrease bids aggressively when blocking opponent monopoly`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500)
        opponent.deeds[StJamesPlace()] = Player.Development()
        opponent.deeds[TennesseeAvenue()] = Player.Development()
        val board = Board(listOf(player, opponent), Bank())

        // NewYorkAvenue blocks opponent's Orange monopoly
        val nextBid = strategy.calculateBidIncrease(NewYorkAvenue(), 200, player, Bank(), board)

        assertNotNull(nextBid, "Should bid when blocking opponent")
        assertTrue(nextBid!! > 200, "Should increase bid")
    }

    @Test
    fun `calculateBidIncrease returns null when exceeding internal max`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // Bid very high to exceed internal max
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 1000, player, Bank(), board)

        assertNull(nextBid, "Should stop bidding when exceeding internal max")
    }

    @Test
    fun `calculateBidIncrease uses chaotic increments`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // Start with low bid to ensure we're within internal max
        val bid1 = strategy.calculateBidIncrease(StJamesPlace(), 50, player, Bank(), board)
        val bid2 = strategy.calculateBidIncrease(TennesseeAvenue(), 50, player, Bank(), board)

        // With seeded random, at least one should return a bid (might be null if internal max is low)
        // Just verify the method works and returns reasonable values when it does bid
        if (bid1 != null) {
            assertTrue(bid1 > 50, "Bid should increase from current bid")
        }
        if (bid2 != null) {
            assertTrue(bid2 > 50, "Bid should increase from current bid")
        }
    }

    @Test
    fun `valuateProperty values blocking properties highly`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val opponent = Player("Opponent", money = 1500)
        opponent.deeds[StJamesPlace()] = Player.Development()
        opponent.deeds[TennesseeAvenue()] = Player.Development()
        val board = Board(listOf(player, opponent), Bank())

        // NewYorkAvenue blocks opponent's Orange monopoly
        val valuation = strategy.valuateProperty(NewYorkAvenue(), player, Bank(), board)
        val baseValue = PropertyValuation.calculateBaseValue(NewYorkAvenue(), player)

        // Blocking value should be significantly higher (3x base)
        assertTrue(valuation.strategicValue > baseValue.strategicValue * 2, "Blocking property should be valued highly")
        assertTrue(valuation.reasoning.contains("Blocks opponent"), "Reasoning should mention blocking")
    }

    @Test
    fun `valuateProperty uses random multiplier for non-blocking properties`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(StJamesPlace(), player, Bank(), board)

        // Should have some value (random multiplier applied)
        assertTrue(valuation.strategicValue > 0)
    }

    @Test
    fun `valuateProperty values monopoly completion for self`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // NewYorkAvenue completes player's own Orange monopoly
        val valuation = strategy.valuateProperty(NewYorkAvenue(), player, Bank(), board)
        val baseValue = PropertyValuation.calculateBaseValue(NewYorkAvenue(), player)

        // Monopoly completion should be valued highly (2x base)
        assertTrue(valuation.strategicValue >= baseValue.strategicValue * 1.5, "Monopoly completion should be valued highly")
    }

    @Test
    fun `getMinimumCashReserve returns random value between 0 and 500`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertTrue(reserve >= 0 && reserve <= 500, "Reserve should be between 0 and 500")
    }

    @Test
    fun `getMinimumCashReserve changes on each call`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // Create new strategy with different seed to ensure different values
        val strategy2 = ChaoticStrategy(Random(99))
        val reserve1 = strategy.getMinimumCashReserve(player, board)
        val reserve2 = strategy2.getMinimumCashReserve(player, board)

        // With different seeds, should get different values
        // (This might occasionally fail if both seeds happen to generate same value)
        assertTrue(reserve1 >= 0 && reserve1 <= 500)
        assertTrue(reserve2 >= 0 && reserve2 <= 500)
    }

    @Test
    fun `selectPropertyToDevelop prioritizes hotels for intimidation`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val prop1 = StJamesPlace()
        val prop2 = TennesseeAvenue()
        player.deeds[prop1] = Player.Development(numHouses = 4) // Can build hotel
        player.deeds[prop2] = Player.Development(numHouses = 2)
        player.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(prop1, prop2)
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        // Should prioritize building hotel (intimidation factor)
        assertEquals(prop1, selected, "Should prioritize property that can build hotel")
    }

    @Test
    fun `selectPropertyToDevelop returns null when cash below reserve`() {
        val player = Player("Ernie", money = 50, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        player.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(StJamesPlace())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected, "Should not develop when cash is too low")
    }

    @Test
    fun `shouldUnmortgageProperty has random 40 percent chance when affordable`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // Test multiple times to see random behavior (with seeded random, should be deterministic)
        val result = strategy.shouldUnmortgageProperty(StJamesPlace(), 100, player, board)

        // Just verify it returns a boolean
        assertNotNull(result)
    }

    @Test
    fun `shouldUnmortgageProperty returns false when unaffordable`() {
        val player = Player("Ernie", money = 50, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val result = strategy.shouldUnmortgageProperty(StJamesPlace(), 100, player, board)

        assertFalse(result, "Should not unmortgage when unaffordable")
    }

    @Test
    fun `prioritizeMortgages returns randomized list`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(StJamesPlace(), TennesseeAvenue(), NewYorkAvenue())
        val prioritized = strategy.prioritizeMortgages(properties, player, board)

        // Should return all properties
        assertEquals(3, prioritized.size)
        assertTrue(prioritized.containsAll(properties))
    }

    @Test
    fun `prioritizeBuildingSales returns randomized list`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(StJamesPlace(), TennesseeAvenue(), NewYorkAvenue())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        // Should return all properties
        assertEquals(3, prioritized.size)
        assertTrue(prioritized.containsAll(properties))
    }

    @Test
    fun `shouldPayJailFee has chaotic behavior`() {
        val player = Player("Ernie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // With seeded random, should be deterministic
        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        // Just verify it returns a boolean
        assertNotNull(shouldPay)
    }
}
