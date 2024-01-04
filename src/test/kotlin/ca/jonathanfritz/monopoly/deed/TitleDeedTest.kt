package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad.*
import ca.jonathanfritz.monopoly.deed.Utility.*
import ca.jonathanfritz.monopoly.exception.PropertyDevelopmentException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
                BAndORailroad::class,
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

    @Test
    fun addingHouseRespectsEvenBuildingRulesTest() {
        val player = Player("Count von Count", 5000)
        val bank = Bank()

        bank.sellPropertyToPlayer(ParkPlace::class, player)
        bank.sellPropertyToPlayer(Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        val parkPlace = player.deeds.keys.first { it::class == ParkPlace::class }
        val boardwalk = player.deeds.keys.first { it::class == Boardwalk::class }

        // can build a single house on either property
        assertTrue(parkPlace.addingHouseRespectsEvenBuildingRules(player))
        bank.buildHouse(ParkPlace::class, player)

        // attempting to build another house on that property without first developing
        // the other property in the monopoly throws an exception
        assertFalse(parkPlace.addingHouseRespectsEvenBuildingRules(player))

        // the other property in the monopoly can be developed
        assertTrue(boardwalk.addingHouseRespectsEvenBuildingRules(player))
        bank.buildHouse(Boardwalk::class, player)

        // because the properties are evenly developed, a second house can be added to either
        assertTrue(boardwalk.addingHouseRespectsEvenBuildingRules(player))
        bank.buildHouse(Boardwalk::class, player)

        // adding a third house to Boardwalk throws an exception
        assertFalse(boardwalk.addingHouseRespectsEvenBuildingRules(player))

        // but Park Place can still be developed, leaving us with two houses on each property
        assertTrue(parkPlace.addingHouseRespectsEvenBuildingRules(player))
        bank.buildHouse(ParkPlace::class, player)

        assertTrue(player.deeds.map { it.value.numHouses }.all { it == 2 })
    }

    @Test
    fun addingHotelRespectsEvenBuildingRulesTest() {
        val player = Player("Count von Count", 5000)
        val bank = Bank()

        bank.sellPropertyToPlayer(ParkPlace::class, player)
        bank.sellPropertyToPlayer(Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        val parkPlace = player.deeds.keys.first { it::class == ParkPlace::class }
        val boardwalk = player.deeds.keys.first { it::class == Boardwalk::class }

        // player builds four houses on Park Place but only three on Boardwalk
        bank.buildHouse(ParkPlace::class, player)
        (1 .. 3).forEach { _ ->
            bank.buildHouse(Boardwalk::class, player)
            bank.buildHouse(ParkPlace::class, player)
        }

        // cannot build a hotel on Park Place, even though it has four houses
        // because Boardwalk only has three
        assertFalse(parkPlace.addingHotelRespectsEvenBuildingRules(player))
        assertFalse(boardwalk.addingHotelRespectsEvenBuildingRules(player))

        // building a house on boardwalk resolves the issue
        bank.buildHouse(Boardwalk::class, player)

        assertTrue(parkPlace.addingHotelRespectsEvenBuildingRules(player))
        assertTrue(boardwalk.addingHotelRespectsEvenBuildingRules(player))
        bank.buildHotel(ParkPlace::class, player)
        bank.buildHotel(Boardwalk::class, player)
    }
}