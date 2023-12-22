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
            0,
            ColourGroup.DarkBlue.titleDeeds().values.associateWith { Player.Development() }.toMutableMap()
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
            0,
            ColourGroup.Brown.titleDeeds().values.associateWith { Player.Development() }.toMutableMap()
        )
        assertTrue(lowRoller.hasMonopoly(ColourGroup.Brown))
        ColourGroup.values().filterNot { it == ColourGroup.Brown }.forEach { assertFalse(lowRoller.hasMonopoly(it)) }
    }
}