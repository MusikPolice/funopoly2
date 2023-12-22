package ca.jonathanfritz.monopoly

import kotlin.reflect.KClass

sealed class Railroad(): TitleDeed(ColourGroup.Railroads, 200, 100) {
    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    class ReadingRailroad(): Railroad()
    class PennsylvaniaRailroad(): Railroad()
    class BandORailroad(): Railroad()
    class ShortlineRailroad(): Railroad()

    companion object {
        val values: Map<KClass<out Railroad>, Railroad> = Railroad::class.sealedSubclasses.associateWith {
            it.constructors.first().call()
        }
        fun <R: Railroad> of(kClass: KClass<R>): Railroad = values.getValue(kClass)
    }

    // TODO: rent is based on number of railroads owned - $25 if one, $50 if 2, $100 if three, $200 if four
}