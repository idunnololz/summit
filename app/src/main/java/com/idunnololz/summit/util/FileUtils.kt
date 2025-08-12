package com.idunnololz.summit.util

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

fun guessMimeType(file: File?): String? = file?.inputStream()?.use {
  BufferedInputStream(it).use {
    guessMimeType(it)
  }
}

fun guessMimeType(inputStream: InputStream): String? = try {
  guessContentTypeFromStream(inputStream)
} catch (_: Exception) {
  null
}

fun extensionForMimeType(mimeType: String) = mimeTypeToExtension[mimeType]

private fun guessContentTypeFromStream(inputStream: InputStream): String? {
  // If we can't read ahead safely, just give up on guessing
  if (!inputStream.markSupported()) return null
  inputStream.mark(16)
  val c1 = inputStream.read()
  val c2 = inputStream.read()
  val c3 = inputStream.read()
  val c4 = inputStream.read()
  val c5 = inputStream.read()
  val c6 = inputStream.read()
  val c7 = inputStream.read()
  val c8 = inputStream.read()
  val c9 = inputStream.read()
  val c10 = inputStream.read()
  val c11 = inputStream.read()
  val c12 = inputStream.read()
  val c13 = inputStream.read()
  val c14 = inputStream.read()
  val c15 = inputStream.read()
  val c16 = inputStream.read()
  inputStream.reset()
  if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
    return "application/java-vm"
  }
  if (c1 == 0xAC && c2 == 0xED) {
    // next two bytes are version number, currently 0x00 0x05
    return "application/x-java-serialized-object"
  }
  if (
    c1 == 'R'.code &&
    c2 == 'I'.code &&
    c3 == 'F'.code &&
    c4 == 'F'.code &&
    c9 == 'W'.code &&
    c10 == 'E'.code &&
    c11 == 'B'.code &&
    c12 == 'P'.code
  ) {
    return "image/webp"
  }
  if (c1 == '<'.code) {
    if ((
      c2 == '!'.code ||
        c2 == 'h'.code &&
        (
          c3 == 't'.code &&
            c4 == 'm'.code &&
            c5 == 'l'.code ||
            c3 == 'e'.code &&
            c4 == 'a'.code &&
            c5 == 'd'.code
          ) ||
        c2 == 'b'.code &&
        c3 == 'o'.code &&
        c4 == 'd'.code &&
        c5 == 'y'.code ||
        (
          (
            c2 == 'H'.code &&
              (
                c3 == 'T'.code &&
                  c4 == 'M'.code &&
                  c5 == 'L'.code ||
                  c3 == 'E'.code &&
                  c4 == 'A'.code &&
                  c5 == 'D'.code
                ) ||
              c2 == 'B'.code &&
              c3 == 'O'.code &&
              c4 == 'D'.code &&
              c5 == 'Y'.code
            )
          )
      )
    ) {
      return "text/html"
    }
    if ((c2 == '?'.code) &&
      (c3 == 'x'.code) &&
      (c4 == 'm'.code) &&
      (c5 == 'l'.code) &&
      (c6 == ' '.code)
    ) {
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
    if ((c3 == 0) &&
      (c4 == '<'.code) &&
      (c5 == 0) &&
      (c6 == '?'.code) &&
      (
        c7 == 0
        ) &&
      (c8 == 'x'.code)
    ) {
      return "application/xml"
    }
  }
  if (c1 == 0xff && c2 == 0xfe) {
    if ((c3 == '<'.code) &&
      (c4 == 0) &&
      (c5 == '?'.code) &&
      (c6 == 0) &&
      (
        c7 == 'x'.code
        ) &&
      (c8 == 0)
    ) {
      return "application/xml"
    }
  }

  // big and little endian UTF-32 encodings, with BOM
  if ((c1 == 0x00) && (c2 == 0x00) && (c3 == 0xfe) && (c4 == 0xff)) {
    if ((c5 == 0) &&
      (c6 == 0) &&
      (c7 == 0) &&
      (c8 == '<'.code) &&
      (
        c9 == 0
        ) &&
      (c10 == 0) &&
      (c11 == 0) &&
      (c12 == '?'.code) &&
      (
        c13 == 0
        ) &&
      (c14 == 0) &&
      (c15 == 0) &&
      (c16 == 'x'.code)
    ) {
      return "application/xml"
    }
  }
  if ((c1 == 0xff) && (c2 == 0xfe) && (c3 == 0x00) && (c4 == 0x00)) {
    if ((c5 == '<'.code) &&
      (c6 == 0) &&
      (c7 == 0) &&
      (c8 == 0) &&
      (
        c9 == '?'.code
        ) &&
      (c10 == 0) &&
      (c11 == 0) &&
      (c12 == 0) &&
      (
        c13 == 'x'.code
        ) &&
      (c14 == 0) &&
      (c15 == 0) &&
      (c16 == 0)
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
  if ((c1 == '!'.code) &&
    (c2 == ' '.code) &&
    (c3 == 'X'.code) &&
    (c4 == 'P'.code) &&
    (
      c5 == 'M'.code
      ) &&
    (c6 == '2'.code)
  ) {
    return "image/x-pixmap"
  }
  if ((c1 == 137) &&
    (c2 == 80) &&
    (c3 == 78) &&
    (
      c4 == 71
      ) &&
    (c5 == 13) &&
    (c6 == 10) &&
    (
      c7 == 26
      ) &&
    (c8 == 10)
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
        (c7 == 'E'.code) &&
          (c8 == 'x'.code) &&
          (c9 == 'i'.code) &&
          (c10 == 'f'.code) &&
          (
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

private val mimeTypeToExtension = buildMap {
  put("application/octet-stream", "bin")
  put("application/gzip", "gz")
  put("application/json", "json")
  put("application/pdf", "pdf")
  put("application/yaml", "yaml")

  put("image/avif", "avif")
  put("image/avif", "avifs")
  put("image/bmp", "bmp")
  put("image/cgm", "cgm")
  put("image/g3fax", "g3")
  put("image/gif", "gif")
  put("image/heic", "heif")
  put("image/heic", "heic")
  put("image/ief", "ief")
  put("image/jpeg", "jpeg")
  put("image/png", "png")
  put("image/prs.btif", "btif")
  put("image/svg+xml", "svg")
  put("image/tiff", "tif")
  put("image/tiff", "tiff")
  put("image/vnd.adobe.photoshop", "psd")
  put("image/vnd.djvu", "djv")
  put("image/vnd.djvu", "djvu")
  put("image/vnd.dwg", "dwg")
  put("image/vnd.dxf", "dxf")
  put("image/vnd.fastbidsheet", "fbs")
  put("image/vnd.fpx", "fpx")
  put("image/vnd.fst", "fst")
  put("image/vnd.fujixerox.edmics-mmr", "mmr")
  put("image/vnd.fujixerox.edmics-rlc", "rlc")
  put("image/vnd.ms-modi", "mdi")
  put("image/vnd.net-fpx", "npx")
  put("image/vnd.wap.wbmp", "wbmp")
  put("image/vnd.xiff", "xif")
  put("image/webp", "webp")
  put("image/x-adobe-dng", "dng")
  put("image/x-canon-cr2", "cr2")
  put("image/x-canon-crw", "crw")
  put("image/x-cmu-raster", "ras")
  put("image/x-cmx", "cmx")
  put("image/x-epson-erf", "erf")
  put("image/x-freehand", "fh")
  put("image/x-freehand", "fh4")
  put("image/x-freehand", "fh5")
  put("image/x-freehand", "fh7")
  put("image/x-freehand", "fhc")
  put("image/x-fuji-raf", "raf")
  put("image/x-icns", "icns")
  put("image/x-icon", "ico")
  put("image/x-kodak-dcr", "dcr")
  put("image/x-kodak-k25", "k25")
  put("image/x-kodak-kdc", "kdc")
  put("image/x-minolta-mrw", "mrw")
  put("image/x-nikon-nef", "nef")
  put("image/x-olympus-orf", "orf")
  put("image/x-panasonic-raw", "raw")
  put("image/x-panasonic-raw", "rw2")
  put("image/x-panasonic-raw", "rwl")
  put("image/x-pcx", "pcx")
  put("image/x-pentax-pef", "pef")
  put("image/x-pentax-pef", "ptx")
  put("image/x-pict", "pct")
  put("image/x-pict", "pic")
  put("image/x-portable-anymap", "pnm")
  put("image/x-portable-bitmap", "pbm")
  put("image/x-portable-graymap", "pgm")
  put("image/x-portable-pixmap", "ppm")
  put("image/x-rgb", "rgb")
  put("image/x-sigma-x3f", "x3f")
  put("image/x-sony-arw", "arw")
  put("image/x-sony-sr2", "sr2")
  put("image/x-sony-srf", "srf")
  put("image/x-xbitmap", "xbm")
  put("image/x-xpixmap", "xpm")
  put("image/x-xwindowdump", "xwd")

  put("text/css", "css")
  put("text/csv", "csv")
  put("text/html", "htm")
  put("text/html", "html")
  put("text/calendar", "ics")
  put("text/javascript", "js")
  put("text/javascript", "mjs")
  put("text/markdown", "md")
  put("text/plain", "txt")
  put("text/xml", "xml")

  put("video/3gpp", "3gp")
  put("video/3gpp2", "3g2")
  put("video/h261", "h261")
  put("video/h263", "h263")
  put("video/h264", "h264")
  put("video/jpeg", "jpgv")
  put("video/jpm", "jpgm")
  put("video/jpm", "jpm")
  put("video/mj2", "mj2")
  put("video/mj2", "mjp2")
  put("video/mp2t", "ts")
  put("video/mp4", "mp4")
  put("video/mp4", "mp4v")
  put("video/mp4", "mpg4")
  put("video/mpeg", "m1v")
  put("video/mpeg", "m2v")
  put("video/mpeg", "mpa")
  put("video/mpeg", "mpe")
  put("video/mpeg", "mpeg")
  put("video/mpeg", "mpg")
  put("video/ogg", "ogv")
  put("video/quicktime", "mov")
  put("video/quicktime", "qt")
  put("video/vnd.fvt", "fvt")
  put("video/vnd.mpegurl", "m4u")
  put("video/vnd.mpegurl", "mxu")
  put("video/vnd.ms-playready.media.pyv", "pyv")
  put("video/vnd.vivo", "viv")
  put("video/webm", "webm")
  put("video/x-f4v", "f4v")
  put("video/x-fli", "fli")
  put("video/x-flv", "flv")
  put("video/x-m4v", "m4v")
  put("video/x-matroska", "mkv")
  put("video/x-ms-asf", "asf")
  put("video/x-ms-asf", "asx")
  put("video/x-ms-wm", "wm")
  put("video/x-ms-wmv", "wmv")
  put("video/x-ms-wmx", "wmx")
  put("video/x-ms-wvx", "wvx")
  put("video/x-msvideo", "avi")
  put("video/x-sgi-movie", "movie")
}
