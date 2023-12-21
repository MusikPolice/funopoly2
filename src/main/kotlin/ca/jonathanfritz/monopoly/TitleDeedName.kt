package ca.jonathanfritz.monopoly
import ca.jonathanfritz.monopoly.ColourGroup.*

enum class TitleDeedName(val colourGroup: ColourGroup) {
    MediterraneanAvenue(Brown),
    BalticAvenue(Brown),

    ReadingRailroad(Railroads),

    OrientalAvenue(LightBlue),
    VermontAvenue(LightBlue),
    ConnecticutAvenue(LightBlue),

    StCharlesPlace(Pink),
    ElectricCompany(Utilities),
    StatesAvenue(Pink),
    VirginiaAvenue(Pink),

    PennsylvaniaRailroad(Railroads),

    StJamesPlace(Orange),
    TennesseeAvenue(Orange),
    NewYorkAvenue(Orange),

    KentuckyAvenue(Red),
    IndianaAvenue(Red),
    IllinoisAvenue(Red),

    BAndORailroad(Railroads),

    AtlanticAvenue(Yellow),
    VentnorAvenue(Yellow),
    WaterWorks(Utilities),
    MarvinGardens(Yellow),

    PacificAvenue(Green),
    NorthCarolinaAvenue(Green),
    PennsylvaniaAvenue(Green),

    ShortLineRailroad(Railroads),

    ParkPlace(DarkBlue),
    Boardwalk(DarkBlue);
}
