package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.Property.*
import ca.jonathanfritz.monopoly.Railroad.*
import ca.jonathanfritz.monopoly.Utility.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TitleDeedTest {

    @Test
    fun `values contains all properties, railroads, and utilities`() {
        kotlin.test.assertEquals(
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
                Boardwalk::class,
                ReadingRailroad::class,
                PennsylvaniaRailroad::class,
                BandORailroad::class,
                ShortlineRailroad::class,
                ElectricCompany::class,
                WaterWorks::class
            ),
            TitleDeed.values.keys
        )
    }

    @Test
    fun `each title deed class is associated with an instance of that type`() {
        TitleDeed.values.forEach { (kClass, instance) ->
            Assertions.assertNotNull(instance)
            assertEquals(kClass, instance::class)
        }
    }

    @Test
    fun `an instance of a title deed can be fetched via sweet sweet syntactical sugar`() {
        TitleDeed.values.forEach { (kClass, instance) ->
            assertEquals(TitleDeed.of(kClass), instance)
        }
    }
}