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
        val expected =
            listOf(
                ChanceCard.PoorTax,
                ChanceCard.BuildingAndLoan,
                ChanceCard.BankPaysYouDividend,
                ChanceCard.GetOutOfJailFree,
            )
        val deck = Deck(expected.toMutableList())

        // draw all four cards
        val actual = (0 until 4).map { deck.draw() }

        // one instance of each of the four cards on the deck was returned (i.e. no dupes until we reshuffle)
        assertEquals(4, actual.distinct().size)
        assertTrue(expected.containsAll(actual))
    }

    @Test
    fun overdrawTest() {
        // we've got a deck of cards
        val expected =
            listOf(
                ChanceCard.PoorTax,
                ChanceCard.BuildingAndLoan,
                ChanceCard.BankPaysYouDividend,
                ChanceCard.GetOutOfJailFree,
            )
        val deck = Deck(expected.toMutableList())

        // draw seven cards
        val actual = (0 until 7).map { deck.draw() }

        // we drew seven cards in total, but only four are distinct
        assertEquals(7, actual.size)
        assertEquals(4, actual.distinct().size)
        assertTrue(expected.containsAll(actual))

        // the deck re-shuffled upon overdraw, causing one unique three duplicate cards to be returned
        val singles =
            actual
                .groupBy { it }
                .filter { it.value.size == 1 }
                .flatMap { it.value }
                .distinct()
        val dupes =
            actual
                .groupBy { it }
                .filter { it.value.size == 2 }
                .flatMap { it.value }
                .distinct()
        assertEquals(1, singles.size)
        assertEquals(3, dupes.size)
    }

    @Test
    fun deterministicDrawTest() {
        val cards =
            listOf(
                ChanceCard.PoorTax,
                ChanceCard.BuildingAndLoan,
                ChanceCard.BankPaysYouDividend,
            )
        val seed = 10

        val expected =
            listOf(
                ChanceCard.BuildingAndLoan,
                ChanceCard.BankPaysYouDividend,
                ChanceCard.PoorTax,
                ChanceCard.BankPaysYouDividend,
                ChanceCard.PoorTax,
            )

        // when supplied with a seed, the order of the draw is fixed
        val deck = Deck(cards.toMutableList(), Random(seed))
        val actual = (0 until 5).map { deck.draw() }
        assertEquals(expected, actual)

        // if we create another deck with the same seed, its draw order will be the same even though it is a different instance
        val otherDeck = Deck(cards.toMutableList(), Random(seed))
        val otherActual = (0 until 5).map { otherDeck.draw() }
        assertEquals(expected, otherActual)
    }
}
