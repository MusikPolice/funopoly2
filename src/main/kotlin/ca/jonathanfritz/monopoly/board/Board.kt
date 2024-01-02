package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Config
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Tile.*
import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.card.CommunityChestCard
import ca.jonathanfritz.monopoly.card.Deck
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Railroad.*
import ca.jonathanfritz.monopoly.deed.Utility.*
import kotlin.random.Random
import kotlin.reflect.KClass

class Board(
    val players: List<Player>,
    private val bank: Bank = Bank(),
    private val rng: Random = Random,
    private val dice: Dice = Dice(rng),
    private val config: Config = Config()
) {
    // the board is made of tiles, starting by convention with Go
    private val tiles: List<Tile> = listOf(
        Go,
        PropertyBuyable(MediterraneanAvenue::class),
        CommunityChest(1),
        PropertyBuyable(BalticAvenue::class),
        IncomeTax,
        RailroadBuyable(ReadingRailroad::class),
        PropertyBuyable(OrientalAvenue::class),
        Chance(1),
        PropertyBuyable(VermontAvenue::class),
        PropertyBuyable(ConnecticutAvenue::class),
        Jail,
        PropertyBuyable(StCharlesPlace::class),
        UtilityBuyable(ElectricCompany::class),
        PropertyBuyable(StatesAvenue::class),
        PropertyBuyable(VirginiaAvenue::class),
        RailroadBuyable(PennsylvaniaRailroad::class),
        PropertyBuyable(StJamesPlace::class),
        CommunityChest(2),
        PropertyBuyable(TennesseeAvenue::class),
        PropertyBuyable(NewYorkAvenue::class),
        FreeParking,
        PropertyBuyable(KentuckyAvenue::class),
        Chance(3),
        PropertyBuyable(IndianaAvenue::class),
        PropertyBuyable(IllinoisAvenue::class),
        RailroadBuyable(BAndORailroad::class),
        PropertyBuyable(AtlanticAvenue::class),
        PropertyBuyable(VentnorAvenue::class),
        UtilityBuyable(WaterWorks::class),
        PropertyBuyable(MarvinGardens::class),
        GoToJail,
        PropertyBuyable(PacificAvenue::class),
        PropertyBuyable(NorthCarolinaAvenue::class),
        CommunityChest(4),
        PropertyBuyable(PennsylvaniaAvenue::class),
        RailroadBuyable(ShortlineRailroad::class),
        Chance(4),
        PropertyBuyable(ParkPlace::class),
        LuxuryTax,
        PropertyBuyable(Boardwalk::class),
    )

    // the Chance deck. See https://monopoly.fandom.com/wiki/Chance#Cards
    private val chance: Deck<ChanceCard> = Deck(
        listOf(
            ChanceCard.AdvanceToGo,
            ChanceCard.AdvanceToProperty(IllinoisAvenue::class),
            ChanceCard.AdvanceToProperty(StCharlesPlace::class),
            ChanceCard.AdvanceToNearestUtility,
            ChanceCard.AdvanceToNearestRailroad,
            ChanceCard.AdvanceToNearestRailroad,
            ChanceCard.BankPaysYouDividend,
            ChanceCard.GetOutOfJailFree,
            ChanceCard.GoBackThreeSpaces,
            ChanceCard.GoToJail,
            ChanceCard.GeneralRepairs,
            ChanceCard.AdvanceToRailroad(ReadingRailroad::class),
            ChanceCard.PoorTax,
            ChanceCard.AdvanceToProperty(Boardwalk::class),
            ChanceCard.ChairmanOfTheBoard,
            ChanceCard.BuildingAndLoan
        ), rng
    )

    // TODO
    private val communityChest: Deck<CommunityChestCard> = Deck(emptyList(), rng)

    fun executeRound() {
        // in each round, every player gets between one and three turns on which to affect the game state
        players.forEach { player ->
            println("\n\tStarting ${player.name}'s turn on ${player.tileName()}")

            // the player can get out of jail early by using a Get Out of Jail Free card or by paying a fee
            if (player.isInJail && player.remainingTurnsInJail > 0) {
                if (player.isUsingGetOutOfJailFreeCard()) {
                    // TODO: test this once GooJFC is implemented
                    bank.useGetOutOfJailFreeCard(player)
                    player.isInJail = false
                } else if (player.isPayingGetOutOfJailEarlyFee(config.getOutOfJailEarlyFeeAmount)) {
                    bank.charge(player, config.getOutOfJailEarlyFeeAmount, "to get out of jail early")
                    player.isInJail = false
                }
            }

            // each player rolls the dice
            var doublesCount = 0
            do {
                if (doublesCount > 0) println("\t${player.name} rolled doubles and gets another turn")
                val roll = dice.roll()
                if (roll.isDoubles) {
                    doublesCount++

                    if (player.isInJail) {
                        // if the player is in jail, they are released upon rolling doubles
                        println("\t\t${player.name} rolled doubles and is released from jail early")
                        player.isInJail = false

                        // the player does not get another turn despite having rolled doubles
                        doublesCount = 3
                    } else if (doublesCount == 3) {
                        // if the player has rolled three consecutive doubles, they go directly to jail
                        println("\t\t${player.name} rolled three consecutive doubles. Go to jail!")
                        goToJail(player)

                        // the player's turn ends immediately
                        break
                    }
                } else if (player.isInJail) {
                    // the player has some number of turns to roll doubles, after which they must pay a fine
                    if (player.decrementRemainingTurnsInJail() == 0) {
                        bank.charge(player, config.getOutOfJailEarlyFeeAmount, "to get out ouf jail")
                    }
                }
                println("\t\t${player.name} rolled a ${roll.amount}")

                // if the player is not in jail, they advance around the board
                if (!player.isInJail) {
                    val (tile, passedGo) = advancePlayerBy(player, roll.amount)
                    if (passedGo) {
                        bank.pay(player, 200, "for passing go")
                    }
                    tile.onLanding(player, bank, this)
                }

                // TODO: trading, mortgaging, developing properties, etc

            } while (roll.isDoubles && doublesCount < 3)
        }
    }

    fun goToJail(player: Player) {
        advancePlayerToTile(player, Jail::class)
        player.isInJail = true
    }

    fun playerTile(player: Player) = tiles[player.position]

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
        player.position = player.positionOffset(positions)
        val newTile = tiles[player.position]

        // a turn action (dice roll, card effect, etc) can advance the player at most tiles.size positions
        // importantly, players are only rewarded for passing go in a clockwise direction (i.e. because of a dice roll)
        // so if direction is positive and position is less than old position, the player either landed on or passed go
        val passedGo = positions > 0 && player.position < oldPosition
        return newTile to passedGo
    }

    // advances the player to the next instance of the indicated tile type
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToTile(player: Player, tileClass: KClass<out Tile>): Pair<Tile, Boolean> {
        (1 until tiles.size).forEach { offset ->
            if (tiles[player.positionOffset(offset)]::class == tileClass) {
                return advancePlayerBy(player, offset)
            }
        }
        throw IllegalArgumentException("Failed to find next ${tileClass.simpleName} after position ${player.position} (${tiles[player.position]::class})")
    }

    // advances the player to the specified property
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToProperty(player: Player, propertyClass: KClass<out Property>): Pair<Tile, Boolean> {
        (1 until tiles.size).forEach { offset ->
            val tile = tiles[player.positionOffset(offset)]
            if (tile is PropertyBuyable && tile.deedClass == propertyClass) {
                return advancePlayerBy(player, offset)
            }
        }
        throw IllegalArgumentException("Failed to find ${propertyClass.simpleName} after position ${player.position} (${tiles[player.position]::class})")
    }

    // advances the player to the specified railroad
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToRailroad(player: Player, railroadClass: KClass<out Railroad>): Pair<Tile, Boolean> {
        (1 until tiles.size).forEach { offset ->
            val tile = tiles[player.positionOffset(offset)]
            if (tile is RailroadBuyable && tile.deedClass == railroadClass) {
                return advancePlayerBy(player, offset)
            }
        }
        throw IllegalArgumentException("Failed to find ${railroadClass.simpleName} after position ${player.position} (${tiles[player.position]::class})")
    }

    private fun Player.positionOffset(offset: Int) =
        (position + offset).mod(tiles.size)

    // TODO: test me
    fun drawChanceCard(player: Player) {
        val card = chance.draw()
        // TODO: if get out of jail free card was drawn AND a player has it in their inventory, draw another card to
        //  effectively skip it without mutating the deck
        card.onDraw(player, bank, this)
    }

}