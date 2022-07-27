package ru.rrtry.silentdroid.di

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.rrtry.silentdroid.db.ApplicationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.rrtry.silentdroid.db.ApplicationDatabase.Companion.DATABASE_ASSET_PATH
import ru.rrtry.silentdroid.db.ApplicationDatabase.Companion.DATABASE_NAME
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(applicationContext: Application): ApplicationDatabase {
        return Room.databaseBuilder(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) applicationContext.createDeviceProtectedStorageContext() else applicationContext,
            ApplicationDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSuggestionsDao(database: ApplicationDatabase) = database.getSuggestionsDao()

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