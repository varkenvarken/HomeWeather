package nl.michelanders.homeweather

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import nl.michelanders.homeweather.R
import kotlinx.android.synthetic.main.fragment_pager.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit
import javax.xml.datatype.DatatypeConstants.MONTHS

class PagerFragment : Fragment(R.layout.fragment_pager) {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        view.setBackgroundColor(Color.parseColor(args.getString(EXTRA_COLOR)))
        setValue(args.getString(EXTRA_NAME),
            args.getString(EXTRA_TIME),
            args.getString(EXTRA_TEMPERATURE),
            args.getString(EXTRA_HUMIDITY),
            args.getString(EXTRA_WINDROSE),
            args.getString(EXTRA_BEAUFORT),
            args.getString(EXTRA_WINDSPEED),
            args.getString(EXTRA_WINDGUST))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setValue(newName: String?, newTime: String?,
                 newTemperature: String?, newHumidity: String?,
                 newWindrose: String?, newBeaufort:String?, newWindspeed:String?, newWindgust:String?) {
        Log.d("setValue", newTime)
        val timeFormatOffset = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ssXXXXX")
        val timeFormatSimple = DateTimeFormatter
            .ofPattern("HH:mm")
        val pt = LocalDateTime.parse(newTime, timeFormatOffset)
        val now = LocalDateTime.now()
        val hours = pt.until(now,ChronoUnit.HOURS)
        val minutes = pt.until(now,ChronoUnit.MINUTES)
        txtTitle.text = newName
        txtTemperature.text = newTemperature
        txtHumidity.text = newHumidity
        val newTimeSimple = pt.format(timeFormatSimple)
        val ago = if (hours>0){"$hours hours ago"} else {"$minutes minutes ago"}
        txtTemperature.tooltipText = "Changed: $newTimeSimple ($ago)"
        wind.visibility = if(newWindrose==null) {ImageView.GONE}else{ImageView.VISIBLE}
        wind.rotation = when(newWindrose){
            "N"-> 0f "NNE"->22.5f "NE"->45f "ENE"->67.5f
            "E"->90f "ESE"->112.5f "SE"->135f "SSE"->157.5f
            "S"->180f "SSW"->202.5f "SW"->225f "WSW"->247.5f
            "W"->270f "WNW"->292.5f "NW"->315f "NNW"->337.5f
            else -> {0f}
        }
        beaufort.visibility = if(newBeaufort==null) {ImageView.GONE}else{ImageView.VISIBLE}
        beaufort.text = newBeaufort
        beaufort.tooltipText = "Average $newWindspeed km/h\nGust $newWindgust km/h"
    }

    companion object {

        private const val EXTRA_COLOR = "color"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_TEMPERATURE = "temperature"
        private const val EXTRA_HUMIDITY = "humidity"
        private const val EXTRA_WINDROSE = "windrose"
        private const val EXTRA_BEAUFORT = "beaufort"
        private const val EXTRA_WINDSPEED = "windspeed"
        private const val EXTRA_WINDGUST = "windgust"

        /***
         * create a new PagerFragment
         */
        fun newInstance(item: PagerItem): PagerFragment {
            Log.d("newInstance", item.time)
            return PagerFragment().apply {
                arguments = Bundle(3).apply {
                    putString(EXTRA_COLOR, item.color)
                    putString(EXTRA_TIME, item.time)
                    putString(EXTRA_NAME, item.name)
                    putString(EXTRA_TEMPERATURE, item.temperature)
                    putString(EXTRA_HUMIDITY, item.humidity)
                    putString(EXTRA_WINDROSE, item.windrose)
                    putString(EXTRA_BEAUFORT, item.beaufort)
                    putString(EXTRA_WINDSPEED, item.windspeed)
                    putString(EXTRA_WINDGUST, item.windgust)
                }
            }
        }
    }
}