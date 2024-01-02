package ca.jonathanfritz.monopoly.card

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class DeckTest {

    @Test
    fun drawTest() {
        // we've got a deck of cards
        val expected = listOf(1, 2, 3, 4, 5)
        val deck = Deck(expected)

        // draw five cards
        val actual = (0 until 5).map { deck.draw() }

        // one instance of each of the five cards on the deck was returned (i.e. no dupes until we reshuffle)
        assertEquals(5, actual.distinct().size)
        assertTrue(expected.containsAll(actual))

        // but they were returned out of order (i.e. the deck was shuffled)
        assertNotEquals(expected, actual)
    }

    @Test
    fun overdrawTest() {
        // we've got a deck of cards
        val expected = listOf(1, 2, 3, 4, 5)
        val deck = Deck(expected)

        // draw seven cards
        val actual = (0 until 7).map { deck.draw() }

        // we drew seven cards in total, but only five are distinct
        assertEquals(7, actual.size)
        assertEquals(5, actual.distinct().size)
        assertTrue(expected.containsAll(actual))

        // the deck re-shuffled upon overdraw, causing exactly two duplicate cards to be returned
        val singles = actual.groupBy { it }.filter { it.value.size == 1}.flatMap { it.value }.distinct()
        val dupes = actual.groupBy { it }.filter { it.value.size == 2}.flatMap { it.value }.distinct()
        assertEquals(3, singles.size)
        assertEquals(2, dupes.size)
    }

    @Test
    fun deterministicDrawTest() {
        val cards = listOf(1, 2, 3)
        val seed = 10

        val expected = listOf(2, 3, 1, 3, 1)

        // when supplied with a seed, the order of the draw is fixed
        val deck = Deck(cards, Random(seed))
        val actual = (0 until 5).map { deck.draw() }
        assertEquals(expected, actual)

        // if we create another deck with the same seed, its draw order will be the same even though it is a different instance
        val otherDeck = Deck(cards, Random(seed))
        val otherActual = (0 until 5).map { otherDeck.draw() }
        assertEquals(expected, otherActual)
    }
}