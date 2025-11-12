@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Config
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Tile.*
import ca.jonathanfritz.monopoly.card.Card
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
    val dice: Dice = Dice(rng),
    // the Chance deck. See https://monopoly.fandom.com/wiki/Chance#Cards
    val chance: Deck<Card> =
        Deck(
            mutableListOf(
                Card.AdvanceToGo,
                ChanceCard.AdvanceToProperty(IllinoisAvenue::class),
                ChanceCard.AdvanceToProperty(StCharlesPlace::class),
                ChanceCard.AdvanceToNearestUtility,
                ChanceCard.AdvanceToNearestRailroad,
                ChanceCard.AdvanceToNearestRailroad,
                ChanceCard.BankPaysYouDividend,
                ChanceCard.GetOutOfJailFree,
                ChanceCard.GoBackThreeSpaces,
                Card.GoToJail,
                ChanceCard.GeneralRepairs,
                ChanceCard.AdvanceToRailroad(ReadingRailroad::class),
                ChanceCard.PoorTax,
                ChanceCard.AdvanceToProperty(Boardwalk::class),
                ChanceCard.ChairmanOfTheBoard,
                ChanceCard.BuildingAndLoan,
            ),
            rng,
        ),
    // the Community Chest deck. See https://monopoly.fandom.com/wiki/Community_Chest#Cards
    val communityChest: Deck<Card> =
        Deck(
            mutableListOf(
                Card.AdvanceToGo,
                CommunityChestCard.BankErrorInYourFavour,
                CommunityChestCard.DoctorsFees,
                CommunityChestCard.SaleOfStock,
                CommunityChestCard.GetOutOfJailFree,
                Card.GoToJail,
                CommunityChestCard.GrandOperaOpening,
                CommunityChestCard.HolidayFundMatures,
                CommunityChestCard.IncomeTaxRefund,
                CommunityChestCard.YourBirthday,
                CommunityChestCard.LifeInsurance,
                CommunityChestCard.HospitalFees,
                CommunityChestCard.SchoolFees,
                CommunityChestCard.ConsultancyFees,
                CommunityChestCard.StreetRepairs,
                CommunityChestCard.BeautyContest,
                CommunityChestCard.Inheritance,
            ),
            rng,
        ),
    private val config: Config = Config(),
) {
    // the board is made of tiles, starting by convention with Go
    private val tiles: List<Tile> =
        listOf(
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

    fun executeRound(round: Int) {
        println("\nRound $round:")

        // in each round, every player gets between one and three turns on which to affect the game state
        players.filterNot { it.isBankrupt() }.forEach { player ->
            println(
                $$"\n\tStarting $${player.name}'s turn $${if (player.isInJail) "In" else "on"} $${player.tileName()} with $$${player.money}",
            )

            // the player can get out of jail early by using a Get Out of Jail Free card or by paying a fee
            if (player.isInJail && player.remainingTurnsInJail > 0) {
                attemptToGetOutOfJail(player)
            }

            // each player rolls the dice
            var doublesCount = 0
            do {
                if (doublesCount > 0) println("\t${player.name} rolled doubles and gets another turn")
                val diceRoll = dice.roll()
                if (diceRoll.isDoubles) {
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
                        bank.charge(config.getOutOfJailEarlyFeeAmount, player, this, "to get out ouf jail")
                    }
                }
                println("\t\t${player.name} rolled a ${diceRoll.amount} ${if (diceRoll.isDoubles) "(doubles)" else ""}")

                // if the player is not in jail, they advance around the board
                if (!player.isInJail) {
                    advancePlayerBy(player, diceRoll.amount, true)
                }

                // after rolling the dice, players can opt to develop their monopolies
                player.developProperties(bank, this)

                // TODO: trading, unmortgaging, etc
            } while (diceRoll.isDoubles && doublesCount < 3 && !player.isBankrupt())
        }
    }

    private fun attemptToGetOutOfJail(player: Player) {
        val card = player.useGetOutOfJailFreeCard()
        if (card != null) {
            // return the card to the appropriate deck and let the player out of jail
            returnGetOutOfJailFreeCard(card)
            player.isInJail = false
        } else if (player.isPayingGetOutOfJailEarlyFee(config.getOutOfJailEarlyFeeAmount)) {
            // charge the player a fee and let them out of jail
            bank.charge(config.getOutOfJailEarlyFeeAmount, player, this, "to get out of jail early")
            player.isInJail = false
        }
    }

    fun returnGetOutOfJailFreeCard(card: Card.GetOutOfJailFreeCard) {
        when (card) {
            is ChanceCard.GetOutOfJailFree -> chance.add(card)
            is CommunityChestCard.GetOutOfJailFree -> communityChest.add(card)
        }
    }

    fun goToJail(player: Player) {
        player.isInJail = true
        advancePlayerToTile(player, Jail::class)
    }

    fun playerTile(player: Player) = tiles[player.position]

    private fun Player.tileName(): String =
        when (val tile = tiles[this.position]) {
            is Buyable -> tile.deedClass.simpleName!!
            is Chance -> "${tile::class.simpleName!!} (side ${tile.side})"
            is CommunityChest -> "${tile::class.simpleName!!} (side ${tile.side})"
            else -> tile::class.simpleName!!
        }

    // advances the player by the specified number of tiles
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    private fun advancePlayerBy(
        player: Player,
        offset: Int,
        collectSalary: Boolean = true,
        rentOverride: ((Player, Bank, Board) -> Int)? = null,
    ) {
        // figure out where the player landed
        val oldPosition = player.position
        player.position = player.positionOffset(offset)
        val newTile = tiles[player.position]

        // a turn action (dice roll, card effect, etc.) can advance the player at most tiles.size positions
        // importantly, players are only rewarded for passing go in a clockwise direction (i.e. because of a dice roll)
        // so if direction is positive and position is less than old position, the player either landed on or passed go
        val passedGo = offset > 0 && player.position < oldPosition
        if (passedGo && collectSalary) {
            bank.pay(200, player, "for passing go")
        }

        // process the events triggered by the player having landed on the new tile
        newTile.onLanding(player, bank, this, rentOverride)
    }

    // advances the player to the next instance of the indicated tile type
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToTile(
        player: Player,
        tileClass: KClass<out Tile>,
        rentOverride: ((Player, Bank, Board) -> Int)? = null,
    ) {
        (1 until tiles.size).forEach { offset ->
            if (tiles[player.positionOffset(offset)]::class == tileClass) {
                advancePlayerBy(player, offset, tileClass != Jail::class, rentOverride)
                return
            }
        }
        throw IllegalArgumentException(
            "Failed to find next ${tileClass.simpleName} after position ${player.position} (${tiles[player.position]::class})",
        )
    }

    // advances the player to the specified property
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToProperty(
        player: Player,
        propertyClass: KClass<out Property>,
    ) {
        (1 until tiles.size).forEach { offset ->
            val tile = tiles[player.positionOffset(offset)]
            if (tile is PropertyBuyable && tile.deedClass == propertyClass) {
                advancePlayerBy(player, offset)
                return
            }
        }
        throw IllegalArgumentException(
            "Failed to find ${propertyClass.simpleName} after position ${player.position} (${tiles[player.position]::class})",
        )
    }

    // advances the player to the specified railroad
    // returns the tile that the player landed on, and a boolean indicating whether they passed go
    fun advancePlayerToRailroad(
        player: Player,
        railroadClass: KClass<out Railroad>,
    ) {
        (1 until tiles.size).forEach { offset ->
            val tile = tiles[player.positionOffset(offset)]
            if (tile is RailroadBuyable && tile.deedClass == railroadClass) {
                advancePlayerBy(player, offset)
                return
            }
        }
        throw IllegalArgumentException(
            "Failed to find ${railroadClass.simpleName} after position ${player.position} (${tiles[player.position]::class})",
        )
    }

    fun goBackThreeSpaces(player: Player) {
        advancePlayerBy(player, -3)
    }

    private fun Player.positionOffset(offset: Int) = (position + offset).mod(tiles.size)
}
