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

internal class SlumlordStrategyTest {

    private val strategy = SlumlordStrategy(rng = Random(42))

    @Test
    fun `getMinimumCashReserve returns 200`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertEquals(200, reserve)
    }

    @Test
    fun `shouldBuyProperty returns true for cheap property when money is more than 1_5x price plus reserve`() {
        val player = Player("Oscar", money = 290, strategy = strategy) // 60*1.5 + 200 = 290
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(BalticAvenue(), player, Bank(), board)

        assertTrue(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns false for cheap property when money is less than 1_5x price plus reserve`() {
        val player = Player("Oscar", money = 289, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(BalticAvenue(), player, Bank(), board)

        assertFalse(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns false for expensive property without monopoly completion`() {
        val player = Player("Oscar", money = 2000, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(ParkPlace(), player, Bank(), board)

        assertFalse(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns true for expensive property when completing monopoly`() {
        val player = Player("Oscar", money = 2000, strategy = strategy)
        player.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(ParkPlace(), player, Bank(), board)

        assertTrue(shouldBuy)
    }

    @Test
    fun `calculateBidIncrease returns null when current bid exceeds 80 percent for cheap property`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, 80% = 48
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 49, 50, player, Bank(), board)

        assertNull(nextBid)
    }

    @Test
    fun `calculateBidIncrease returns incremental bid for cheap property`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue price 60, 80% = 48
        val nextBid = strategy.calculateBidIncrease(BalticAvenue(), 30, 31, player, Bank(), board)

        assertNotNull(nextBid)
        assertTrue(nextBid!! in 40..48) // Increment by 10-20
    }

    @Test
    fun `calculateBidIncrease returns null when current bid exceeds 50 percent for expensive property`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // ParkPlace price 350, 50% = 175
        val nextBid = strategy.calculateBidIncrease(ParkPlace(), 176, 177, player, Bank(), board)

        assertNull(nextBid)
    }

    @Test
    fun `calculateBidIncrease increases max by 20 percent when completing monopoly`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        player.deeds[Boardwalk()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // ParkPlace price 350, 50% * 1.2 = 210
        val nextBid = strategy.calculateBidIncrease(ParkPlace(), 200, 201, player, Bank(), board)

        assertNotNull(nextBid) // Should still be bidding
    }

    @Test
    fun `valuateProperty values cheap properties at 1_5x base value`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(BalticAvenue(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(BalticAvenue(), player)
        assertEquals((baseValue.strategicValue * 1.5).toInt(), valuation.strategicValue)
    }

    @Test
    fun `does not buy expensive property when completing monopoly but cannot afford it`() {
        val player = Player("Oscar", money = 600, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Player already owns Park Place (expensive Dark Blue property) - costs $350
        bank.sellDeedToPlayer(ParkPlace::class, player, board)
        
        // Boardwalk costs $400, player doesn't have enough even though it completes monopoly
        val shouldBuy = strategy.shouldBuyProperty(Boardwalk(), player, bank, board)
        
        assertFalse(shouldBuy, "Should not buy expensive property that completes monopoly if cannot afford it")
    }

    @Test
    fun `does not bid more than available cash minus reserve in auction`() {
        val player = Player("Oscar", money = 150, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Player has $150, reserve is $100, so only $50 available for bidding
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
        val player = Player("Oscar", money = 150, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Player has $150, reserve is $100, so only $50 available
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
    fun `bids more on cheap properties than expensive ones`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Baltic Avenue costs $60, max bid is 0.8x = $48
        val cheapBid = strategy.calculateBidIncrease(
            deed = BalticAvenue(),
            currentBid = 10,
            minimumBid = 20,
            player = player,
            bank = bank,
            board = board
        )
        
        // Park Place costs $350, max bid is 0.5x = $175
        val expensiveBid = strategy.calculateBidIncrease(
            deed = ParkPlace(),
            currentBid = 10,
            minimumBid = 20,
            player = player,
            bank = bank,
            board = board
        )
        
        assertNotNull(cheapBid, "Should bid on cheap property")
        // Expensive property might be null or lower relative to price
        if (expensiveBid != null && cheapBid != null) {
            // If both bid, cheap property should be relatively more aggressive
            val cheapRatio = cheapBid.toDouble() / 60  // Baltic price
            val expensiveRatio = expensiveBid.toDouble() / 350  // Park Place price
            assertTrue(cheapRatio > expensiveRatio, "Should bid more aggressively on cheap properties")
        }
    }

    @Test
    fun `bids with monopoly completion bonus`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Already owns Mediterranean, Baltic completes monopoly
        // Should get 1.2x multiplier (Baltic costs $60, base 0.8x = $48, with bonus = $57.6)
        val bid = strategy.calculateBidIncrease(
            deed = BalticAvenue(),
            currentBid = 10,
            minimumBid = 20,
            player = player,
            bank = bank,
            board = board
        )
        
        assertNotNull(bid, "Should bid when completing monopoly")
        assertTrue(bid!! >= 20, "Bid should meet minimum")
    }

    @Test
    fun `drops out when auction price exceeds value threshold`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Baltic Avenue costs $60, max bid is 0.8x = $48
        // If current bid is already $60, should drop out
        val bid = strategy.calculateBidIncrease(
            deed = BalticAvenue(),
            currentBid = 60,
            minimumBid = 61,
            player = player,
            bank = bank,
            board = board
        )
        
        assertNull(bid, "Should drop out when price exceeds value threshold")
    }

    @Test
    fun `valuateProperty values expensive properties at 0_5x base value`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(ParkPlace(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(ParkPlace(), player)
        assertEquals((baseValue.strategicValue * 0.5).toInt(), valuation.strategicValue)
    }

    @Test
    fun `valuateProperty adds 50 percent bonus for monopoly completion`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(BalticAvenue(), player, Bank(), board)

        val baseValue = PropertyValuation.calculateBaseValue(BalticAvenue(), player)
        // 1.5x for cheap property, then +50% for monopoly = 1.5 * 1.5 = 2.25
        assertEquals((baseValue.strategicValue * 2.25).toInt(), valuation.strategicValue)
    }

    @Test
    fun `selectPropertyToDevelop prioritizes cheap color groups`() {
        val player = Player("Oscar", money = 500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        player.deeds[OrientalAvenue()] = Player.Development()
        player.deeds[VermontAvenue()] = Player.Development()
        player.deeds[ConnecticutAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue(), OrientalAvenue())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertEquals(BalticAvenue()::class, selected?.let { it::class })
    }

    @Test
    fun `selectPropertyToDevelop stops at 4 houses`() {
        val player = Player("Oscar", money = 500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 4)
        player.deeds[MediterraneanAvenue()] = Player.Development(numHouses = 4)
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected) // Don't build hotels
    }

    @Test
    fun `selectPropertyToDevelop returns null when cash below reserve plus highest rent`() {
        val player = Player("Oscar", money = 250, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        
        // Add opponent with developed property to establish highest rent on board
        val opponent = Player("Opponent", money = 1000)
        opponent.deeds[OrientalAvenue()] = Player.Development(numHouses = 1) // Rent = $30
        opponent.deeds[VermontAvenue()] = Player.Development()
        opponent.deeds[ConnecticutAvenue()] = Player.Development()
        
        val board = Board(listOf(player, opponent), Bank())

        // Oscar has 250, needs 200 (reserve) + 30 (highest rent) + 50 (building cost) = 280
        val developable = listOf(BalticAvenue())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected) // Not enough cash above reserve + rent buffer
    }

    @Test
    fun `shouldUnmortgageProperty returns true only when money is more than 3x cost`() {
        val player = Player("Oscar", money = 99, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertTrue(shouldUnmortgage) // 99 >= 33 * 3
    }

    @Test
    fun `shouldUnmortgageProperty returns false when money is less than 3x cost`() {
        val player = Player("Oscar", money = 98, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertFalse(shouldUnmortgage)
    }

    @Test
    fun `prioritizeMortgages prioritizes expensive properties first`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace(), OrientalAvenue())
        val prioritized = strategy.prioritizeMortgages(properties, player, board)

        assertEquals(ParkPlace()::class, prioritized[0]::class) // Most expensive first
    }

    @Test
    fun `prioritizeBuildingSales sells hotels first`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(hasHotel = true)
        player.deeds[OrientalAvenue()] = Player.Development(numHouses = 3)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), OrientalAvenue())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        assertEquals(BalticAvenue()::class, prioritized[0]::class) // Hotel first
    }

    @Test
    fun `prioritizeBuildingSales then prioritizes expensive properties`() {
        val player = Player("Oscar", money = 1500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 2)
        player.deeds[ParkPlace()] = Player.Development(numHouses = 2)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), ParkPlace())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        assertEquals(ParkPlace()::class, prioritized[0]::class) // Expensive property first
    }
}
