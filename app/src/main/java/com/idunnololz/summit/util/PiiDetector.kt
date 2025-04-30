package com.idunnololz.summit.util

import java.util.regex.Pattern

class PiiDetector {
  private class ColumnRegexes {
    val personRegex: Pattern = Pattern.compile(
      "^.*(firstname|fname|lastname|lname|fullname|maidenname|_name|" +
        "nickname|name_suffix|name|person).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val emailRegex: Pattern = Pattern.compile(
      "^.*(date_of_birth|dateofbirth|dob|" +
        "birthday|date_of_death|dateofdeath|birthdate).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val dobRegex: Pattern = Pattern.compile(
      "^.*(date_of_birth|dateofbirth|dob|" +
        "birthday|date_of_death|dateofdeath|birthdate).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val genderRegex: Pattern = Pattern.compile(
      "^.*(gender).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val nationalityRegex: Pattern = Pattern.compile(
      "^.*(nationality).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val addressRegex: Pattern = Pattern.compile(
      "^.*(address|city|state|county|country|zone|borough).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val zipCodeRegex: Pattern = Pattern.compile(
      "^.*(zipcode|zip_code|postal|postal_code|zip).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val userNameRegex: Pattern = Pattern.compile(
      "^.*user(id|name|).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val passwordRegex: Pattern = Pattern.compile(
      "^.*pass.*$",
      Pattern.CASE_INSENSITIVE,
    )
    val ssnRegex: Pattern = Pattern.compile(
      "^.*(ssn|social_number|social_security|" +
        "social_security_number|social_security_no).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val poBoxRegex: Pattern = Pattern.compile(
      "^.*(po_box|pobox).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val creditCardRegex: Pattern = Pattern.compile(
      "^.*(credit_card|cc_number|cc_num|creditcard|" +
        "credit_card_num|creditcardnumber).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val phoneRegex: Pattern = Pattern.compile(
      "^.*(phone|phone_number|phone_no|phone_num|" +
        "telephone|telephone_num|telephone_no).*$",
      Pattern.CASE_INSENSITIVE,
    )
    val authTokenRegex: Pattern = Pattern.compile(
      "^.*(token|jwt).*$",
      Pattern.CASE_INSENSITIVE,
    )
  }
  private class ValueRegexes {
    val dateRegex: Pattern = Pattern.compile(
      """(?:(?<!\:)(?<!\:\d)[0-3]?\d(?:st|nd|rd|th)?\s+(?:of\s+)?(?:jan\.?|january|feb\.?|february|mar\.?|march|apr\.?|april|may|jun\.?|june|jul\.?|july|aug\.?|august|sep\.?|september|oct\.?|october|nov\.?|november|dec\.?|december)|(?:jan\.?|january|feb\.?|february|mar\.?|march|apr\.?|april|may|jun\.?|june|jul\.?|july|aug\.?|august|sep\.?|september|oct\.?|october|nov\.?|november|dec\.?|december)\s+(?<!\:)(?<!\:\d)[0-3]?\d(?:st|nd|rd|th)?)(?:\,)?\s*(?:\d{4})?|[0-3]?\d[-\./][0-3]?\d[-\./]\d{2,4}""".trimIndent(),
    )
    val timeRegex: Pattern = Pattern.compile(
      """\d{1,2}:\d{2} ?(?:[ap]\.?m\.?)?|\d[ap]\.?m\.?""",
    )
    val phoneRegex: Pattern = Pattern.compile(
      """((?:(?<![\d-])(?:\+?\d{1,3}[-.\s*]?)?(?:\(?\d{3}\)?[-.\s*]?)?\d{3}[-.\s*]?\d{4}(?![\d-]))|(?:(?<![\d-])(?:(?:\(\+?\d{2}\))|(?:\+?\d{2}))\s*\d{2}\s*\d{3}\s*\d{4}(?![\d-])))""",
    )
    val phonesWithExtsRegex: Pattern = Pattern.compile(
      """((?:(?:\+?1\s*(?:[.-]\s*)?)?(?:\(\s*(?:[2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\s*\)|(?:[2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\s*(?:[.-]\s*)?)?(?:[2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\s*(?:[.-]\s*)?(?:[0-9]{4})(?:\s*(?:#|x\.?|ext\.?|extension)\s*(?:\d+)?))""",
    )
    val emailRegex: Pattern = Pattern.compile(
      """([a-z0-9!#$%&'*+\/=?^_`{|.}~-]+(@|at)(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(\.|dot))+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)""",
    )
    val ipRegex: Pattern = Pattern.compile(
      """(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)""",
    )
    val ipv6Regex: Pattern = Pattern.compile(
      """\s*(?!.*::.*::)(?:(?!:)|:(?=:))(?:[0-9a-f]{0,4}(?:(?<=::)|(?<!::):)){6}(?:[0-9a-f]{0,4}(?:(?<=::)|(?<!::):)[0-9a-f]{0,4}(?:(?<=::)|(?<!:)|(?<=:)(?<!::):)|(?:25[0-4]|2[0-4]\d|1\d\d|[1-9]?\d)(?:\.(?:25[0-4]|2[0-4]\d|1\d\d|[1-9]?\d)){3})\s*""",
    )
    val creditCardRegex: Pattern = Pattern.compile(
      """((?:(?:\\d{4}[- ]?){3}\\d{4}|\\d{15,16}))(?![\\d])""",
    )
    val btcAddressRegex: Pattern = Pattern.compile(
      """(?<![a-km-zA-HJ-NP-Z0-9])[13][a-km-zA-HJ-NP-Z0-9]{26,33}(?![a-km-zA-HJ-NP-Z0-9])""",
    )
    val streetAddressRegex: Pattern = Pattern.compile(
      """\d{1,4} [\w\s]{1,20}(?:street|st|avenue|ave|road|rd|highway|hwy|square|sq|trail|trl|drive|dr|court|ct|park|parkway|pkwy|circle|cir|boulevard|blvd)\W?(?=\s|$)""",
    )
    val zipCodeRegex: Pattern = Pattern.compile("""\b\d{5}(?:[-\s]\d{4})?\b""")
    val poBoxRegex: Pattern = Pattern.compile("""P\.? ?O\.? Box \d+""")
    val ssnRegex: Pattern = Pattern.compile(
      """(?!000|666|333)0*(?:[0-6][0-9][0-9]|[0-7][0-6][0-9]|[0-7][0-7][0-2])[- ](?!00)[0-9]{2}[- ](?!0000)[0-9]{4}""",
    )
    val jwtTokenRegex: Pattern = Pattern.compile(
      """e[yw][A-Za-z0-9-_]+\.(?:e[yw][A-Za-z0-9-_]+)?\.[A-Za-z0-9-_]{2,}(?:(?:\.[A-Za-z0-9-_]{2,}){2})?""",
    )
  }

