package com.tim.weatherapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tim.weatherapp.MainViewModel
import com.tim.weatherapp.R
import com.tim.weatherapp.adapters.WeatherAdapter
import com.tim.weatherapp.adapters.WeatherModel
import com.tim.weatherapp.databinding.FragmentTenDaysNewBinding
import com.tim.weatherapp.databinding.FragmentTodayInfoBinding
import com.tim.weatherapp.databinding.FragmentTomorrowBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class TenDaysFragmentNew : Fragment() {
    private lateinit var binding: FragmentTodayInfoBinding
    private lateinit var adapter: WeatherAdapter
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
        ): View? {
        return null
    }
    
    companion object {

        @JvmStatic

        fun newInstance() = TenDaysFragmentNew()
    }
}