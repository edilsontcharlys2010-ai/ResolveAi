package com.example.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

enum class PixKeyType(val label: String) {
  AUTO("Auto"),
  CPF("CPF"),
  CNPJ("CNPJ"),
  TELEFONE("Celular"),
  EMAIL("E-mail"),
  ALEATORIA("Aleatória")
}

data class ParsedPixBrCode(
  val payloadFormatIndicator: String,
  val gui: String,
  val pixKey: String,
  val merchantCategoryCode: String,
  val currencyCode: String,
  val amount: String?,
  val countryCode: String,
  val merchantName: String,
  val merchantCity: String,
  val txId: String?,
  val crc: String
)

object WalletRechargeHelper {
  const val MIN_RECHARGE_AMOUNT = 5.0
  const val MAX_RECHARGE_AMOUNT = 5000.0
  const val DEFAULT_PIX_KEY = "pagamento@resolveaiservicos.com.br"
  const val DEFAULT_MERCHANT_NAME = "RESOLVEAI SERVICOS"
  const val DEFAULT_MERCHANT_CITY = "BRASILIA"
  const val BACEN_PIX_GUI = "br.gov.bcb.pix"
  const val PIX_CHARGE_TTL_MS = 15 * 60_000L

  private val UUID_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

  fun validateAmount(amount: Double?): String? {
    if (amount == null || amount.isNaN() || amount <= 0.0) {
      return "Informe um valor de recarga válido."
    }
    if (amount < MIN_RECHARGE_AMOUNT) {
      return "O valor mínimo de recarga é R$ ${String.format(Locale.US, "%.2f", MIN_RECHARGE_AMOUNT)}."
    }
    if (amount > MAX_RECHARGE_AMOUNT) {
      return "O valor máximo por recarga é R$ ${String.format(Locale.US, "%.2f", MAX_RECHARGE_AMOUNT)}."
    }
    return null
  }

  /**
   * Normalizes a Brazilian Pix key into exact BACEN DICT format expected inside EMV tag 26-01:
   * - E-mail: trimmed lowercase
   * - CPF: 11 pure digits (no dots/hyphens)
   * - CNPJ: 14 pure digits (no dots/slashes/hyphens)
   * - Phone: E.164 (+55DDDNNNNNNNNN)
   * - EVP (random): lowercase UUID with hyphens
   */
  fun normalizePixKey(rawKey: String, type: PixKeyType = PixKeyType.AUTO): String {
    val clean = rawKey.trim()
    if (clean.isEmpty()) return ""

    return when (type) {
      PixKeyType.EMAIL -> clean.lowercase(Locale.US).filter { !it.isWhitespace() }
      PixKeyType.CPF -> clean.filter { it.isDigit() }.take(11)
      PixKeyType.CNPJ -> clean.filter { it.isDigit() }.take(14)
      PixKeyType.TELEFONE -> normalizePhonePixKey(clean)
      PixKeyType.ALEATORIA -> clean.lowercase(Locale.US).filter { !it.isWhitespace() }
      PixKeyType.AUTO -> autoNormalizePixKey(clean)
    }.take(77)
  }

  private fun normalizePhonePixKey(clean: String): String {
    val digits = clean.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    if (clean.startsWith("+")) {
      return "+$digits"
    }
    val withoutLeadingZero = if (digits.length == 12 && digits.startsWith("0")) {
      digits.drop(1)
    } else {
      digits
    }
    return if (withoutLeadingZero.startsWith("55") && withoutLeadingZero.length in 12..13) {
      "+$withoutLeadingZero"
    } else {
      "+55$withoutLeadingZero"
    }
  }

  private fun autoNormalizePixKey(clean: String): String {
    if (clean.contains("@")) {
      return clean.lowercase(Locale.US).filter { !it.isWhitespace() }
    }
    val lower = clean.lowercase(Locale.US).filter { !it.isWhitespace() }
    if (UUID_REGEX.matches(lower)) {
      return lower
    }
    if (clean.startsWith("+")) {
      return "+${clean.filter { it.isDigit() }}"
    }
    if (clean.contains("(") || clean.contains(")")) {
      return normalizePhonePixKey(clean)
    }
    if (clean.contains("/")) {
      return clean.filter { it.isDigit() }.take(14)
    }
    val digits = clean.filter { it.isDigit() }
    if (clean.contains(".") && clean.contains("-") && digits.length == 11) {
      return digits
    }
    if (clean.all { it.isDigit() || it.isWhitespace() || it == '-' }) {
      return when {
        digits.length == 14 -> digits
        digits.length == 11 && CpfValidator.isValid(digits) -> digits
        digits.length == 11 && digits[2] == '9' -> "+55$digits"
        digits.length == 10 -> "+55$digits"
        digits.length in 12..13 && digits.startsWith("55") -> "+$digits"
        digits.isNotEmpty() -> digits
        else -> clean
      }
    }
    return lower
  }

