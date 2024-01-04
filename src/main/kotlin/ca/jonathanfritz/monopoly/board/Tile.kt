package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.deed.Utility
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import kotlin.reflect.KClass

sealed class Tile {

    abstract fun onLanding(player: Player, bank: Bank, board: Board)

    object Go : Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            // nothing special happens here unless we're playing with house rules that double salary when the player lands on go
            println("\t\t${player.name} landed on Go")
        }
    }

    abstract class Buyable(val deedClass: KClass<out TitleDeed>): Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            val owner = board.players.firstOrNull { it.isOwner(deedClass) }
            if (owner != null) {
                // paying yourself for a thing you own is silly
                if (owner == player) {
                    println("\t\t${player.name} landed on ${deedClass.simpleName}, a deed that they own")
                    return
                }

                // property is owned by someone else, player that landed on it must pay rent
                // TODO: rules say "The owner may not collect the rent if he/she fails to ask for it before the second player following throws the dice"
                //  which suggests that the owner should roll an attention check that has a chance of failing to demand rent
                println("\t\t${player.name} landed on ${deedClass.simpleName}. It is owned by ${owner.name}")
                val deed = owner.deeds.keys.first { it::class == deedClass }
                val rent = deed.calculateRent(owner, board)
                player.pay(owner, rent, "in rent")
            } else {
                // property is unowned, player that landed on it has the option to buy
                val deed = bank.deed(deedClass) ?: throw PropertyOwnershipException("${deedClass.simpleName} is not available for purchase")
                println("\t\t${player.name} landed on ${deedClass.simpleName}. It can be purchased for \$${deed.price}")

                if (player.isBuying(deed)) {
                   bank.sellPropertyToPlayer(deedClass, player)
               } else {
                   // TODO: a wild auction appears!
                   println("\t\t${player.name} declines to purchase the property")
               }
            }
        }
    }

    class PropertyBuyable(deedClass: KClass<out Property>): Buyable(deedClass)

    class RailroadBuyable(deedClass: KClass<out Railroad>): Buyable(deedClass)

    class UtilityBuyable(deedClass: KClass<out Utility>): Buyable(deedClass)

    class CommunityChest(val side: Int): Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on CommunityChest (side $side)")
            board.communityChest.draw().onDraw(player, bank, board)
        }
    }

    object IncomeTax : Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            val amount = player.incomeTaxAmount()
            bank.charge(player, amount, "in income tax")
        }
    }

    class Chance(val side: Int): Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on Chance (side $side)")
            board.chance.draw().onDraw(player, bank, board)
        }
    }

    object Jail : Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            // the player is just visiting, so this is a no-op
            println("\t\t${player.name} landed on Jail")
        }
    }

    object FreeParking : Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            // this does nothing unless house rule that awards the pot is active
            println("\t\t${player.name} landed on FreeParking")
        }
    }

    object GoToJail : Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            println("\t\t${player.name} landed on GoToJail")

            // TODO: player is collecting salary :/
            board.goToJail(player)
        }
    }

    object LuxuryTax : Tile() {
        override fun onLanding(player: Player, bank: Bank, board: Board) {
            bank.charge(player, 100, "in luxury tax")
        }
    }
}