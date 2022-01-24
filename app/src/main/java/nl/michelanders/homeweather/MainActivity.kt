package nl.michelanders.homeweather


import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
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

        val fm: FragmentManager = supportFragmentManager

        pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter

        pagerItems = generatePagerItems()
        pagerAdapter.setItems(pagerItems)

        val handler = Handler()

        val runnable: Runnable = object : Runnable {
            override fun run() {
                val downloadTask = DownloadTask(handler, this, object : DownloadCallback{
                    override fun updateFromDownload(result: String?) {
                        Log.d("updateFromDownLoad", result)
                        if (result != null) {
                            try {
                                var jArray = JSONArray(result)
                                Log.d("updateFromDownLoad", jArray.toString())
                                var newPagerItems: MutableList<PagerItem> = mutableListOf()
                                for (i in 0 until jArray.length()) {
                                    val roomitem = jArray.getJSONObject(i)
                                    Log.d("updateFromDownLoad", roomitem.toString())
                                    val name = roomitem.get("name").toString()
                                    val temperature = roomitem.get("temperature").toString().toFloat()
                                    val humidity = roomitem.get("humidity").toString().toFloat()
                                    val time = roomitem.get("time").toString()
                                    val newItem = pagerItems[i].copy(name = name, time = time, temperature = "%.1f".format(temperature), humidity = "%.0f".format(humidity))
                                    newPagerItems.add(newItem)
                                }
                                pagerAdapter.setItems(newPagerItems)
                            } catch (e: JSONException) {
                                Log.d("updateFromDownLoad", e.toString())
                            }
                        } else {
                            Log.d("updateFromDownLoad", "received null result")
                        }
                        // handler.postDelayed(, 5000)
                    }

                    override fun getActiveNetworkInfo(): NetworkInfo {
                        val connectivityManager =
                            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                        return connectivityManager.activeNetworkInfo
                    }

                    override fun onProgressUpdate(progressCode: Int, percentComplete: Int) {
                        when (progressCode) {
                            DownloadCallback.Progress.ERROR -> {
                            }
                            DownloadCallback.Progress.CONNECT_SUCCESS -> {
                            }
                            DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS -> {
                            }
                            DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS -> {
                            }
                            DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS -> {
                            }
                        }
                    }

                    override fun finishDownloading() {

                    }
                })
                downloadTask.execute(getString(R.string.datasource))
            }


        }
        handler.postDelayed(runnable, 0)
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