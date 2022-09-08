package com.tim.weatherapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tim.weatherapp.R
import com.tim.weatherapp.databinding.ListItemBinding

class WeatherAdapter : ListAdapter<WeatherModel, WeatherAdapter.Holder>(Comparator()) {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = ListItemBinding.bind(view)

        fun bind(item: WeatherModel) = with(binding) {

            textDataOfItem.text = item.time
            textTemperatureOfItem.text =
                item.currentTemperature.ifEmpty { "${item.maxTemperature}°C" }
            windSpeed.text = item.windSpeed
            Picasso.get().load("https:" + item.imageUrl).into(imageItemCondition)
        }
    }

    class Comparator : DiffUtil.ItemCallback<WeatherModel>() {
        override fun areItemsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {

            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {

            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {

        holder.bind(getItem(position))
    }
}