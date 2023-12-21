package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.ColourGroup.*
import ca.jonathanfritz.monopoly.TitleDeedName.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ColourGroupTest {

    // ensures that all the property colour associations are configured correctly
    @Test
    fun colourGroupTitleDeedsTest() {
        assertEquals(listOf(MediterraneanAvenue, BalticAvenue), Brown.titleDeedNames())
        assertEquals(listOf(OrientalAvenue, VermontAvenue, ConnecticutAvenue), LightBlue.titleDeedNames())
        assertEquals(listOf(StCharlesPlace, StatesAvenue, VirginiaAvenue), Pink.titleDeedNames())
        assertEquals(listOf(StJamesPlace, TennesseeAvenue, NewYorkAvenue), Orange.titleDeedNames())
        assertEquals(listOf(KentuckyAvenue, IndianaAvenue, IllinoisAvenue), Red.titleDeedNames())
        assertEquals(listOf(AtlanticAvenue, VentnorAvenue, MarvinGardens), Yellow.titleDeedNames())
        assertEquals(listOf(PacificAvenue, NorthCarolinaAvenue, PennsylvaniaAvenue), Green.titleDeedNames())
        assertEquals(listOf(ParkPlace, Boardwalk), DarkBlue.titleDeedNames())
        assertEquals(listOf(ReadingRailroad, PennsylvaniaRailroad, BAndORailroad, ShortLineRailroad), Railroads.titleDeedNames())
        assertEquals(listOf(ElectricCompany, WaterWorks), Utilities.titleDeedNames())
    }
}