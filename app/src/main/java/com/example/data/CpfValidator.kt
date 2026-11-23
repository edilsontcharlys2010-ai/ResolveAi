package com.example.data

object CpfValidator {

  fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }.take(11)

  fun sanitizeInput(raw: String): String =
    raw.filter { it.isDigit() || it == '.' || it == '-' }.take(14)

  fun isValid(rawCpf: String): Boolean {
    val digits = rawCpf.filter { it.isDigit() }
    if (digits.length != 11) return false
    if (digits.all { it == digits[0] }) return false

    val firstSum = (0..8).sumOf { idx -> digits[idx].digitToInt() * (10 - idx) }
    val firstRem = (firstSum * 10) % 11
    val firstCheckDigit = if (firstRem == 10) 0 else firstRem
    if (firstCheckDigit != digits[9].digitToInt()) return false

    val secondSum = (0..9).sumOf { idx -> digits[idx].digitToInt() * (11 - idx) }
    val secondRem = (secondSum * 10) % 11
    val secondCheckDigit = if (secondRem == 10) 0 else secondRem
    return secondCheckDigit == digits[10].digitToInt()
  }

  fun formatCpf(rawCpf: String): String {
    val digits = rawCpf.filter { it.isDigit() }
    return if (digits.length == 11) {
      "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9, 11)}"
    } else {
      rawCpf.trim()
    }
  }

  fun validateProviderCpf(rawCpf: String): String? {
    val digits = rawCpf.filter { it.isDigit() }
    return when {
      digits.isEmpty() -> "Digite o CPF obrigatório do prestador (11 dígitos)."
      digits.length != 11 -> "CPF incompleto: digite todos os 11 dígitos numéricos."
      !isValid(rawCpf) -> "CPF inválido: confira os dígitos informados e digite um CPF válido."
      else -> null
    }
  }

  fun validateClientCpf(rawCpf: String): String? {
    val digits = rawCpf.filter { it.isDigit() }
    return when {
      digits.isEmpty() -> "Informe um CPF válido (11 dígitos) para concluir o cadastro."
      digits.length != 11 -> "CPF incompleto: informe os 11 dígitos do seu CPF."
      !isValid(rawCpf) -> "CPF inválido: confira os dígitos informados."
      else -> null
    }
  }

  fun providerMissingRequirements(
    name: String? = null,
    requireName: Boolean = false,
    email: String? = null,
    phone: String = "",
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    requireEmailOrPhone: Boolean = false,
    requireEmailAndPhone: Boolean = false,
    requirePhoneOnly: Boolean = false,
    password: String? = null,
    requirePassword: Boolean = false,
    city: String,
    document: String,
    profilePhotoUri: String? = null,
    requireCameraProfilePhoto: Boolean = false,
    requireAttachedProfilePhoto: Boolean = false,
    rgOrCnhAttachmentUri: String?,
    criminalRecordAttachmentUri: String?,
    selfieAttachmentUri: String? = null,
    selfieMissingMessage: String = "Anexo 3: Selfie na câmera com documento"
  ): List<String> {
    val missing = mutableListOf<String>()
    if (requireName && (name?.trim()?.length ?: 0) < 2) {
      missing += "Nome completo"
    }
    if (requireEmailAndPhone) {
      val cleanEmail = email?.trim().orEmpty()
      if (!ContactValidator.isValidEmail(cleanEmail)) {
        missing += "E-mail válido"
      }
      if (!ContactValidator.isValidPhone(phone, countryCode)) {
        missing += "Telefone válido com DDD"
      }
    } else if (requireEmailOrPhone) {
      val cleanEmail = email?.trim().orEmpty()
      val validEmail = cleanEmail.isNotBlank() && ContactValidator.isValidEmail(cleanEmail)
      val validPhone = ContactValidator.isValidPhone(phone, countryCode)
      if (!validEmail && !validPhone) {
        missing += "E-mail válido ou celular com DDD"
      } else if (cleanEmail.isNotBlank() && !validEmail) {
        missing += "E-mail em formato válido"
      }
    } else if (requirePhoneOnly && !ContactValidator.isValidPhone(phone, countryCode)) {
      missing += "Telefone válido com DDD"
    }
    if (requirePassword && (password?.length ?: 0) < 6) {
      missing += "Senha (mínimo 6 caracteres)"
    }
    if (city.trim().length < 3) {
      missing += "Cidade / UF"
    }
    val cpfError = validateProviderCpf(document)
    if (cpfError != null) {
      val digits = document.filter { it.isDigit() }
      missing += when {
        digits.isEmpty() -> "CPF obrigatório (11 dígitos)"
        digits.length != 11 -> "CPF completo (11 dígitos)"
        else -> "CPF matematicamente válido"
      }
    }
    if (requireCameraProfilePhoto && profilePhotoUri.isNullOrBlank()) {
      missing += "Foto de perfil tirada na câmera"
    } else if (requireAttachedProfilePhoto && profilePhotoUri.isNullOrBlank()) {
      missing += "Foto de perfil anexada"
    }
    if (rgOrCnhAttachmentUri.isNullOrBlank()) {
      missing += "Anexo 1: foto ou PDF do RG/CNH"
    }
    if (criminalRecordAttachmentUri.isNullOrBlank()) {
      missing += "Anexo 2: Certidão de Antecedentes"
    }
    return missing
  }

  fun clientMissingRequirements(
    name: String? = null,
    requireName: Boolean = false,
    email: String? = null,
    requireEmail: Boolean = false,
    phone: String = "",
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    requirePhone: Boolean = true,
    password: String? = null,
    requirePassword: Boolean = false,
    city: String,
    document: String
  ): List<String> {
    val missing = mutableListOf<String>()
    if (requireName && (name?.trim()?.length ?: 0) < 2) {
      missing += "Nome completo"
    }
    if (requireEmail && !ContactValidator.isValidEmail(email.orEmpty())) {
      missing += "E-mail válido"
    }
    if (requirePhone && !ContactValidator.isValidPhone(phone, countryCode)) {
      missing += "Telefone válido com DDD"
    }
    if (requirePassword && (password?.length ?: 0) < 6) {
      missing += "Senha (mínimo 6 caracteres)"
    }
    if (city.trim().length < 3) {
      missing += "Cidade / UF"
    }
    if (!isValid(document)) {
      missing += "CPF válido (11 dígitos)"
    }
    return missing
  }
}
