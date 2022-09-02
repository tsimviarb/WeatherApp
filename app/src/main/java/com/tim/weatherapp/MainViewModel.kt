package com.tim.weatherapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tim.weatherapp.adapters.WeatherModel

class MainViewModel : ViewModel() {

    val liveDataCurrent = MutableLiveData<WeatherModel>()
    val liveDataTomorrow = MutableLiveData<WeatherModel>()
    val liveDataList = MutableLiveData<List<WeatherModel>>()
}