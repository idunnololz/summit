package com.idunnololz.summit.links

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.collections.mutableListOf

// Data classes for configuration
data class Extractor(
  val format: String = "yyyy-MM-dd",
  val min: LocalDate? = null,
  val max: LocalDate? = null,
  val original: Boolean = false
)

// Constants
object DateExtractionConstants {
  const val CACHE_SIZE = 1000
  const val MIN_SEGMENT_LEN = 6
  const val MAX_SEGMENT_LEN = 52

  const val DAY_RE = "[0-3]?[0-9]"
  const val MONTH_RE = "[0-1]?[0-9]"
  const val YEAR_RE = "199[0-9]|20[0-3][0-9]"

  const val REGEX_MONTHS = """
        January?|February?|March|A[pv]ril|Ma[iy]|Jun[ei]|Jul[iy]|August|September|O[ck]tober|November|De[csz]ember|
        Jan|Feb|M[aä]r|Apr|Jun|Jul|Aug|Sep|O[ck]t|Nov|De[cz]|
        Januari|Februari|Maret|Mei|Agustus|
        Jänner|Feber|März|
        janvier|février|mars|juin|juillet|aout|septembre|octobre|novembre|décembre|
        Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık|
        Oca|Şub|Mar|Nis|Haz|Tem|Ağu|Eyl|Eki|Kas|Ara
    """

  val MONTHS = listOf(
    listOf("jan", "januar", "jänner", "january", "januari", "janvier", "ocak", "oca"),
    listOf("feb", "februar", "feber", "february", "februari", "février", "şubat", "şub"),
    listOf("mar", "mär", "märz", "march", "maret", "mart", "mars"),
    listOf("apr", "april", "avril", "nisan", "nis"),
    listOf("may", "mai", "mei", "mayıs"),
    listOf("jun", "juni", "june", "juin", "haziran", "haz"),
    listOf("jul", "juli", "july", "juillet", "temmuz", "tem"),
    listOf("aug", "august", "agustus", "ağustos", "ağu", "aout"),
    listOf("sep", "september", "septembre", "eylül", "eyl"),
    listOf("oct", "oktober", "october", "octobre", "okt", "ekim", "eki"),
    listOf("nov", "november", "kasım", "kas", "novembre"),
    listOf("dec", "dez", "dezember", "december", "desember", "décembre", "aralık", "ara")
  )

  val TEXT_MONTHS = MONTHS.flatMapIndexed { index, monthList ->
    monthList.map { it to index + 1 }
  }.toMap()
}

