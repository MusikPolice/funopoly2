package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.Utility
import ca.jonathanfritz.monopoly.exception.InsufficientFundsException
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PlayerTest {

    @Test
    fun isOwnerTest() {
        // this player owns Boardwalk and Park Place, giving them a Monopoly on the dark blue properties
        val highRoller = Player(
            "High Roller",
            deeds = ColourGroup.DarkBlue.titleDeeds().values.associateWith { Player.Development() }.toMutableMap()
        )
        assertTrue(highRoller.isOwner(ParkPlace::class))
        assertTrue(highRoller.isOwner(Boardwalk::class))
        Property.values.keys.filterNot { it == ParkPlace::class || it == Boardwalk::class }
            .forEach { unownedProperty -> assertFalse(highRoller.isOwner(unownedProperty)) }
    }

    @Test
    fun `player has monopoly on properties`() {
        val player = Player(
            "Low Roller",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(),
                BalticAvenue() to Player.Development()
            )
        )
        assertTrue(player.hasMonopoly(ColourGroup.Brown))
        ColourGroup.values().filterNot { it == ColourGroup.Brown }.forEach { assertFalse(player.hasMonopoly(it)) }
    }

    @Test
    fun `player has monopoly on railroads`() {
        val player = Player(
            "Engineer",
            deeds = mutableMapOf(
                Railroad.ReadingRailroad() to Player.Development(),
                Railroad.PennsylvaniaRailroad() to Player.Development(),
                Railroad.BAndORailroad() to Player.Development(),
                Railroad.ShortlineRailroad() to Player.Development()
            )
        )
        assertTrue(player.hasMonopoly(ColourGroup.Railroads))
        ColourGroup.values().filterNot { it == ColourGroup.Railroads }.forEach { assertFalse(player.hasMonopoly(it)) }
    }

    @Test
    fun `player has monopoly on utilities`() {
        val player = Player(
            "Industrialist",
            deeds = mutableMapOf(
                Utility.ElectricCompany() to Player.Development(),
                Utility.WaterWorks() to Player.Development(),
            )
        )
        assertTrue(player.hasMonopoly(ColourGroup.Utilities))
        ColourGroup.values().filterNot { it == ColourGroup.Utilities }.forEach { assertFalse(player.hasMonopoly(it)) }
    }

    @Test
    fun `player networth with cash only`() {
        val player = Player(
            "Cash Only",
            money = 5000
        )
        assertEquals(5000, player.netWorth())
        assertEquals(200, player.incomeTaxAmount())
    }

    @Test
    fun `player networth with property only`() {
        val player = Player(
            "Property Only",
            deeds = mutableMapOf(
                OrientalAvenue() to Player.Development(),
                NewYorkAvenue() to Player.Development(),
                Railroad.ReadingRailroad() to Player.Development(),
                Utility.WaterWorks() to Player.Development()
            )
        )
        assertEquals(650, player.netWorth())
        assertEquals(65, player.incomeTaxAmount())
    }

    @Test
    fun `player networth with cash, property, and houses`() {
        val player = Player(
            "Houses",
            money = 200,
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(),
                BalticAvenue() to Player.Development(numHouses = 2)
            )
        )
        assertEquals(420, player.netWorth())
        assertEquals(42, player.incomeTaxAmount())
    }

    @Test
    fun `player networth with cash, property, houses, and hotels`() {
        val withHotels = Player(
            "Hotels",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 4),
                BalticAvenue() to Player.Development(numHouses = 4, hasHotel = true)
            )
        )
        assertEquals(570, withHotels.netWorth())
        assertEquals(57, withHotels.incomeTaxAmount())
    }

    @Test
    fun `isInJail and decrementRemainingTurnsInJail mutate player state as expected`() {
        val player = Player("Snuffleupagus")
        assertPlayerIsNotInJail(player)

        // attempting to decrement the value when the player is not in jail is a no-op
        assertEquals(0, player.decrementRemainingTurnsInJail())
        assertPlayerIsNotInJail(player)

        // but once the player is in jail, the decrement function can be used to track how many turns the player must spend there
        player.isInJail = true
        assertTrue(player.isInJail)
        assertEquals(3, player.remainingTurnsInJail)

        assertEquals(2, player.decrementRemainingTurnsInJail())
        assertEquals(1, player.decrementRemainingTurnsInJail())

        // the final decrement operation mutates isInJail too
        assertEquals(0, player.decrementRemainingTurnsInJail())
        assertPlayerIsNotInJail(player)

        // if we put the player back in jail, we can cancel that state
        player.isInJail = true
        assertTrue(player.isInJail)
        assertEquals(3, player.remainingTurnsInJail)
        assertEquals(2, player.decrementRemainingTurnsInJail())
        player.isInJail = false
        assertPlayerIsNotInJail(player)
    }

    private fun assertPlayerIsNotInJail(player: Player) {
        assertFalse(player.isInJail)
        assertEquals(0, player.remainingTurnsInJail)
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns false if player is not in jail`() {
        val player = Player("Cookie Monster", money = 100)
        assertFalse(player.isInJail)
        assertFalse(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns false if player is in jail but does not have enough money to pay the fine`() {
        val player = Player("Cookie Monster", money = 10)
        player.isInJail = true
        assertFalse(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns false if player is in jail and can afford the fine and has get out of jail free card`() {
        val player = Player("Cookie Monster", money = 100, getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        player.isInJail = true
        assertFalse(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns true if player is in jail and can afford the fine`() {
        val player = Player("Cookie Monster", money = 100)
        player.isInJail = true
        assertTrue(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `useGetOutOfJailFreeCard returns null if the player is not in jail`() {
        val player = Player("Cookie Monster", money = 100, getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        assertNull(player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `UseGetOutOfJailFreeCard returns null if the player does not have a card to play`() {
        val player = Player("Cookie Monster", money = 100)
        player.isInJail = true
        assertNull(player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `useGetOutOfJailFreeCard returns a card if the player is in jail and has a card in their inventory`() {
        val player = Player("Cookie Monster", money = 100, getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        player.isInJail = true
        assertEquals(ChanceCard.GetOutOfJailFree, player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `pay player negative value throws exception`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        assertThrows<IllegalArgumentException> {
            source.pay(target, -10)
        }

        assertEquals(10, source.money)
        assertEquals(0, target.money)
    }

    @Test
    fun `pay player zero doesn't change balances`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        source.pay(target, 0)

        assertEquals(10, source.money)
        assertEquals(0, target.money)
    }

    @Test
    fun `pay more money than available throws InsufficientFundsException`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        assertThrows<InsufficientFundsException> {
            source.pay(target, 20)
        }

        assertEquals(10, source.money)
        assertEquals(0, target.money)
    }

    @Test
    fun `pay player test`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        source.pay(target, 10)

        assertEquals(0, source.money)
        assertEquals(10, target.money)
    }

    @Test
    fun `getDevelopment for unowned property throws property ownership`() {
        val player = Player("Cookie")
        assertThrows<PropertyOwnershipException> {
            player.getDevelopment(BalticAvenue::class)
        }
    }

    @Test
    fun `getDevelopment for owned property`() {
        val expected = Player.Development()
        val player = Player(
            "Cookie",
            deeds = mutableMapOf(BalticAvenue() to expected)
        )
        assertEquals(expected, player.getDevelopment(BalticAvenue::class))
    }

    @Test
    fun `isBuying returns false if player cannot afford deed`() {
        val player = Player("Ernie")
        assertFalse(player.isBuying(MediterraneanAvenue()))
    }

    @Test
    fun `isBuying returns true if player can afford deed`() {
        val player = Player("Ernie", money = 100)
        assertTrue(player.isBuying(MediterraneanAvenue()))
    }

    @Test
    fun `developProperties upgrades a monopoly as expected`() {
        val player = Player("Elmo", 2751)
        val bank = Bank()
        val board = Board(listOf(player), bank)

        bank.sellPropertyToPlayer(ParkPlace::class, player)
        bank.sellPropertyToPlayer(Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // first house is built on Boardwalk because it has the higher rent
        player.developProperties(bank, board)
        assertEquals(0, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(1, player.getDevelopment(Boardwalk::class).numHouses)

        // next house is built on Park Place to respect even building rules
        player.developProperties(bank, board)
        assertEquals(1, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(1, player.getDevelopment(Boardwalk::class).numHouses)

        // back to Boardwalk
        player.developProperties(bank, board)
        assertEquals(1, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(2, player.getDevelopment(Boardwalk::class).numHouses)

        // then Park Place
        player.developProperties(bank, board)
        assertEquals(2, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(2, player.getDevelopment(Boardwalk::class).numHouses)

        // Boardwalk again
        player.developProperties(bank, board)
        assertEquals(2, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(3, player.getDevelopment(Boardwalk::class).numHouses)

        // Another for Park Place
        player.developProperties(bank, board)
        assertEquals(3, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(3, player.getDevelopment(Boardwalk::class).numHouses)

        // last house for Boardwalk
        player.developProperties(bank, board)
        assertEquals(3, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(4, player.getDevelopment(Boardwalk::class).numHouses)

        // and a final house for Park Place
        player.developProperties(bank, board)
        assertEquals(4, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(4, player.getDevelopment(Boardwalk::class).numHouses)

        // Boardwalk gets a hotel
        player.developProperties(bank, board)
        assertEquals(4, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(0, player.getDevelopment(Boardwalk::class).numHouses)
        assertTrue(player.getDevelopment(Boardwalk::class).hasHotel)

        // as does Park Place
        player.developProperties(bank, board)
        assertEquals(0, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(0, player.getDevelopment(Boardwalk::class).numHouses)
        assertTrue(player.getDevelopment(ParkPlace::class).hasHotel)
        assertTrue(player.getDevelopment(Boardwalk::class).hasHotel)

        // both properties are fully upgraded, so nothing changes if we call again
        player.developProperties(bank, board)
        assertEquals(0, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(0, player.getDevelopment(Boardwalk::class).numHouses)
        assertTrue(player.getDevelopment(ParkPlace::class).hasHotel)
        assertTrue(player.getDevelopment(Boardwalk::class).hasHotel)

        // Elmo has a single solitary dollar left to his name
        assertEquals(1, player.money)
    }
}