  private val columnRegexes = ColumnRegexes()
  private val valueRegexes = ValueRegexes()

  /**
   * Looks for suspicious column names.
   */
  fun isColumnNameSafe(columnName: String): ColumnNamePiiIssue? {
    with(columnRegexes) {
      if (personRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Person
      }
      if (emailRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Email
      }
      if (dobRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Dob
      }
      if (genderRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Gender
      }
      if (nationalityRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Nationality
      }
      if (addressRegex.matcher(columnName).matches() ||
        zipCodeRegex.matcher(columnName).matches() ||
        poBoxRegex.matcher(columnName).matches()
      ) {
        return ColumnNamePiiIssue.Address
      }
      if (userNameRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.UserName
      }
      if (passwordRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Password
      }
      if (ssnRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Ssn
      }
      if (creditCardRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.CreditCard
      }
      if (phoneRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.Phone
      }
      if (authTokenRegex.matcher(columnName).matches()) {
        return ColumnNamePiiIssue.AuthToken
      }
    }

    return null
  }

  fun isValueSafe(value: String): ValuePiiIssue? {
    with(valueRegexes) {
      if (dateRegex.matcher(value).matches() ||
        timeRegex.matcher(value).matches()
      ) {
        return ValuePiiIssue.DateTime
      }
      if (phoneRegex.matcher(value).matches() ||
        phonesWithExtsRegex.matcher(value).matches()
      ) {
        return ValuePiiIssue.Phone
      }
      if (emailRegex.matcher(value).matches()) {
        return ValuePiiIssue.Email
      }
      if (ipRegex.matcher(value).matches() ||
        ipv6Regex.matcher(value).matches()
      ) {
        return ValuePiiIssue.Ip
      }
      if (creditCardRegex.matcher(value).matches() ||
        btcAddressRegex.matcher(value).matches()
      ) {
        return ValuePiiIssue.CreditCard
      }
      if (streetAddressRegex.matcher(value).matches() ||
        zipCodeRegex.matcher(value).matches() ||
        poBoxRegex.matcher(value).matches()
      ) {
        return ValuePiiIssue.Address
      }
      if (ssnRegex.matcher(value).matches()) {
        return ValuePiiIssue.Ssn
      }
      if (jwtTokenRegex.matcher(value).matches()) {
        return ValuePiiIssue.AuthToken
      }
    }

    return null
  }

  enum class ColumnNamePiiIssue {
    Person,
    Email,
    Dob,
    Gender,
    Nationality,
    Address,
    UserName,
    Password,
    Ssn,
    CreditCard,
    Phone,
    AuthToken,
  }

  enum class ValuePiiIssue {
    DateTime,
    Email,
    Ip,
    Address,
    Ssn,
    CreditCard,
    Phone,
    AuthToken,
  }
}
