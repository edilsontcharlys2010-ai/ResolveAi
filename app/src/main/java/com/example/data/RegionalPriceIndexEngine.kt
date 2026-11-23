package com.example.data

import com.example.model.RegionalPriceEstimate
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

enum class RegionalPriceComparisonLevel {
  IN_RANGE,
  BELOW_AVERAGE,
  ABOVE_AVERAGE
}

data class RegionalPriceComparison(
  val level: RegionalPriceComparisonLevel,
  val percentDiff: Int
)

/**
 * Motor de medição automática de valor regional por categoria com atualização diária.
 *
 * Combina:
 * 1. Base de referência regional brasileira (UF, macro-região e porte urbano/capital).
 * 2. Atualização diária determinística por data (yyyy-MM-dd) refletindo o ciclo diário do mercado.
 * 3. Amostra real dos pedidos da categoria na mesma cidade/estado/região registrados no aplicativo/Firebase.
 * 4. Sincronização diária compartilhada com o Firestore (/regionalPriceBenchmarks/{benchmarkId}).
 */
object RegionalPriceIndexEngine {

  private val brazilTimeZone: TimeZone = TimeZone.getTimeZone("America/Sao_Paulo")

  data class ResolvedRegion(
    val regionKey: String,
    val regionLabel: String,
    val cityName: String,
    val stateCode: String,
    val macroRegion: String,
    val stateFactor: Double,
    val urbanTierFactor: Double
  ) {
    val combinedFactor: Double get() = stateFactor * urbanTierFactor
  }

  private data class StateProfile(
    val code: String,
    val fullName: String,
    val macroRegion: String,
    val factor: Double,
    val capital: String
  )

  private data class CategoryBaseline(
    val canonicalKey: String,
    val keywords: List<String>,
    val basePrice: Double,
    val minRatio: Double = 0.78,
    val maxRatio: Double = 1.28
  )

  private val stateProfiles: List<StateProfile> = listOf(
    StateProfile("DF", "Distrito Federal", "Centro-Oeste", 1.18, "Brasília"),
    StateProfile("SP", "São Paulo", "Sudeste", 1.15, "São Paulo"),
    StateProfile("RJ", "Rio de Janeiro", "Sudeste", 1.12, "Rio de Janeiro"),
    StateProfile("SC", "Santa Catarina", "Sul", 1.09, "Florianópolis"),
    StateProfile("RS", "Rio Grande do Sul", "Sul", 1.07, "Porto Alegre"),
    StateProfile("PR", "Paraná", "Sul", 1.05, "Curitiba"),
    StateProfile("MT", "Mato Grosso", "Centro-Oeste", 1.03, "Cuiabá"),
    StateProfile("ES", "Espírito Santo", "Sudeste", 1.02, "Vitória"),
    StateProfile("MS", "Mato Grosso do Sul", "Centro-Oeste", 1.02, "Campo Grande"),
    StateProfile("MG", "Minas Gerais", "Sudeste", 1.01, "Belo Horizonte"),
    StateProfile("GO", "Goiás", "Centro-Oeste", 1.00, "Goiânia"),
    StateProfile("AM", "Amazonas", "Norte", 0.99, "Manaus"),
    StateProfile("RR", "Roraima", "Norte", 0.95, "Boa Vista"),
    StateProfile("AP", "Amapá", "Norte", 0.94, "Macapá"),
    StateProfile("BA", "Bahia", "Nordeste", 0.93, "Salvador"),
    StateProfile("RO", "Rondônia", "Norte", 0.93, "Porto Velho"),
    StateProfile("PE", "Pernambuco", "Nordeste", 0.92, "Recife"),
    StateProfile("PA", "Pará", "Norte", 0.92, "Belém"),
    StateProfile("AC", "Acre", "Norte", 0.92, "Rio Branco"),
    StateProfile("TO", "Tocantins", "Norte", 0.91, "Palmas"),
    StateProfile("CE", "Ceará", "Nordeste", 0.90, "Fortaleza"),
    StateProfile("RN", "Rio Grande do Norte", "Nordeste", 0.90, "Natal"),
    StateProfile("SE", "Sergipe", "Nordeste", 0.89, "Aracaju"),
    StateProfile("PB", "Paraíba", "Nordeste", 0.88, "João Pessoa"),
    StateProfile("AL", "Alagoas", "Nordeste", 0.88, "Maceió"),
    StateProfile("MA", "Maranhão", "Nordeste", 0.87, "São Luís"),
    StateProfile("PI", "Piauí", "Nordeste", 0.86, "Teresina")
  )