  fun detectPixKeyTypeLabel(rawKey: String, selectedType: PixKeyType = PixKeyType.AUTO): String {
    if (selectedType != PixKeyType.AUTO) return selectedType.label
    val normalized = normalizePixKey(rawKey, PixKeyType.AUTO)
    return when {
      normalized.contains("@") -> "E-mail"
      UUID_REGEX.matches(normalized) -> "Chave aleatória"
      normalized.startsWith("+") -> "Celular E.164"
      normalized.length == 14 && normalized.all { it.isDigit() } -> "CNPJ"
      normalized.length == 11 && normalized.all { it.isDigit() } -> "CPF"
      else -> "Personalizada"
    }
  }

  fun validatePixKey(pixKey: String, type: PixKeyType = PixKeyType.AUTO): String? {
    val clean = pixKey.trim()
    if (clean.isEmpty()) {
      return "Informe uma chave Pix válida."
    }
    val normalized = normalizePixKey(clean, type)
    if (normalized.length < 5) {
      return "Informe uma chave Pix válida."
    }
    if (normalized.any { it.isWhitespace() }) {
      return "A chave Pix não pode conter espaços."
    }
    if (normalized.length > 77) {
      return "A chave Pix não pode ultrapassar 77 caracteres."
    }

    return when (type) {
      PixKeyType.EMAIL -> {
        if (!FirebaseAuthHelper.isValidEmail(normalized)) {
          "Informe um e-mail válido para a chave Pix."
        } else null
      }
      PixKeyType.CPF -> {
        if (!CpfValidator.isValid(normalized)) {
          "Informe um CPF válido (11 dígitos) para a chave Pix."
        } else null
      }
      PixKeyType.CNPJ -> {
        if (normalized.length != 14 || !normalized.all { it.isDigit() }) {
          "Informe um CNPJ válido com 14 dígitos para a chave Pix."
        } else null
      }
      PixKeyType.TELEFONE -> {
        val phoneDigits = normalized.removePrefix("+")
        if (!normalized.startsWith("+") || phoneDigits.length !in 12..15) {
          "Informe DDD + celular válido (ex: (61) 99999-9999)."
        } else null
      }
      PixKeyType.ALEATORIA -> {
        if (!UUID_REGEX.matches(normalized)) {
          "Chave aleatória deve estar no formato UUID (8-4-4-4-12)."
        } else null
      }
      PixKeyType.AUTO -> null
    }
  }

  fun generateUniqueTxId(providerId: String = "RSV"): String {
    val prefix = providerId.filter { it.isLetterOrDigit() }.uppercase(Locale.US).take(6).ifBlank { "RSV" }
    val randomSuffix = UUID.randomUUID().toString().replace("-", "").uppercase(Locale.US).take(12)
    return ("PIX$prefix$randomSuffix").take(25)
  }

