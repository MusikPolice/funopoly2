@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.event

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class EventBusTest {
    @Test
    fun `can register a listener`() {
        val eventBus = EventBus()
        val listener = TestListener()

        eventBus.register(listener)

        // listener is registered successfully (no exception thrown)
        assertTrue(true)
    }

    @Test
    fun `can unregister a listener`() {
        val eventBus = EventBus()
        val listener = TestListener()

        eventBus.register(listener)
        eventBus.unregister(listener)

        // listener is unregistered successfully (no exception thrown)
        assertTrue(true)
    }

    @Test
    fun `emitting an event delivers it to registered listener`() {
        val eventBus = EventBus()
        val listener = TestListener()
        eventBus.register(listener)

        val event = GameEvent.RoundStarted(1)
        eventBus.emit(event)

        assertEquals(1, listener.eventsReceived.size)
        assertEquals(event, listener.eventsReceived[0])
    }

    @Test
    fun `emitting an event delivers it to multiple listeners`() {
        val eventBus = EventBus()
        val listener1 = TestListener()
        val listener2 = TestListener()
        eventBus.register(listener1)
        eventBus.register(listener2)

        val event = GameEvent.RoundStarted(1)
        eventBus.emit(event)

        assertEquals(1, listener1.eventsReceived.size)
        assertEquals(event, listener1.eventsReceived[0])
        assertEquals(1, listener2.eventsReceived.size)
        assertEquals(event, listener2.eventsReceived[0])
    }

    @Test
    fun `unregistered listener does not receive events`() {
        val eventBus = EventBus()
        val listener = TestListener()
        eventBus.register(listener)
        eventBus.unregister(listener)

        eventBus.emit(GameEvent.RoundStarted(1))

        assertEquals(0, listener.eventsReceived.size)
    }

    @Test
    fun `emitting with no listeners does not throw exception`() {
        val eventBus = EventBus()

        eventBus.emit(GameEvent.RoundStarted(1))

        // no exception thrown
        assertTrue(true)
    }

    @Test
    fun `events are delivered in order of emission`() {
        val eventBus = EventBus()
        val listener = TestListener()
        eventBus.register(listener)

        val event1 = GameEvent.RoundStarted(1)
        val event2 = GameEvent.RoundStarted(2)
        val event3 = GameEvent.RoundStarted(3)

        eventBus.emit(event1)
        eventBus.emit(event2)
        eventBus.emit(event3)

        assertEquals(3, listener.eventsReceived.size)
        assertEquals(event1, listener.eventsReceived[0])
        assertEquals(event2, listener.eventsReceived[1])
        assertEquals(event3, listener.eventsReceived[2])
    }

    @Test
    fun `listener exception does not prevent other listeners from receiving event`() {
        val eventBus = EventBus()
        val throwingListener = ThrowingListener()
        val normalListener = TestListener()

        eventBus.register(throwingListener)
        eventBus.register(normalListener)

        val event = GameEvent.RoundStarted(1)
        eventBus.emit(event)

        // throwing listener still received the event
        assertEquals(1, throwingListener.eventsReceived.size)
        // normal listener still received the event despite the exception from throwing listener
        assertEquals(1, normalListener.eventsReceived.size)
        assertEquals(event, normalListener.eventsReceived[0])
    }
}

// Test fixtures

private class TestListener : GameEventListener {
    val eventsReceived = mutableListOf<GameEvent>()

    override fun onEvent(event: GameEvent) {
        eventsReceived.add(event)
    }
}

private class ThrowingListener : GameEventListener {
    val eventsReceived = mutableListOf<GameEvent>()

    override fun onEvent(event: GameEvent) {
        eventsReceived.add(event)
        throw RuntimeException("Test exception")
    }
}
