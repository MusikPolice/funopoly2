package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.FakeDice
import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Board
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
        assertEquals(0, WaterWorks().calculateRent(owner, Board(listOf(owner))))
        assertEquals(0, ElectricCompany().calculateRent(owner, Board(listOf(owner))))
    }

    @Test
    fun `rent for unmortgaged utilities ignores ownership of another utility that is mortgaged`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(
                ElectricCompany() to Player.Development(isMortgaged = true),
                WaterWorks() to Player.Development()
            )
        )
        val fakeDice = FakeDice(Dice.Roll(3, 1))
        val board = Board(listOf(owner), dice = fakeDice)

        // player rolled a four
        fakeDice.roll()

        // because both utilities are owned, rent would normally be 10x dice roll, but one utility is mortgaged, so it
        // is not counted and rent falls back to 4x the dice roll
        assertEquals(16, WaterWorks().calculateRent(owner, board))
    }

    @Test
    fun `rent is 4x dice roll if one utility is owned`() {
        val owner = Player(
            "Big Bird",
            deeds = mutableMapOf(ElectricCompany() to Player.Development())
        )
        val fakeDice = FakeDice(Dice.Roll(3, 1))
        val board = Board(listOf(owner), dice = fakeDice)

        // player rolled a four
        fakeDice.roll()

        // so the rent is 4 x $4 == $16
        assertEquals(16, ElectricCompany().calculateRent(owner, board))
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

        val fakeDice = FakeDice(Dice.Roll(3, 1))
        val board = Board(listOf(owner), dice = fakeDice)

        // player rolled a four
        fakeDice.roll()

        // so the rent is 10 x $4 == $40
        assertEquals(40, WaterWorks().calculateRent(owner, board))
        assertEquals(40, ElectricCompany().calculateRent(owner, board))
    }
}