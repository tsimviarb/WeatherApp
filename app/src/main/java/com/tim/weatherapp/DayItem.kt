package com.tim.weatherapp

data class DayItem(
    val cityName: String,
    val dateAndTime: String,
    val condition: String,
    val imageURL: String,
    val currentTemperature: String,
    val maxTemperature: String,
    val minTemperature: String,
    val hours: String)
