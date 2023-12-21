package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.ColourGroup.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import ca.jonathanfritz.monopoly.Property.*

internal class PropertyTest {

    @Test
    fun colourGroupsTest() {
        assertEquals(setOf(MediterraneanAvenue::class, BalticAvenue::class), Property.of(Brown).map { it::class }.toSet())
        assertEquals(setOf(OrientalAvenue::class, VermontAvenue::class, ConnecticutAvenue::class), Property.of(LightBlue).map { it::class }.toSet())
        assertEquals(setOf(StCharlesPlace::class, StatesAvenue::class, VirginiaAvenue::class), Property.of(Pink).map { it::class }.toSet())
        assertEquals(setOf(StJamesPlace::class, TennesseeAvenue::class, NewYorkAvenue::class), Property.of(Orange).map { it::class }.toSet())
        assertEquals(setOf(KentuckyAvenue::class, IndianaAvenue::class, IllinoisAvenue::class), Property.of(Red).map { it::class }.toSet())
        assertEquals(setOf(AtlanticAvenue::class, VentnorAvenue::class, MarvinGardens::class), Property.of(Yellow).map { it::class }.toSet())
        assertEquals(setOf(PacificAvenue::class, NorthCarolinaAvenue::class, PennsylvaniaAvenue::class), Property.of(Green).map { it::class }.toSet())
        assertEquals(setOf(ParkPlace::class, Boardwalk::class), Property.of(DarkBlue).map { it::class }.toSet())
        /*assertEquals(listOf(ReadingRailroad, PennsylvaniaRailroad, BAndORailroad, ShortLineRailroad), ColourGroup.Railroads.titleDeedNames())
        assertEquals(listOf(ElectricCompany, WaterWorks), ColourGroup.Utilities.titleDeedNames())*/
    }
}