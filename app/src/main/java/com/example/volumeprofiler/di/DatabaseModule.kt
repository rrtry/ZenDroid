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
    fun provideEventDao(database: ApplicationDatabase) = database.getEventDao()

    @Provides
    @Singleton
    fun provideEventRelationDao(database: ApplicationDatabase) = database.getEventRelationDao()

    @Provides
    @Singleton
    fun provideProfileDao(database: ApplicationDatabase) = database.getProfileDao()

    @Provides
    @Singleton
    fun provideAlarmDao(database: ApplicationDatabase) = database.getAlarmDao()

    @Provides
    @Singleton
    fun provideAlarmRelationDao(database: ApplicationDatabase) = database.getAlarmRelationDao()

    @Provides
    @Singleton
    fun provideLocationDao(database: ApplicationDatabase) = database.getLocationDao()

    @Provides
    @Singleton
    fun provideLocationRelationDao(database: ApplicationDatabase) = database.getLocationRelationDao()
}