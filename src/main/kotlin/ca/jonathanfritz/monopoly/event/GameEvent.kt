package ca.jonathanfritz.monopoly.event

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.card.Card
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Base class for all game events that can be emitted during gameplay.
 * Events represent significant state changes or actions in the game.
 */
sealed class GameEvent {
    // Movement Events
    data class RoundStarted(val round: Int) : GameEvent()
    data class TurnStarted(val player: Player, val round: Int) : GameEvent()
    data class DiceRolled(val player: Player, val die1: Int, val die2: Int, val isDoubles: Boolean) : GameEvent()
    data class PlayerMoved(val player: Player, val from: Int, val to: Int, val passedGo: Boolean) : GameEvent()
    data class TileLanded(val player: Player, val tile: Tile) : GameEvent()
    data class TurnEnded(val player: Player, val round: Int) : GameEvent()
    data class RoundEnded(val round: Int) : GameEvent()

    // Financial Events
    data class BankPaidPlayer(val player: Player, val amount: Int, val reason: String) : GameEvent()
    data class PlayerChargedByBank(val player: Player, val amount: Int, val reason: String) : GameEvent()
    data class RentPaid(val payer: Player, val recipient: Player, val amount: Int, val property: TitleDeed) : GameEvent()

    // Property Events
    data class PropertyPurchased(val player: Player, val deed: TitleDeed, val price: Int) : GameEvent()
    data class PropertyMortgaged(val player: Player, val deed: TitleDeed, val mortgageValue: Int) : GameEvent()
    data class PropertyUnmortgaged(val player: Player, val deed: TitleDeed, val cost: Int) : GameEvent()

    // Development Events
    data class HousePurchased(val player: Player, val property: Property, val houseCount: Int, val cost: Int) : GameEvent()
    data class HotelPurchased(val player: Player, val property: Property, val cost: Int) : GameEvent()
    data class HouseSold(val player: Player, val property: Property, val houseCount: Int, val proceeds: Int) : GameEvent()
    data class HotelSold(val player: Player, val property: Property, val proceeds: Int) : GameEvent()

    // Jail Events
    data class PlayerSentToJail(val player: Player, val reason: String) : GameEvent()
    data class PlayerLeftJail(val player: Player, val method: String) : GameEvent() // "rolled doubles", "paid fee", "used card"

    // Card Events
    data class CardDrawn(val player: Player, val deck: String, val card: Card) : GameEvent() // deck: "Chance" or "Community Chest"

    // Strategy Decision Events
    data class PropertyOffered(val player: Player, val deed: TitleDeed, val price: Int) : GameEvent()
    data class PurchaseDecision(val player: Player, val deed: TitleDeed, val decision: Boolean) : GameEvent() // true = buy, false = decline

    // Auction Events
    data class AuctionStarted(val deed: TitleDeed, val participants: List<Player>, val startingBid: Int) : GameEvent()
    data class AuctionBid(val player: Player, val deed: TitleDeed, val bidAmount: Int, val previousBid: Int) : GameEvent()
    data class AuctionPlayerDropped(val player: Player, val deed: TitleDeed, val finalBid: Int) : GameEvent()
    data class AuctionEnded(val deed: TitleDeed, val winner: Player?, val winningBid: Int?, val participantCount: Int, val totalRounds: Int) : GameEvent()

    // Bankruptcy Events
    data class PlayerBankrupted(val player: Player, val creditor: Any, val round: Int, val netWorth: Int) : GameEvent() // creditor: Player or Bank
    data class AssetTransferred(val from: Player, val to: Any, val asset: String, val value: Int) : GameEvent() // to: Player or Bank

    // Game End Events
    data class GameEnded(val winner: Player?, val rounds: Int, val reason: String) : GameEvent()
}
