package com.example.data

object ContactValidator {

  private val EMAIL_REGEX = Regex(
    "^[A-Za-z0-9](?:[A-Za-z0-9+_.-]*[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$"
  )

  private val VALID_BR_DDDS = setOf(
    11, 12, 13, 14, 15, 16, 17, 18, 19,
    21, 22, 24, 27, 28,
    31, 32, 33, 34, 35, 37, 38,
    41, 42, 43, 44, 45, 46, 47, 48, 49,
    51, 53, 54, 55,
    61, 62, 63, 64, 65, 66, 67, 68, 69,
    71, 73, 74, 75, 77, 79,
    81, 82, 83, 84, 85, 86, 87, 88, 89,
    91, 92, 93, 94, 95, 96, 97, 98, 99
  )

  fun isValidEmail(rawEmail: String): Boolean {
    val clean = rawEmail.trim()
    if (clean.isEmpty() || clean.length > 150) return false
    if (clean.contains("..")) return false
    return EMAIL_REGEX.matches(clean)
  }

  fun validateEmail(rawEmail: String): String? {
    val clean = rawEmail.trim()
    return when {
      clean.isEmpty() -> "Informe um e-mail obrigatório (ex: nome@dominio.com)."
      !isValidEmail(clean) -> "E-mail inválido: confira o endereço digitado (ex: nome@dominio.com)."
      else -> null
    }
  }

  fun extractLocalPhoneDigits(
    rawPhone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String {
    val trimmed = rawPhone.trim()
    val digits = trimmed.filter { it.isDigit() }
    val dialDigits = countryCode.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val baseDigits = when {
      dialDigits.isNotEmpty() &&
        trimmed.startsWith("+") &&
        digits.startsWith(dialDigits) &&
        digits.length > dialDigits.length -> digits.removePrefix(dialDigits)
      dialDigits == "55" &&
        digits.startsWith("55") &&
        digits.length in 12..13 -> digits.removePrefix("55")
      else -> digits
    }
    return if (dialDigits == "55" && baseDigits.startsWith("0") && baseDigits.length in 11..12) {
      baseDigits.drop(1)
    } else {
      baseDigits
    }
  }

  fun isValidPhone(
    rawPhone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): Boolean = validatePhone(rawPhone, countryCode) == null

  fun validatePhone(
    rawPhone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String? {
    val trimmed = rawPhone.trim()
    val rawDigits = trimmed.filter { it.isDigit() }
    if (rawDigits.isEmpty()) {
      return "Informe um telefone válido com DDD."
    }

    val dialDigits = countryCode.filter { it.isDigit() }.ifBlank { "55" }
    val localDigits = extractLocalPhoneDigits(trimmed, countryCode)

    if (dialDigits == "55") {
      if (localDigits.length < 10) {
        return "Telefone incompleto: informe DDD + número (${localDigits.length}/10+ dígitos)."
      }
      if (localDigits.length > 11) {
        return "Telefone inválido: informe apenas DDD + número (10 ou 11 dígitos)."
      }
      val ddd = localDigits.substring(0, 2).toIntOrNull()
      if (ddd == null || ddd !in VALID_BR_DDDS) {
        return "DDD inválido: informe um DDD brasileiro válido (ex: 11, 21, 61)."
      }
      val subscriber = localDigits.substring(2)
      if (localDigits.all { it == localDigits[0] } || subscriber.all { it == subscriber[0] }) {
        return "Telefone inválido: informe um número de telefone real."
      }
      if (localDigits.length == 11) {
        if (subscriber[0] != '9') {
          return "Celular inválido: números de 11 dígitos devem começar com 9 após o DDD."
        }
        if (subscriber.drop(1).all { it == '0' }) {
          return "Telefone inválido: informe um número de telefone real."
        }
      } else {
        if (subscriber[0] !in '2'..'8') {
          return "Telefone fixo inválido: confira o número após o DDD."
        }
        if (subscriber.drop(1).all { it == '0' }) {
          return "Telefone inválido: informe um número de telefone real."
        }
      }
      return null
    }

    if (localDigits.length < 8) {
      return "Telefone incompleto: informe código de área + número."
    }
    if (localDigits.length > 13) {
      return "Telefone inválido: quantidade de dígitos acima do permitido."
    }
    if (localDigits.all { it == localDigits[0] }) {
      return "Telefone inválido: informe um número de telefone real."
    }
    return null
  }

  fun formatNationalBrIfPossible(rawPhone: String): String {
    val local = extractLocalPhoneDigits(rawPhone, "+55")
    return when (local.length) {
      11 -> "(${local.substring(0, 2)}) ${local.substring(2, 7)}-${local.substring(7, 11)}"
      10 -> "(${local.substring(0, 2)}) ${local.substring(2, 6)}-${local.substring(6, 10)}"
      else -> rawPhone.trim()
    }
  }
}
