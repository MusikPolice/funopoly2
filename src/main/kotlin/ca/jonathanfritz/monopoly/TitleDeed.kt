package ca.jonathanfritz.monopoly

sealed class TitleDeed (
    val colourGroup: ColourGroup,
    val price: Int,
    val mortgageValue: Int,
    val isUtility: Boolean = false,
    val isRailroad: Boolean = false,
    val isBuildable: Boolean = !(isUtility || isRailroad)
)
