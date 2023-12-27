package ca.jonathanfritz.monopoly.exception

// thrown when a player runs out of money
class InsufficientFundsException(message: String): RuntimeException(message)