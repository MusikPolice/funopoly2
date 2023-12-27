package ca.jonathanfritz.monopoly.board

import kotlin.random.Random

// represents a set of two six-sided dice that can be rolled repeatedly
open class Dice (
    private val rng: Random = Random.Default
) {

    open fun roll() = Roll(
        rng.nextInt(1, 6),
        rng.nextInt(1, 6),
    )

    data class Roll (
        val die1: Int,
        val die2: Int
    ) {
        val amount = die1 + die2
        val isDoubles = die1 == die2
        val highest = setOf(die1, die2).maxOrNull()!!
    }
}