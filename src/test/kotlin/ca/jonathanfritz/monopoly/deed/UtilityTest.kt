package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Dice
import ca.jonathanfritz.monopoly.deed.Utility.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class UtilityTest {

    @Test
    fun `values contains expected utilities`() {
        assertEquals(
            setOf(
                ElectricCompany::class,
                WaterWorks::class
            ),
            Utility.values.keys
        )
    }

    @Test
    fun `each utility class is associated with an instance of that type`() {
        Utility.values.forEach { (kClass, instance) ->
            assertNotNull(instance)
            assertEquals(kClass, instance::class)
        }
    }

    @Test
    fun `an instance of a utility can be fetched via sweet sweet syntactical sugar`() {
        Utility.values.forEach { (kClass, instance) ->
            assertEquals(Utility.of(kClass), instance)
        }
    }

    @Test
    fun `rent is $0 if railroad is mortgaged`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                ElectricCompany() to Player.Development(isMortgaged = true),
                WaterWorks() to Player.Development(isMortgaged = true)
            )
        )
        assertEquals(0, WaterWorks().calculateRent(owner, Dice.Roll(2, 2)))
        assertEquals(0, ElectricCompany().calculateRent(owner, Dice.Roll(2, 2)))
    }

    @Test
    fun `rent is 4x dice roll if one utility is owned`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(ElectricCompany() to Player.Development())
        )
        assertEquals(16, ElectricCompany().calculateRent(owner, Dice.Roll(2, 2)))
    }

    @Test
    fun `rent is 10x dice roll if both utilities are owned`() {
        val owner = Player(
            "Oscar",
            deeds = mutableMapOf(
                ElectricCompany() to Player.Development(),
                WaterWorks() to Player.Development()
            )
        )
        assertEquals(40, WaterWorks().calculateRent(owner, Dice.Roll(2, 2)))
        assertEquals(40, ElectricCompany().calculateRent(owner, Dice.Roll(2, 2)))
    }
}