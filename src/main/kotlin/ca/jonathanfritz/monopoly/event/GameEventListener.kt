package ca.jonathanfritz.monopoly.event

/**
 * Interface for components that want to observe game events.
 * Implementations should handle events without throwing exceptions when possible.
 */
interface GameEventListener {
    /**
     * Called when an event is emitted.
     * @param event The event that occurred
     */
    fun onEvent(event: GameEvent)
}