  private fun sanitizeAscii(raw: String, maxLen: Int, fallback: String): String {
    val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
    val ascii = decomposed
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace(Regex("[^A-Za-z0-9 ]"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(maxLen)
    return ascii.ifBlank { fallback }
  }

  private fun tlv(id: String, value: String): String {
    val byteLength = value.toByteArray(Charsets.UTF_8).size
    val length = byteLength.toString().padStart(2, '0')
    return "$id$length$value"
  }

  /**
   * Computes CRC16-CCITT (0xFFFF, polynomial 0x1021) required by BACEN EMV BR Code.
   */
  fun computeCrc16Ccitt(payloadWith6304: String): String {
    var crc = 0xFFFF
    for (byte in payloadWith6304.toByteArray(Charsets.UTF_8)) {
      crc = crc xor ((byte.toInt() and 0xFF) shl 8)
      repeat(8) {
        crc = if ((crc and 0x8000) != 0) {
          (crc shl 1) xor 0x1021
        } else {
          crc shl 1
        }
        crc = crc and 0xFFFF
      }
    }
    return String.format(Locale.US, "%04X", crc)
  }

  /**
   * Builds a 100% BACEN EMV QRCPS-MPM compliant Pix Copia e Cola / QR payload:
   * - Lowercase GUI `br.gov.bcb.pix` in tag 26-00 (mandatory by Brazilian banking apps)
   * - Normalized DICT key in tag 26-01 (no CPF/CNPJ punctuation, E.164 +55 for phone)
   * - ASCII-clean merchant name/city and UTF-8 byte-accurate TLV lengths + CRC16-CCITT
   */
  fun buildPixCopyPasteCode(
    amount: Double,
    providerId: String = "resolveai",
    pixKey: String = DEFAULT_PIX_KEY,
    txId: String = generateUniqueTxId(providerId),
    merchantName: String = DEFAULT_MERCHANT_NAME,
    merchantCity: String = DEFAULT_MERCHANT_CITY,
    pixKeyType: PixKeyType = PixKeyType.AUTO
  ): String {
    val safeAmount = amount.coerceIn(MIN_RECHARGE_AMOUNT, MAX_RECHARGE_AMOUNT)
    val formattedAmount = String.format(Locale.US, "%.2f", safeAmount)
    val normalizedKey = normalizePixKey(pixKey.ifBlank { DEFAULT_PIX_KEY }, pixKeyType)
      .ifBlank { DEFAULT_PIX_KEY }
      .take(77)
    val cleanMerchantName = sanitizeAscii(merchantName.uppercase(Locale.US), 25, DEFAULT_MERCHANT_NAME)
    val cleanCity = sanitizeAscii(merchantCity.uppercase(Locale.US), 15, DEFAULT_MERCHANT_CITY)
    val cleanTxId = txId.filter { it.isLetterOrDigit() }.uppercase(Locale.US).take(25).ifBlank { "RESOLVEAI" }

    val merchantAccountInfo = tlv("00", BACEN_PIX_GUI) + tlv("01", normalizedKey)
    val additionalData = tlv("05", cleanTxId)

    val payloadWithoutCrc = buildString {
      append(tlv("00", "01"))
      append(tlv("26", merchantAccountInfo))
      append(tlv("52", "0000"))
      append(tlv("53", "986"))
      append(tlv("54", formattedAmount))
      append(tlv("58", "BR"))
      append(tlv("59", cleanMerchantName))
      append(tlv("60", cleanCity))
      append(tlv("62", additionalData))
      append("6304")
    }

    val crc = computeCrc16Ccitt(payloadWithoutCrc)
    return payloadWithoutCrc + crc
  }

  private fun parseTlvMap(emvPayload: String): Map<String, String>? {
    val result = linkedMapOf<String, String>()
    var idx = 0
    while (idx + 4 <= emvPayload.length) {
      val id = emvPayload.substring(idx, idx + 2)
      val lenStr = emvPayload.substring(idx + 2, idx + 4)
      val len = lenStr.toIntOrNull() ?: return null
      if (len < 0 || idx + 4 + len > emvPayload.length) return null
      val value = emvPayload.substring(idx + 4, idx + 4 + len)
      result[id] = value
      idx += 4 + len
    }
    return if (idx == emvPayload.length) result else null
  }

  fun parsePixBrCode(brCode: String): ParsedPixBrCode? {
    val trimmed = brCode.trim()
    if (trimmed.length < 12) return null
    val topTags = parseTlvMap(trimmed) ?: return null
    val crc = topTags["63"] ?: return null
    if (crc.length != 4) return null
    val bodyWithoutCrc = trimmed.dropLast(4)
    if (!bodyWithoutCrc.endsWith("6304")) return null
    if (computeCrc16Ccitt(bodyWithoutCrc) != crc) return null

    val merchantAccountRaw = topTags["26"] ?: return null
    val merchantTags = parseTlvMap(merchantAccountRaw) ?: return null
    val gui = merchantTags["00"] ?: return null
    val pixKey = merchantTags["01"] ?: return null
    if (gui != BACEN_PIX_GUI || pixKey.isBlank()) return null

    val additionalRaw = topTags["62"]
    val additionalTags = additionalRaw?.let { parseTlvMap(it) }

    return ParsedPixBrCode(
      payloadFormatIndicator = topTags["00"] ?: return null,
      gui = gui,
      pixKey = pixKey,
      merchantCategoryCode = topTags["52"] ?: return null,
      currencyCode = topTags["53"] ?: return null,
      amount = topTags["54"],
      countryCode = topTags["58"] ?: return null,
      merchantName = topTags["59"] ?: return null,
      merchantCity = topTags["60"] ?: return null,
      txId = additionalTags?.get("05"),
      crc = crc
    )
  }

  fun isValidPixBrCode(brCode: String): Boolean {
    val parsed = parsePixBrCode(brCode) ?: return false
    return parsed.payloadFormatIndicator == "01" &&
      parsed.gui == BACEN_PIX_GUI &&
      parsed.currencyCode == "986" &&
      parsed.countryCode == "BR" &&
      parsed.pixKey.isNotBlank() &&
      !parsed.pixKey.any { it.isWhitespace() }
  }

  /**
   * Encodes any Pix BR Code string into a square boolean QR module matrix using ZXing
   * with standard 4-module quiet zone so phone bank cameras detect the QR code reliably.
   */
  fun buildQrMatrix(payload: String): Array<BooleanArray> {
    val hints = mapOf(
      EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
      EncodeHintType.MARGIN to 4,
      EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
    val width = bitMatrix.width
    val height = bitMatrix.height
    return Array(height) { y ->
      BooleanArray(width) { x -> bitMatrix.get(x, y) }
    }
  }
}
