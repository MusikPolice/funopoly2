package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Board.Tile.*
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad.*
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.deed.Utility.*
import kotlin.random.Random
import kotlin.reflect.KClass

class Board (
    val players: List<Player>,
    val bank: Bank = Bank(),
    val rng: Random = Random.Default,
    val dice: Dice = Dice(rng),
    // TODO: decks of Community Chest and Chance cards
) {
    // the board is made of tiles, starting by convention with Go
    private val tiles: List<Tile> = listOf(
        Go(),
        Buyable(MediterraneanAvenue::class),
        CommunityChest(1),
        Buyable(BalticAvenue::class),
        IncomeTax(),
        Buyable(ReadingRailroad::class),
        Buyable(OrientalAvenue::class),
        Chance(1),
        Buyable(VermontAvenue::class),
        Buyable(ConnecticutAvenue::class),
        Jail(),
        Buyable(StCharlesPlace::class),
        Buyable(ElectricCompany::class),
        Buyable(StatesAvenue::class),
        Buyable(VirginiaAvenue::class),
        Buyable(PennsylvaniaRailroad::class),
        Buyable(StJamesPlace::class),
        CommunityChest(2),
        Buyable(TennesseeAvenue::class),
        Buyable(NewYorkAvenue::class),
        FreeParking(),
        Buyable(KentuckyAvenue::class),
        Chance(3),
        Buyable(IndianaAvenue::class),
        Buyable(IllinoisAvenue::class),
        Buyable(BandORailroad::class),
        Buyable(AtlanticAvenue::class),
        Buyable(VentnorAvenue::class),
        Buyable(WaterWorks::class),
        Buyable(MarvinGardens::class),
        GoToJail(),
        Buyable(PacificAvenue::class),
        Buyable(NorthCarolinaAvenue::class),
        CommunityChest(4),
        Buyable(PennsylvaniaAvenue::class),
        Buyable(ShortlineRailroad::class),
        Chance(4),
        Buyable(ParkPlace::class),
        LuxuryTax(),
        Buyable(Boardwalk::class),
    )
    private val playerPositions: MutableMap<Player, Int> = mutableMapOf()

    init {
        // bank grants each player $1500 starting cash
        players.forEach { player ->
            bank.pay(player, 1500)
        }

        // each player starts on Go
        playerPositions.putAll(players.associateWith { 0 })
    }

    fun executeRound() {
        // in each round, every player gets one turn on which to affect the game state
        players.forEach { player ->
            println("\n\tStarting ${player.name}'s turn on ${player.tileName()}")
            var doublesCount = 0
            do {
                if (doublesCount > 0) println("\t${player.name} rolled doubles and gets another turn")
                val roll = dice.roll()
                if (roll.isDoubles) {
                    doublesCount++
                    if (doublesCount == 3) {
                        println("\t\t${player.name} rolled three consecutive doubles. Go to jail!")
                        val (tile, passedGo) = player.advanceToNext(Jail::class)
                        // TODO: put the player in jail? that's a different state from "just visiting"
                    }
                }
                println("\t\t${player.name} rolled a ${roll.amount}")

                val (tile, passedGo) = player.advanceBy(roll.amount)
                if (passedGo) println("\t\t${player.name} passed Go!")
                tile.onLanding(player)
            } while (roll.isDoubles && doublesCount < 3)
        }
    }

    private fun Player.tileName(): String {
        return when (val tile = tiles[playerPositions.getValue(this)]) {
            is Buyable -> tile.deedClass.simpleName!!
            is Chance -> "${tile::class.simpleName!!} (side ${tile.side})"
            is CommunityChest -> "${tile::class.simpleName!!} (side ${tile.side})"
            else -> tile::class.simpleName!!
        }
    }

    private fun Player.advanceBy(positions: Int): Pair<Tile, Boolean> {
        val oldPosition = playerPositions.getValue(this)
        val newPosition = (oldPosition + positions) % tiles.size
        playerPositions[this] = newPosition
        val newTile = tiles[playerPositions.getValue(this)]

        // a turn action (dice roll, card effect, etc) can advance the player at most tiles.size positions
        // if new position is less than old position, the player either landed on or passed go
        return newTile to (newPosition < oldPosition)
    }

    private fun Player.advanceToNext(tileClass: KClass<out Tile>): Pair<Tile, Boolean> {
        val currentPosition = playerPositions.getValue(this)
        (1 until tiles.size).forEach { offset ->
           if (tiles[(currentPosition + offset) % tiles.size]::class == tileClass) {
               return this.advanceBy(offset)
           }
        }
        throw IllegalArgumentException("Failed to find next ${tileClass.simpleName} after position $currentPosition (${tiles[currentPosition]::class})")
    }

    private sealed class Tile {

        // TODO: start implementing game logic
        abstract fun onLanding(player: Player)

        class Go: Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on Go")
            }
        }

        class Buyable(val deedClass: KClass<out TitleDeed>): Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on ${deedClass.simpleName}")
            }
        }

        class CommunityChest(val side: Int): Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on CommunityChest (side $side)")
            }
        }

        class IncomeTax: Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on IncomeTax")
            }
        }

        class Chance(val side: Int): Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on Chance (side $side)")
            }
        }

        class Jail: Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on Jail")
            }
        }

        class FreeParking: Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on FreeParking")
            }
        }

        class GoToJail: Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on GoToJail")
            }
        }

        class LuxuryTax: Tile() {
            override fun onLanding(player: Player) {
                println("\t\t${player.name} landed on LuxuryTax")
            }
        }
    }
}