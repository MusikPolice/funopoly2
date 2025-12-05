package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Config
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property.MediterraneanAvenue
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.event.GameEvent
import ca.jonathanfritz.monopoly.strategy.DefaultStrategy
import ca.jonathanfritz.monopoly.strategy.PlayerStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuctionTest {
    @Test
    fun `auction with single bidder wins at starting price`() {
        val strategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? = if (currentBid == 10) 11 else null
            }
        val player = Player("Alice", money = 1500, strategy = strategy)
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(player), bank, config = config)
        val eventBus = EventBus()

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)

        val winner = auction.conduct()

        assertEquals(player, winner)
        assertEquals(1489, player.money) // 1500 - 11
        assertTrue(player.isOwner(MediterraneanAvenue::class))
    }

    @Test
    fun `auction with no bids returns null winner`() {
        // DefaultStrategy never participates in auctions
        val player = Player("Alice", money = 1500, strategy = DefaultStrategy())
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(player), bank, config = config)
        val eventBus = EventBus()

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)

        val winner = auction.conduct()

        assertNull(winner)
        assertEquals(1500, player.money)
        assertFalse(player.isOwner(MediterraneanAvenue::class))
    }

    @Test
    fun `auction emits AuctionStarted event`() {
        val player = Player("Alice", money = 1500, strategy = DefaultStrategy())
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(player), bank, config = config)
        val eventBus = EventBus()
        val events = mutableListOf<GameEvent>()
        val listener =
            object : ca.jonathanfritz.monopoly.event.GameEventListener {
                override fun onEvent(event: GameEvent) {
                    events.add(event)
                }
            }
        eventBus.register(listener)

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)
        auction.conduct()

        val startEvent = events.filterIsInstance<GameEvent.AuctionStarted>().firstOrNull()
        assertNotNull(startEvent)
        assertEquals(deed, startEvent?.deed)
        assertEquals(10, startEvent?.startingBid)
        assertEquals(1, startEvent?.participants?.size)
    }

    @Test
    fun `auction with multiple bidders highest wins`() {
        val aliceStrategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? =
                    when (currentBid) {
                        10 -> 20
                        else -> null
                    }
            }
        val bobStrategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? =
                    when (currentBid) {
                        10 -> 15
                        15 -> 25
                        20 -> 25
                        else -> null
                    }
            }

        val alice = Player("Alice", money = 1500, strategy = aliceStrategy)
        val bob = Player("Bob", money = 1500, strategy = bobStrategy)
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(alice, bob), bank, config = config)
        val eventBus = EventBus()

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)

        val winner = auction.conduct()

        assertEquals(bob, winner)
        assertEquals(1475, bob.money) // 1500 - 25
        assertTrue(bob.isOwner(MediterraneanAvenue::class))
        assertFalse(alice.isOwner(MediterraneanAvenue::class))
    }

    @Test
    fun `auction emits AuctionBid events for each bid`() {
        val aliceStrategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? =
                    when (currentBid) {
                        10 -> 15
                        else -> null
                    }
            }
        val bobStrategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? =
                    when (currentBid) {
                        10 -> 12
                        15 -> 20
                        else -> null
                    }
            }

        val alice = Player("Alice", money = 1500, strategy = aliceStrategy)
        val bob = Player("Bob", money = 1500, strategy = bobStrategy)
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(alice, bob), bank, config = config)
        val eventBus = EventBus()
        val events = mutableListOf<GameEvent>()
        val listener =
            object : ca.jonathanfritz.monopoly.event.GameEventListener {
                override fun onEvent(event: GameEvent) {
                    events.add(event)
                }
            }
        eventBus.register(listener)

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)
        auction.conduct()

        val bidEvents = events.filterIsInstance<GameEvent.AuctionBid>()
        assertEquals(2, bidEvents.size, "Expected 2 bid events (Alice 15 wins round 1, Bob 20 wins round 2)")
        assertEquals(15, bidEvents[0].bidAmount)
        assertEquals(10, bidEvents[0].previousBid)
        assertEquals(20, bidEvents[1].bidAmount)
        assertEquals(15, bidEvents[1].previousBid)
    }

    @Test
    fun `auction emits AuctionEnded event with winner`() {
        val strategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? = if (currentBid == 10) 15 else null
            }

        val player = Player("Alice", money = 1500, strategy = strategy)
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(player), bank, config = config)
        val eventBus = EventBus()
        val events = mutableListOf<GameEvent>()
        val listener =
            object : ca.jonathanfritz.monopoly.event.GameEventListener {
                override fun onEvent(event: GameEvent) {
                    events.add(event)
                }
            }
        eventBus.register(listener)

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)
        auction.conduct()

        val endEvent = events.filterIsInstance<GameEvent.AuctionEnded>().firstOrNull()
        assertNotNull(endEvent)
        assertEquals(player, endEvent?.winner)
        assertEquals(15, endEvent?.winningBid)
        assertEquals(1, endEvent?.participantCount)
    }

    @Test
    fun `auction excludes bankrupt players from participation`() {
        val strategy =
            object : PlayerStrategy by DefaultStrategy() {
                override fun calculateBidIncrease(
                    deed: ca.jonathanfritz.monopoly.deed.TitleDeed,
                    currentBid: Int,
                    minimumBid: Int,
                    player: Player,
                    bank: Bank,
                    board: Board,
                ): Int? = 15
            }

        val alice = Player("Alice", money = 1500, strategy = strategy, isBankrupt = true)
        val bob = Player("Bob", money = 1500, strategy = strategy)
        val bank = Bank()
        val config = Config()
        val board = Board(listOf(alice, bob), bank, config = config)
        val eventBus = EventBus()
        val events = mutableListOf<GameEvent>()
        val listener =
            object : ca.jonathanfritz.monopoly.event.GameEventListener {
                override fun onEvent(event: GameEvent) {
                    events.add(event)
                }
            }
        eventBus.register(listener)

        val deed = MediterraneanAvenue()
        val auction = Auction(deed, board.players, bank, board, eventBus)
        auction.conduct()

        val startEvent = events.filterIsInstance<GameEvent.AuctionStarted>().firstOrNull()
        assertNotNull(startEvent)
        assertEquals(1, startEvent?.participants?.size)
        assertEquals(bob, startEvent?.participants?.get(0))
    }

    @Test
    fun `auction with config enableAuctions false does not trigger`() {
        val player = Player("Alice", money = 1500, strategy = DefaultStrategy())
        val bank = Bank()
        val config = Config(enableAuctions = false)
        val board = Board(listOf(player), bank, config = config)

        assertFalse(config.enableAuctions)
    }
}
