package com.example.volumeprofiler.di

import android.app.Application
import android.os.Build
import androidx.room.Room
import com.example.volumeprofiler.database.ApplicationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(applicationContext: Application) = Room.databaseBuilder(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) applicationContext.createDeviceProtectedStorageContext() else applicationContext,
            ApplicationDatabase::class.java,
            "volumeprofiler_database"
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideProfileDao(database: ApplicationDatabase) = database.profileDao()

    @Provides
    @Singleton
    fun provideAlarmDao(database: ApplicationDatabase) = database.alarmDao()

    @Provides
    @Singleton
    fun provideAlarmTriggerDao(database: ApplicationDatabase) = database.alarmTriggerDao()
}