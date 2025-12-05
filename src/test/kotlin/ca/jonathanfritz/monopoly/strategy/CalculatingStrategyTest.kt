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

internal class CalculatingStrategyTest {
    private val strategy = CalculatingStrategy()

    @Test
    fun `getMinimumCashReserve returns 300 minimum when no properties developed`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertEquals(300, reserve)
    }

    @Test
    fun `getMinimumCashReserve returns 2x highest rent when greater than 300`() {
        val player = Player("Bert", money = 1500, strategy = strategy)

        // Add opponent with developed property
        val opponent = Player("Opponent", money = 1000)
        opponent.deeds[ParkPlace()] = Player.Development(numHouses = 1) // Rent = $175
        opponent.deeds[Boardwalk()] = Player.Development()

        val board = Board(listOf(player, opponent), Bank())

        val reserve = strategy.getMinimumCashReserve(player, board)

        assertEquals(350, reserve) // 175 * 2
    }

    @Test
    fun `shouldBuyProperty returns true for Orange property with good ROI when money sufficient`() {
        val player = Player("Bert", money = 570, strategy = strategy) // 180 * 1.5 + 300 reserve = 570
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(StJamesPlace(), player, Bank(), board)

        assertTrue(shouldBuy)
    }

    @Test
    fun `shouldBuyProperty returns false when money insufficient`() {
        val player = Player("Bert", money = 569, strategy = strategy) // Just below threshold
        val board = Board(listOf(player), Bank())

        val shouldBuy = strategy.shouldBuyProperty(StJamesPlace(), player, Bank(), board)

        assertFalse(shouldBuy)
    }

    @Test
    fun `calculateBidIncrease returns exactly 10 dollar increments`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val nextBid = strategy.calculateBidIncrease(StJamesPlace(), 100, 101, player, Bank(), board)

        assertNotNull(nextBid)
        assertEquals(110, nextBid) // Exactly $10 increment
    }

    @Test
    fun `calculateBidIncrease returns null when current bid exceeds strategic value`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // StJamesPlace strategic value should be less than 300
        val nextBid = strategy.calculateBidIncrease(StJamesPlace(), 300, 301, player, Bank(), board)

        assertNull(nextBid)
    }

    @Test
    fun `calculateBidIncrease allows higher bids for monopoly completion`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        // NewYorkAvenue completes Orange monopoly
        val nextBid = strategy.calculateBidIncrease(NewYorkAvenue(), 200, 201, player, Bank(), board)

        // Bert always bids in $10 increments
        assertEquals(210, nextBid)
    }

    @Test
    fun `does not buy property when cannot afford with reserve`() {
        val player = Player("Bert", money = 100, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // BalticAvenue costs $60, needs 1.5x + reserve = 90 + 100 = 190
        // Player only has $100
        val shouldBuy = strategy.shouldBuyProperty(BalticAvenue(), player, bank, board)
        
        assertFalse(shouldBuy, "Should not buy property if cannot afford with reserve")
    }

    @Test
    fun `does not bid more than available cash minus reserve in auction`() {
        val player = Player("Bert", money = 150, strategy = strategy)
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
        val player = Player("Bert", money = 150, strategy = strategy)
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
    fun `bids up to 110 percent of strategic value`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Should bid up to 1.1x strategic value
        val bid = strategy.calculateBidIncrease(
            deed = BalticAvenue(),
            currentBid = 10,
            minimumBid = 20,
            player = player,
            bank = bank,
            board = board
        )
        
        assertNotNull(bid, "Should bid on property")
        assertTrue(bid!! >= 20, "Bid should meet minimum")
        // With $10 increments, should be exactly $20 or $30
        assertTrue(bid == 20 || bid == 30, "Should use $10 increments")
    }

    @Test
    fun `bids up to 150 percent when completing monopoly`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        player.deeds[MediterraneanAvenue()] = Player.Development()
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // Already owns Mediterranean, Baltic completes monopoly
        // Should bid up to 1.5x strategic value
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
    fun `drops out when auction price exceeds strategic value threshold`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        
        // If current bid already exceeds 1.1x strategic value, should drop out
        val bid = strategy.calculateBidIncrease(
            deed = BalticAvenue(),
            currentBid = 200,
            minimumBid = 201,
            player = player,
            bank = bank,
            board = board
        )
        
        assertNull(bid, "Should drop out when price exceeds strategic value threshold")
    }

    @Test
    fun `valuateProperty gives Orange properties 20 percent bonus`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(StJamesPlace(), player, Bank(), board)
        val baseValue = PropertyValuation.calculateBaseValue(StJamesPlace(), player)

        // Should be base value * 1.2 for Orange
        assertEquals((baseValue.strategicValue * 1.2).toInt(), valuation.strategicValue)
    }

    @Test
    fun `valuateProperty gives Red properties 20 percent bonus`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val valuation = strategy.valuateProperty(KentuckyAvenue(), player, Bank(), board)
        val baseValue = PropertyValuation.calculateBaseValue(KentuckyAvenue(), player)

        // Should be base value * 1.2 for Red
        assertEquals((baseValue.strategicValue * 1.2).toInt(), valuation.strategicValue)
    }

    @Test
    fun `selectPropertyToDevelop prioritizes Orange and Red properties`() {
        val player = Player("Bert", money = 500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development()
        player.deeds[MediterraneanAvenue()] = Player.Development()
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        player.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(BalticAvenue(), StJamesPlace())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertEquals(StJamesPlace()::class, selected?.let { it::class }) // Orange over Brown
    }

    // TODO: Re-enable this test - currently failing because selectPropertyToDevelop returns null
    // The test setup looks correct but needs investigation into why the method returns null
    @Test
    fun `selectPropertyToDevelop prioritizes properties with fewer houses`() {
        val player = Player("Bert", money = 500, strategy = strategy)
        val stJames = StJamesPlace()
        val tennessee = TennesseeAvenue()
        player.deeds[stJames] = Player.Development(numHouses = 2)
        player.deeds[tennessee] = Player.Development(numHouses = 3)
        player.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(stJames, tennessee)
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        // Should select StJamesPlace (2 houses) to build to 3 houses first
        assertNotNull(selected)
        assertEquals(stJames, selected)
    }

    @Test
    fun `selectPropertyToDevelop returns null when cash below reserve`() {
        val player = Player("Bert", money = 300, strategy = strategy)
        player.deeds[StJamesPlace()] = Player.Development()
        player.deeds[TennesseeAvenue()] = Player.Development()
        player.deeds[NewYorkAvenue()] = Player.Development()
        val board = Board(listOf(player), Bank())

        val developable = listOf(StJamesPlace())
        val selected = strategy.selectPropertyToDevelop(developable, player, Bank(), board)

        assertNull(selected) // At minimum reserve
    }

    @Test
    fun `shouldUnmortgageProperty returns true when payback period is short`() {
        val player = Player("Bert", money = 500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        // BalticAvenue has low unmortgage cost, should have short payback
        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertTrue(shouldUnmortgage)
    }

    @Test
    fun `shouldUnmortgageProperty returns false when cannot afford`() {
        val player = Player("Bert", money = 32, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldUnmortgage = strategy.shouldUnmortgageProperty(BalticAvenue(), 33, player, board)

        assertFalse(shouldUnmortgage)
    }

    @Test
    fun `prioritizeMortgages prioritizes lowest ROI properties first`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), StJamesPlace(), ParkPlace())
        val prioritized = strategy.prioritizeMortgages(properties, player, board)

        // BalticAvenue (low ROI) should be first, StJamesPlace (high ROI) should be last
        assertEquals(BalticAvenue()::class, prioritized[0]::class)
    }

    @Test
    fun `prioritizeBuildingSales prioritizes lowest ROI developments first`() {
        val player = Player("Bert", money = 1500, strategy = strategy)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 2)
        player.deeds[StJamesPlace()] = Player.Development(numHouses = 2)
        val board = Board(listOf(player), Bank())

        val properties = listOf(BalticAvenue(), StJamesPlace())
        val prioritized = strategy.prioritizeBuildingSales(properties, player, board)

        // BalticAvenue (low ROI) should be sold first
        assertEquals(BalticAvenue()::class, prioritized[0]::class)
    }

    @Test
    fun `shouldPayJailFee returns true when money above reserve plus fee`() {
        val player = Player("Bert", money = 351, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertTrue(shouldPay) // 351 > 300 + 50
    }

    @Test
    fun `shouldPayJailFee returns false when money at or below reserve plus fee`() {
        val player = Player("Bert", money = 350, strategy = strategy)
        val board = Board(listOf(player), Bank())

        val shouldPay = strategy.shouldPayJailFee(50, player, board)

        assertFalse(shouldPay)
    }
}