  private val stateByCode: Map<String, StateProfile> = stateProfiles.associateBy { it.code }
  private val stateByNormalizedName: Map<String, StateProfile> =
    stateProfiles.associateBy { normalizeToken(it.fullName) }

  // Principais cidades brasileiras -> (Nome formatado, UF, fator urbano)
  private data class CityLookup(val displayName: String, val stateCode: String, val urbanFactor: Double)

  private val majorCities: Map<String, CityLookup> = buildMap {
    // Capitais (tier metropolitano/capital)
    stateProfiles.forEach { st ->
      val factor = when (st.code) {
        "SP", "RJ", "DF" -> 1.07
        "MG", "PR", "RS", "SC", "BA", "PE", "CE", "GO", "AM", "PA" -> 1.05
        else -> 1.03
      }
      put(normalizeToken(st.capital), CityLookup(st.capital, st.code, factor))
    }
    // Polos regionais e grandes cidades
    val hubs = listOf(
      Triple("Campinas", "SP", 1.05),
      Triple("Guarulhos", "SP", 1.05),
      Triple("Santo André", "SP", 1.05),
      Triple("São Bernardo do Campo", "SP", 1.05),
      Triple("Osasco", "SP", 1.05),
      Triple("Barueri", "SP", 1.06),
      Triple("Santos", "SP", 1.04),
      Triple("São José dos Campos", "SP", 1.04),
      Triple("Ribeirão Preto", "SP", 1.03),
      Triple("Sorocaba", "SP", 1.03),
      Triple("Jundiaí", "SP", 1.03),
      Triple("Piracicaba", "SP", 1.02),
      Triple("Bauru", "SP", 1.01),
      Triple("Niterói", "RJ", 1.05),
      Triple("Duque de Caxias", "RJ", 1.03),
      Triple("Nova Iguaçu", "RJ", 1.02),
      Triple("Petrópolis", "RJ", 1.02),
      Triple("Campos dos Goytacazes", "RJ", 1.01),
      Triple("Uberlândia", "MG", 1.03),
      Triple("Contagem", "MG", 1.03),
      Triple("Juiz de Fora", "MG", 1.02),
      Triple("Betim", "MG", 1.02),
      Triple("Montes Claros", "MG", 1.00),
      Triple("Uberaba", "MG", 1.01),
      Triple("Vila Velha", "ES", 1.03),
      Triple("Serra", "ES", 1.02),
      Triple("Londrina", "PR", 1.02),
      Triple("Maringá", "PR", 1.02),
      Triple("Cascavel", "PR", 1.01),
      Triple("Ponta Grossa", "PR", 1.01),
      Triple("Foz do Iguaçu", "PR", 1.02),
      Triple("Joinville", "SC", 1.03),
      Triple("Blumenau", "SC", 1.03),
      Triple("Balneário Camboriú", "SC", 1.06),
      Triple("Itajaí", "SC", 1.03),
      Triple("Chapecó", "SC", 1.01),
      Triple("Caxias do Sul", "RS", 1.03),
      Triple("Canoas", "RS", 1.03),
      Triple("Pelotas", "RS", 1.01),
      Triple("Santa Maria", "RS", 1.01),
      Triple("Novo Hamburgo", "RS", 1.02),
      Triple("Aparecida de Goiânia", "GO", 1.02),
      Triple("Anápolis", "GO", 1.01),
      Triple("Taguatinga", "DF", 1.05),
      Triple("Águas Claras", "DF", 1.06),
      Triple("Dourados", "MS", 1.00),
      Triple("Rondonópolis", "MT", 1.01),
      Triple("Sinop", "MT", 1.02),
      Triple("Feira de Santana", "BA", 1.01),
      Triple("Vitória da Conquista", "BA", 1.00),
      Triple("Camaçari", "BA", 1.02),
      Triple("Jaboatão dos Guararapes", "PE", 1.02),
      Triple("Olinda", "PE", 1.02),
      Triple("Caruaru", "PE", 1.00),
      Triple("Petrolina", "PE", 1.00),
      Triple("Caucaia", "CE", 1.01),
      Triple("Juazeiro do Norte", "CE", 0.99),
      Triple("Sobral", "CE", 0.99),
      Triple("Mossoró", "RN", 0.99),
      Triple("Campina Grande", "PB", 1.00),
      Triple("Imperatriz", "MA", 1.00),
      Triple("Santarém", "PA", 0.99),
      Triple("Ananindeua", "PA", 1.01)
    )
    hubs.forEach { (name, uf, factor) ->
      put(normalizeToken(name), CityLookup(name, uf, factor))
    }
  }

