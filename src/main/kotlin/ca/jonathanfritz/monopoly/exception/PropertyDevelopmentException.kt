package ca.jonathanfritz.monopoly.exception

// thrown if a player attempts to build an illegal number of houses or hotels on a property
class PropertyDevelopmentException(message: String) : RuntimeException(message)