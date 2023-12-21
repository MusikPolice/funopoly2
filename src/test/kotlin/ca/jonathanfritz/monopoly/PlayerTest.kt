package ca.jonathanfritz.monopoly

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PlayerTest {

    @Test
    fun isOwnerTest() {
        val darkBlueOwner = Player(
            "Tony",
            0,
            mutableListOf(Property.parkPlace, Property.boardwalk)
        )
        assertTrue(darkBlueOwner.isOwner(Property.parkPlace))
        assertTrue(darkBlueOwner.isOwner(Property.boardwalk))
        assertFalse(darkBlueOwner.isOwner(Property.marvinGardens))  // TODO: extend me to test all unowned properties
    }

    @Test
    fun monopoliesTest() {
        val brownMonopoly = Player(
            "Joe",
            0,
            mutableListOf(Property.mediterraneanAvenue, Property.balticAvenue)
        )
        assertTrue(brownMonopoly.hasMonopoly(ColourGroup.Brown))
        ColourGroup.values().filterNot { it == ColourGroup.Brown }.forEach { assertFalse(brownMonopoly.hasMonopoly(it)) }
    }
}