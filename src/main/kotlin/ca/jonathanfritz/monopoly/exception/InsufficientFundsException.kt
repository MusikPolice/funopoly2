package ca.jonathanfritz.monopoly.exception

// thrown when a player runs out of money
// TODO: catch this and give the player a chance to liquidate assets before declaring bankruptcy
//  also, the game can (and should) proceed without that player
class InsufficientFundsException(message: String): RuntimeException(message)