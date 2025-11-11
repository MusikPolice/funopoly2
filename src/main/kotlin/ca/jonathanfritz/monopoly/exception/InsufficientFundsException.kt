package ca.jonathanfritz.monopoly.exception

// thrown if a player tries to buy an asset that they don't have the money for in cases where implicit liquidation of
// assets is not supported
class InsufficientFundsException(message: String) : RuntimeException(message)