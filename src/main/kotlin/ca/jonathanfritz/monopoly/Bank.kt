package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.Property.*

class Bank {
    // https://www.hasbro.com/common/instruct/00009.pdf
    var houses = 32
    var hotels = 12

    // https://www.monopolyland.com/how-much-money-in-monopoly-set/
    // TODO: apparently old sets (pre-2008) shipped with $15,140 - could be a difference that we can test
    // TODO: per the rules, the bank can never run out of money, so maybe this doesn't matter?
    var money = 20580

    // the title deeds that the bank can sell to players
    val titleDeeds: MutableList<TitleDeed> = mutableListOf(
        Property.of(MediterraneanAvenue::class),
        Property.of(BalticAvenue::class),
        Railroad.of(Railroad.ReadingRailroad::class),
        Property.of(OrientalAvenue::class),
        Property.of(VermontAvenue::class),
        Property.of(ConnecticutAvenue::class),

        Property.of(StCharlesPlace::class),
        Utility.of(Utility.ElectricCompany::class),
        Property.of(StatesAvenue::class),
        Property.of(VirginiaAvenue::class),
        Railroad.of(Railroad.PennsylvaniaRailroad::class),
        Property.of(StJamesPlace::class),
        Property.of(TennesseeAvenue::class),
        Property.of(NewYorkAvenue::class),

        Property.of(KentuckyAvenue::class),
        Property.of(IndianaAvenue::class),
        Property.of(IllinoisAvenue::class),
        Railroad.of(Railroad.BandORailroad::class),
        Property.of(AtlanticAvenue::class),
        Property.of(VentnorAvenue::class),
        Utility.of(Utility.WaterWorks::class),
        Property.of(MarvinGardens::class),

        Property.of(PacificAvenue::class),
        Property.of(NorthCarolinaAvenue::class),
        Property.of(PennsylvaniaAvenue::class),
        Railroad.of(Railroad.ShortlineRailroad::class),
        Property.of(ParkPlace::class),
        Property.of(Boardwalk::class)
    )
}