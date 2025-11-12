@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.deed

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.ColourGroup.*
import kotlin.reflect.KClass

sealed class Property(
    colourGroup: ColourGroup,
    price: Int,
    mortgageValue: Int,
    val buildingCost: Int,
    // once built, houses and hotels charge rent over and above the base rent for the property
    private val rentNoHouse: Int,
    rentOneHouse: Int,
    rentTwoHouse: Int,
    rentThreeHouse: Int,
    rentFourHouse: Int,
    private val rentHotel: Int,
) : TitleDeed(colourGroup, price, mortgageValue) {
    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    class MediterraneanAvenue : Property(Brown, 60, 30, 50, 2, 10, 30, 90, 160, 250)

    class BalticAvenue : Property(Brown, 60, 30, 50, 4, 20, 60, 180, 320, 450)

    class OrientalAvenue : Property(LightBlue, 100, 50, 50, 6, 30, 90, 270, 400, 550)

    class VermontAvenue : Property(LightBlue, 100, 50, 50, 6, 30, 90, 270, 400, 550)

    class ConnecticutAvenue : Property(LightBlue, 120, 60, 50, 8, 40, 100, 300, 450, 600)

    class StCharlesPlace : Property(Pink, 140, 70, 100, 10, 50, 150, 450, 625, 750)

    class StatesAvenue : Property(Pink, 140, 70, 100, 10, 50, 150, 450, 625, 750)

    class VirginiaAvenue : Property(Pink, 160, 80, 100, 12, 60, 180, 500, 700, 900)

    class StJamesPlace : Property(Orange, 180, 90, 100, 14, 70, 200, 550, 750, 950)

    class TennesseeAvenue : Property(Orange, 180, 90, 100, 14, 70, 200, 550, 750, 950)

    class NewYorkAvenue : Property(Orange, 200, 100, 100, 16, 80, 220, 600, 800, 1000)

    class KentuckyAvenue : Property(Red, 220, 110, 150, 18, 90, 250, 700, 875, 1050)

    class IndianaAvenue : Property(Red, 220, 110, 150, 18, 90, 250, 700, 875, 1050)

    class IllinoisAvenue : Property(Red, 240, 120, 150, 20, 100, 300, 750, 925, 1100)

    class AtlanticAvenue : Property(Yellow, 260, 130, 150, 22, 110, 330, 800, 975, 1150)

    class VentnorAvenue : Property(Yellow, 260, 130, 150, 22, 110, 330, 800, 975, 1150)

    class MarvinGardens : Property(Yellow, 280, 140, 150, 24, 120, 360, 850, 1025, 1200)

    class PacificAvenue : Property(Green, 300, 150, 200, 26, 130, 390, 900, 1100, 1275)

    class NorthCarolinaAvenue : Property(Green, 300, 150, 200, 26, 130, 390, 900, 1100, 1275)

    class PennsylvaniaAvenue : Property(Green, 320, 160, 200, 28, 150, 450, 1000, 1200, 1400)

    class ParkPlace : Property(DarkBlue, 350, 175, 200, 35, 175, 500, 110, 1300, 1500)

    class Boardwalk : Property(DarkBlue, 400, 200, 200, 50, 200, 600, 1400, 1700, 2000)

    override val isBuildable: Boolean = true

    // uses reflection to build a list of all instances of the sealed class once at initialization time
    // this basically mimics the way that an enum's elements can be accessed, while still allowing for the use of inheritance
    companion object {
        val values = Property::class.sealedSubclasses.associateWith { it.constructors.first().call() }

        fun <P : Property> of(kClass: KClass<P>) = values.getValue(kClass)
    }

    private val houseRents = listOf(rentNoHouse, rentOneHouse, rentTwoHouse, rentThreeHouse, rentFourHouse)

    override fun calculateRent(
        owner: Player,
        board: Board,
    ): Int {
        val development = owner.getDevelopment(this::class)
        return if (development.isMortgaged) {
            0
        } else if (development.hasHotel) {
            rentHotel
        } else if (development.numHouses == 0 && owner.hasMonopoly(this.colourGroup)) {
            rentNoHouse * 2
        } else {
            houseRents[development.numHouses]
        }
    }
}
