package ca.jonathanfritz.monopoly

import kotlin.reflect.KClass

sealed class Railroad(): TitleDeed(ColourGroup.Railroads, 200, 100) {
    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    class ReadingRailroad(): Railroad()
    class PennsylvaniaRailroad(): Railroad()
    class BandORailroad(): Railroad()
    class ShortlineRailroad(): Railroad()

    // uses reflection to build a list of all instances of the sealed class once at initialization time
    // this basically mimics the way that an enum's elements can be accessed, while still allowing for the use of inheritance
    companion object {
        val values = Railroad::class.sealedSubclasses.associateWith { it.constructors.first().call() }
        val colourGroups = mapOf(ColourGroup.Railroads to values.values)
        fun <R: Railroad> of(kclass: KClass<R>) = values.getValue(kclass)
    }

    // TODO: rent is based on number of railroads owned - $25 if one, $50 if 2, $100 if three, $200 if four
}