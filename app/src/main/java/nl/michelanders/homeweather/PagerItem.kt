package nl.michelanders.homeweather

import java.util.*

data class PagerItem (val id: Long = UUID.randomUUID().leastSignificantBits, val color: String="#dd3333", val time: String = "2000-01-01T00:00:00+01:00", val name:String="unknown", val temperature:String="-273", val humidity:String="0")