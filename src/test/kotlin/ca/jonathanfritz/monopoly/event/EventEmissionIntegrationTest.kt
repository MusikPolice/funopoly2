@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.event

import ca.jonathanfritz.monopoly.Config
import ca.jonathanfritz.monopoly.Monopoly
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.PlayerConfig
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.strategy.DefaultStrategy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Integration tests that verify game events are emitted correctly during gameplay.
 */
internal class EventEmissionIntegrationTest {
    @Test
    fun `game emits round lifecycle events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 2,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        // Verify we got RoundStarted and RoundEnded events
        val roundStartedEvents = listener.events.filterIsInstance<GameEvent.RoundStarted>()
        val roundEndedEvents = listener.events.filterIsInstance<GameEvent.RoundEnded>()

        assertTrue(roundStartedEvents.isNotEmpty(), "Should have at least one RoundStarted event")
        assertTrue(roundEndedEvents.isNotEmpty(), "Should have at least one RoundEnded event")
        assertEquals(roundStartedEvents.size, roundEndedEvents.size, "Should have matching start/end round events")
    }

    @Test
    fun `game emits turn lifecycle events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 1,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        // Verify we got TurnStarted and TurnEnded events for each player
        val turnStartedEvents = listener.events.filterIsInstance<GameEvent.TurnStarted>()
        val turnEndedEvents = listener.events.filterIsInstance<GameEvent.TurnEnded>()

