package nl.michelanders.homeweather


import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    val TAG = "MainActivityTask"

    private lateinit var pagerAdapter: PagerAdapter
    private var pagerItems: MutableList<PagerItem> = mutableListOf()

    private fun updatePagerItems(response: JSONArray, pagerItems: MutableList<PagerItem> ): MutableList<PagerItem> {
        val newPagerItems: MutableList<PagerItem> = mutableListOf()
        for (i in 0 until response.length()) {
            val roomitem = response.getJSONObject(i)
            Log.d("updateFromDownLoad", roomitem.toString())
            val name = roomitem.get("name").toString()
            val temperature = roomitem.get("temperature").toString().toFloat()
            val humidity = roomitem.get("humidity").toString().toFloat()
            val time = roomitem.get("time").toString()
            val newItem = pagerItems[i].copy(name = name, time = time, temperature = "%.1f".format(temperature), humidity = "%.0f".format(humidity))
            newPagerItems.add(newItem)
        }
        return newPagerItems
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.thetoolbar))

        val swiperefreshlayout = (findViewById<SwipeRefreshLayout>(R.id.swiperefresh))
        swiperefreshlayout.setOnRefreshListener {
            val jsonArrayRequest = JsonArrayRequest(
                Request.Method.GET, getString(R.string.datasource), null,
                { response -> pagerAdapter.setItems(updatePagerItems(response, pagerItems))},
                { error -> Log.d("Swipe refresh", error.toString()) }
            )
            jsonArrayRequest.tag = TAG
            SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonArrayRequest)
            swiperefreshlayout.isRefreshing = false
        }

        // we don't use the fragment manager
        // val fm: FragmentManager = supportFragmentManager

        pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter
        pagerItems = generatePagerItems()
        pagerAdapter.setItems(pagerItems)

        imageButtonLeft.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var tab = viewPager.currentItem - 1
                if (tab < 0) { tab = pagerAdapter.itemCount - 1 }
                viewPager.currentItem = tab
            }
        })
        imageButtonRight.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var tab = viewPager.currentItem + 1
                if(tab >= pagerAdapter.itemCount){ tab = 0 }
                viewPager.currentItem = tab
            }
        })

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, getString(R.string.datasource), null,
            { response -> pagerAdapter.setItems(updatePagerItems(response, pagerItems))},
            { error -> Log.d("On create", error.toString())}
        )
        jsonArrayRequest.tag = TAG
        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonArrayRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                val jsonArrayRequest = JsonArrayRequest(
                    Request.Method.GET, getString(R.string.datasource), null,
                    { response -> pagerAdapter.setItems(updatePagerItems(response, pagerItems))},
                    { error -> Log.d("Refresh menu item", error.toString())}
                )
                jsonArrayRequest.tag = TAG
                SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonArrayRequest)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        SingletonRequestQueue.getInstance(this).cancelAll(TAG)
    }
    override fun onDestroy() {
        viewPager.adapter = null
        SingletonRequestQueue.getInstance(this).cancelAll(TAG)
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generatePagerItems(): MutableList<PagerItem> {
        return (1..ITEMS_COUNT).map {
            val color = if (it < colors.size) it else it % colors.size
                PagerItem(color=colors[color])
        }.toMutableList()
    }

    companion object {

        private const val ITEMS_COUNT = 10
        private val colors = listOf(
            "#e53935",
            "#d81b60",
            "#8e24aa",
            "#2196f3",
            "#df78ef",
            "#d3b8ae"
        )
    }
}