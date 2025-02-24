package com.idunnololz.summit.util.dagger

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object KotlinSerializationModule {
    @Provides
    @Singleton
    fun provideKotlinSerialization(): Json = json
}

val json: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
}
