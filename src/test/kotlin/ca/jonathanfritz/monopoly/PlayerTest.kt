package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.exception.InsufficientFundsException
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
    fun monopoliesTest() {
        // this player owns Mediterranean and Baltic Avenues, giving them a Monopoly on the brown properties
        val lowRoller = Player(
            "Low Roller",
            deeds = ColourGroup.Brown.titleDeeds().values.associateWith { Player.Development() }.toMutableMap()
        )
        assertTrue(lowRoller.hasMonopoly(ColourGroup.Brown))
        ColourGroup.values().filterNot { it == ColourGroup.Brown }.forEach { assertFalse(lowRoller.hasMonopoly(it)) }
    }

    @Test
    fun networthTest() {
        val cashOnly = Player(
            "Cash Only",
            money = 5000
        )
        assertEquals(5000, cashOnly.netWorth())
        assertEquals(200, cashOnly.incomeTaxAmount())

        val propertyOnly = Player(
            "Property Only",
            deeds = mutableMapOf(
                OrientalAvenue() to Player.Development(),
                NewYorkAvenue() to Player.Development(),
                AtlanticAvenue() to Player.Development(),
                PennsylvaniaAvenue() to Player.Development()
            )
        )
        assertEquals(880, propertyOnly.netWorth())
        assertEquals(88, propertyOnly.incomeTaxAmount())

        val withHouses = Player(
            "Houses",
            deeds = mutableMapOf(
                BalticAvenue() to Player.Development(numHouses = 2)
            )
        )
        assertEquals(160, withHouses.netWorth())
        assertEquals(16, withHouses.incomeTaxAmount())

        val withHotels = Player(
            "Hotels",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 4),
                BalticAvenue() to Player.Development(numHouses = 4, hotel = true)
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
}