  private val categoryBaselines: List<CategoryBaseline> = listOf(
    CategoryBaseline("cabeleireiro", listOf("cabeleireir", "cabelo", "barbeir", "salao", "escova", "corte"), 115.0, 0.72, 1.38),
    CategoryBaseline("manicure", listOf("manicur", "pedicur", "unha", "alongamento", "esmalt"), 80.0, 0.72, 1.35),
    CategoryBaseline("eletricista", listOf("eletric", "disjuntor", "tomada", "fiacao", "chuveiro", "quadro"), 240.0, 0.76, 1.30),
    CategoryBaseline("hidraulica", listOf("hidraulic", "encanador", "vazamento", "torneira", "desentup", "caixa d agua", "bombeiro"), 230.0, 0.76, 1.30),
    CategoryBaseline("pintura", listOf("pintur", "pintor", "acabamento", "textura", "grafiato", "massa corrida"), 380.0, 0.75, 1.35),
    CategoryBaseline("ti_informatica", listOf("informatic", "rede", "computador", "notebook", "wifi", "roteador", "ti,", "ti "), 210.0, 0.76, 1.30),
    CategoryBaseline("limpeza_diarista", listOf("limpez", "diarist", "faxin", "passadeira", "higieniz"), 185.0, 0.78, 1.28),
    CategoryBaseline("montagem_moveis", listOf("montage", "movei", "montador", "marcenar", "guarda roupa", "armario"), 220.0, 0.75, 1.32),
    CategoryBaseline("reformas_alvenaria", listOf("reform", "alvenar", "pedreir", "obra", "piso", "azulej", "constru"), 460.0, 0.74, 1.38),
    CategoryBaseline("fretes_mudancas", listOf("frete", "mudanc", "carret", "transport", "caminha"), 340.0, 0.75, 1.35),
    CategoryBaseline("ar_condicionado", listOf("ar condicionado", "ar-condicionado", "climatiz", "refrigerac", "split"), 295.0, 0.78, 1.32),
    CategoryBaseline("jardinagem", listOf("jardin", "poda", "paisag", "grama", "roca"), 195.0, 0.76, 1.30),
    CategoryBaseline("chaveiro", listOf("chaveir", "fechadur", "cadead", "tranca"), 150.0, 0.75, 1.35),
    CategoryBaseline("dedetizacao", listOf("dedetiz", "praga", "cupim", "desratiz"), 260.0, 0.78, 1.30),
    CategoryBaseline("serralheria", listOf("serralh", "solda", "portao", "grade", "ferro"), 330.0, 0.75, 1.35),
    CategoryBaseline("vidracaria", listOf("vidrac", "vidro", "box", "espelho", "blindex"), 315.0, 0.76, 1.34),
    CategoryBaseline("gesso_drywall", listOf("gess", "drywall", "forro", "sanca"), 360.0, 0.76, 1.34),
    CategoryBaseline("eletrodomesticos", listOf("eletrodomest", "geladeir", "lavadora", "maquina de lavar", "microondas", "fogao"), 225.0, 0.76, 1.30),
    CategoryBaseline("seguranca_cftv", listOf("camera", "cftv", "alarme", "cerca eletrica", "interfone", "seguranca"), 320.0, 0.76, 1.32),
    CategoryBaseline("mecanica_auto", listOf("mecanic", "auto", "carro", "moto", "guincho", "bateria"), 275.0, 0.76, 1.34),
    CategoryBaseline("pet", listOf("pet", "banho e tosa", "adestr", "cachorr", "gato"), 120.0, 0.75, 1.32),
    CategoryBaseline("aulas", listOf("aula", "professor", "reforco", "idioma", "musica"), 125.0, 0.75, 1.35)
  )

  fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = brazilTimeZone }
    return sdf.format(Date(nowMillis))
  }

  fun formatDisplayDate(dateKey: String): String {
    val parts = dateKey.trim().split("-")
    return if (parts.size == 3 && parts[0].length == 4 && parts[1].length == 2 && parts[2].length == 2) {
      "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
      dateKey
    }
  }

  fun benchmarkDocId(dateKey: String, regionKey: String, category: String): String {
    val cleanDate = dateKey.filter { it.isDigit() }.ifBlank { todayDateKey().filter { it.isDigit() } }
    val cleanRegion = slugify(regionKey).ifBlank { "br_nacional" }.take(36)
    val cleanCategory = slugify(category).ifBlank { "geral" }.take(44)
    return "${cleanDate}__${cleanRegion}__${cleanCategory}".take(120)
  }

  fun resolveRegion(locationText: String, fallbackCity: String = ""): ResolvedRegion {
    val candidate = locationText.trim().ifBlank { fallbackCity.trim() }
    if (candidate.isBlank()) {
      return ResolvedRegion(
        regionKey = "br_nacional",
        regionLabel = "Brasil (Média Nacional)",
        cityName = "",
        stateCode = "",
        macroRegion = "Nacional",
        stateFactor = 1.0,
        urbanTierFactor = 1.0
      )
    }

    val normalizedCandidate = normalizeToken(candidate)

    // 1. Tenta identificar UF explícita ("- SP", ", PR", "/RJ", " MG")
    val explicitUf = extractStateCode(candidate)

    // 2. Tenta identificar nome completo de estado ("Minas Gerais", "Bahia", "São Paulo", etc.)
    val matchedStateByName = stateByNormalizedName.entries
      .firstOrNull { (stateNorm, _) -> containsWholePhrase(normalizedCandidate, stateNorm) }
      ?.value

    // 3. Se houver separador de cidade + nome do estado (ex.: "Campinas, São Paulo" ou "Niterói, Rio de Janeiro"),
    // analisa primeiro a parte anterior ao estado para evitar confundir o nome do estado com a capital homônima.
    val citySearchText = if (matchedStateByName != null) {
      val extractedBeforeState = extractCityBeforeUf(
        rawText = candidate,
        uf = matchedStateByName.code,
        stateFullName = matchedStateByName.fullName
      )
      if (extractedBeforeState.isNotBlank()) {
        normalizeToken(extractedBeforeState)
      } else {
        normalizedCandidate
      }
    } else {
      normalizedCandidate
    }

    // 4. Tenta identificar cidade conhecida na string (mais específico)
    val matchedCity = findKnownCity(citySearchText) ?: if (citySearchText != normalizedCandidate) {
      null
    } else {
      findKnownCity(normalizedCandidate)
    }

    val resolvedState: StateProfile? = when {
      explicitUf != null -> stateByCode[explicitUf]
      matchedCity != null -> stateByCode[matchedCity.stateCode]
      matchedStateByName != null -> matchedStateByName
      else -> null
    }

    if (matchedCity != null && (resolvedState == null || resolvedState.code == matchedCity.stateCode)) {
      val st = stateByCode[matchedCity.stateCode]
      val uf = matchedCity.stateCode
      val label = "${matchedCity.displayName} - $uf"
      return ResolvedRegion(
        regionKey = "${uf.lowercase(Locale.ROOT)}_${slugify(matchedCity.displayName)}",
        regionLabel = label,
        cityName = matchedCity.displayName,
        stateCode = uf,
        macroRegion = st?.macroRegion ?: "Nacional",
        stateFactor = st?.factor ?: 1.0,
        urbanTierFactor = matchedCity.urbanFactor
      )
    }

    if (resolvedState != null) {
      val extractedCity = extractCityBeforeUf(
        rawText = candidate,
        uf = resolvedState.code,
        stateFullName = resolvedState.fullName
      )
      val cleanCity = extractedCity.takeIf {
        it.isNotBlank() && !normalizeToken(it).equals(normalizeToken(resolvedState.fullName), ignoreCase = true)
      }
      val label = if (cleanCity != null) {
        "$cleanCity - ${resolvedState.code}"
      } else {
        "${resolvedState.fullName} (${resolvedState.code})"
      }
      val key = if (cleanCity != null) {
        "${resolvedState.code.lowercase(Locale.ROOT)}_${slugify(cleanCity)}"
      } else {
        "${resolvedState.code.lowercase(Locale.ROOT)}_estado"
      }
      return ResolvedRegion(
        regionKey = key,
        regionLabel = label,
        cityName = cleanCity.orEmpty(),
        stateCode = resolvedState.code,
        macroRegion = resolvedState.macroRegion,
        stateFactor = resolvedState.factor,
        urbanTierFactor = if (cleanCity != null) 0.99 else 1.00
      )
    }

    // Não identificou UF/cidade conhecida -> usa rótulo curto da localidade com base nacional
    val shortLoc = candidate.split(",", "-", "/").lastOrNull()?.trim().orEmpty().take(36)
    return ResolvedRegion(
      regionKey = "br_nacional",
      regionLabel = if (shortLoc.length >= 3) "$shortLoc (Ref. Nacional)" else "Brasil (Média Nacional)",
      cityName = shortLoc,
      stateCode = "",
      macroRegion = "Nacional",
      stateFactor = 1.0,
      urbanTierFactor = 1.0
    )
  }

  /**
   * Calcula a estimativa regional diária para uma categoria e localização.
   *
   * - Atualiza automaticamente quando muda o dia ([dateKey]).
   * - Pondera pedidos reais da plataforma na mesma categoria e região (cidade > estado > macro-região).
   * - Pode integrar um benchmark diário sincronizado do Firestore ([cloudBenchmark]) quando for da mesma data.
   */
  fun estimate(
    category: String,
    locationText: String,
    fallbackCity: String = "",
    historicalServices: List<ServiceRequest> = emptyList(),
    cloudBenchmark: RegionalPriceEstimate? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): RegionalPriceEstimate {
    val cleanCategory = category.trim().ifBlank { "Serviço Geral" }
    val dateKey = todayDateKey(nowMillis)
    val displayDate = formatDisplayDate(dateKey)
    val region = resolveRegion(locationText, fallbackCity)
    val docId = benchmarkDocId(dateKey, region.regionKey, cleanCategory)

    val baseline = resolveCategoryBaseline(cleanCategory)
    val dailyFactor = computeDailyMarketFactor(
      dateKey = dateKey,
      stateCode = region.stateCode.ifBlank { "BR" },
      categoryKey = baseline.canonicalKey,
      nowMillis = nowMillis
    )

    val regionalBaseAverage = baseline.basePrice * region.combinedFactor * dailyFactor

    // Ponderação com pedidos reais da mesma categoria na região
    val sampleAccumulator = computeRegionalOrderSamples(
      category = cleanCategory,
      categoryKey = baseline.canonicalKey,
      region = region,
      services = historicalServices
    )

    val localBlendedAverage = if (sampleAccumulator.totalWeight > 0.0) {
      val sampleMean = sampleAccumulator.weightedPriceSum / sampleAccumulator.totalWeight
      // Clampa outliers extremos da amostra local (entre 0.55x e 1.85x da base regional)
      val clampedSampleMean = sampleMean.coerceIn(regionalBaseAverage * 0.55, regionalBaseAverage * 1.85)
      // Shrinkage Bayesiano: quanto mais pedidos reais na região, maior o peso dos dados locais (até 80%)
      val sampleShare = (sampleAccumulator.totalWeight / (sampleAccumulator.totalWeight + 5.0)).coerceIn(0.0, 0.80)
      (clampedSampleMean * sampleShare) + (regionalBaseAverage * (1.0 - sampleShare))
    } else {
      regionalBaseAverage
    }

    // Se houver benchmark diário de hoje sincronizado do Firestore para esta mesma região/categoria,
    // combina preservando a data atual e incorporando maior amostra quando disponível.
    val validCloud = cloudBenchmark?.takeIf {
      it.dateKey == dateKey &&
        it.averagePrice > 0.0 &&
        it.regionKey.equals(region.regionKey, ignoreCase = true)
    }

    val finalRawAverage: Double
    val finalSamplesCount: Int
    val isFromCloud: Boolean

    if (validCloud != null) {
      if (validCloud.localSamplesCount > sampleAccumulator.sampleCount) {
        finalRawAverage = (validCloud.averagePrice * 0.70) + (localBlendedAverage * 0.30)
        finalSamplesCount = validCloud.localSamplesCount
      } else if (sampleAccumulator.sampleCount > validCloud.localSamplesCount) {
        finalRawAverage = (localBlendedAverage * 0.70) + (validCloud.averagePrice * 0.30)
        finalSamplesCount = sampleAccumulator.sampleCount
      } else {
        finalRawAverage = (validCloud.averagePrice + localBlendedAverage) / 2.0
        finalSamplesCount = maxOf(validCloud.localSamplesCount, sampleAccumulator.sampleCount)
      }
      isFromCloud = true
    } else {
      finalRawAverage = localBlendedAverage
      finalSamplesCount = sampleAccumulator.sampleCount
      isFromCloud = false
    }

    val roundedAvg = roundToNearestFive(finalRawAverage).coerceAtLeast(40.0)
    val roundedMin = roundToNearestFive(roundedAvg * baseline.minRatio)
      .coerceAtLeast(30.0)
      .coerceAtMost(roundedAvg - 5.0)
    val roundedMax = roundToNearestFive(roundedAvg * baseline.maxRatio)
      .coerceAtLeast(roundedAvg + 10.0)

    val sourceLabel = when {
      finalSamplesCount > 0 && isFromCloud ->
        "Índice regional diário + $finalSamplesCount pedido(s) na região (nuvem)"
      finalSamplesCount > 0 ->
        "Índice regional diário + $finalSamplesCount pedido(s) na região"
      isFromCloud ->
        "Índice regional diário sincronizado"
      else ->
        "Índice regional diário da categoria"
    }

    return RegionalPriceEstimate(
      id = docId,
      dateKey = dateKey,
      displayDate = displayDate,
      category = cleanCategory,
      regionKey = region.regionKey,
      regionLabel = region.regionLabel,
      stateCode = region.stateCode,
      macroRegion = region.macroRegion,
      averagePrice = roundedAvg,
      minPrice = roundedMin,
      maxPrice = roundedMax,
      regionalFactor = ((region.combinedFactor * dailyFactor) * 100.0).roundToInt() / 100.0,
      localSamplesCount = finalSamplesCount,
      sourceLabel = sourceLabel,
      syncedFromCloud = isFromCloud,
      updatedAt = nowMillis
    )
  }

  fun compareProposedToRegional(
    proposedPrice: Double,
    estimate: RegionalPriceEstimate
  ): RegionalPriceComparison {
    if (proposedPrice <= 0.0 || estimate.averagePrice <= 0.0) {
      return RegionalPriceComparison(RegionalPriceComparisonLevel.IN_RANGE, 0)
    }
    val pct = (((proposedPrice - estimate.averagePrice) / estimate.averagePrice) * 100.0).roundToInt()
    val level = when {
      proposedPrice < estimate.minPrice || pct <= -15 -> RegionalPriceComparisonLevel.BELOW_AVERAGE
      proposedPrice > estimate.maxPrice || pct >= 20 -> RegionalPriceComparisonLevel.ABOVE_AVERAGE
      else -> RegionalPriceComparisonLevel.IN_RANGE
    }
    return RegionalPriceComparison(level = level, percentDiff = pct)
  }

  fun formatCurrencyBrl(value: Double): String {
    return String.format(Locale("pt", "BR"), "R$ %.2f", value)
  }

  fun formatEditableDecimal(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
  }

  private data class SampleAccumulator(
    val weightedPriceSum: Double,
    val totalWeight: Double,
    val sampleCount: Int
  )

  private fun computeRegionalOrderSamples(
    category: String,
    categoryKey: String,
    region: ResolvedRegion,
    services: List<ServiceRequest>
  ): SampleAccumulator {
    if (services.isEmpty()) return SampleAccumulator(0.0, 0.0, 0)

    val normCategory = normalizeToken(category)
    var weightedSum = 0.0
    var totalWeight = 0.0
    var count = 0

    for (srv in services) {
      if (srv.status == ServiceStatus.CANCELADO) continue
      val effectivePrice = (srv.finalAgreedPrice ?: srv.proposedBudget)
      if (effectivePrice < 25.0 || effectivePrice > 25_000.0) continue

      val srvNormCat = normalizeToken(srv.category)
      val srvBaseline = resolveCategoryBaseline(srv.category)
      val categoryMatches = srvNormCat == normCategory ||
        srvNormCat.contains(normCategory) ||
        normCategory.contains(srvNormCat) ||
        (categoryKey != "geral" && srvBaseline.canonicalKey == categoryKey)

      if (!categoryMatches) continue

      val srvRegion = resolveRegion(srv.location)
      val isAgreed = srv.finalAgreedPrice != null ||
        srv.status == ServiceStatus.CONCLUIDO ||
        srv.status == ServiceStatus.EM_EXECUCAO ||
        srv.status == ServiceStatus.ACEITO

      val weight = when {
        region.cityName.isNotBlank() &&
          srvRegion.cityName.isNotBlank() &&
          normalizeToken(srvRegion.cityName) == normalizeToken(region.cityName) &&
          (region.stateCode.isBlank() || srvRegion.stateCode == region.stateCode) ->
          if (isAgreed) 3.0 else 1.6

        region.stateCode.isNotBlank() && srvRegion.stateCode == region.stateCode ->
          if (isAgreed) 2.0 else 1.0

        region.macroRegion != "Nacional" && srvRegion.macroRegion == region.macroRegion ->
          if (isAgreed) 0.9 else 0.45

        region.stateCode.isBlank() ->
          if (isAgreed) 0.7 else 0.35

        else -> 0.0
      }

      if (weight > 0.0) {
        weightedSum += effectivePrice * weight
        totalWeight += weight
        count++
      }
    }

    return SampleAccumulator(weightedSum, totalWeight, count)
  }

  private fun computeDailyMarketFactor(
    dateKey: String,
    stateCode: String,
    categoryKey: String,
    nowMillis: Long
  ): Double {
    val cal = Calendar.getInstance(brazilTimeZone).apply { timeInMillis = nowMillis }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Demanda ligeiramente maior em fins de semana (sábado/domingo) e segunda-feira
    val weekdayFactor = when (dayOfWeek) {
      Calendar.SATURDAY -> 1.025
      Calendar.SUNDAY -> 1.035
      Calendar.FRIDAY -> 1.010
      else -> 1.000
    }
    // Micro-ajuste diário determinístico por (data + UF + categoria) dentro de ±2.0%
    val seed = "$dateKey|$stateCode|$categoryKey"
    val hash = abs(seed.hashCode() % 41) // 0..40
    val dailyDrift = 0.98 + (hash * 0.001) // 0.980 .. 1.020
    return weekdayFactor * dailyDrift
  }

  private fun resolveCategoryBaseline(category: String): CategoryBaseline {
    val norm = normalizeToken(category)
    return categoryBaselines.firstOrNull { baseline ->
      baseline.keywords.any { kw -> norm.contains(kw) }
    } ?: CategoryBaseline(
      canonicalKey = slugify(category).ifBlank { "geral" },
      keywords = emptyList(),
      basePrice = 200.0,
      minRatio = 0.75,
      maxRatio = 1.30
    )
  }

  private fun findKnownCity(normalizedText: String): CityLookup? {
    // Prefere correspondências mais longas (ex.: "sao bernardo do campo" antes de "sao paulo")
    var bestMatch: Pair<Int, CityLookup>? = null
    for ((cityNorm, info) in majorCities) {
      if (containsWholePhrase(normalizedText, cityNorm)) {
        val score = cityNorm.length
        if (bestMatch == null || score > bestMatch.first) {
          bestMatch = score to info
        }
      }
    }
    return bestMatch?.second
  }

  private fun extractStateCode(rawText: String): String? {
    // Procura padrões típicos brasileiros: "- SP", "/SP", ", SP", "(SP)" ou UF no final da string
    val upper = rawText.uppercase(Locale.ROOT)
    val regex = Regex("""(?:[\s,\-/(]|^)([A-Z]{2})(?:\s*[\)\.,]?\s*$|(?:\s*-\s*BRASIL|\s*,\s*BRASIL|\s+BRASIL))""")
    val match = regex.find(upper)
    val code = match?.groupValues?.getOrNull(1)
    if (code != null && stateByCode.containsKey(code)) return code

    // Fallback: procura delimitador explícito "- UF" ou "/UF" no meio de endereço completo (ex: "Curitiba - PR, 80000-000")
    val midRegex = Regex("""(?:-|/|,\s*)\s*([A-Z]{2})(?=[\s,/\-\)]|$)""")
    val allMatches = midRegex.findAll(upper).toList()
    for (m in allMatches.asReversed()) {
      val candidate = m.groupValues.getOrNull(1)
      if (candidate != null && stateByCode.containsKey(candidate)) {
        return candidate
      }
    }
    return null
  }

  private fun extractCityBeforeUf(rawText: String, uf: String, stateFullName: String? = null): String {
    val parts = rawText.split("-", ",", "/")
      .map { it.trim() }
      .filter { it.isNotBlank() && !it.equals("Brasil", ignoreCase = true) && !it.matches(Regex("^\\d{5}-?\\d{3}$")) }

    val normalizedStateFull = stateFullName?.let { normalizeToken(it) }
    val ufIndex = parts.indexOfLast { part ->
      part.equals(uf, ignoreCase = true) ||
        (normalizedStateFull != null && normalizeToken(part) == normalizedStateFull)
    }
    if (ufIndex > 0) {
      val candidate = parts[ufIndex - 1].trim()
      if (candidate.length in 3..40 && candidate.none { it.isDigit() }) {
        return candidate
      }
    }
    return ""
  }

  private fun roundToNearestFive(value: Double): Double {
    return (value / 5.0).roundToInt() * 5.0
  }

  private fun containsWholePhrase(text: String, phrase: String): Boolean {
    if (phrase.isBlank()) return false
    val pattern = Regex("(?<![a-z0-9])${Regex.escape(phrase)}(?![a-z0-9])")
    return pattern.containsMatchIn(text)
  }

  internal fun normalizeToken(input: String): String {
    val decomposed = Normalizer.normalize(input.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    return decomposed
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  internal fun slugify(input: String): String {
    return normalizeToken(input)
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
  }
}
