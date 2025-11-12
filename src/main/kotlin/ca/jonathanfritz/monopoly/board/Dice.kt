package ca.jonathanfritz.monopoly.board

import kotlin.random.Random

// represents a set of two six-sided dice that can be rolled repeatedly
open class Dice(
    private val rng: Random = Random.Default,
) {
    // Electric Company's rent is dependent on the value of the previous dice roll, so we'll cache it for use in that
    // calculation to avoid having to pass Roll to every function in Board
    internal lateinit var previous: Roll

    open fun roll(): Roll {
        previous =
            Roll(
                rng.nextInt(1, 6),
                rng.nextInt(1, 6),
            )
        return previous
    }

    fun previousRoll() = previous

    data class Roll(
        val die1: Int,
        val die2: Int,
    ) {
        val amount = die1 + die2
        val isDoubles = die1 == die2
        val highest = setOf(die1, die2).maxOrNull()!!
    }
}
