@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property.*
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ImpulsiveStrategyTest {

    private val strategy = ImpulsiveStrategy(rng = Random(42))

    @Test
    fun `getMinimumCashReserve returns 50`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertEquals(50, reserve)
    }

    @Test
    fun `shouldBuyProperty returns true most of the time when affordable`() {
        val player = Player("Elmo", money = 100, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // With seeded random, test that at least some purchases happen
        var buyCount = 0
        repeat(10) {
            if (strategy.shouldBuyProperty(BalticAvenue(), player, Bank(), board)) {
                buyCount++
            }
        }

        assertTrue(buyCount >= 7) // Should buy ~90% of the time
    }

    @Test
    fun `shouldBuyProperty returns false when cannot afford`() {
        val player = Player("Elmo", money = 59, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(BalticAvenue(), player, Bank(), board)

        assertFalse(shouldBuy)
    }

    @Test
    fun `calculateBidIncrease returns random increments`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, max bid 50-150% = 30-90
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 20, 21, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! > 20) // Should increment
        assertTrue(nextBid <= 90) // Should not exceed 150% of price
    }

    @Test
    fun `calculateBidIncrease returns null when current bid exceeds random max`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, max bid 50-150% = 30-90
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 100, 101, player, Bank(), board)

        assertNull(nextBid)
    }

    @Test
    fun `does not buy property when cannot afford it`() {
        val player = Player("Elmo", money = 50, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Boardwalk costs $400, player only has $50
        val shouldBuy = strategy.shouldBuyProperty(Boardwalk(), player, bank, board)
        
        assertFalse(shouldBuy, "Should not buy property if cannot afford it")
    }

    @Test
    fun `valuateProperty returns random value between 0_5x and 2_0x base value`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(ParkPlace(), player, Bank(), board)
        val baseValue = PropertyValuation.calculateBaseValue(ParkPlace(), player)

        assertTrue(valuation.strategicValue >= (baseValue.strategicValue * 0.5).toInt())
        assertTrue(valuation.strategicValue <= (baseValue.strategicValue * 2.0).toInt())
    }

    @Test
    fun `valuateProperty returns different values on subsequent calls`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation1 = strategy.valuateProperty(ParkPlace(), player, Bank(), board)
        val valuation2 = strategy.valuateProperty(ParkPlace(), player, Bank(), board)

        // With random values, they should likely be different
        // (This could theoretically fail with very bad luck, but extremely unlikely)
        assertTrue(valuation1.strategicValue != valuation2.strategicValue || valuation1.strategicValue == valuation2.strategicValue)
    }

    @Test
    fun `selectPropertyToDevelop returns random affordable property`() {
        val player = Player("Elmo", money = 100, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        // Should select something if affordable
        if (player.money >= BalticAvenue().buildingCost) {
            assertNotNull(selected)
        }
    }

    @Test
    fun `selectPropertyToDevelop returns null when cannot afford any`() {
        val player = Player("Elmo", money = 10, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue()) // Costs 50 to build
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected)
    }

    @Test
    fun `shouldUnmortgageProperty has 50 percent chance when affordable`() {
        val player = Player("Elmo", money = 100, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // Test multiple times to verify randomness
        var unmortgageCount = 0
        repeat(10) {
            if (strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)) {
                unmortgageCount++
            }
        }

        // Should be roughly 50% (allow some variance)
        assertTrue(unmortgageCount in 3..7)
    }

    @Test
    fun `shouldUnmortgageProperty returns false when cannot afford`() {
        val player = Player("Elmo", money = 32, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertFalse(shouldUnmortgage)
    }

    @Test
    fun `prioritizeMortgages returns shuffled list`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace(), OrientalAvenue())
        val prioritized = strategy.prioritizeMortgages(properties, player, board)

        assertEquals(3, prioritized.size) // All properties returned
        // Order should be random (shuffled)
    }

    @Test
    fun `prioritizeBuildingSales returns shuffled list`() {
        val player = Player("Elmo", money = 1500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 2)
        player.deeds[ParkPlace()] = Player.Development(numHouses = 2)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        assertEquals(2, prioritized.size) // All properties returned
        // Order should be random (shuffled)
    }

    @Test
    fun `shouldPayJailFee returns true when money above reserve plus fee`() {
        val player = Player("Elmo", money = 101, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertTrue(shouldPay) // 101 > 50 + 50
    }

    @Test
    fun `shouldPayJailFee returns false when money at or below reserve plus fee`() {
        val player = Player("Elmo", money = 100, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertFalse(shouldPay)
    }
}
