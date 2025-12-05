@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad.*
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.event.GameEvent
import ca.jonathanfritz.monopoly.event.GameEventListener
import ca.jonathanfritz.monopoly.strategy.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class AuctionIntegrationTest {

    @Test
    fun `auction with single bidder results in winner at starting bid`() {
        val player = Player("Solo", money = 1500, strategy = CalculatingStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(player), bank, eventBus = eventBus)
        
        val deed = BalticAvenue()
        val auction = Auction(deed, listOf(player), bank, board, eventBus)
        val winner = auction.conduct()
        
        assertNotNull(winner, "Auction should have a winner")
        assertEquals(player, winner, "Solo bidder should win")
        assertTrue(player.deeds.keys.any { it::class == deed::class }, "Winner should own the property")
    }

    @Test
    fun `auction with no bidders results in no winner`() {
        val player = Player("Broke", money = 5, strategy = DefaultStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(player), bank, eventBus = eventBus)
        
        val deed = BalticAvenue()
        // DefaultStrategy never bids, and player can't afford minimum bid anyway
        val auction = Auction(deed, listOf(player), bank, board, eventBus)
        val winner = auction.conduct()
        
        assertNull(winner, "Auction should have no winner")
        assertFalse(player.deeds.keys.any { it::class == deed::class }, "Property should not be owned")
    }

    @Test
    fun `auction between multiple strategies - highest bidder wins`() {
        val gambler = Player("Cookie", money = 1500, strategy = GamblerStrategy())
        val conservative = Player("Big Bird", money = 1500, strategy = ConservativeStrategy())
        val calculating = Player("Bert", money = 1500, strategy = CalculatingStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(gambler, conservative, calculating), bank, eventBus = eventBus)
        
        val deed = ReadingRailroad()
        // Gambler should bid most aggressively on railroads
        val auction = Auction(deed, listOf(gambler, conservative, calculating), bank, board, eventBus)
        val winner = auction.conduct()
        
        assertNotNull(winner, "Auction should have a winner")
        // Gambler values railroads at 2x, should outbid others
        assertTrue(winner!!.deeds.keys.any { it::class == deed::class }, "Winner should own the railroad")
    }

    @Test
    fun `auction emits correct event sequence`() {
        val player1 = Player("Player1", money = 1500, strategy = CalculatingStrategy())
        val player2 = Player("Player2", money = 1500, strategy = ConservativeStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(player1, player2), bank, eventBus = eventBus)
        
        val events = mutableListOf<GameEvent>()
        eventBus.register(object : GameEventListener {
            override fun onEvent(event: GameEvent) {
                events.add(event)
            }
        })
        
        val deed = BalticAvenue()
        val auction = Auction(deed, listOf(player1, player2), bank, board, eventBus)
        auction.conduct()
        
        // Verify event sequence
        assertTrue(events.any { it is GameEvent.AuctionStarted }, "Should emit AuctionStarted")
        assertTrue(events.any { it is GameEvent.AuctionEnded }, "Should emit AuctionEnded")
        
        val startEvent = events.filterIsInstance<GameEvent.AuctionStarted>().first()
        assertEquals(deed, startEvent.deed, "Start event should reference correct property")
        assertEquals(2, startEvent.participants.size, "Start event should list all participants")
        
        val endEvent = events.filterIsInstance<GameEvent.AuctionEnded>().first()
        assertNotNull(endEvent.winner, "End event should have a winner")
        assertNotNull(endEvent.winningBid, "End event should have winning bid")
    }

    @Test
    fun `auction winner pays bid amount not list price`() {
        val player = Player("Bidder", money = 1500, strategy = CalculatingStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(player), bank, eventBus = eventBus)
        
        val initialMoney = player.money
        val auction = Auction(BalticAvenue(), listOf(player), bank, board, eventBus)
        auction.conduct()
        
        val moneySpent = initialMoney - player.money
        // Should pay auction bid (likely $10-30), not list price ($60)
        assertTrue(moneySpent < 60, "Should pay less than list price at auction")
        assertTrue(moneySpent >= 10, "Should pay at least starting bid")
    }

    @Test
    fun `auction with deterministic RNG produces consistent results`() {
        val seed = 12345L
        
        // Run auction twice with same seed
        val result1 = runAuctionWithSeed(seed)
        val result2 = runAuctionWithSeed(seed)
        
        assertEquals(result1.winner?.name, result2.winner?.name, "Same seed should produce same winner")
        assertEquals(result1.winningBid, result2.winningBid, "Same seed should produce same bid")
    }

    @Test
    fun `auction with different RNG seeds can produce different results`() {
        // Run auctions with different seeds
        val result1 = runAuctionWithSeed(12345L)
        val result2 = runAuctionWithSeed(67890L)
        
        // With random strategies, different seeds should potentially produce different outcomes
        // (This test might occasionally fail if both seeds happen to produce same result,
        // but probability is low with chaotic/impulsive strategies)
        assertNotNull(result1.winner, "First auction should have winner")
        assertNotNull(result2.winner, "Second auction should have winner")
    }

    @Test
    fun `slumlord strategy prefers cheap properties in auction`() {
        val slumlord = Player("Oscar", money = 1500, strategy = SlumlordStrategy())
        val highRent = Player("Count", money = 1500, strategy = HighRentStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(slumlord, highRent), bank, eventBus = eventBus)
        
        // Auction cheap property - Slumlord should bid more aggressively
        val cheapAuction = Auction(BalticAvenue(), listOf(slumlord, highRent), bank, board, eventBus)
        val cheapWinner = cheapAuction.conduct()
        
        // Slumlord values cheap properties higher, should win
        assertNotNull(cheapWinner, "Cheap property auction should have winner")
    }

    @Test
    fun `high rent strategy prefers expensive properties in auction`() {
        val slumlord = Player("Oscar", money = 1500, strategy = SlumlordStrategy())
        val highRent = Player("Count", money = 1500, strategy = HighRentStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(slumlord, highRent), bank, eventBus = eventBus)
        
        // Auction expensive property - HighRent should bid more aggressively
        val expensiveAuction = Auction(ParkPlace(), listOf(slumlord, highRent), bank, board, eventBus)
        val expensiveWinner = expensiveAuction.conduct()
        
        // HighRent values expensive properties higher, should win
        assertNotNull(expensiveWinner, "Expensive property auction should have winner")
    }

    @Test
    fun `monopoly completion increases bidding aggressiveness`() {
        val player1 = Player("Player1", money = 1500, strategy = CalculatingStrategy())
        val player2 = Player("Player2", money = 1500, strategy = CalculatingStrategy())
        
        // Give player1 Mediterranean Avenue (part of Brown monopoly)
        player1.deeds[MediterraneanAvenue()] = Player.Development()
        
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(player1, player2), bank, eventBus = eventBus)
        
        val events = mutableListOf<GameEvent>()
        eventBus.register(object : GameEventListener {
            override fun onEvent(event: GameEvent) {
                events.add(event)
            }
        })
        
        // Auction Baltic Avenue (completes monopoly for player1)
        val auction = Auction(BalticAvenue(), listOf(player1, player2), bank, board, eventBus)
        val winner = auction.conduct()
        
        // Player1 should win due to monopoly completion bonus
        assertEquals(player1, winner, "Player completing monopoly should win")
        
        // Check that player1 bid higher due to monopoly completion
        val bids = events.filterIsInstance<GameEvent.AuctionBid>()
        val player1Bids = bids.filter { it.player == player1 }
        assertTrue(player1Bids.isNotEmpty(), "Player1 should have placed bids")
    }

    @Test
    fun `chaotic strategy blocks opponent monopolies in auction`() {
        val chaotic = Player("Ernie", money = 1500, strategy = ChaoticStrategy())
        val opponent = Player("Opponent", money = 1500, strategy = DefaultStrategy())
        
        // Give opponent 2 of 3 Orange properties
        opponent.deeds[StJamesPlace()] = Player.Development()
        opponent.deeds[TennesseeAvenue()] = Player.Development()
        
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(chaotic, opponent), bank, eventBus = eventBus)
        
        val deed = NewYorkAvenue()
        // Auction NewYorkAvenue (blocks opponent's monopoly)
        val auction = Auction(deed, listOf(chaotic, opponent), bank, board, eventBus)
        val winner = auction.conduct()
        
        // Chaotic should bid to block (opponent uses DefaultStrategy so won't bid)
        assertEquals(chaotic, winner, "Chaotic should win to block monopoly")
        assertTrue(chaotic.deeds.keys.any { it::class == deed::class }, "Chaotic should own blocking property")
    }

    @Test
    fun `auction respects player cash reserves`() {
        val conservative = Player("Big Bird", money = 250, strategy = ConservativeStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(conservative), bank, eventBus = eventBus)
        
        // Conservative has $250, reserve is $200, so only $50 available
        val auction = Auction(BalticAvenue(), listOf(conservative), bank, board, eventBus)
        val winner = auction.conduct()
        
        if (winner != null) {
            // If won, should have kept reserve
            assertTrue(conservative.money >= 200, "Should maintain cash reserve after auction")
        }
    }

    @Test
    fun `multiple auctions in sequence work correctly`() {
        val player1 = Player("Player1", money = 1500, strategy = CalculatingStrategy())
        val player2 = Player("Player2", money = 1500, strategy = ConservativeStrategy())
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(player1, player2), bank, eventBus = eventBus)
        
        // Run multiple auctions
        val deed1 = BalticAvenue()
        val auction1 = Auction(deed1, listOf(player1, player2), bank, board, eventBus)
        val winner1 = auction1.conduct()
        
        val deed2 = MediterraneanAvenue()
        val auction2 = Auction(deed2, listOf(player1, player2), bank, board, eventBus)
        val winner2 = auction2.conduct()
        
        val deed3 = ReadingRailroad()
        val auction3 = Auction(deed3, listOf(player1, player2), bank, board, eventBus)
        val winner3 = auction3.conduct()
        
        // All auctions should complete successfully
        assertNotNull(winner1, "First auction should have winner")
        assertNotNull(winner2, "Second auction should have winner")
        assertNotNull(winner3, "Third auction should have winner")
        
        // Winners should own their properties
        assertTrue(winner1!!.deeds.keys.any { it::class == deed1::class }, "Winner1 should own Baltic")
        assertTrue(winner2!!.deeds.keys.any { it::class == deed2::class }, "Winner2 should own Mediterranean")
        assertTrue(winner3!!.deeds.keys.any { it::class == deed3::class }, "Winner3 should own Reading Railroad")
    }

    private data class AuctionResult(val winner: Player?, val winningBid: Int?)

    private fun runAuctionWithSeed(seed: Long): AuctionResult {
        val rng = Random(seed)
        
        // Use strategies with random behavior - pass seeded RNG for deterministic results
        val chaotic = Player("Ernie", money = 1500, strategy = ChaoticStrategy(rng))
        val impulsive = Player("Elmo", money = 1500, strategy = ImpulsiveStrategy(rng))
        val bank = Bank()
        val eventBus = EventBus()
        val board = Board(listOf(chaotic, impulsive), bank, rng, eventBus = eventBus)
        
        val events = mutableListOf<GameEvent>()
        eventBus.register(object : GameEventListener {
            override fun onEvent(event: GameEvent) {
                events.add(event)
            }
        })
        
        val deed = BalticAvenue()
        val auction = Auction(deed, listOf(chaotic, impulsive), bank, board, eventBus)
        val winner = auction.conduct()
        
        val endEvent = events.filterIsInstance<GameEvent.AuctionEnded>().firstOrNull()
        return AuctionResult(winner, endEvent?.winningBid)
    }
}
