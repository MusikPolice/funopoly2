package ca.jonathanfritz.monopoly.deed

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
}