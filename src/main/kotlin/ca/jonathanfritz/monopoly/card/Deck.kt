package ca.jonathanfritz.monopoly.card

import kotlin.random.Random

// Represents a deck of cards that can be drawn from
// The deck will be shuffled upon initialization and re-shuffled once drawn down
class Deck<T> (
    private var cards: List<T>,
    private val rng: Random = Random.Default
) {
    // first draw will shuffle the deck
    private var offset = cards.size

    fun draw(): T {
        if (offset == cards.size) {
            cards = cards.shuffled(rng)
            offset = 0
        }
        return cards[offset++]
    }
}