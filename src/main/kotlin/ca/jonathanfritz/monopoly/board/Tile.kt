package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.deed.Utility
import kotlin.reflect.KClass

sealed class Tile {

    abstract fun onLanding(player: Player, bank: Bank, board: Board)

    class Go: Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            // TODO: nothing special happens here unless we're playing with house rules that double salary when the player lands on go
            println("\t\t${player.name} landed on Go")
        }
    }

    abstract class Buyable(val deedClass: KClass<out TitleDeed>): Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on ${deedClass.simpleName}")
            // TODO: give player the option to buy, trigger an auction if they decline
        }
    }

    class PropertyBuyable(deedClass: KClass<out Property>): Buyable(deedClass)

    class RailroadBuyable(deedClass: KClass<out Railroad>): Buyable(deedClass)

    class UtilityBuyable(deedClass: KClass<out Utility>): Buyable(deedClass)

    class CommunityChest(val side: Int): Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on CommunityChest (side $side)")
            // TODO: draw a card and do what it says
        }
    }

    class IncomeTax: Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            val amount = player.incomeTaxAmount()
            bank.charge(player, amount, "in income tax")
        }
    }

    class Chance(val side: Int): Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on Chance (side $side)")
            // TODO: draw a card and do what it says
        }
    }

    class Jail: Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            // the player is just visiting, so this is a no-op
            println("\t\t${player.name} landed on Jail")
        }
    }

    class FreeParking: Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            // this does nothing unless house rule that awards the pot is active
            println("\t\t${player.name} landed on FreeParking")
        }
    }

    class GoToJail: Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on GoToJail")
            board.goToJail(player)
        }
    }

    class LuxuryTax: Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            bank.charge(player, 100, "in luxury tax")
        }
    }
}