package ca.jonathanfritz.monopoly.board

import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DiceTest {

    @RepeatedTest(100)
    fun rollTest() {
        val dice = Dice()
        val roll = dice.roll()
        assertEquals(roll, dice.previous)

        // two values between 1 and 6 inclusive were returned
        assertTrue(roll.die1 >= 1)
        assertTrue(roll.die1 <= 6)
        assertTrue(roll.die2 >= 1)
        assertTrue(roll.die2 <= 6)

        // their sum is greater than 1 and less than 13
        assertEquals(roll.die1 + roll.die2, roll.amount)
        assertTrue(roll.die1 + roll.die2 >= 2)
        assertTrue(roll.die1 + roll.die2 <= 12)

        if (roll.die1 >= roll.die2) {
            assertEquals(roll.die1, roll.highest)
        } else {
            assertEquals(roll.die2, roll.highest)
        }

        if (roll.die1 == roll.die2) {
            assertTrue(roll.isDoubles)
        } else {
            assertFalse(roll.isDoubles)
        }
    }

    @Test
    fun deterministicRollTest() {
        val seed = 11
        val expected = listOf(
            Dice.Roll(2, 2),
            Dice.Roll(4, 4),
            Dice.Roll(5, 1),
            Dice.Roll(2, 2),
            Dice.Roll(3, 1),
            Dice.Roll(1, 4),
            Dice.Roll(5, 4),
            Dice.Roll(2, 4),
            Dice.Roll(5, 1),
            Dice.Roll(3, 3),
            Dice.Roll(1, 1)
        )

        // a pair of dice with a known seed will always produce the same sequence of rolls
        val dice = Dice(Random(seed))
        val actual = (0 .. 10).map { dice.roll() }
        assertEquals(expected, actual)

        // even if we create another instance with that same seed
        val otherDice = Dice(Random(seed))
        val otherActual = (0 .. 10).map { otherDice.roll() }
        assertEquals(expected, otherActual)
    }
}