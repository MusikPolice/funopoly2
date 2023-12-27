package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.deed.Utility
import kotlin.reflect.KClass

sealed class Tile {

    abstract fun onLanding(player: Player, bank: Bank)

    class Go: Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on Go")
        }
    }

    abstract class Buyable(val deedClass: KClass<out TitleDeed>): Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on ${deedClass.simpleName}")
        }
    }

    class PropertyBuyable(deedClass: KClass<out Property>): Buyable(deedClass)

    class RailroadBuyable(deedClass: KClass<out Railroad>): Buyable(deedClass)

    class UtilityBuyable(deedClass: KClass<out Utility>): Buyable(deedClass)

    class CommunityChest(val side: Int): Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on CommunityChest (side $side)")
        }
    }

    // TODO test me!
    class IncomeTax: Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            val amount = player.incomeTaxAmount()
            println("\t\t${player.name} landed on IncomeTax and paid \$$amount")
            bank.charge(player, amount)
        }
    }

    class Chance(val side: Int): Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on Chance (side $side)")
        }
    }

    class Jail: Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on Jail")
        }
    }

    class FreeParking: Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on FreeParking")
        }
    }

    class GoToJail: Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on GoToJail")
        }
    }

    class LuxuryTax: Tile() {
        override fun onLanding(player: Player, bank: Bank) {
            println("\t\t${player.name} landed on LuxuryTax")
        }
    }
}