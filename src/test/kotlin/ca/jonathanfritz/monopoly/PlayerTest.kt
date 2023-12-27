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
}