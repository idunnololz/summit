package com.idunnololz.summit.models.processed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagParserTest {

  @Test
  fun `ad`() {
    val title = ""
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `GIVEN title with no tags WHEN parseTags THEN no tags returned`() {
    val title = "asdf"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `a`() {
    val title = "[asdf"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `aa`() {
    val title = "dfs]df"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `aaa`() {
    val title = "dfsdf]"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `aaa2`() {
    val title = "d]fsd]f]asd[ad"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `aaaa`() {
    val title = "[aa] dfs [df]"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo("dfs")
    assertThat(result.tags).containsExactly(PostTag.CustomTag("aa"), PostTag.CustomTag("df"))
  }

  @Test
  fun `aaaaa`() {
    val title = "[abc]"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo("")
    assertThat(result.tags).containsExactly(PostTag.CustomTag("abc"))
  }

  @Test
  fun `aaaaa 32432 42`() {
    val title = "[abc]["
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo("[")
    assertThat(result.tags).containsExactly(PostTag.CustomTag("abc"))
  }

  @Test
  fun `aaaaa 32432 422`() {
    val title = "][abc]"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo("]")
    assertThat(result.tags).containsExactly(PostTag.CustomTag("abc"))
  }

  @Test
  fun `aaaaa 32432 4232`() {
    val title = "[]test[]test[]"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }

  @Test
  fun `aaaaa 32432 42321`() {
    val title = "[][asdf]test[]test[asdf][]"
    val result = TagParser.parseTags(title)

    assertThat(result.title).isEqualTo(title)
    assertThat(result.tags).isEmpty()
  }
}
