package com.dicoding.kaloriku.data.di

import FoodItemDao
import android.content.Context
import com.dicoding.kaloriku.data.pref.UserPreference
import com.dicoding.kaloriku.data.pref.UserRepository
import com.dicoding.kaloriku.data.pref.dataStore
import com.dicoding.kaloriku.data.retrofit.ApiConfig

class Injection {
    companion object {
        fun provideFoodItemDao(context: Context): FoodItemDao {
            return AppDatabase.getDatabase(context).foodItemDao()
        }

        // Function to provide UserRepository
        fun provideUserRepository(context: Context): UserRepository {
            val pref = UserPreference.getInstance(context.dataStore)
            val apiService = ApiConfig.getApiService()
            return UserRepository.getInstance(pref, apiService)
        }
    }
}