package com.example.model

import java.util.regex.Pattern

data class ContactFilterResult(
  val isAllowed: Boolean,
  val violationType: String? = null,
  val detectedSnippet: String? = null,
  val reasonMessage: String? = null
)

object ContactFilter {

  // Email pattern
  private val EMAIL_PATTERN = Pattern.compile(
    "[a-zA-Z0-9._%+-]+(?:@|\\s*\\[at\\]\\s*|\\s*arroba\\s*)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
    Pattern.CASE_INSENSITIVE
  )

  // Web links and URLs
  private val URL_PATTERN = Pattern.compile(
    "(https?://|www\\.)\\S+|\\b[a-zA-Z0-9-]+\\.(com|com\\.br|org|net|me|io|app)\\b",
    Pattern.CASE_INSENSITIVE
  )

  // Social networks
  private val SOCIAL_PATTERN = Pattern.compile(
    "\\b(instagram|insta|facebook|face|fb|telegram|t\\.me|tiktok|twitter|linkedin)\\b\\s*[:@/\\-]?[a-zA-Z0-9_.]+|@[a-zA-Z0-9_]{3,}",
    Pattern.CASE_INSENSITIVE
  )

  // Direct contact keywords (WhatsApp, Zap, Telefone, Celular)
  private val CONTACT_KEYWORD_PATTERN = Pattern.compile(
    "\\b(whats|whatsapp|zap|wpp|zapzap|telegram|contato|telefone|celular|fone|me liga|chama la|chama no|passa o numero|meu numero)\\b",
    Pattern.CASE_INSENSITIVE
  )

  // Brazilian CPF (often shared as PIX or direct contact)
  private val CPF_PATTERN = Pattern.compile(
    "\\b\\d{3}[.\\s]?\\d{3}[.\\s]?\\d{3}[-\\s]?\\d{2}\\b"
  )

  // Phone numbers: Brazilian formats with or without DDD, e.g. (11) 98765-4321, 11 987654321, 98765-4321, or 8-11 consecutive digits
  private val PHONE_PATTERN = Pattern.compile(
    "(?:(?:\\+?55\\s?)?(?:\\(?\\d{2}\\)?\\s?)?(?:9\\s?\\d{4}[-\\s]?\\d{4}|[2-8]\\d{3}[-\\s]?\\d{4}))"
  )

  // Sequence of 7 to 11 digits that might be spaced out like "9 8 7 6 5 4 3 2 1"
  private val SPACED_DIGITS_PATTERN = Pattern.compile(
    "(?:\\d[\\s.-]*){8,11}"
  )

  /**
   * Evaluates text and returns whether it is allowed or violates anti-contact sharing policy.
   */
  fun evaluate(message: String): ContactFilterResult {
    val clean = message.trim()
    if (clean.isEmpty()) {
      return ContactFilterResult(isAllowed = true)
    }

    // 1. Check for URLs & links
    val urlMatcher = URL_PATTERN.matcher(clean)
    if (urlMatcher.find()) {
      val snippet = urlMatcher.group()
      return ContactFilterResult(
        isAllowed = false,
        violationType = "Link externo ou site",
        detectedSnippet = snippet,
        reasonMessage = "Não é permitido compartilhar links externos. Todas as negociações devem ocorrer dentro da plataforma."
      )
    }

    // 2. Check for Email
    val emailMatcher = EMAIL_PATTERN.matcher(clean)
    if (emailMatcher.find()) {
      val snippet = emailMatcher.group()
      return ContactFilterResult(
        isAllowed = false,
        violationType = "Endereço de e-mail",
        detectedSnippet = snippet,
        reasonMessage = "Compartilhamento de e-mail bloqueado por segurança. Utilize o chat privado da plataforma."
      )
    }

    // 3. Check for Social Handles
    val socialMatcher = SOCIAL_PATTERN.matcher(clean)
    if (socialMatcher.find()) {
      val snippet = socialMatcher.group()
      return ContactFilterResult(
        isAllowed = false,
        violationType = "Rede social externa",
        detectedSnippet = snippet,
        reasonMessage = "Não é permitida a divulgação de redes sociais (Instagram, Telegram, Facebook, etc.)."
      )
    }

    // 4. Check for CPF pattern
    val cpfMatcher = CPF_PATTERN.matcher(clean)
    if (cpfMatcher.find()) {
      val snippet = cpfMatcher.group()
      return ContactFilterResult(
        isAllowed = false,
        violationType = "Documento / Chave Pix",
        detectedSnippet = snippet,
        reasonMessage = "Envio de CPF ou chave Pix externa não é permitido. Pagamentos e taxas são processados via carteira."
      )
    }

    // 5. Check for phone number matches
    val phoneMatcher = PHONE_PATTERN.matcher(clean)
    if (phoneMatcher.find()) {
      val snippet = phoneMatcher.group()
      // ensure it's not just a harmless short year like 2026 or small money
      val digitsOnly = snippet.filter { it.isDigit() }
      if (digitsOnly.length in 8..11) {
        return ContactFilterResult(
          isAllowed = false,
          violationType = "Número de telefone / celular",
          detectedSnippet = snippet,
          reasonMessage = "Números de telefone não podem ser compartilhados. Mantenha as mensagens no chat protegido."
        )
      }
    }

    // 6. Check for spaced out digits sequence (attempt to bypass phone filter)
    val spacedMatcher = SPACED_DIGITS_PATTERN.matcher(clean)
    if (spacedMatcher.find()) {
      val snippet = spacedMatcher.group()
      val digits = snippet.filter { it.isDigit() }
      if (digits.length in 8..11) {
        // Exclude common price mentions like "R$ 1.500,00" unless it's purely digits
        if (!snippet.contains("R$") && !snippet.contains("$")) {
          return ContactFilterResult(
            isAllowed = false,
            violationType = "Sequência numérica / Telefone camuflado",
            detectedSnippet = snippet.trim(),
            reasonMessage = "Tentativa de troca de número de telefone detectada e bloqueada pelas diretrizes de segurança."
          )
        }
      }
    }

    // 7. Check for contact keywords accompanied by numbers
    val keywordMatcher = CONTACT_KEYWORD_PATTERN.matcher(clean)
    if (keywordMatcher.find()) {
      val keyword = keywordMatcher.group()
      // If the message contains a keyword like "zap" or "whats" or "meu numero"
      // and has digits or says "chama"
      val hasDigits = clean.any { it.isDigit() }
      if (hasDigits || clean.lowercase().contains("chama") || clean.lowercase().contains("passa")) {
        return ContactFilterResult(
          isAllowed = false,
          violationType = "Menção a contato externo ($keyword)",
          detectedSnippet = keyword,
          reasonMessage = "Termos como '$keyword' são proibidos para evitar evasão da plataforma e proteger ambas as partes."
        )
      }
    }

    return ContactFilterResult(isAllowed = true)
  }
}
