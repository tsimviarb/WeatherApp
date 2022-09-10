package com.tim.weatherapp.fragments

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import com.tim.weatherapp.MainViewModel
import com.tim.weatherapp.R
import com.tim.weatherapp.adapters.ViewPagerAdapter
import com.tim.weatherapp.adapters.WeatherModel
import com.tim.weatherapp.databinding.FragmentMainBinding
import org.json.JSONObject
import kotlin.math.roundToInt

const val API_Key = "05be604b8a704207b3a135103220709"

class MainFragment : Fragment() {
    private val tabsList = listOf(
        "Today",
        "Tomorrow",
        "10 days"
    )
    private val fragmentsList = listOf(
        TodayInfoFragment.newInstance(),
        TomorrowFragment.newInstance(),
        TenDaysFragmentNew.newInstance()
    )

    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding){
        super.onViewCreated(view, savedInstanceState)
        backgroundImg.setImageResource(R.drawable.summer_day_sunny)
        checkPermission()
        init()
        requestWeatherData("Minsk")
        cardView.setBackgroundResource(R.drawable.borders_for_view)
        cardView2.setBackgroundResource(R.drawable.borders_for_view)
        updateCurrentCard()
    }

    private fun init() = with(binding) {

        ViewPager.isUserInputEnabled = false
        val adapter = ViewPagerAdapter(activity as FragmentActivity, fragmentsList)
        ViewPager.adapter = adapter

        TabLayoutMediator(tabLayoutDaysSwitcher, ViewPager) {

                tab, position ->
            tab.text = tabsList[position]
        }.attach()
    }

    private fun updateCurrentCard() = with(binding) {

        model.liveDataCurrent.observe(viewLifecycleOwner) {
            val maxMinTemperature = "${it.maxTemperature}°C/${it.minTemperature}°C"
            textViewData.text = it.time.subSequence(11, 16)
            textViewCityName.text = it.city
            textViewCurrentTemperature.text = it.currentTemperature
            textViewCondition.text = it.condition
            textViewMaxMinTemperature.text = maxMinTemperature
            Picasso.get().load("https:" + it.imageUrl).into(imageWeather)
            changeBackgroundImage(it.time,
                it.sunrise,
                it.sunset,
                "Moderate or heavy sleet showers" /*it.condition*/,
                it.precipitation)
            //ChangeBackgroundImage(binding).changeBackgroundImage(3)
        }
    }

    private fun permissionListener() {
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission() {
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String) {

        val url = "https://api.weatherapi.com/v1/forecast.json?" +
                "key=" + API_Key +
                "&q=" + city +
                "&days=10" +
                "&aqi=no&alerts=no"

        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            { result -> parseWeatherData(result) },
            { error -> Log.d("MyLog", "ERROR : $error") }
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String) {

        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
        parseTomorrowData(mainObject, list[1])
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel> {

        val list = ArrayList<WeatherModel>()

        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val cityName = mainObject.getJSONObject("location").getString("name")

        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject

            val windSpeedMS = (day.getJSONObject("day").getString("maxwind_kph")
                .toDouble() * 2.8).roundToInt()

            val item = WeatherModel(
                cityName,
                day.getString("date"),
                day.getJSONObject("day")
                    .getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("mintemp_c"),
                day.getJSONObject("day").getString("maxtemp_c"),
                "${windSpeedMS / 10}.${windSpeedMS % 10}m/s",
                day.getJSONObject("day")
                    .getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString(),
                day.getJSONObject("astro").getString("sunrise"),
                day.getJSONObject("astro").getString("sunset"),
                ""
            )
            list.add(item)
        }
        model.liveDataTenDays.value = list
        return list
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel) {

        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.minTemperature,
            weatherItem.maxTemperature,
            weatherItem.windSpeed,
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("icon"),
            weatherItem.hours,
            weatherItem.sunrise,
            weatherItem.sunset,
            mainObject.getJSONObject("current").getString("precip_mm")
        )
        model.liveDataCurrent.value = item
    }

    private fun parseTomorrowData(mainObject: JSONObject, weatherItem: WeatherModel) {

        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.minTemperature,
            weatherItem.maxTemperature,
            weatherItem.windSpeed,
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("icon"),
            weatherItem.hours,
            weatherItem.sunrise,
            weatherItem.sunset,
            weatherItem.precipitation
        )
        model.liveDataTomorrow.value = item
    }

    private fun changeBackgroundImage(
        month: String,
        sunriseX: String,
        sunsetX: String,
        condition: String,
        precipitation: String) = with(binding){

        val season = 12//month[5].digitToInt() * 10 + month[6].digitToInt()
        val currentTime = 2300//month[11].digitToInt() * 1000 + month[12].digitToInt() * 100
        val sunrise = sunriseX[0].digitToInt() * 1000 +
                sunriseX[1].digitToInt() * 100 +
                sunriseX[3].digitToInt() * 10 +
                sunriseX[4].digitToInt()
        val sunset = sunsetX[0].digitToInt() * 1000 +
                sunsetX[1].digitToInt() * 100 +
                sunsetX[3].digitToInt() * 10 +
                sunsetX[4].digitToInt() + 12 * 100
        val snowPrecipitation = precipitation[0].digitToInt() * 10

        Toast.makeText(activity, "$currentTime $sunrise $sunset", Toast.LENGTH_LONG).show()

        if (season in 9..11){

            if (currentTime in sunrise .. 1200){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.autumn_morning_sunny)
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.autumn_morning_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_morning_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_morning_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.autumn_morning_thunder)
                }
            }

            if (currentTime in 1200 until sunset - 100){


                if (condition == "Sunny" || condition == "Clear"|| condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.autumn_day_sunny)
                }

                if (condition == "Cloudy" || condition == "Overcast" ||
                    condition == "Mist" || condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.autumn_day_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_day_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_day_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.autumn_day_thunder)
                }
            }

            if (currentTime in (sunset - 100) .. (sunset + 100)){


                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.autumn_evening_sunny)
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.autumn_evening_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_evening_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_evening_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.autumn_evening_thunder)
                }
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise){

                if (condition == "Sunny" || condition == "Clear"|| condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.autumn_night_sunny)
                }

                if (condition == "Cloudy" || condition == "Overcast" ||
                    condition == "Mist" || condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.autumn_night_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_night_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.autumn_night_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.autumn_night_thunder)
                }
            }
        }

        if (season == 12 || season in 1..2){

            if (currentTime in sunrise .. 1200){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    if(snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_morning_light_sunny)
                    } else{

                        backgroundImg.setImageResource(R.drawable.winter_morning_dark_sunny)
                    }
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog" ||
                    condition == "Freezing fog"){

                    backgroundImg.setImageResource(R.drawable.winter_morning_dark_cloudy)
                }

                if (condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Light showers of ice pellets" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light snow showers" ||
                    condition == "Light sleet showers" ||
                    condition == "Patchy snow possible" ||
                    condition == "Blowing snow"||
                    condition == "Blizzard" ||
                    condition == "Light snow" ||
                    condition == "Ice pellets") {

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_morning_light_littlesnowy)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_morning_dark_littlesnowy)
                    }
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy showers of ice pellets" ||
                    condition == "Moderate or heavy snow showers" ||
                    condition == "Moderate or heavy sleet showers" ||
                    condition == "Patchy moderate snow"||
                    condition == "Moderate snow" ||
                    condition == "Patchy heavy snow"||
                    condition == "Heavy snow"){

                    backgroundImg.setImageResource(R.drawable.winter_morning_light_snowy)
                }
            }

            if (currentTime in 1200 until sunset - 100){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_day_light_sunny)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_day_dark_sunny)
                    }
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog" ||
                    condition == "Freezing fog"){

                    backgroundImg.setImageResource(R.drawable.winter_day_dark_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Light showers of ice pellets" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light snow showers" ||
                    condition == "Light sleet showers"||
                    condition == "Blowing snow"||
                    condition == "Blizzard" ||
                    condition == "Light snow" ||
                    condition == "Ice pellets"){

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_day_light_littlesnowy)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_day_dark_littlesnowy)
                    }
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy showers of ice pellets" ||
                    condition == "Moderate or heavy snow showers" ||
                    condition == "Moderate or heavy sleet showers"||
                    condition == "Patchy moderate snow"||
                    condition == "Moderate snow"||
                    condition == "Patchy heavy snow"||
                    condition == "Heavy snow"){

                     backgroundImg.setImageResource(R.drawable.winter_day_light_snowy)
                }
            }

            if (currentTime in (sunset - 100) .. (sunset + 100)){


                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_evening_light_sunny)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_evening_dark_sunny)
                    }
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog" ||
                    condition == "Freezing fog"){

                    backgroundImg.setImageResource(R.drawable.winter_evening_dark_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Light showers of ice pellets" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light snow showers" ||
                    condition == "Light sleet showers"||
                    condition == "Blowing snow"||
                    condition == "Blizzard" ||
                    condition == "Light snow" ||
                    condition == "Ice pellets"){

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_evening_light_littlesnowy)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_evening_dark_littlesnowy)
                    }
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy showers of ice pellets" ||
                    condition == "Moderate or heavy snow showers" ||
                    condition == "Moderate or heavy sleet showers"||
                    condition == "Patchy moderate snow"||
                    condition == "Moderate snow"||
                    condition == "Patchy heavy snow"||
                    condition == "Heavy snow"){

                    backgroundImg.setImageResource(R.drawable.winter_evening_light_snowy)
                }
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_night_light_sunny)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_night_dark_sunny)
                    }
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog" ||
                    condition == "Freezing fog"){

                    backgroundImg.setImageResource(R.drawable.winter_night_dark_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Light showers of ice pellets" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light snow showers" ||
                    condition == "Light sleet showers"||
                    condition == "Blowing snow" ||
                    condition == "Blizzard" ||
                    condition == "Light snow" ||
                    condition == "Ice pellets"){

                    if (snowPrecipitation > 30.0){

                        backgroundImg.setImageResource(R.drawable.winter_night_light_littlesnowy)
                    } else {

                        backgroundImg.setImageResource(R.drawable.winter_night_dark_littlesnowy)
                    }
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy showers of ice pelletsr" ||
                    condition == "Moderate or heavy snow showers" ||
                    condition == "Moderate or heavy sleet showers"||
                    condition == "Patchy moderate snow" ||
                    condition == "Moderate snow"||
                    condition == "Patchy heavy snow" ||
                    condition == "Heavy snow"){

                    backgroundImg.setImageResource(R.drawable.winter_night_light_snowy)
                }
            }
        }

        if (season in 3..5){

            if (currentTime in sunrise .. 1200){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.spring_morning_sunny)
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.spring_morning_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_morning_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_morning_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.spring_morning_thunder)
                }
            }

            if (currentTime in 1200 until sunset - 100){


                if (condition == "Sunny" || condition == "Clear"|| condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.spring_day_sunny)
                }

                if (condition == "Cloudy" || condition == "Overcast" ||
                    condition == "Mist" || condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.spring_day_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_day_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_day_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.spring_day_thunder)
                }
            }

            if (currentTime in (sunset - 100) .. (sunset + 100)){


                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.spring_evening_sunny)
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.spring_evening_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_evening_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_evening_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.spring_evening_thunder)
                }
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise){

                if (condition == "Sunny" || condition == "Clear"|| condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.spring_night_sunny)
                }

                if (condition == "Cloudy" || condition == "Overcast" ||
                    condition == "Mist" || condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.spring_night_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_night_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.spring_night_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.spring_night_thunder)
                }
            }
        }

        if (season in 6..8){

            if (currentTime in sunrise .. 1200){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.summer_morning_sunny)
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.summer_morning_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_morning_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_morning_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.summer_morning_thunder)
                }
            }

            if (currentTime in 1200 until sunset - 100){


                if (condition == "Sunny" || condition == "Clear"|| condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.summer_day_sunny)
                }

                if (condition == "Cloudy" || condition == "Overcast" ||
                    condition == "Mist" || condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.summer_day_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_day_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_day_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.summer_day_thunder)
                }
            }

            if (currentTime in (sunset - 100) .. (sunset + 100)){


                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.summer_evening_sunny)
                }

                if (condition == "Cloudy" ||
                    condition == "Overcast" ||
                    condition == "Mist" ||
                    condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.summer_evening_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_evening_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_evening_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.summer_evening_thunder)
                }
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise){

                if (condition == "Sunny" ||
                    condition == "Clear"||
                    condition == "Partly cloudy"){

                    backgroundImg.setImageResource(R.drawable.summer_night_sunny)
                }

                if (condition == "Cloudy" || condition == "Overcast" ||
                    condition == "Mist" || condition == "Fog"){

                    backgroundImg.setImageResource(R.drawable.summer_night_cloudy)
                }

                if (condition == "Patchy rain possible" ||
                    condition == "Patchy sleet possible" ||
                    condition == "Patchy freezing drizzle possible" ||
                    condition == "Patchy light drizzle" ||
                    condition == "Light drizzle" ||
                    condition == "Freezing drizzle" ||
                    condition == "Patchy light rain" ||
                    condition == "Light rain" ||
                    condition == "Moderate rain at times" ||
                    condition == "Moderate rain" ||
                    condition == "Light freezing rain" ||
                    condition == "Light sleet" ||
                    condition == "Moderate or heavy sleet"||
                    condition == "Light rain shower" ||
                    condition == "Light sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_night_rainy)
                }

                if (condition == "Heavy freezing drizzle" ||
                    condition == "Heavy rain at times" ||
                    condition == "Heavy rain" ||
                    condition == "Moderate or heavy freezing rain" ||
                    condition == "Moderate or heavy rain shower" ||
                    condition == "Torrential rain shower" ||
                    condition == "Moderate or heavy sleet showers"){

                    backgroundImg.setImageResource(R.drawable.summer_night_hardrain)
                }

                if (condition == "Patchy light rain with thunder" ||
                    condition == "Moderate or heavy rain with thunder" ||
                    condition == "Patchy light snow with thunder" ||
                    condition == "Moderate or heavy snow with thunder" ||
                    condition == "Thundery outbreaks possible"){

                    backgroundImg.setImageResource(R.drawable.summer_night_thunder)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}