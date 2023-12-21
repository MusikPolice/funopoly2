package ca.jonathanfritz.monopoly
import ca.jonathanfritz.monopoly.TitleDeedName.*

class Property(
    titleDeedName: TitleDeedName,
    price: Int,
    mortgageValue: Int,
    val buildingCost: Int,

    // once built, houses and hotels charge rent over and above the base rent for the property
    val rentNoHouse: Int,
    val rentOneHouse: Int,
    val rentTwoHouse: Int,
    val rentThreeHouse: Int,
    val rentFourHouse: Int,
    val rentHotel: Int
): TitleDeed(titleDeedName, price, mortgageValue) {

    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    companion object {
        // TODO: companion objects kinda blow here - I want to be able to look up a Property instance by TitleDeedName or ColourGroup
        //  should they be enum classes instead???
        val mediterraneanAvenue = Property(MediterraneanAvenue,60, 30, 50, 2, 10, 30, 90, 160, 250)
        val balticAvenue = Property(BalticAvenue,60, 30, 50, 4, 20, 60, 180, 320, 450)

        val orientalAvenue = Property(OrientalAvenue, 100, 50, 50, 6, 30, 90, 270, 400, 550)
        val vermontAvenue = Property(VermontAvenue, 100, 50, 50, 6, 30, 90, 270, 400, 550)
        val connecticutAvenue = Property(ConnecticutAvenue, 120, 60, 50, 8, 40, 100, 300, 450, 600)

        val stCharlesPlace = Property(StCharlesPlace, 140, 70, 100, 10, 50, 150, 450, 625, 750)
        val statesAvenue = Property(StatesAvenue, 140, 70, 100, 10, 50, 150, 450, 625, 750)
        val virginiaAvenue = Property(VirginiaAvenue, 160, 80, 100, 12, 60, 180, 500, 700, 900)

        val stJamesPlace = Property(StJamesPlace, 180, 90, 100, 14, 70, 200, 550, 750, 950)
        val tennesseeAvenue = Property(TennesseeAvenue, 180, 90, 100, 14, 70, 200, 550, 750, 950)
        val newYorkAvenue = Property(NewYorkAvenue, 200, 100, 100, 16, 80, 220, 600, 800, 1000)

        val kentuckyAvenue = Property(KentuckyAvenue, 220, 110, 150, 18, 90, 250, 700, 875, 1050)
        val indianaAvenue = Property(IndianaAvenue, 220, 110, 150, 18, 90, 250, 700, 875, 1050)
        val illinoisAvenue = Property(IllinoisAvenue, 240, 120, 150, 20, 100, 300, 750, 925, 1100)

        val atlanticAvenue = Property(AtlanticAvenue, 260, 130, 150, 22, 110, 330, 800, 975, 1150)
        val ventnorAvenue = Property(VentnorAvenue, 260, 130, 150, 22, 110, 330, 800, 975, 1150)
        val marvinGardens = Property(MarvinGardens, 280, 140, 150, 24, 120, 360, 850, 1025, 1200)

        val pacificAvenue = Property(PacificAvenue, 300, 150, 200, 26, 130, 390, 900, 1100, 1275)
        val northCarolinaAvenue = Property(NorthCarolinaAvenue, 300, 150, 200, 26, 130, 390, 900, 1100, 1275)
        val pennsylvaniaAvenue = Property(PennsylvaniaAvenue, 320, 160, 200, 28, 150, 450, 1000, 1200, 1400)

        val parkPlace = Property(ParkPlace, 350, 175, 200, 35, 175, 500, 110, 1300, 1500)
        val boardwalk = Property(Boardwalk, 400, 200, 200, 50, 200, 600, 1400, 1700, 2000)
    }

    // if all properties in a set are owned, rent on undeveloped properties doubles
    val rentNoHouseWithMonopoly = rentNoHouse * 2

    // lookup the appropriate rent based on the number of houses
    private val houseRents = listOf(rentNoHouse, rentOneHouse, rentTwoHouse, rentThreeHouse, rentFourHouse)
}