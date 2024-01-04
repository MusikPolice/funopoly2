package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.deed.Property.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropertyTest {

    @Test
    fun `values contains expected properties`() {
        assertEquals(
            setOf(
                MediterraneanAvenue::class,
                BalticAvenue::class,
                OrientalAvenue::class,
                VermontAvenue::class,
                ConnecticutAvenue::class,
                StCharlesPlace::class,
                StatesAvenue::class,
                VirginiaAvenue::class,
                StJamesPlace::class,
                TennesseeAvenue::class,
                NewYorkAvenue::class,
                KentuckyAvenue::class,
                IndianaAvenue::class,
                IllinoisAvenue::class,
                AtlanticAvenue::class,
                VentnorAvenue::class,
                MarvinGardens::class,
                PacificAvenue::class,
                NorthCarolinaAvenue::class,
                PennsylvaniaAvenue::class,
                ParkPlace::class,
                Boardwalk::class
            ),
            Property.values.keys
        )
    }

    @Test
    fun `each property class is associated with an instance of that type`() {
        Property.values.forEach { (kClass, instance) ->
            assertNotNull(instance)
            assertEquals(kClass, instance::class)
        }
    }

    @Test
    fun `an instance of a property can be fetched via sweet sweet syntactical sugar`() {
        Property.values.forEach { (kClass, instance) ->
            assertEquals(Property.of(kClass), instance)
        }
    }

    @Test
    fun `rent on a mortgaged property is $0`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(BalticAvenue() to Player.Development(isMortgaged = true))
        )
        assertEquals(0, BalticAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for property with one hotel test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 4),
                BalticAvenue() to Player.Development(hasHotel = true)
            )
        )
        assertEquals(450, BalticAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for property with four houses test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 4),
                BalticAvenue() to Player.Development(hasHotel = true)
            )
        )
        assertEquals(160, MediterraneanAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for property with three houses test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 4),
                BalticAvenue() to Player.Development(numHouses = 3)
            )
        )
        assertEquals(180, BalticAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for property with two houses test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 3),
                BalticAvenue() to Player.Development(numHouses = 2)
            )
        )
        assertEquals(60, BalticAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for property with one house test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(numHouses = 2),
                BalticAvenue() to Player.Development(numHouses = 1)
            )
        )
        assertEquals(20, BalticAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for undeveloped property test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(),
            )
        )
        assertEquals(2, MediterraneanAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for undeveloped property in monopoly test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(),
                BalticAvenue() to Player.Development()
            )
        )
        assertEquals(4, MediterraneanAvenue().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for undeveloped property in monopoly with one other property mortgaged test`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                MediterraneanAvenue() to Player.Development(),
                BalticAvenue() to Player.Development(isMortgaged = true)
            )
        )
        assertEquals(4, MediterraneanAvenue().calculateRent(owner, Board(listOf(owner))))
    }
}