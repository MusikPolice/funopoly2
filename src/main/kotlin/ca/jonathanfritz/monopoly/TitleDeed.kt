package ca.jonathanfritz.monopoly

sealed class TitleDeed(
    open val titleDeedName: TitleDeedName,
    open val price: Int,
    open val mortgageValue: Int,
    open val isUtility: Boolean = false,
    open val isRailroad: Boolean = false,
    val isBuildable: Boolean = !(isUtility || isRailroad)
) {
    // TODO: compute ROI for owned properties - basically all monies earned divided by all monies invested over the duration of a game
}
