package com.idunnololz.summit.util.dagger

import com.idunnololz.summit.util.moshi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MoshiModule {
    @Provides
    fun provideMoshi(): Moshi = moshi
}
