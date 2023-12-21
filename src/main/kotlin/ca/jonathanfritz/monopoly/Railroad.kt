package ca.jonathanfritz.monopoly
import ca.jonathanfritz.monopoly.TitleDeedName.*

class Railroad(
    titleName: TitleDeedName
): TitleDeed(titleName, 200, 100) {
    // TODO: rent is based on number of railroads owned - $25 if one, $50 if 2, $100 if three, $200 if four

    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    companion object {
        val reading = Railroad(ReadingRailroad)
        val pennsylvania = Railroad(PennsylvaniaRailroad)
        val bo = Railroad(BAndORailroad)
        val shortLine = Railroad(ShortLineRailroad)
    }
}