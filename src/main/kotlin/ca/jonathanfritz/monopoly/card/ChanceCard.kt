package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import kotlin.reflect.KClass

// TODO: tests! also the rest of the implementation. and update the Chance tile to pull the deck
sealed class ChanceCard : Card() {

    // Advance to "Go". (Collect $200)
    object AdvanceToGo : ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.advancePlayerToTile(player, Tile.Go::class)
            bank.pay(player, 200, "for passing go")
        }
    }

    // Advance to Illinois Ave. If you pass Go, collect $200.
    // Advance to St. Charles Place. If you pass Go, collect $200.
    // Take a walk on the Boardwalk. Advance token to Boardwalk.
    class AdvanceToProperty(
        private val propertyClass: KClass<out Property>
    ) : ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            val (_, passedGo) = board.advancePlayerToProperty(player, propertyClass)
            if (passedGo) {
                bank.pay(player, 200, "for passing go")
            }
        }
    }

    // Take a trip to Reading Railroad. If you pass Go, collect $200.
    class AdvanceToRailroad(
        private val railroadClass: KClass<out Railroad>
    ) : ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            val (_, passedGo) = board.advancePlayerToRailroad(player, railroadClass)
            if (passedGo) {
                bank.pay(player, 200, "for passing go")
            }
        }
    }

    // Advance token to the nearest Utility.
    // If unowned, you may buy it from the Bank. If owned, throw dice and pay owner a total 10 (ten) times the amount thrown.
    object AdvanceToNearestUtility: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            // TODO: how to temporarily modify rent amount?
            board.advancePlayerToTile(player, Tile.UtilityBuyable::class)
        }
    }

    // Advance to the nearest Railroad.
    // If unowned, you may buy it from the Bank. If owned, pay owner twice the rental to which they are otherwise entitled.
    object AdvanceToNearestRailroad: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            // TODO: how to temporarily modify rent amount?
            board.advancePlayerToTile(player, Tile.RailroadBuyable::class)
        }
    }

    // Bank pays you dividend of $50.
    object BankPaysYouDividend: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            bank.pay(player, 50, "bank dividend")
        }
    }

    object GetOutOfJailFree: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            player.hasGetOutOfJailFreeCard = true
        }
    }

    // Go back three spaces
    object GoBackThreeSpaces: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.advancePlayerBy(player, -3)
        }
    }

    // Go to Jail. Go directly to Jail. Do not pass GO, do not collect $200.
    object GoToJail: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.goToJail(player)
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
    object PoorTax: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            bank.charge(player, 15, "in poor tax")
        }
    }

    // You have been elected Chairman of the Board. Pay each player $50.
    object ChairmanOfTheBoard: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.players.filter { it != player }.forEach { other ->
                player.pay(other, 50, "as Chairman of the Board")
            }
        }
    }

    // Building and loan matures. Receive $150.
    object BuildingAndLoan: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            bank.pay(player, 150, "as building and loan matures")
        }
    }

    // You have won a crossword competition. Collect $100.
    object CrosswordCompetition: ChanceCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            bank.pay(player, 100, "for winning a crossword competition")
        }
    }

}
