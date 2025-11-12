@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.deed.Railroad.*
import ca.jonathanfritz.monopoly.deed.Utility.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ColourGroupTest {
    @Test
    fun `brown contains expected title deeds`() {
        assertEquals(
            setOf(Property.MediterraneanAvenue::class, Property.BalticAvenue::class),
            ColourGroup.Brown.titleDeeds().keys,
        )
    }

    @Test
    fun `light blue contains expected title deeds`() {
        assertEquals(
            setOf(
                Property.OrientalAvenue::class,
                Property.VermontAvenue::class,
                Property.ConnecticutAvenue::class,
            ),
            ColourGroup.LightBlue.titleDeeds().keys,
        )
    }

    @Test
    fun `pink contains expected title deeds`() {
        assertEquals(
            setOf(
                Property.StCharlesPlace::class,
                Property.StatesAvenue::class,
                Property.VirginiaAvenue::class,
            ),
            ColourGroup.Pink.titleDeeds().keys,
        )
    }

    @Test
    fun `orange contains expected title deeds`() {
        assertEquals(
            setOf(
                Property.StJamesPlace::class,
                Property.TennesseeAvenue::class,
                Property.NewYorkAvenue::class,
            ),
            ColourGroup.Orange.titleDeeds().keys,
        )
    }

    @Test
    fun `red contains expected title deeds`() {
        assertEquals(
            setOf(
                Property.KentuckyAvenue::class,
                Property.IndianaAvenue::class,
                Property.IllinoisAvenue::class,
            ),
            ColourGroup.Red.titleDeeds().keys,
        )
    }

    @Test
    fun `yellow contains expected title deeds`() {
        assertEquals(
            setOf(
                Property.AtlanticAvenue::class,
                Property.VentnorAvenue::class,
                Property.MarvinGardens::class,
            ),
            ColourGroup.Yellow.titleDeeds().keys,
        )
    }

    @Test
    fun `green contains expected title deeds`() {
        assertEquals(
            setOf(
                Property.PacificAvenue::class,
                Property.NorthCarolinaAvenue::class,
                Property.PennsylvaniaAvenue::class,
            ),
            ColourGroup.Green.titleDeeds().keys,
        )
    }

    @Test
    fun `dark blue contains expected title deeds`() {
        assertEquals(
            setOf(Property.ParkPlace::class, Property.Boardwalk::class),
            ColourGroup.DarkBlue.titleDeeds().keys,
        )
    }

    @Test
    fun `railroads contains expected title deeds`() {
        assertEquals(
            setOf(ReadingRailroad::class, PennsylvaniaRailroad::class, BAndORailroad::class, ShortlineRailroad::class),
            ColourGroup.Railroads.titleDeeds().keys,
        )
    }

    @Test
    fun `utilities contains expected title deeds`() {
        assertEquals(
            setOf(ElectricCompany::class, WaterWorks::class),
            ColourGroup.Utilities.titleDeeds().keys,
        )
    }
}
