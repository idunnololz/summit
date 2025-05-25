package com.idunnololz.summit.util

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

fun guessMimeType(file: File?): String? =
  file?.inputStream()?.use {
    guessMimeType(it)
  }

fun guessMimeType(inputStream: InputStream): String? {
  val bufferedStream = inputStream as? BufferedInputStream
    ?: BufferedInputStream(inputStream)

  return try {
    guessContentTypeFromStream(bufferedStream)
  } catch (ignored: Exception) {
    null
  } finally {
    try {
      bufferedStream.close()
    } catch (ignore: Exception) { }
    try {
      inputStream.close()
    } catch (ignore: Exception) { }
  }
}

@Throws(IOException::class)
private fun guessContentTypeFromStream(`is`: InputStream): String? {
  // If we can't read ahead safely, just give up on guessing
  if (!`is`.markSupported()) return null
  `is`.mark(16)
  val c1 = `is`.read()
  val c2 = `is`.read()
  val c3 = `is`.read()
  val c4 = `is`.read()
  val c5 = `is`.read()
  val c6 = `is`.read()
  val c7 = `is`.read()
  val c8 = `is`.read()
  val c9 = `is`.read()
  val c10 = `is`.read()
  val c11 = `is`.read()
  val c12 = `is`.read()
  val c13 = `is`.read()
  val c14 = `is`.read()
  val c15 = `is`.read()
  val c16 = `is`.read()
  `is`.reset()
  if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
    return "application/java-vm"
  }
  if (c1 == 0xAC && c2 == 0xED) {
    // next two bytes are version number, currently 0x00 0x05
    return "application/x-java-serialized-object"
  }
  if (c1 == '<'.code) {
    if ((
        c2 == '!'.code || c2 == 'h'.code && (
          c3 == 't'.code && c4 == 'm'.code && c5 == 'l'.code ||
            c3 == 'e'.code && c4 == 'a'.code && c5 == 'd'.code
          ) || c2 == 'b'.code && c3 == 'o'.code && c4 == 'd'.code && c5 == 'y'.code ||
          (
            (
              c2 == 'H'.code && (
                c3 == 'T'.code && c4 == 'M'.code && c5 == 'L'.code ||
                  c3 == 'E'.code && c4 == 'A'.code && c5 == 'D'.code
                ) || c2 == 'B'.code && c3 == 'O'.code && c4 == 'D'.code && c5 == 'Y'.code
              )
            )
        )
    ) {
      return "text/html"
    }
    if ((c2 == '?'.code) && (c3 == 'x'.code) && (c4 == 'm'.code) && (c5 == 'l'.code) && (c6 == ' '.code)) {
      return "application/xml"
    }
  }

  // big and little (identical) endian UTF-8 encodings, with BOM
  if ((c1 == 0xef) && (c2 == 0xbb) && (c3 == 0xbf)) {
    if ((c4 == '<'.code) && (c5 == '?'.code) && (c6 == 'x'.code)) {
      return "application/xml"
    }
  }

  // big and little endian UTF-16 encodings, with byte order mark
  if (c1 == 0xfe && c2 == 0xff) {
    if ((c3 == 0) && (c4 == '<'.code) && (c5 == 0) && (c6 == '?'.code) && (
        c7 == 0
        ) && (c8 == 'x'.code)
    ) {
      return "application/xml"
    }
  }
  if (c1 == 0xff && c2 == 0xfe) {
    if ((c3 == '<'.code) && (c4 == 0) && (c5 == '?'.code) && (c6 == 0) && (
        c7 == 'x'.code
        ) && (c8 == 0)
    ) {
      return "application/xml"
    }
  }

  // big and little endian UTF-32 encodings, with BOM
  if ((c1 == 0x00) && (c2 == 0x00) && (c3 == 0xfe) && (c4 == 0xff)) {
    if ((c5 == 0) && (c6 == 0) && (c7 == 0) && (c8 == '<'.code) && (
        c9 == 0
        ) && (c10 == 0) && (c11 == 0) && (c12 == '?'.code) && (
        c13 == 0
        ) && (c14 == 0) && (c15 == 0) && (c16 == 'x'.code)
    ) {
      return "application/xml"
    }
  }
  if ((c1 == 0xff) && (c2 == 0xfe) && (c3 == 0x00) && (c4 == 0x00)) {
    if ((c5 == '<'.code) && (c6 == 0) && (c7 == 0) && (c8 == 0) && (
        c9 == '?'.code
        ) && (c10 == 0) && (c11 == 0) && (c12 == 0) && (
        c13 == 'x'.code
        ) && (c14 == 0) && (c15 == 0) && (c16 == 0)
    ) {
      return "application/xml"
    }
  }
  if ((c1 == 'G'.code) && (c2 == 'I'.code) && (c3 == 'F'.code) && (c4 == '8'.code)) {
    return "image/gif"
  }
  if ((c1 == '#'.code) && (c2 == 'd'.code) && (c3 == 'e'.code) && (c4 == 'f'.code)) {
    return "image/x-bitmap"
  }
  if ((c1 == '!'.code) && (c2 == ' '.code) && (c3 == 'X'.code) && (c4 == 'P'.code) && (
      c5 == 'M'.code
      ) && (c6 == '2'.code)
  ) {
    return "image/x-pixmap"
  }
  if ((c1 == 137) && (c2 == 80) && (c3 == 78) && (
      c4 == 71
      ) && (c5 == 13) && (c6 == 10) && (
      c7 == 26
      ) && (c8 == 10)
  ) {
    return "image/png"
  }
  if ((c1 == 0xFF) && (c2 == 0xD8) && (c3 == 0xFF)) {
    if (c4 == 0xE0 || c4 == 0xEE) {
      return "image/jpeg"
    }
    /**
     * File format used by digital cameras to store images.
     * Exif Format can be read by any application supporting
     * JPEG. Exif Spec can be found at:
     * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
     */
    if ((c4 == 0xE1) &&
      (
        (c7 == 'E'.code) && (c8 == 'x'.code) && (c9 == 'i'.code) && (c10 == 'f'.code) && (
          c11 == 0
          )
        )
    ) {
      return "image/jpeg"
    }
    return "image/jpeg"
  }
  if ((
      ((c1 == 0x49) && (c2 == 0x49) && (c3 == 0x2a) && (c4 == 0x00)) ||
        ((c1 == 0x4d) && (c2 == 0x4d) && (c3 == 0x00) && (c4 == 0x2a))
      )
  ) {
    return "image/tiff"
  }
  if ((c1 == 0x2E) && (c2 == 0x73) && (c3 == 0x6E) && (c4 == 0x64)) {
    return "audio/basic" // .au format, big endian
  }
  if ((c1 == 0x64) && (c2 == 0x6E) && (c3 == 0x73) && (c4 == 0x2E)) {
    return "audio/basic" // .au format, little endian
  }
  return if ((c1 == 'R'.code) && (c2 == 'I'.code) && (c3 == 'F'.code) && (c4 == 'F'.code)) {
    /* I don't know if this is official but evidence
     * suggests that .wav files start with "RIFF" - brown
     */
    "audio/x-wav"
  } else {
    null
  }
}