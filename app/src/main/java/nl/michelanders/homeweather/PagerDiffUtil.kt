package nl.michelanders.homeweather

import androidx.recyclerview.widget.DiffUtil

class PagerDiffUtil(private val oldList: List<PagerItem>, private val newList: List<PagerItem>) : DiffUtil.Callback() {

    enum class PayloadKey {
        VALUE
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return ( (oldList[oldItemPosition].name == newList[newItemPosition].name)
                && (oldList[oldItemPosition].time == newList[newItemPosition].time)
                && (oldList[oldItemPosition].temperature == newList[newItemPosition].temperature)
                && (oldList[oldItemPosition].humidity == newList[newItemPosition].humidity)
                && (oldList[oldItemPosition].beaufort == newList[newItemPosition].beaufort)
                && (oldList[oldItemPosition].windrose == newList[newItemPosition].windrose)
                && (oldList[oldItemPosition].windspeed == newList[newItemPosition].windspeed)
                && (oldList[oldItemPosition].windgust == newList[newItemPosition].windgust)
                && (oldList[oldItemPosition].rain8h == newList[newItemPosition].rain8h)
                && (oldList[oldItemPosition].rainrate == newList[newItemPosition].rainrate))
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return listOf(PayloadKey.VALUE)
    }
}