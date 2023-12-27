package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Tile.*
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad.*
import ca.jonathanfritz.monopoly.deed.Utility.*
import kotlin.random.Random
import kotlin.reflect.KClass

class Board (
    private val bank: Bank,
    private val rng: Random = Random.Default,
    private val dice: Dice = Dice(rng),
    // TODO: decks of Community Chest and Chance cards
) {
    // the board is made of tiles, starting by convention with Go
    private val tiles: List<Tile> = listOf(
        Go(),
        PropertyBuyable(MediterraneanAvenue::class),
        CommunityChest(1),
        PropertyBuyable(BalticAvenue::class),
        IncomeTax(),
        RailroadBuyable(ReadingRailroad::class),
        PropertyBuyable(OrientalAvenue::class),
        Chance(1),
        PropertyBuyable(VermontAvenue::class),
        PropertyBuyable(ConnecticutAvenue::class),
        Jail(),
        PropertyBuyable(StCharlesPlace::class),
        UtilityBuyable(ElectricCompany::class),
        PropertyBuyable(StatesAvenue::class),
        PropertyBuyable(VirginiaAvenue::class),
        RailroadBuyable(PennsylvaniaRailroad::class),
        PropertyBuyable(StJamesPlace::class),
        CommunityChest(2),
        PropertyBuyable(TennesseeAvenue::class),
        PropertyBuyable(NewYorkAvenue::class),
        FreeParking(),
        PropertyBuyable(KentuckyAvenue::class),
        Chance(3),
        PropertyBuyable(IndianaAvenue::class),
        PropertyBuyable(IllinoisAvenue::class),
        RailroadBuyable(BAndORailroad::class),
        PropertyBuyable(AtlanticAvenue::class),
        PropertyBuyable(VentnorAvenue::class),
        UtilityBuyable(WaterWorks::class),
        PropertyBuyable(MarvinGardens::class),
        GoToJail(),
        PropertyBuyable(PacificAvenue::class),
        PropertyBuyable(NorthCarolinaAvenue::class),
        CommunityChest(4),
        PropertyBuyable(PennsylvaniaAvenue::class),
        RailroadBuyable(ShortlineRailroad::class),
        Chance(4),
        PropertyBuyable(ParkPlace::class),
        LuxuryTax(),
        PropertyBuyable(Boardwalk::class),
    )

    fun executeRound(players: List<Player>) {
        // in each round, every player gets between one and three turns on which to affect the game state
        players.forEach { player ->

            // each player rolls the dice
            println("\n\tStarting ${player.name}'s turn on ${player.tileName()}")
            var doublesCount = 0
            do {
                if (doublesCount > 0) println("\t${player.name} rolled doubles and gets another turn")
                val roll = dice.roll()
                if (roll.isDoubles) {
                    doublesCount++

                    // if the player has rolled three consecutive doubles, they go to jail without collecting a salary
                    if (doublesCount == 3) {
                        println("\t\t${player.name} rolled three consecutive doubles. Go to jail!")
                        player.goToJail()
                        break
                    }
                }
                println("\t\t${player.name} rolled a ${roll.amount}")

                // the player advances around the board
                val (tile, passedGo) = advancePlayerBy(player, roll.amount)
                if (passedGo) println("\t\t${player.name} passed Go!")
                tile.onLanding(player, bank)
            } while (roll.isDoubles && doublesCount < 3)
        }
    }

    private fun Player.goToJail() {
        advancePlayerToNext(this, Jail::class)
        inJail = true
    }

    private fun Player.tileName(): String {
        return when (val tile = tiles[this.position]) {
            is Buyable -> tile.deedClass.simpleName!!
            is Chance -> "${tile::class.simpleName!!} (side ${tile.side})"
            is CommunityChest -> "${tile::class.simpleName!!} (side ${tile.side})"
            else -> tile::class.simpleName!!
        }
    }

    // advances the player by the specified number of tiles
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerBy(player: Player, positions: Int): Pair<Tile, Boolean> {
        val oldPosition = player.position
        player.position = (oldPosition + positions) % tiles.size
        val newTile = tiles[player.position]

        // a turn action (dice roll, card effect, etc) can advance the player at most tiles.size positions
        // if new position is less than old position, the player either landed on or passed go
        return newTile to (player.position < oldPosition)
    }

    // advances the player to the next instance of the indicated tile type
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToNext(player: Player, tileClass: KClass<out Tile>): Pair<Tile, Boolean> {
        (1 until tiles.size).forEach { offset ->
           if (tiles[(player.position + offset) % tiles.size]::class == tileClass) {
               return advancePlayerBy(player, offset)
           }
        }
        throw IllegalArgumentException("Failed to find next ${tileClass.simpleName} after position ${player.position} (${tiles[player.position]::class})")
    }

    // advances the player to the specified property
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerTo(player: Player, propertyClass: KClass<out Property>): Pair<Tile, Boolean> {
        (1 until tiles.size).forEach { offset ->
            val tile = tiles[(player.position + offset) % tiles.size]
            if (tile is PropertyBuyable && tile.deedClass == propertyClass) {
                return advancePlayerBy(player, offset)
            }
        }
        throw IllegalArgumentException("Failed to find ${propertyClass.simpleName} after position ${player.position} (${tiles[player.position]::class})")
    }

}