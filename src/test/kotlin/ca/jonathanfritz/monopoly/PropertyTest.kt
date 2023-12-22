package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.Property.*
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
}