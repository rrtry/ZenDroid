package com.example.volumeprofiler.di

import android.content.Context
import com.example.volumeprofiler.adapters.viewPager.MainActivityPagerAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object MainActivityModule {

    @Provides
    @ActivityScoped
    fun providePagerAdapter(@ActivityContext context: Context): MainActivityPagerAdapter {
        return MainActivityPagerAdapter(context)
    }
}