@RequiresApi(Build.VERSION_CODES.O)
class DateExtractor {
  companion object {
    private val logger = Logger.getLogger(DateExtractor::class.java.name)

    // Regex patterns
    private val YMD_NO_SEP_PATTERN = Pattern.compile("\\b(\\d{8})\\b")
    private val YMD_PATTERN = Pattern.compile(
      "(?:\\D|^)(?:(?<year>${DateExtractionConstants.YEAR_RE})[\\-/.](?<month>${DateExtractionConstants.MONTH_RE})[\\-/.](?<day>${DateExtractionConstants.DAY_RE})|" +
        "(?<day2>${DateExtractionConstants.DAY_RE})[\\-/.](?<month2>${DateExtractionConstants.MONTH_RE})[\\-/.](?<year2>\\d{2,4}))(?:\\D|$)"
    )
    private val YM_PATTERN = Pattern.compile(
      "(?:\\D|^)(?:(?<year>${DateExtractionConstants.YEAR_RE})[\\-/.](?<month>${DateExtractionConstants.MONTH_RE})|" +
        "(?<month2>${DateExtractionConstants.MONTH_RE})[\\-/.](?<year2>${DateExtractionConstants.YEAR_RE}))(?:\\D|$)"
    )

    private val LONG_TEXT_PATTERN = Pattern.compile(
      "(?<month>${DateExtractionConstants.REGEX_MONTHS})\\s" +
        "(?<day>${DateExtractionConstants.DAY_RE})(?:st|nd|rd|th)?,? (?<year>${DateExtractionConstants.YEAR_RE})|" +
        "(?<day2>${DateExtractionConstants.DAY_RE})(?:st|nd|rd|th|\\.)? (?:of )?" +
        "(?<month2>${DateExtractionConstants.REGEX_MONTHS})[,.]? (?<year2>${DateExtractionConstants.YEAR_RE})",
      Pattern.CASE_INSENSITIVE
    )

    private val COMPLETE_URL = Pattern.compile("\\D(${DateExtractionConstants.YEAR_RE})[/_-](${DateExtractionConstants.MONTH_RE})[/_-](${DateExtractionConstants.DAY_RE})(?:\\D|$)")

    private val JSON_MODIFIED = Pattern.compile("\"dateModified\": ?\"(${DateExtractionConstants.YEAR_RE}-${DateExtractionConstants.MONTH_RE}-${DateExtractionConstants.DAY_RE})", Pattern.CASE_INSENSITIVE)
    private val JSON_PUBLISHED = Pattern.compile("\"datePublished\": ?\"(${DateExtractionConstants.YEAR_RE}-${DateExtractionConstants.MONTH_RE}-${DateExtractionConstants.DAY_RE})", Pattern.CASE_INSENSITIVE)

    private val TIMESTAMP_PATTERN = Pattern.compile("(${DateExtractionConstants.YEAR_RE}-${DateExtractionConstants.MONTH_RE}-${DateExtractionConstants.DAY_RE}).[0-9]{2}:[0-9]{2}:[0-9]{2}")

    private val DISCARD_PATTERNS = Pattern.compile(
      "^\\d{2}:\\d{2}(?: |:|$)|" +
        "^\\D*\\d{4}\\D*$|" +
        "[$€¥Ұ£¢₽₱฿#₹]|" +
        "[A-Z]{3}[^A-Z]|" +
        "(?:^|\\D)(?:\\+\\d{2}|\\d{3}|\\d{5})\\D|" +
        "ftps?|https?|sftp|" +
        "\\.(?:com|net|org|info|gov|edu|de|fr|io)\\b|" +
        "IBAN|[A-Z]{2}[0-9]{2}|" +
        "®"
    )

    private val TEXT_PATTERNS = Pattern.compile(
      "(?:date[^0-9\"]{0,20}|updated|last-modified|published|posted|on)(?:[ :])*?([0-9]{1,4})[./]([0-9]{1,2})[./]([0-9]{2,4})|" +
        "(?:Datum|Stand|Veröffentlicht am):? ?([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{2,4})|" +
        "(?:güncellen?me|yayı(?:m|n)lan?ma) *?(?:tarihi)? *?: *?([0-9]{1,2})[./]([0-9]{1,2})[./]([0-9]{2,4})|" +
        "([0-9]{1,2})[./]([0-9]{1,2})[./]([0-9]{2,4}) *?(?:'de|'da|'te|'ta|’de|’da|’te|’ta|tarihinde) *(?:güncellendi|yayı(?:m|n)landı)",
      Pattern.CASE_INSENSITIVE
    )

    private val TEXT_DATE_PATTERN = Pattern.compile("[.:,_/ -]|^\\d+$")

    // Cache for parsed dates
    private val dateCache = mutableMapOf<String, String?>()
  }

  /**
   * Extract the date out of a URL string complying with the Y-M-D format
   */
  fun extractUrlDate(testUrl: String?, options: Extractor): String? {
    if (testUrl == null) return null

    val matcher = COMPLETE_URL.matcher(testUrl)
    if (matcher.find()) {
      logger.info("Found date in URL: ${matcher.group(0)}")
      try {
        val year = matcher.group(1).toInt()
        val month = matcher.group(2).toInt()
        val day = matcher.group(3).toInt()
        val date = LocalDate.of(year, month, day)

        if (isValidDate(date, options.format, options.min, options.max)) {
          return date.format(DateTimeFormatter.ofPattern(options.format))
        }
      } catch (e: Exception) {
        logger.warning("Conversion error: ${matcher.group(0)} ${e.message}")
      }
    }
    return null
  }

  /**
   * Adapt year from YY to YYYY format
   */
  private fun correctYear(year: Int): Int {
    return when {
      year < 100 -> if (year >= 90) year + 1900 else year + 2000
      else -> year
    }
  }

  /**
   * Swap day and month values if it seems feasible
   */
  private fun trySwapValues(day: Int, month: Int): Pair<Int, Int> {
    return if (month > 12 && day <= 12) Pair(month, day) else Pair(day, month)
  }

