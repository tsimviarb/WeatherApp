package com.tim.weatherapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tim.weatherapp.MainViewModel
import com.tim.weatherapp.adapters.WeatherAdapter
import com.tim.weatherapp.adapters.WeatherModel
import com.tim.weatherapp.databinding.FragmentTomorrowBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class TomorrowFragment : Fragment() {

    private lateinit var binding: FragmentTomorrowBinding
    private lateinit var adapter: WeatherAdapter
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTomorrowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        model.liveDataTomorrow.observe(viewLifecycleOwner){

            adapter.submitList(getTomorrowHoursModel(it))
        }
    }

    private fun initRecyclerView() = with(binding){

        RecyclerViewTomorrow.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        adapter = WeatherAdapter()
        RecyclerViewTomorrow.adapter = adapter
    }

    private fun getTomorrowHoursModel(weatherItem: WeatherModel): List<WeatherModel> {

        val hoursArray = JSONArray(weatherItem.hours)
        val list = ArrayList<WeatherModel>()

        for (i in 0 until hoursArray.length()) {

            val windSpeedMS = ((hoursArray[i] as JSONObject).getString("wind_mph")
                .toDouble() * 2.8).roundToInt()

            val item = WeatherModel(
                "",
                (hoursArray[i] as JSONObject).getString("time"),
                "",
                (hoursArray[i] as JSONObject).getString("temp_c") + "°C",
                "",
                "",
                "${windSpeedMS / 10}.${windSpeedMS % 10} m/s",
                (hoursArray[i] as JSONObject)
                    .getJSONObject("condition").getString("icon"),
                ""
            )
            list.add(item)
        }
        return list
    }

    companion object {

        @JvmStatic
        fun newInstance() = TomorrowFragment()
    }
}