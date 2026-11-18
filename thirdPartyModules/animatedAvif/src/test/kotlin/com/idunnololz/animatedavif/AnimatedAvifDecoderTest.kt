package com.idunnololz.animatedavif

import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedAvifDecoderTest {
  @Test
  fun recognizesMajorAndCompatibleSequenceBrandsWithoutConsumingInput() {
    val major = fileTypeBox("avis", "avif", "msf1")
    val compatible = fileTypeBox("msf1", "miaf", "avis")
    val expected = compatible.clone().readByteString()

    assertTrue(major.isAvifSequence())
    assertTrue(compatible.isAvifSequence())
    assertEquals(expected, compatible.readByteString())
  }

  @Test
  fun recognizesSequenceBrandInExtendedSizeBox() {
    val header = Buffer()
      .writeInt(1)
      .writeUtf8("ftyp")
      .writeLong(28)
      .writeUtf8("msf1")
      .writeInt(0)
      .writeUtf8("avis")

    assertTrue(header.isAvifSequence())
  }

  @Test
  fun ignoresStillAvifOtherFormatsAndBrandsOutsideTheBox() {
    val truncated = Buffer().write(fileTypeBox("avif", "avis").readByteString(18))
    assertFalse(fileTypeBox("avif", "mif1").isAvifSequence())
    assertFalse(fileTypeBox("hevc", "msf1").isAvifSequence())
    assertFalse(fileTypeBox("avif").writeUtf8("avis").isAvifSequence())
    assertFalse(truncated.isAvifSequence())
  }

  private fun fileTypeBox(majorBrand: String, vararg compatibleBrands: String): Buffer = Buffer()
    .writeInt(16 + 4 * compatibleBrands.size)
    .writeUtf8("ftyp")
    .writeUtf8(majorBrand)
    .writeInt(0)
    .apply { compatibleBrands.forEach { writeUtf8(it) } }
}
