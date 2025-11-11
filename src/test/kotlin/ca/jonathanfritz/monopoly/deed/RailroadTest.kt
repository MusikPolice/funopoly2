package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.deed.Railroad.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RailroadTest {

    @Test
    fun `values contains expected railroads`() {
        assertEquals(
            setOf(
                ReadingRailroad::class,
                PennsylvaniaRailroad::class,
                BAndORailroad::class,
                ShortlineRailroad::class
            ),
            Railroad.values.keys
        )
    }

    @Test
    fun `each railroad class is associated with an instance of that type`() {
        Railroad.values.forEach { (kClass, instance) ->
            assertNotNull(instance)
            assertEquals(kClass, instance::class)
        }
    }

    @Test
    fun `an instance of a railroad can be fetched via sweet sweet syntactical sugar`() {
        Railroad.values.forEach { (kClass, instance) ->
            assertEquals(Railroad.of(kClass), instance)
        }
    }

    @Test
    fun `rent for mortgaged railroad is $0`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(ReadingRailroad() to Player.Development(isMortgaged = true))
        )
        assertEquals(0, ReadingRailroad().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for unmortgaged railroad ignores ownership of another railroad that is mortgaged`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                ReadingRailroad() to Player.Development(isMortgaged = true),
                PennsylvaniaRailroad() to Player.Development(),
            )
        )
        assertEquals(25, PennsylvaniaRailroad().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent with one owned railroad is $25`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(ReadingRailroad() to Player.Development())
        )
        assertEquals(25, ReadingRailroad().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent with two owned railroads is $50`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                ReadingRailroad() to Player.Development(),
                PennsylvaniaRailroad() to Player.Development()
            )
        )
        assertEquals(50, ReadingRailroad().calculateRent(owner, Board(listOf(owner))))
        assertEquals(50, PennsylvaniaRailroad().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent with three owned railroads is $100`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                ReadingRailroad() to Player.Development(),
                PennsylvaniaRailroad() to Player.Development(),
                BAndORailroad() to Player.Development()
            )
        )
        assertEquals(100, ReadingRailroad().calculateRent(owner, Board(listOf(owner))))
        assertEquals(100, PennsylvaniaRailroad().calculateRent(owner, Board(listOf(owner))))
        assertEquals(100, BAndORailroad().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent with four owned railroads is $200`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                ReadingRailroad() to Player.Development(),
                PennsylvaniaRailroad() to Player.Development(),
                BAndORailroad() to Player.Development(),
                ShortlineRailroad() to Player.Development()
            )
        )
        assertEquals(200, ReadingRailroad().calculateRent(owner, Board(listOf(owner))))
        assertEquals(200, PennsylvaniaRailroad().calculateRent(owner, Board(listOf(owner))))
        assertEquals(200, BAndORailroad().calculateRent(owner, Board(listOf(owner))))
        assertEquals(200, ShortlineRailroad().calculateRent(owner, Board(listOf(owner))))
    }
}