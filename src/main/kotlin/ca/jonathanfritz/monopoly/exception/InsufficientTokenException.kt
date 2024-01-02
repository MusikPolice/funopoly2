package ca.jonathanfritz.monopoly.exception

// thrown if a player attempts to buy a house or hotel that the bank doesn't have
class InsufficientTokenException (message: String): RuntimeException(message)