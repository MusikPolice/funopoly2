package ca.jonathanfritz.monopoly.event

/**
 * Event bus for distributing game events to registered listeners.
 * Listeners are notified synchronously in the order they were registered.
 * Listener exceptions are caught and logged to prevent disrupting other listeners.
 */
class EventBus {
    private val listeners: MutableList<GameEventListener> = mutableListOf()

    /**
     * Register a listener to receive events.
     * @param listener The listener to register
     */
    fun register(listener: GameEventListener) {
        listeners.add(listener)
    }

    /**
     * Unregister a listener so it no longer receives events.
     * @param listener The listener to unregister
     */
    fun unregister(listener: GameEventListener) {
        listeners.remove(listener)
    }

    /**
     * Emit an event to all registered listeners.
     * If a listener throws an exception, it is caught and logged, but other listeners still receive the event.
     * @param event The event to emit
     */
    fun emit(event: GameEvent) {
        listeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                // Log but don't propagate listener exceptions
                // In a production system, we'd use proper logging here
                System.err.println("Listener ${listener::class.simpleName} threw exception handling event $event: ${e.message}")
            }
        }
    }
}
