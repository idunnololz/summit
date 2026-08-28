package com.idunnololz.summit.models.processed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.idunnololz.summit.AaaList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AaaListTest {

  @Test
  fun aaaa() = runTest {
    assertThat(AaaList.isInList("Alessio")).isTrue()
    assertThat(AaaList.isInList("Aaaa")).isFalse()
  }
}
