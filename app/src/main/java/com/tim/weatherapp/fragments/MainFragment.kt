package com.tim.weatherapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.tim.weatherapp.databinding.FragmentMainBinding
import android.Manifest
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import com.tim.weatherapp.MainViewModel
import com.tim.weatherapp.adapters.ViewPagerAdapter
import com.tim.weatherapp.adapters.WeatherModel
import kotlinx.coroutines.internal.SynchronizedObject
import org.json.JSONArray
import org.json.JSONObject

const val API_Key = "918ad9c39d634039b4592144222408"

class MainFragment : Fragment() {
    private val tabsList = listOf(
        "Today",
        "Tomorrow",
        "10 days"
    )
    private val fragmentsList = listOf(TodayInfoFragment.newInstance(),
        TomorrowFragment.newInstance(),
        TenDaysFragmentNew.newInstance())

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        requestWeatherData("Minsk")
        updateCurrentCard()
    }

    private fun init() = with(binding){

        val adapter = ViewPagerAdapter(activity as FragmentActivity, fragmentsList)
        ViewPager.adapter = adapter

        TabLayoutMediator(tabLayoutDaysSwitcher, ViewPager){

            tab, position -> tab.text = tabsList[position]
        }.attach()
    }

    private fun updateCurrentCard() = with(binding){

        model.liveDataCurrent.observe(viewLifecycleOwner){
            val maxMinTemperature = "${it.maxTemperature}°C/${it.minTemperature}°C"
            textViewData.text = it.time
            textViewCityName.text = it.city
            textViewCurrentTemperature.text = it.currentTemperature
            textViewCondition.text = it.condition
            textViewMaxMinTemperature.text = maxMinTemperature
            Picasso.get().load("https:" + it.imageUrl).into(imageWeather)
        }
    }

    private fun permissionListener(){
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String){

        val url = "https://api.weatherapi.com/v1/forecast.json?" +
                "key=" + API_Key +
                "&q=" + city +
                "&days=10" +
                "&aqi=no&alerts=no"

        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            { result -> parseWeatherData(result)},
            { error -> Log.d("MyLog", "ERROR : $error")}
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String){

        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
        parseTomorrowData(mainObject, list[1])
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel>{

        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val cityName = mainObject.getJSONObject("location").getString("name")

        for(i in 0 until daysArray.length()){
            val day = daysArray[i] as JSONObject

            val item = WeatherModel(
                cityName,
                day.getString("date"),
                day.getJSONObject("day")
                    .getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c"),
                day.getJSONObject("day").getString("mintemp_c"),
                "",
                day.getJSONObject("day")
                    .getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel){

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
            weatherItem.hours
        )
        model.liveDataCurrent.value = item
    }

    private fun parseTomorrowData(mainObject: JSONObject, weatherItem: WeatherModel){

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
            weatherItem.hours
        )
        model.liveDataTomorrow.value = item
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}