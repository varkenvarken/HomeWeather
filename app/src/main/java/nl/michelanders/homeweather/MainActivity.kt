package nl.michelanders.homeweather


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/***
 * Show a series a pages for temperature and humidity data collected by Shelly H&T devices.
 *
 * Currently the source of this information is a URL that provides the data as JSON.
 * The URL is a string resource with the name data_source.
 * This information is retrieved on startup as well as on swipe to refresh.
 * It is also refreshed automatically every minute (or five minutes when on a metered connection)
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val TAG = "MainActivityTask"

    private lateinit var pagerAdapter: PagerAdapter
    private var pagerItems: MutableList<PagerItem> = mutableListOf()

    @RequiresApi(Build.VERSION_CODES.O)
    var updated = LocalDateTime.now()

    /***
     * Given a JSONArray and a mutable list of pager items, it updates the list of pager items.
     *
     * It adds new items automatically but currently does not remove any.
     *
     * The JSONArray should contain objects with the following attributes:
     * (note the that some numerical info is present as floats while others are expected to be strings even if they represent a number!)
     * name:            the name of the observation station (String)
     * time:            the time in ISO 8601 format YYYY-mm-ddTHH:MM:SSXXX:XX, e.g. 2022-01-31T10:26:38+01:00 (String)
     * temperature:     the temperature in degrees Celcius (Float)
     * humidity:        the relative humidity (Float)
     * The object might also contain the following String attributes:
     * windrose:        the average wind direction in compass directions in the last five minutes (String) e.g. 'E', 'WNW',
     * winddir:         the average wind direction in degrees in the last five minutes (String)
     * beaufort:        the average wind speed on the Beaufort scale (String)
     * windgust:        the maximum wind speed in km/h in the last five minutes (String)
     * windspeed:       the average wind speed in km/h in the last five minutes (String)
     * rain8h:          the amount of rain fallen in the last 8 hours in mm (String)
     * rainRate:        the current rate of rainfall in mm/h (String)
     */
    private fun updatePagerItems(response: JSONArray, pagerItems: MutableList<PagerItem> ): MutableList<PagerItem> {
        val newPagerItems: MutableList<PagerItem> = mutableListOf()
        for (i in 0 until response.length()) {
            val roomitem = response.getJSONObject(i)
            Log.d("updateFromDownLoad", roomitem.toString())
            val name = roomitem.get("name").toString()
            val temperature = roomitem.get("temperature").toString().toFloat()
            val humidity = roomitem.get("humidity").toString().toFloat()
            val time = roomitem.get("time").toString()
            val windrose = if(roomitem.has("windrose")){roomitem.getString("windrose")}else{null}
            val beaufort = if(roomitem.has("beaufort")){roomitem.getString("beaufort")}else{null}
            val windspeed = if(roomitem.has("windspeed")){roomitem.getString("windspeed")}else{null}
            val windgust = if(roomitem.has("windgust")){roomitem.getString("windgust")}else{null}
            val rain8h = if(roomitem.has("rain8h")){roomitem.getString("rain8h")}else{null}
            val rainrate = if(roomitem.has("rainRate")){roomitem.getString("rainRate")}else{null}
            val newItem = pagerItems[i].copy(name = name, time = time,
                temperature = "%.1f".format(temperature), humidity = "%.0f".format(humidity),
                windrose = windrose, beaufort = beaufort, windspeed = windspeed, windgust = windgust,
                rain8h = rain8h, rainrate = rainrate)
            newPagerItems.add(newItem)
        }
        return newPagerItems
    }


    /***
     * Create a JSONarray request to get the JSON from a URL using the volley package.
     *
     * Any response will updated the pagerAdapter in this activity while an error will produce a debug log message.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRequest(errorTag: String): JsonArrayRequest {
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, getString(R.string.datasource), null,
            { response -> pagerAdapter.setItems(updatePagerItems(response, pagerItems)); updated = LocalDateTime.now()},
            { error -> Log.d(errorTag, error.toString()) }
        )
        jsonArrayRequest.tag = TAG
        return jsonArrayRequest
    }

    /***
     * Create an activity with a pager and swipe to refresh functionality.
     * It also adds an overlay with left and right buttons because swiping sideways to get to a new
     * page is not always intuitive.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize the main layout with and action/toolbar
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.thetoolbar))

        // configure the swiperefreshlayout to queue an update request
        val swiperefreshlayout = (findViewById<SwipeRefreshLayout>(R.id.swiperefresh))
        swiperefreshlayout.setOnRefreshListener {
            SingletonRequestQueue.getInstance(this).addToRequestQueue(createRequest("Swipe refresh failed"))
            swiperefreshlayout.isRefreshing = false
        }

        // initialize the pager items
        pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter
        pagerItems = generatePagerItems()
        pagerAdapter.setItems(pagerItems)

        // configure the buttons.
        // unlike the swipe actions on the pager the buttons will wrap around
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

        // perform the initial data request
        SingletonRequestQueue.getInstance(this).addToRequestQueue(createRequest("Load data in onCreate failed"))
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    /***
     * update the text of the refresh menu item to reflect the time since the last update.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val refresh = menu?.findItem(R.id.menu_refresh)
        val now = LocalDateTime.now()
        val minutes = updated.until(now, ChronoUnit.MINUTES)
        val seconds = updated.until(now, ChronoUnit.SECONDS) % 60
        val secs = String.format("%02d",seconds)
        refresh?.title = "Refresh (updated $minutes:$secs min ago)"
        return true
    }

    /***
     * configure the refreesh menu item to queue a data refresh
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                SingletonRequestQueue.getInstance(this).addToRequestQueue(createRequest("Refresh from options menu failed"))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /***
     * configure a broadcast receiver to act on the ACTION_TIME_TICK intents that are sent by the system every minute.
     * If we are on a non-metered connection we queue an update request on every tick, otherwise just every 5 ticks.
     * We assume that if *any* available network is non-metered the routing will take care of actually using the cheapest option.
     */
    var ticks = 0
    val br: BroadcastReceiver = MyBroadcastReceiver()
    val filter = IntentFilter(Intent.ACTION_TIME_TICK)

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, p1: Intent?) {
            Log.d("Broadcast", p1.toString())
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var notMetered = false
            connectivityManager.allNetworks.forEach { network ->
                connectivityManager.getNetworkCapabilities(network).apply {
                    if (hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                        notMetered = true
                    }
                }
            }
            Log.d("Broadcast", "Not metered: $notMetered")
            ticks++
            if(notMetered or (ticks%5 == 0)){
                Log.d("Broadcast", "reloading data. ticks= "+ticks)
                SingletonRequestQueue.getInstance(context).addToRequestQueue(createRequest("Automatic reloading failed"))
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        SingletonRequestQueue.getInstance(this).addToRequestQueue(createRequest("Reload on resume failed"))
        ticks = 0
        registerReceiver(br, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(br)
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
                PagerItem(color =colors[color])
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


