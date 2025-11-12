package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.deed.Utility
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import kotlin.reflect.KClass

sealed class Tile {
    abstract fun onLanding(
        player: Player,
        bank: Bank,
        board: Board,
        rentOverride: ((Player, Bank, Board) -> Int)? = null,
    )

    object Go : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            // nothing special happens here unless we're playing with house rules that double salary when the player lands on go
            println("\t\t${player.name} landed on Go")
        }
    }

    abstract class Buyable(
        val deedClass: KClass<out TitleDeed>,
    ) : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
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
                val deed = owner.deeds.keys.first { it::class == deedClass }
                if (owner.getDevelopment(deedClass).isMortgaged) {
                    println(
                        "\t\t${player.name} landed on ${deedClass.simpleName}. It is owned by ${owner.name}, but is mortgaged. No rent due.",
                    )
                } else {
                    println("\t\t${player.name} landed on ${deedClass.simpleName}. It is owned by ${owner.name}")
                    val rent = rentOverride?.invoke(player, bank, board) ?: deed.calculateRent(owner, board)
                    player.pay(rent, owner, bank, board, "in rent")
                }
            } else {
                // property is unowned, player that landed on it has the option to buy
                val deed = bank.deed(deedClass) ?: throw PropertyOwnershipException("${deedClass.simpleName} is not available for purchase")
                println($$"\t\t$${player.name} landed on $${deedClass.simpleName}. It can be purchased for $$${deed.price}")

                if (player.isBuying(deed)) {
                    bank.sellDeedToPlayer(deedClass, player, board)
                } else {
                    // TODO: a wild auction appears!
                    println("\t\t${player.name} declines to purchase the property")
                }
            }
        }
    }

    class PropertyBuyable(
        deedClass: KClass<out Property>,
    ) : Buyable(deedClass)

    class RailroadBuyable(
        deedClass: KClass<out Railroad>,
    ) : Buyable(deedClass)

    class UtilityBuyable(
        deedClass: KClass<out Utility>,
    ) : Buyable(deedClass)

    class CommunityChest(
        val side: Int,
    ) : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            println("\t\t${player.name} landed on CommunityChest (side $side)")
            board.communityChest.draw().onDraw(player, bank, board)
        }
    }

    object IncomeTax : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            val amount = player.incomeTaxAmount()
            bank.charge(amount, player, board, "in income tax")
        }
    }

    class Chance(
        val side: Int,
    ) : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            println("\t\t${player.name} landed on Chance (side $side)")
            board.chance.draw().onDraw(player, bank, board)
        }
    }

    object Jail : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            if (player.isInJail) {
                println("\t\t${player.name} is In Jail")
            } else {
                println("\t\t${player.name} is Just Visiting the Jail")
            }
        }
    }

    object FreeParking : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            // this does nothing unless house rule that awards the pot is active
            println("\t\t${player.name} landed on FreeParking")
        }
    }

    object GoToJail : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            println("\t\t${player.name} landed on GoToJail")
            board.goToJail(player)
        }
    }

    object LuxuryTax : Tile() {
        override fun onLanding(
            player: Player,
            bank: Bank,
            board: Board,
            rentOverride: ((Player, Bank, Board) -> Int)?,
        ) {
            bank.charge(100, player, board, "in luxury tax")
        }
    }
}
