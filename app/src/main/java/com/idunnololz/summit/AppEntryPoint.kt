package com.idunnololz.summit

import com.idunnololz.summit.preferences.ThemeManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun themeManager(): ThemeManager
}