package nl.michelanders.homeweather

// inspired by https://github.com/rmyhal/ViewPager2-DiffUtil

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder

class PagerAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val items: ArrayList<PagerItem> = arrayListOf()

    override fun createFragment(position: Int): Fragment =
        PagerFragment.newInstance(items[position])

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].id
    }

    override fun containsItem(itemId: Long): Boolean {
        return items.any { it.id == itemId }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(
        holder: FragmentViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val tag = "f" + holder.itemId
            val fragment = activity.supportFragmentManager.findFragmentByTag(tag)
            // safe check ,but fragment should not be null
            Log.d("onBindViewHolder",items[position].time)
            if (fragment != null) {
                (fragment as PagerFragment).setValue(items[position].name, items[position].time, items[position].temperature, items[position].humidity)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setItems(newItems: List<PagerItem>) {
        val callback = PagerDiffUtil(items, newItems)
        val diff = DiffUtil.calculateDiff(callback)

        items.clear()
        items.addAll(newItems)

        diff.dispatchUpdatesTo(this)
    }
}