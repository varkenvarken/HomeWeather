package nl.michelanders.homeweather


import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONException
import java.time.LocalDateTime


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var pagerAdapter: PagerAdapter
    private var pagerItems: MutableList<PagerItem> = mutableListOf()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.thetoolbar))

        val fm: FragmentManager = supportFragmentManager

        pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter

        pagerItems = generatePagerItems()
        pagerAdapter.setItems(pagerItems)

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, getString(R.string.datasource), null,
            Response.Listener { response ->
                Log.d("JSONARRAY RECEIVED", response.toString())
                var newPagerItems: MutableList<PagerItem> = mutableListOf()
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
                pagerAdapter.setItems(newPagerItems)
            },
            Response.ErrorListener { error ->
                // TODO: Handle error
                Log.d("JSONARRAY RECEIVED", error.toString())
            }
        )

        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonArrayRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            // Check if user triggered a refresh:
            R.id.menu_refresh -> {
                Log.i("REFRESH", "Refresh menu item selected")
                val jsonArrayRequest = JsonArrayRequest(
                    Request.Method.GET, getString(R.string.datasource), null,
                    Response.Listener { response ->
                        Log.d("JSONARRAY RECEIVED", response.toString())
                        var newPagerItems: MutableList<PagerItem> = mutableListOf()
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
                        pagerAdapter.setItems(newPagerItems)
                    },
                    Response.ErrorListener { error ->
                        // TODO: Handle error
                        Log.d("JSONARRAY RECEIVED", error.toString())
                    }
                )

                SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonArrayRequest)
                // Start the refresh background task.
                // This method calls setRefreshing(false) when it's finished.
                //myUpdateOperation()

                return true
            }
        }

        // User didn't trigger a refresh, let the superclass handle this action
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        viewPager.adapter = null
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