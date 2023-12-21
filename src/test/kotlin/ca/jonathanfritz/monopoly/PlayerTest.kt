package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.Property.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PlayerTest {

    @Test
    fun isOwnerTest() {
        val darkBlueOwner = Player(
            "Tony",
            0,
            mutableMapOf(
                Property.of(ParkPlace::class) to Player.Development(),
                Property.of(Boardwalk::class) to Player.Development()
            )
        )
        assertTrue(darkBlueOwner.isOwner(ParkPlace::class))
        assertTrue(darkBlueOwner.isOwner(Boardwalk::class))
        Property.values.keys.filterNot { it == ParkPlace::class || it == Boardwalk::class }
            .forEach { unownedProperty -> assertFalse(darkBlueOwner.isOwner(unownedProperty)) }
    }

    @Test
    fun monopoliesTest() {
        val brownMonopoly = Player(
            "Joe",
            0,
            mutableMapOf(
                Property.of(MediterraneanAvenue::class) to Player.Development(),
                Property.of(BalticAvenue::class) to Player.Development()
            )
        )
        assertTrue(brownMonopoly.hasMonopoly(ColourGroup.Brown))
        ColourGroup.values().filterNot { it == ColourGroup.Brown }.forEach { assertFalse(brownMonopoly.hasMonopoly(it)) }
    }
}