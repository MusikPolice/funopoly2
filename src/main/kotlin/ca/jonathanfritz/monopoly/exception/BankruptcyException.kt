package ca.jonathanfritz.monopoly.exception

// thrown when a player declares bankruptcy - this removes them from the game and transfers their remaining assets to
// either the bank or to the player who bankrupted them
class BankruptcyException(message: String): RuntimeException(message)