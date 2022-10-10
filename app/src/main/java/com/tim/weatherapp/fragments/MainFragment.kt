package com.tim.weatherapp.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.Debug.getLocation
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import com.tim.weatherapp.DialogManager
import com.tim.weatherapp.MainViewModel
import com.tim.weatherapp.R
import com.tim.weatherapp.adapters.ViewPagerAdapter
import com.tim.weatherapp.adapters.WeatherModel
import com.tim.weatherapp.databinding.FragmentMainBinding
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.system.exitProcess


const val API_Key = "a4a2d1e991a441618e375056220310"

class MainFragment : Fragment() {

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var city = "Minsk"

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        super.onViewCreated(view, savedInstanceState)
        backgroundImg.setImageResource(R.drawable.summer_day_sunny)
        checkPermission()
        init()
        requestWeatherData(city)
        cardView.setBackgroundResource(R.drawable.borders_for_view)
        cardView2.setBackgroundResource(R.drawable.borders_for_view)
        updateCurrentCard()
        checkLocation()
    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }

    private fun init() = with(binding) {

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        ViewPager.isUserInputEnabled = false
        val adapter = ViewPagerAdapter(activity as FragmentActivity, fragmentsList)
        ViewPager.adapter = adapter

        TabLayoutMediator(tabLayoutDaysSwitcher, ViewPager) { tab, position ->
            tab.text = tabsList[position]
        }.attach()

        updateCity()
        checkLocation()
    }

    private fun checkLocation() {

        if (locationEnabled()) {
            getLocation()
        } else {
            DialogManager.locationSettingDialog(requireContext(), object : DialogManager.Listener {
                override fun onClick() {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

                }
            })

            return
        }
    }

    private fun locationEnabled(): Boolean {

        val locationManager =
            activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getLocation() {
        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ct.token)
            .addOnCompleteListener {

                requestWeatherData("${it.result.latitude},${it.result.longitude}")
            }
    }

    private fun updateCity() = with(binding) {

        imageButtonSearchCity.setOnClickListener {

            imageButtonSearchCity.visibility = View.INVISIBLE
            imageButtonSearchCity2.visibility = View.VISIBLE
            editCity.visibility = View.VISIBLE
            textViewCityName.visibility = View.INVISIBLE

            textViewCityName.text = editCity.text.ifEmpty { city }
            city = editCity.text.toString().ifEmpty { city }
            requestWeatherData("${textViewCityName.text}")

            editCity.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                    // if the event is a key down event on the enter button
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER
                    ) {

                        imageButtonSearchCity2.visibility = View.INVISIBLE
                        imageButtonSearchCity.visibility = View.VISIBLE
                        editCity.visibility = View.INVISIBLE
                        textViewCityName.visibility = View.VISIBLE

                        textViewCityName.text = editCity.text.ifEmpty { city }
                        city = editCity.text.toString().ifEmpty { city }
                        editCity.text = null
                        editCity.hint = "Enter city"

                        requestWeatherData("${textViewCityName.text}")

                        return true
                    } else {

                        return false
                    }

                }
            })

            imageButtonSearchCity2.setOnClickListener {

                imageButtonSearchCity2.visibility = View.INVISIBLE
                imageButtonSearchCity.visibility = View.VISIBLE
                editCity.visibility = View.INVISIBLE
                textViewCityName.visibility = View.VISIBLE

                textViewCityName.text = editCity.text.ifEmpty { city }
                city = editCity.text.toString().ifEmpty { city }
                editCity.text = null
                editCity.hint = "Enter city"

                requestWeatherData("${textViewCityName.text}")
            }
        }
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

            changeBackgroundImage(
                it.time,
                it.sunrise,
                it.sunset,
                it.maxTemperature.toFloat(),
                it.minTemperature.toFloat()
            )
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

    @SuppressLint("ResourceAsColor")
    private fun changeBackgroundImage(
        month: String,
        sunriseX: String,
        sunsetX: String,
        maxTemperature: Float,
        minTemperature: Float
    ) = with(binding) {

        val season = month[5].digitToInt() * 10 + month[6].digitToInt()
        val currentTime = month[11].digitToInt() * 1000 +
                month[12].digitToInt() * 100 +
                month[14].digitToInt() * 10 +
                month[15].digitToInt()
        val sunrise = sunriseX[0].digitToInt() * 1000 +
                sunriseX[1].digitToInt() * 100 +
                sunriseX[3].digitToInt() * 10 +
                sunriseX[4].digitToInt()
        val sunset = sunsetX[0].digitToInt() * 1000 +
                sunsetX[1].digitToInt() * 100 +
                sunsetX[3].digitToInt() * 10 +
                sunsetX[4].digitToInt() + 12 * 100

        if (season in 9..11) {

            if (currentTime in sunrise..1200) {
                backgroundImg.setImageResource(R.drawable.autumn_morning_sunny)
            }

            if (currentTime in 1200 until sunset - 100) {

                backgroundImg.setImageResource(R.drawable.autumn_day_sunny)
            }

            if (currentTime in (sunset - 100)..(sunset + 100)) {

                backgroundImg.setImageResource(R.drawable.autumn_evening_sunny)
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise) {
                backgroundImg.setImageResource(R.drawable.autumn_night_sunny)
            }
        }

        if (season == 12 || season in 1..2) {

            if (currentTime in sunrise..1200) {

                if (minTemperature < -5) {

                    backgroundImg.setImageResource(R.drawable.winter_morning_light_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.winter_morning_dark_sunny)
                }
            }

            if (currentTime in 1200 until sunset - 100) {

                if (minTemperature < -5) {

                    backgroundImg.setImageResource(R.drawable.winter_day_light_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.winter_day_dark_sunny)
                }
            }

            if (currentTime in (sunset - 100)..(sunset + 100)) {

                if (minTemperature < -5) {

                    backgroundImg.setImageResource(R.drawable.winter_evening_light_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.winter_evening_dark_sunny)
                }
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise) {

                if (minTemperature < -5) {

                    backgroundImg.setImageResource(R.drawable.winter_night_light_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.winter_night_dark_sunny)
                }
            }
        }

        if (season in 3..5) {

            if (currentTime in sunrise until 1200) {

                backgroundImg.setImageResource(R.drawable.spring_morning_sunny)
            }

            if (currentTime in 1200 until sunset - 100) {

                backgroundImg.setImageResource(R.drawable.spring_day_sunny)
            }

            if (currentTime in (sunset - 100)..(sunset + 100)) {

                backgroundImg.setImageResource(R.drawable.spring_evening_sunny)
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise) {

                backgroundImg.setImageResource(R.drawable.spring_night_sunny)
            }
        }

        if (season in 6..8) {

            if (currentTime in sunrise..1200) {

                if (maxTemperature > 27) {

                    backgroundImg.setImageResource(R.drawable.summer_desert_morning_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.summer_morning_sunny)
                }
            }

            if (currentTime in 1200 until sunset - 100) {

                if (maxTemperature > 27) {

                    backgroundImg.setImageResource(R.drawable.summer_desert_day_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.summer_day_sunny)
                }
            }

            if (currentTime in (sunset - 100)..(sunset + 100)) {

                if (maxTemperature > 27) {

                    backgroundImg.setImageResource(R.drawable.summer_desert_evening_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.summer_evening_sunny)
                }
            }

            if (currentTime in (sunset + 100) until 2400 || currentTime in 0 until sunrise) {

                if (maxTemperature > 27) {

                    backgroundImg.setImageResource(R.drawable.summer_desert_night_sunny)
                } else {

                    backgroundImg.setImageResource(R.drawable.summer_night_sunny)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}