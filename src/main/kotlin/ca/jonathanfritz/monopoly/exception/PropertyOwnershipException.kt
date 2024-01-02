package ca.jonathanfritz.monopoly.exception

// thrown if a specified property is not owned
class PropertyOwnershipException(message: String): RuntimeException(message)