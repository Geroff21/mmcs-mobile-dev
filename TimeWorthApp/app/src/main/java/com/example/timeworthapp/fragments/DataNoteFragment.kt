package com.example.timeworthapp.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.example.timeworthapp.R
import com.example.timeworthapp.api.GeoCoderResponse
import com.example.timeworthapp.api.RetrofitInstance
import com.example.timeworthapp.api.WeatherResponse
import com.example.timeworthapp.databinding.FragmentDataBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DataNoteFragment : Fragment(R.layout.fragment_data), SearchView.OnQueryTextListener, MenuProvider {

    private var dataBinding: FragmentDataBinding? = null
    private var searchView: SearchView? = null
    private val binding get() = dataBinding!!

    private lateinit var weatherTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        dataBinding = FragmentDataBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        clearSearchView()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        super.onViewCreated(view, savedInstanceState)
        fetchGeoCodeData("Bataysk")
    }

    private fun fetchWeatherData(city: String, latitude: Double, longitude: Double) {
        RetrofitInstance.api_weather.getCurrentWeather(latitude, longitude).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    weatherResponse?.let {
                        val weatherInfo = "Город: ${city} \n" +
                                "Температура: ${it.current_weather.temperature}°C\n" +
                                "Скорость ветра: ${it.current_weather.windspeed} км/ч\n" +
                                "Код погоды: ${it.current_weather.weathercode}"
                        binding.weatherTextView.text = weatherInfo
                    }
                } else {
                    binding.weatherTextView.text = "Ошибка получения данных."
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                binding.weatherTextView.text = "Ошибка: ${t.message}"
            }
        })
    }

    private fun fetchGeoCodeData(query: String) {

        val apiKey = ""

        RetrofitInstance.api_geocoder.getCurrentCoords(query, apiKey).enqueue(object : Callback<List<GeoCoderResponse>> {
            override fun onResponse(call: Call<List<GeoCoderResponse>>, response: Response<List<GeoCoderResponse>>) {
                if (response.isSuccessful) {
                    val geoResponse = response.body()
                    if (!geoResponse.isNullOrEmpty()) {

                        val lat = geoResponse[0].lat
                        val lon = geoResponse[0].lon

                        fetchWeatherData(query, lat, lon)

                    } else {
                        binding.weatherTextView.text = "Город не найден."
                    }
                } else {
                    binding.weatherTextView.text = "Ошибка получения данных."
                }
            }

            override fun onFailure(call: Call<List<GeoCoderResponse>>, t: Throwable) {
                binding.weatherTextView.text = "Ошибка: ${t.message}"
            }
        })
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {

            binding.weatherTextView.text = "Выполняется поиск..."
            fetchGeoCodeData(query)

        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

        menu.clear()
        menuInflater.inflate(R.menu.api_menu, menu)

        clearSearchView()

        val menuSearch = menu.findItem(R.id.searchCityMenu).actionView as SearchView
        menuSearch.isSubmitButtonEnabled = true
        menuSearch.setOnQueryTextListener(this)

        clearSearchView()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    private fun clearSearchView() {
        searchView?.setQuery("", false)
        searchView?.clearFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearSearchView()
    }

    override fun onPause() {
        super.onPause()
        clearSearchView()
    }

}