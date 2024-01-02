package ca.jonathanfritz.monopoly.exception

// thrown if the player attempts to build on a property without having a monopoly
// on that property's colour group
class MonopolyOwnershipException(message: String): RuntimeException(message)