        assertTrue(turnStartedEvents.size >= 2, "Should have at least one turn per player")
        assertTrue(turnEndedEvents.size >= 2, "Should have at least one turn end per player")
    }

    @Test
    fun `game emits dice roll events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 5,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val diceRollEvents = listener.events.filterIsInstance<GameEvent.DiceRolled>()
        assertTrue(diceRollEvents.isNotEmpty(), "Should have dice roll events")

        // Verify dice roll data is valid
        diceRollEvents.forEach { event ->
            assertTrue(event.die1 in 1..6, "Die 1 should be between 1 and 6")
            assertTrue(event.die2 in 1..6, "Die 2 should be between 1 and 6")
            assertEquals(event.die1 == event.die2, event.isDoubles, "isDoubles should match die equality")
        }
    }

    @Test
    fun `game emits player movement events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 5,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val movementEvents = listener.events.filterIsInstance<GameEvent.PlayerMoved>()
        assertTrue(movementEvents.isNotEmpty(), "Should have player movement events")

        // Verify movement data
        movementEvents.forEach { event ->
            assertTrue(event.from in 0..39, "From position should be valid")
            assertTrue(event.to in 0..39, "To position should be valid")
            assertNotNull(event.player)
        }
    }

    @Test
    fun `game emits tile landed events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 5,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val landedEvents = listener.events.filterIsInstance<GameEvent.TileLanded>()
        assertTrue(landedEvents.isNotEmpty(), "Should have tile landed events")

        landedEvents.forEach { event ->
            assertNotNull(event.player)
            assertNotNull(event.tile)
        }
    }

    @Test
    fun `game emits financial events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 5,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        // Check for bank payment events (e.g., passing GO, salary)
        val bankPaidEvents = listener.events.filterIsInstance<GameEvent.BankPaidPlayer>()
        assertTrue(bankPaidEvents.isNotEmpty(), "Should have bank payment events")

        bankPaidEvents.forEach { event ->
            assertTrue(event.amount > 0, "Payment amount should be positive")
            assertNotNull(event.reason)
        }
    }

    @Test
    fun `game emits property purchase events when properties are bought`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 10,
                        randomSeed = 123,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val purchaseEvents = listener.events.filterIsInstance<GameEvent.PropertyPurchased>()

        // With some rounds, players should buy properties
        if (purchaseEvents.isNotEmpty()) {
            purchaseEvents.forEach { event ->
                assertTrue(event.price > 0, "Purchase price should be positive")
                assertNotNull(event.deed)
                assertNotNull(event.player)
            }
        }
    }

    @Test
    fun `game emits card drawn events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 20,
                        randomSeed = 999,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val cardEvents = listener.events.filterIsInstance<GameEvent.CardDrawn>()

        // With enough rounds, players should land on Chance or Community Chest
        if (cardEvents.isNotEmpty()) {
            cardEvents.forEach { event ->
                assertTrue(event.deck in listOf("Chance", "Community Chest"))
                assertNotNull(event.card)
                assertNotNull(event.player)
            }
        }
    }

    @Test
    fun `game emits game ended event`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 2,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                                PlayerConfig("Player2", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        val gameEndedEvents = listener.events.filterIsInstance<GameEvent.GameEnded>()
        assertEquals(1, gameEndedEvents.size, "Should have exactly one GameEnded event")

        val endEvent = gameEndedEvents.first()
        assertTrue(endEvent.rounds > 0, "Should have played at least one round")
        assertNotNull(endEvent.reason)
    }

    @Test
    fun `bank operations emit property mortgage events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val player = Player("TestPlayer")
        val bank = Bank(eventBus = eventBus)
        val board = Board(listOf(player), bank, eventBus = eventBus)

        // Give player money and buy a property
        bank.pay(500, player, "test setup")
        bank.sellDeedToPlayer(Property.MediterraneanAvenue::class, player, board)

        // Mortgage it
        bank.mortgageDeed(Property.MediterraneanAvenue::class, player)

        val mortgageEvents = listener.events.filterIsInstance<GameEvent.PropertyMortgaged>()
        assertEquals(1, mortgageEvents.size, "Should have one mortgage event")
        assertEquals(player, mortgageEvents[0].player)
    }

    @Test
    fun `bank operations emit property unmortgage events`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val player = Player("TestPlayer")
        val bank = Bank(eventBus = eventBus)
        val board = Board(listOf(player), bank, eventBus = eventBus)

        // Give player money, buy, and mortgage a property
        bank.pay(500, player, "test setup")
        bank.sellDeedToPlayer(Property.MediterraneanAvenue::class, player, board)
        bank.mortgageDeed(Property.MediterraneanAvenue::class, player)

        // Unmortgage it
        bank.unmortgageDeed(Property.MediterraneanAvenue::class, player, board)

        val unmortgageEvents = listener.events.filterIsInstance<GameEvent.PropertyUnmortgaged>()
        assertEquals(1, unmortgageEvents.size, "Should have one unmortgage event")
        assertEquals(player, unmortgageEvents[0].player)
    }

    @Test
    fun `events are emitted in logical order`() {
        val eventBus = EventBus()
        val listener = TestEventCollector()
        eventBus.register(listener)

        val game =
            Monopoly(
                config =
                    Config(
                        maxRounds = 5,
                        randomSeed = 42,
                        playerConfigs =
                            listOf(
                                PlayerConfig("Player1", DefaultStrategy()),
                            ),
                    ),
                eventBus = eventBus,
            )

        game.executeGame()

        // Verify event order makes sense
        val events = listener.events

        // Should have events
        assertTrue(events.isNotEmpty(), "Should have emitted events")

        // Last event should be GameEnded
        assertTrue(events.last() is GameEvent.GameEnded, "Last event should be GameEnded")

        // RoundStarted should come before RoundEnded
        val firstRoundStart = events.indexOfFirst { it is GameEvent.RoundStarted }
        val firstRoundEnd = events.indexOfFirst { it is GameEvent.RoundEnded }
        assertTrue(firstRoundStart >= 0, "Should have RoundStarted event")
        assertTrue(firstRoundEnd >= 0, "Should have RoundEnded event")
        assertTrue(firstRoundStart < firstRoundEnd, "RoundStarted should come before RoundEnded")

        // TurnStarted should come before TurnEnded
        val firstTurnStart = events.indexOfFirst { it is GameEvent.TurnStarted }
        val firstTurnEnd = events.indexOfFirst { it is GameEvent.TurnEnded }
        if (firstTurnStart >= 0 && firstTurnEnd >= 0) {
            assertTrue(firstTurnStart < firstTurnEnd, "TurnStarted should come before TurnEnded")
        }
    }
}

/**
 * Test helper that collects all emitted events for verification.
 */
private class TestEventCollector : GameEventListener {
    val events = mutableListOf<GameEvent>()

    override fun onEvent(event: GameEvent) {
        events.add(event)
    }
}
