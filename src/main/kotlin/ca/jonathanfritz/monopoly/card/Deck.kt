package ca.jonathanfritz.monopoly.card

import kotlin.random.Random

// Represents a deck of cards that can be drawn from
// The deck will be shuffled upon initialization and re-shuffled once drawn down
class Deck<T : Card> (
    private var cards: MutableList<T>,
    private val rng: Random = Random.Default
) {
    // first draw will shuffle the deck
    private var offset = cards.size

    fun draw(): T {
        if (offset >= cards.size) {
            cards = cards.shuffled(rng).toMutableList()
            offset = 0
        }
        return cards[offset++]
    }

    // inserts the specified card at the start of the deck so that it won't be drawn again until after the next shuffle
    fun add(card: T) = cards.add(0, card)

    // removes the specified card from the deck
    fun remove(card: T) = cards.remove(card)

    fun contains(card: T) = cards.contains(card)
}