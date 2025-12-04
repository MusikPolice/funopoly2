@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad.*
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GamblerStrategyTest {

    private val strategy = GamblerStrategy(rng = Random(42))

    @Test
    fun `getMinimumCashReserve returns 0`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertEquals(0, reserve)
    }

    @Test
    fun `shouldBuyProperty returns true for any affordable property`() {
        val player = Player("Cookie", money = 100, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(BalticAvenue(), player, Bank(), board)

        assertTrue(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns false when cannot afford`() {
        val player = Player("Cookie", money = 59, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(BalticAvenue(), player, Bank(), board)

        assertFalse(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty always buys railroads when affordable`() {
        val player = Player("Cookie", money = 200, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(ReadingRailroad(), player, Bank(), board)

        assertTrue(shouldBuy)
    }

    @Test
    fun `calculateBidIncrease returns aggressive increments`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, max bid 150-200% = 90-120
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 50, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! > 50) // Should increment from current bid
        assertTrue(nextBid <= 120) // Should not exceed 200% of price
    }

    @Test
    fun `calculateBidIncrease returns null when current bid exceeds max`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, max bid 150-200% = 90-120
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 121, player, Bank(), board)

        assertNull(nextBid)
    }

    @Test
    fun `calculateBidIncrease is extra aggressive on railroads`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // ReadingRailroad price 200, max bid 200-250% = 400-500
        val nextBid = strategy.calculateBidIncrease(ReadingRailroad(), 300, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! in 350..500) // Aggressive increment, high max
    }

    @Test
    fun `calculateBidIncrease is extra aggressive when completing monopoly`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, max bid 200-250% when completing monopoly = 120-150
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 100, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! in 120..150) // Extra aggressive for monopoly
    }

    @Test
    fun `valuateProperty values railroads at 2x base value`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(ReadingRailroad(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(ReadingRailroad(), player)
        assertEquals((baseValue.strategicValue * 2.0).toInt(), valuation.strategicValue)
    }

    @Test
    fun `valuateProperty values monopoly completion at 2_5x base value`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(BalticAvenue(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(BalticAvenue(), player)
        assertEquals((baseValue.strategicValue * 2.5).toInt(), valuation.strategicValue)
    }

    @Test
    fun `valuateProperty values other properties at 1_2x base value`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(ParkPlace(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(ParkPlace(), player)
        assertEquals((baseValue.strategicValue * 1.2).toInt(), valuation.strategicValue)
    }

    @Test
    fun `selectPropertyToDevelop builds on everything possible`() {
        val player = Player("Cookie", money = 100, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNotNull(selected)
        assertEquals(BalticAvenue()::class, selected!!::class)
    }

    @Test
    fun `selectPropertyToDevelop rushes to hotels`() {
        val player = Player("Cookie", money = 500, strategy = strategy)
        player.deeds[ParkPlace()] = Player.Development(numHouses = 4)
        player.deeds[Boardwalk()] = Player.Development(numHouses = 4)
        val board = Board(listOf(player), Bank())

        val developable = listOf(ParkPlace())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNotNull(selected)
        assertEquals(ParkPlace()::class, selected!!::class) // Should build hotel
    }

    @Test
    fun `selectPropertyToDevelop returns null when cannot afford any`() {
        val player = Player("Cookie", money = 10, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue()) // Costs 50 to build
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected)
    }

    @Test
    fun `shouldUnmortgageProperty returns true when money equals cost`() {
        val player = Player("Cookie", money = 33, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertTrue(shouldUnmortgage)
    }

    @Test
    fun `shouldUnmortgageProperty returns false when money less than cost`() {
        val player = Player("Cookie", money = 32, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertFalse(shouldUnmortgage)
    }

    @Test
    fun `prioritizeMortgages returns properties in any order`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace(), OrientalAvenue())
        val prioritized = strategy.prioritizeMortgages(properties, player, board)

        assertEquals(3, prioritized.size) // All properties returned
    }

    @Test
    fun `prioritizeBuildingSales returns properties in any order`() {
        val player = Player("Cookie", money = 1500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 2)
        player.deeds[ParkPlace()] = Player.Development(numHouses = 2)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        assertEquals(2, prioritized.size) // All properties returned
    }

    @Test
    fun `shouldPayJailFee returns true when money exceeds fee`() {
        val player = Player("Cookie", money = 51, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertTrue(shouldPay)
    }

    @Test
    fun `shouldPayJailFee returns false when money equals fee`() {
        val player = Player("Cookie", money = 50, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertFalse(shouldPay)
    }
}
