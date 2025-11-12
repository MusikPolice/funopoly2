package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.card.CommunityChestCard
import ca.jonathanfritz.monopoly.card.Deck
import ca.jonathanfritz.monopoly.deed.Property
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TileTest {
    @Test
    fun `income tax tile charges player upon landing`() {
        val player = Player("Big Bird", 100)
        val bank = Bank(money = 0)
        val incomeTax = Tile.IncomeTax

        incomeTax.onLanding(player, bank, Board(listOf(player)))

        assertEquals(90, player.money)
        assertEquals(10, bank.money)
    }

    @Test
    fun `luxury tax tile charges player upon landing`() {
        val player = Player("Snuffy", 200)
        val bank = Bank(money = 0)
        val luxuryTax = Tile.LuxuryTax

        luxuryTax.onLanding(player, bank, Board(listOf(player)))

        assertEquals(100, player.money)
        assertEquals(100, bank.money)
    }

    @Test
    fun `go to jail tile puts player in jail upon landing`() {
        val player = Player("Ernie")
        val board = Board(listOf(player))

        // player lands on Go To Jail, does not receive their salary for passing Go
        board.advancePlayerToTile(player, Tile.GoToJail::class)
        assertTrue(player.isInJail)
        assertEquals(0, player.money)
    }

    @Test
    fun `community chest draws and plays a card upon landing`() {
        val player = Player("Bert", money = 0)
        val bank = Bank(money = 100)
        val board = Board(listOf(player), communityChest = Deck(mutableListOf(CommunityChestCard.Inheritance)))

        // player draws the only available community chest card and is paid a $100 inheritance
        Tile.CommunityChest(1).onLanding(player, bank, board)
        assertEquals(100, player.money)
        assertEquals(0, bank.money)
    }

    @Test
    fun `chance draws and plays a card upon landing`() {
        val player = Player("Bert", money = 15)
        val bank = Bank(money = 0)
        val board = Board(listOf(player), chance = Deck(mutableListOf(ChanceCard.PoorTax)))

        // player draws the only available chance card and pays a $15 poor tax
        Tile.Chance(1).onLanding(player, bank, board)
        assertEquals(0, player.money)
        assertEquals(15, bank.money)
    }

    @Test
    fun `player does not pay rent upon landing on buyable that they own`() {
        val player =
            Player(
                "Ernie",
                money = 10,
                deeds =
                    mutableMapOf(
                        Property.BalticAvenue() to Player.Development(),
                    ),
            )
        val bank = Bank()
        val board = Board(listOf(player))

        // this test doesn't really stop a player from paying rent to themselves, but it does ensure that they don't
        // pay rent to some other player or to the bank
        Tile.PropertyBuyable(Property.BalticAvenue::class).onLanding(player, bank, board)
        assertEquals(10, player.money)
    }

    @Test
    fun `player pays rent to owner upon landing on owned buyable`() {
        val owner =
            Player(
                "Bert",
                deeds =
                    mutableMapOf(
                        Property.BalticAvenue() to Player.Development(),
                    ),
            )
        val player = Player("Ernie", money = 10)
        val bank = Bank()
        val board = Board(listOf(owner, player))

        Tile.PropertyBuyable(Property.BalticAvenue::class).onLanding(player, bank, board)
        assertEquals(6, player.money)
        assertEquals(4, owner.money)
    }

    @Test
    fun `player buys deed upon landing on unowned buyable`() {
        val player = Player("Ernie", money = 100)
        val bank = Bank(money = 0)
        val board = Board(listOf(player))

        Tile.PropertyBuyable(Property.BalticAvenue::class).onLanding(player, bank, board)
        assertEquals(40, player.money)
        assertEquals(60, bank.money)
    }
}
