package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.exception.InsufficientTokenException
import kotlin.reflect.KClass

sealed class ChanceCard : Card() {

    // Advance to Illinois Ave. If you pass Go, collect $200.
    // Advance to St. Charles Place. If you pass Go, collect $200.
    // Take a walk on the Boardwalk. Advance token to Boardwalk.
    class AdvanceToProperty(
        private val propertyClass: KClass<out Property>
    ) : ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} drew Advance to ${propertyClass.simpleName}")
            val (_, passedGo) = board.advancePlayerToProperty(player, propertyClass)
            if (passedGo) {
                bank.pay(player, 200, "for passing go")
            }
            // TODO: player should have to buy or pay rent on the property that they landed on :/
        }
    }

    // Take a trip to Reading Railroad. If you pass Go, collect $200.
    class AdvanceToRailroad(
        private val railroadClass: KClass<out Railroad>
    ) : ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} drew Advance to ${railroadClass.simpleName}")
            val (_, passedGo) = board.advancePlayerToRailroad(player, railroadClass)
            if (passedGo) {
                bank.pay(player, 200, "for passing go")
            }
            // TODO: player should have to buy or pay rent on the railroad that they landed on :/
        }
    }

    // Advance token to the nearest Utility.
    // If unowned, you may buy it from the Bank. If owned, throw dice and pay owner a total 10 (ten) times the amount thrown.
    object AdvanceToNearestUtility: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} drew Advance to Nearest Utility")
            // TODO: how to temporarily modify rent amount?
            board.advancePlayerToTile(player, Tile.UtilityBuyable::class)
            // TODO: player should have to buy or pay rent on the railroad that they landed on :/
        }
    }

    // Advance to the nearest Railroad.
    // If unowned, you may buy it from the Bank. If owned, pay owner twice the rental to which they are otherwise entitled.
    object AdvanceToNearestRailroad: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} drew Advance to Nearest Railroad")
            // TODO: how to temporarily modify rent amount?
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
            // TODO: player should have to buy or pay rent on the railroad that they landed on :/
        }
    }

    // Bank pays you dividend of $50.
    object BankPaysYouDividend: BankPaysYou(50, "bank dividend")

    // Get out of Jail Free - This card may be kept until needed or sold/traded
    object GetOutOfJailFree: GetOutOfJailFreeCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            if (board.chance.remove(this)) {
                println("\t\t${player.name} added a Get out of Jail Free card to their inventory")
                player.grantGetOutOfJailFreeCard(this)
            } else {
                throw InsufficientTokenException("Failed to grant ${player.name} Get Out of Jail Free card: Card is not present in Chance deck")
            }
        }
    }

    // Go back three spaces
    object GoBackThreeSpaces: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} drew Go Back Three Spaces")
            board.advancePlayerBy(player, -3)
        }
    }

    // Make general repairs on all your property: For each house pay $25, For each hotel $100.
    object GeneralRepairs: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            val (houses, hotels) = player.countDevelopments()
            val fee = houses * 25 + hotels * 100
            bank.charge(player, fee, "for general repairs on $houses houses and $hotels hotels")
        }
    }

    // Pay poor tax of $15
    object PoorTax: YouPayBank( 15, "in poor tax")

    // You have been elected Chairman of the Board. Pay each player $50.
    object ChairmanOfTheBoard: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.players.filter { it != player }.forEach { other ->
                player.pay(other, 50, "as Chairman of the Board")
            }
        }
    }

    // Building and loan matures. Receive $150.
    object BuildingAndLoan: BankPaysYou(150, "as building and loan matures")

    // You have won a crossword competition. Collect $100.
    object CrosswordCompetition: BankPaysYou(100, "for winning a crossword competition")
}
