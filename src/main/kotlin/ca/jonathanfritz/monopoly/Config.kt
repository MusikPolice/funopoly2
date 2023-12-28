package ca.jonathanfritz.monopoly

// captures all configurable aspects of the game, including options that deviate from the official ruleset
data class Config(

    // the amount that a player who is in jail must pay to be released if they do not have a Get Out of Jail Free card
    // to play, if they have failed to roll doubles for three consecutive turns, or if they wish to leave jail early
    val getOutOfJailEarlyFeeAmount: Int = 50
)
