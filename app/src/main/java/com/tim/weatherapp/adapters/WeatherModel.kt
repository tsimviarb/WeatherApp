package com.tim.weatherapp.adapters

data class WeatherModel (
    val city: String,
    val time: String,
    val condition: String,
    val currentTemperature: String,
    val minTemperature: String,
    val maxTemperature: String,
    val windSpeed: String,
    val imageUrl: String,
    val hours: String,
    val sunrise: String,
    val sunset: String,
    val precipitation: String) {
}