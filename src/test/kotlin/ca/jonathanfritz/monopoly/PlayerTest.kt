package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
        assertEquals(5000, cashOnly.networth())
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
        assertEquals(880, propertyOnly.networth())
        assertEquals(88, propertyOnly.incomeTaxAmount())

        val withHouses = Player(
            "Houses",
            deeds = mutableMapOf(
                BalticAvenue() to Player.Development(numHouses = 2)
            )
        )
        assertEquals(160, withHouses.networth())
        assertEquals(16, withHouses.incomeTaxAmount())

        val withHotels = Player(
            "Hotels",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 4),
                BalticAvenue() to Player.Development(numHouses = 4, hotel = true)
            )
        )
        assertEquals(570, withHotels.networth())
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
    fun `isPayingGetOutOfJailEarlyFee returns true if player is in jail and can afford the fine`() {
        val player = Player("Cookie Monster", money = 100)
        player.isInJail = true
        assertTrue(player.isPayingGetOutOfJailEarlyFee(50))
    }
}