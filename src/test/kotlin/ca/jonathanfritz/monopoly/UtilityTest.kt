package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.Utility.*
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
}