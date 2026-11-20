package com.snj.travelvista

import android.app.Application
import com.snj.travelvista.data.local.TravelDatabase
import com.snj.travelvista.data.remote.RetrofitClient
import com.snj.travelvista.data.repository.GlobalTravelRepository
import com.snj.travelvista.data.repository.TravelRepository
import com.snj.travelvista.utils.NetworkConnectivityMonitor
import com.snj.travelvista.utils.UserPreferences
import org.osmdroid.config.Configuration

class TravelApplication : Application() {

    val database: TravelDatabase by lazy {
        TravelDatabase.getDatabase(this)
    }

    val connectivityMonitor: NetworkConnectivityMonitor by lazy {
        NetworkConnectivityMonitor(this)
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(this)
    }

    val repository: TravelRepository by lazy {
        TravelRepository(
            wikivoyageApiService = RetrofitClient.wikivoyageApiService,
            destinationDao = database.destinationDao(),
            fallbackApiService = RetrofitClient.apiService,
            connectivityMonitor = connectivityMonitor,
            geocodingService = RetrofitClient.geocodingService
        )
    }

    val globalRepository: GlobalTravelRepository by lazy {
        GlobalTravelRepository(
            globalTravelDao = database.globalTravelDao(),
            geocodingService = RetrofitClient.geocodingService,
            weatherService = RetrofitClient.weatherService,
            countryInfoService = RetrofitClient.countryInfoService,
            currencyService = RetrofitClient.currencyService,
            wikivoyageApiService = RetrofitClient.wikivoyageApiService,
            userPreferences = userPreferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        userPreferences.applySavedTheme()
        // Configure osmdroid User-Agent per OpenStreetMap Tile Usage Policy
        Configuration.getInstance().userAgentValue = BuildConfig.API_USER_AGENT
    }
}
