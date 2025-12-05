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

internal class HighRentStrategyTest {

    private val strategy = HighRentStrategy(rng = Random(42))

    @Test
    fun `getMinimumCashReserve returns 300`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertEquals(300, reserve)
    }

    @Test
    fun `shouldBuyProperty returns true for expensive property when money is more than 1_2x price plus reserve`() {
        val player = Player("Count", money = 721, strategy = strategy) // 350*1.2 + 300 = 720
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(ParkPlace(), player, Bank(), board)

        assertTrue(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns false for expensive property when money is less than 1_2x price plus reserve`() {
        val player = Player("Count", money = 719, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(ParkPlace(), player, Bank(), board)

        assertFalse(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns true when completing monopoly regardless of cash`() {
        val player = Player("Count", money = 400, strategy = strategy) // Less than 1.2x + reserve
        player.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(ParkPlace(), player, Bank(), board)

        assertTrue(shouldBuy) // Always buy to complete monopoly
    }

    @Test
    fun `calculateBidIncrease returns null when current bid exceeds 120 percent for non-monopoly`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // ParkPlace price 350, 120% = 420
        val nextBid = strategy.calculateBidIncrease(ParkPlace(), 421, 422, player, Bank(), board)

        assertNull(nextBid)
    }

    @Test
    fun `calculateBidIncrease returns incremental bid for expensive property`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // ParkPlace price 350, 120% = 420
        val nextBid = strategy.calculateBidIncrease(ParkPlace(), 300, 301, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! in 320..350) // Increment by 20-50
    }

    @Test
    fun `calculateBidIncrease increases max to 150 percent when completing monopoly`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        player.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // ParkPlace price 350, 150% = 525
        val nextBid = strategy.calculateBidIncrease(ParkPlace(), 500, 501, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! in 520..525) // Increment by 20-50, capped at max
    }

    @Test
    fun `valuateProperty values expensive properties at 1_5x base value`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(ParkPlace(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(ParkPlace(), player)
        assertEquals((baseValue.strategicValue * 1.5).toInt(), valuation.strategicValue)
    }

    @Test
    fun `does not buy property when completing monopoly but cannot afford it`() {
        val player = Player("Count", money = 500, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Player already owns Park Place (Dark Blue) - costs $350
        bank.sellDeedToPlayer(ParkPlace::class, player, board)
        
        // Boardwalk costs $400, player only has $100 (after buying Park Place)
        // Even though it completes monopoly, should not buy
        val shouldBuy = strategy.shouldBuyProperty(Boardwalk(), player, bank, board)
        
        assertFalse(shouldBuy, "Should not buy property that completes monopoly if cannot afford it")
    }

    @Test
    fun `does not bid more than available cash minus reserve in auction`() {
        val player = Player("Count", money = 250, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Player has $250, reserve is $200, so only $50 available for bidding
        val bid = strategy.calculateBidIncrease(
            deed = BalticAvenue(),
            currentBid = 10,
            minimumBid = 20,
            player = player,
            bank = bank,
            board = board
        )
        
        // Should either drop out (null) or bid at most $50 (money - reserve)
        if (bid != null) {
            assertTrue(bid <= 50, "Should not bid more than available cash minus reserve ($50), but bid $bid")
        }
    }

    @Test
    fun `drops out of auction when minimum bid exceeds available cash minus reserve`() {
        val player = Player("Count", money = 250, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Player has $250, reserve is $200, so only $50 available
        // Minimum bid is $100
        val bid = strategy.calculateBidIncrease(
            deed = TennesseeAvenue(),
            currentBid = 90,
            minimumBid = 100,
            player = player,
            bank = bank,
            board = board
        )
        
        assertNull(bid, "Should drop out when minimum bid exceeds available cash minus reserve")
    }

    @Test
    fun `valuateProperty values cheap properties at 0_7x base value`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(BalticAvenue(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(BalticAvenue(), player)
        assertEquals((baseValue.strategicValue * 0.7).toInt(), valuation.strategicValue)
    }

    @Test
    fun `selectPropertyToDevelop prioritizes expensive color groups`() {
        val player = Player("Count", money = 1000, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        player.deeds[ParkPlace()] = Player.Development()
        player.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue(), ParkPlace())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertEquals(ParkPlace()::class, selected?.let { it::class })
    }

    @Test
    fun `selectPropertyToDevelop builds hotels on expensive properties`() {
        val player = Player("Count", money = 1000, strategy = strategy)
        player.deeds[ParkPlace()] = Player.Development(numHouses = 4)
        player.deeds[Boardwalk()] = Player.Development(numHouses = 4)
        val board = Board(listOf(player), Bank())

        val developable = listOf(ParkPlace()) // Can upgrade to hotel
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNotNull(selected)
        assertEquals(ParkPlace()::class, selected!!::class) // Should select ParkPlace for hotel
    }

    @Test
    fun `selectPropertyToDevelop returns null when cash at reserve`() {
        val player = Player("Count", money = 300, strategy = strategy) // Exactly at reserve
        player.deeds[ParkPlace()] = Player.Development()
        player.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(ParkPlace())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected) // Not enough cash above reserve
    }

    @Test
    fun `shouldUnmortgageProperty returns true when money is more than 1_8x cost`() {
        val player = Player("Count", money = 60, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertTrue(shouldUnmortgage) // 60 > 33 * 1.8 (59.4)
    }

    @Test
    fun `shouldUnmortgageProperty returns false when money is less than 1_8x cost`() {
        val player = Player("Count", money = 59, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertFalse(shouldUnmortgage)
    }

    @Test
    fun `prioritizeMortgages prioritizes cheap properties first`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace(), OrientalAvenue())
        val prioritized = strategy.prioritizeMortgages(properties, player, board)

        assertEquals(BalticAvenue()::class, prioritized[0]::class) // Cheapest first
    }

    @Test
    fun `prioritizeBuildingSales sells cheap properties first`() {
        val player = Player("Count", money = 1500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 2)
        player.deeds[ParkPlace()] = Player.Development(numHouses = 2)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        assertEquals(BalticAvenue()::class, prioritized[0]::class) // Cheap property first
    }

    @Test
    fun `shouldPayJailFee returns true when money above reserve plus fee`() {
        val player = Player("Count", money = 351, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertTrue(shouldPay) // 351 > 300 + 50
    }

    @Test
    fun `shouldPayJailFee returns false when money at or below reserve plus fee`() {
        val player = Player("Count", money = 350, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertFalse(shouldPay)
    }
}
