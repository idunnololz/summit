package com.idunnololz.summit.coroutine

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


interface CoroutineScopeFactory {

  fun create(): CoroutineScope

  fun createConfined(): CoroutineScope
}


@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

  @Binds
  abstract fun bindCoroutineScopeFactory(
    realCoroutineScopeFactory: RealCoroutineScopeFactory
  ): CoroutineScopeFactory
}

@Singleton
class RealCoroutineScopeFactory @Inject constructor() : CoroutineScopeFactory {

  override fun create() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun createConfined() = CoroutineScope(
    SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
  )
}