  /**
   * Try full-text parse for date elements using regex with emphasis on multilingual support
   */
  private fun regexParse(string: String): LocalDateTime? {
    val matcher = LONG_TEXT_PATTERN.matcher(string)
    if (!matcher.find()) return null

    return try {
      val (dayGroup, monthGroup, yearGroup) = when {
        matcher.group("year") != null -> Triple("day", "month", "year")
        else -> Triple("day2", "month2", "year2")
      }

      val day = matcher.group(dayGroup).toInt()
      val monthStr = matcher.group(monthGroup).lowercase().trimEnd('.')
      val month = DateExtractionConstants.TEXT_MONTHS[monthStr] ?: return null
      val year = correctYear(matcher.group(yearGroup).toInt())

      val (finalDay, finalMonth) = trySwapValues(day, month)
      val date = LocalDate.of(year, finalMonth, finalDay)
      logger.info("Multilingual text found: $date")
      date.atStartOfDay()
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Try to bypass slow date parsing with custom logic
   */
  private fun customParse(
    string: String,
    outputFormat: String,
    minDate: LocalDate?,
    maxDate: LocalDate?
  ): String? {
    logger.info("Custom parse test: $string")

    // 1. Shortcut for strings starting with digits
    if (string.length >= 4 && string.substring(0, 4).all { it.isDigit() }) {
      var candidate: LocalDate? = null

      // a. Handle YYYYMMDD format
      if (string.length >= 8 && string.substring(4, 8).all { it.isDigit() }) {
        try {
          val year = string.substring(0, 4).toInt()
          val month = string.substring(4, 6).toInt()
          val day = string.substring(6, 8).toInt()
          candidate = LocalDate.of(year, month, day)
        } catch (e: Exception) {
          logger.warning("8-digit error: ${string.substring(0, 8)}")
        }
      } else {
        // b. Try ISO format parsing
        try {
          candidate = LocalDate.parse(string.substring(0, 10))
        } catch (e: DateTimeParseException) {
          logger.info("Not an ISO date string: $string")
        }
      }

      // c. Plausibility test
      if (candidate != null && isValidDate(candidate, outputFormat, minDate, maxDate)) {
        logger.info("Parsing result: $candidate")
        return candidate.format(DateTimeFormatter.ofPattern(outputFormat))
      }
    }

    // 2. Try YYYYMMDD with regex
    val ymdMatcher = YMD_NO_SEP_PATTERN.matcher(string)
    if (ymdMatcher.find()) {
      try {
        val dateStr = ymdMatcher.group(1)
        val year = dateStr.substring(0, 4).toInt()
        val month = dateStr.substring(4, 6).toInt()
        val day = dateStr.substring(6, 8).toInt()
        val candidate = LocalDate.of(year, month, day)

        if (isValidDate(candidate, "yyyy-MM-dd", minDate, maxDate)) {
          logger.info("YYYYMMDD match: $candidate")
          return candidate.format(DateTimeFormatter.ofPattern(outputFormat))
        }
      } catch (e: Exception) {
        logger.warning("YYYYMMDD value error: ${ymdMatcher.group(0)}")
      }
    }

    // 3. Try YMD, Y-M-D, and D-M-Y patterns
    val ymdPatternMatcher = YMD_PATTERN.matcher(string)
    if (ymdPatternMatcher.find()) {
      try {
        val (year, month, day) = when {
          ymdPatternMatcher.group("day") != null -> Triple(
            ymdPatternMatcher.group("year").toInt(),
            ymdPatternMatcher.group("month").toInt(),
            ymdPatternMatcher.group("day").toInt()
          )
          else -> {
            val rawDay = ymdPatternMatcher.group("day2").toInt()
            val rawMonth = ymdPatternMatcher.group("month2").toInt()
            val rawYear = correctYear(ymdPatternMatcher.group("year2").toInt())
            val (finalDay, finalMonth) = trySwapValues(rawDay, rawMonth)
            Triple(rawYear, finalMonth, finalDay)
          }
        }

        val candidate = LocalDate.of(year, month, day)
        if (isValidDate(candidate, "yyyy-MM-dd", minDate, maxDate)) {
          logger.info("Regex match: $candidate")
          return candidate.format(DateTimeFormatter.ofPattern(outputFormat))
        }
      } catch (e: Exception) {
        logger.warning("Regex value error: ${ymdPatternMatcher.group(0)}")
      }
    }

    // 4. Try Y-M and M-Y patterns
    val ymMatcher = YM_PATTERN.matcher(string)
    if (ymMatcher.find()) {
      try {
        val candidate = when {
          ymMatcher.group("month") != null -> LocalDate.of(
            ymMatcher.group("year").toInt(),
            ymMatcher.group("month").toInt(),
            1
          )
          else -> LocalDate.of(
            ymMatcher.group("year2").toInt(),
            ymMatcher.group("month2").toInt(),
            1
          )
        }

        if (isValidDate(candidate, "yyyy-MM-dd", minDate, maxDate)) {
          logger.info("Y-M match: $candidate")
          return candidate.format(DateTimeFormatter.ofPattern(outputFormat))
        }
      } catch (e: Exception) {
        logger.warning("Y-M value error: ${ymMatcher.group(0)}")
      }
    }

    // 5. Try regex parsing
    val dateObject = regexParse(string)
    return validateAndConvert(dateObject?.toLocalDate(), outputFormat, minDate, maxDate)
  }

  /**
   * Use a series of heuristics and rules to parse a potential date expression
   */
  fun tryDateExpr(
    string: String?,
    outputFormat: String,
    extensiveSearch: Boolean,
    minDate: LocalDate?,
    maxDate: LocalDate?
  ): String? {
    if (string.isNullOrBlank()) return null

    // Check cache first
    val cacheKey = "$string|$outputFormat|$extensiveSearch"
    dateCache[cacheKey]?.let { return it }

    // Trim string
    val trimmedString = string.trim().take(DateExtractionConstants.MAX_SEGMENT_LEN)

    // Formal constraint: 4 to 18 digits
    val digitCount = trimmedString.count { it.isDigit() }
    if (trimmedString.isEmpty() || digitCount !in 4..18) {
      return null
    }

    // Check if string contains patterns to discard
    if (DISCARD_PATTERNS.matcher(trimmedString).find()) {
      return null
    }

    // Try custom parsing first (faster)
    val customResult = customParse(trimmedString, outputFormat, minDate, maxDate)
    if (customResult != null) {
      dateCache[cacheKey] = customResult
      return customResult
    }

    // Use extensive search if enabled
    if (extensiveSearch && TEXT_DATE_PATTERN.matcher(trimmedString).find()) {
      // Here you would call external date parser if available
      // For now, return null as we don't have external parser in Kotlin
      logger.info("Extensive search would be performed here")
    }

    dateCache[cacheKey] = null
    return null
  }

  /**
   * Look for date expressions using regex on text
   */
  fun patternSearch(text: String, pattern: Pattern, options: Extractor): String? {
    val matcher = pattern.matcher(text)
    if (matcher.find() && isValidDate(matcher.group(1), "yyyy-MM-dd", options.min, options.max)) {
      logger.info("Regex found: $pattern ${matcher.group(0)}")
      return convertDate(matcher.group(1), "yyyy-MM-dd", options.format)
    }
    return null
  }

  /**
   * Look for author-written dates throughout the web page
   */
  fun idiosyncrasiesSearch(htmlString: String, options: Extractor): String? {
    val matcher = TEXT_PATTERNS.matcher(htmlString)
    if (matcher.find()) {
      val parts = (0 until matcher.groupCount())
        .map { matcher.group(it) }
        .drop(1) // Skip the full match
        .filter { it != null && it.isNotEmpty() }

      if (parts.size >= 3) {
        try {
          val candidate = when {
            parts[0].length == 4 -> { // Year in first position
              LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            }
            else -> { // DD/MM/YY format
              val (day, month) = trySwapValues(parts[0].toInt(), parts[1].toInt())
              val year = correctYear(parts[2].toInt())
              LocalDate.of(year, month, day)
            }
          }

          if (isValidDate(candidate, "yyyy-MM-dd", options.min, options.max)) {
            return candidate.format(DateTimeFormatter.ofPattern(options.format))
          }
        } catch (e: Exception) {
          logger.warning("Cannot process idiosyncrasies: ${matcher.group(0)}")
        }
      }
    }
    return null
  }

  // Utility functions
  private fun isValidDate(
    date: LocalDate?,
    format: String,
    earliest: LocalDate?,
    latest: LocalDate?
  ): Boolean {
    if (date == null) return false
    if (earliest != null && date.isBefore(earliest)) return false
    if (latest != null && date.isAfter(latest)) return false
    return true
  }

  private fun isValidDate(
    dateString: String,
    format: String,
    earliest: LocalDate?,
    latest: LocalDate?
  ): Boolean {
    return try {
      val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(format))
      isValidDate(date, format, earliest, latest)
    } catch (e: Exception) {
      false
    }
  }

  private fun validateAndConvert(
    date: LocalDate?,
    outputFormat: String,
    earliest: LocalDate?,
    latest: LocalDate?
  ): String? {
    return if (isValidDate(date, outputFormat, earliest, latest)) {
      date?.format(DateTimeFormatter.ofPattern(outputFormat))
    } else null
  }

  private fun convertDate(dateString: String, inputFormat: String, outputFormat: String): String? {
    return try {
      val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(inputFormat))
      date.format(DateTimeFormatter.ofPattern(outputFormat))
    } catch (e: Exception) {
      null
    }
  }
}

// Extension function to trim text (equivalent to Python's trim_text utility)
fun String.trimText(): String = this.trim().replace(Regex("\\s+"), " ")