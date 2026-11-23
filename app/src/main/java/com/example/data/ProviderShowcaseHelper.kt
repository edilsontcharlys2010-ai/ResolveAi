package com.example.data

import androidx.annotation.DrawableRes
import com.example.R
import com.example.model.ProviderServicePhoto
import com.example.model.ProviderVerificationStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole

data class ProviderShowcaseEntry(
  val providerId: String,
  val providerName: String,
  val providerCategory: String,
  val providerCity: String,
  val providerBio: String,
  val providerPhotoUrl: String?,
  val providerAvatarColorHex: Long,
  val providerRating: Double,
  val completedJobs: Int,
  val photos: List<ProviderServicePhoto>
) {
  val featuredPhoto: ProviderServicePhoto get() = photos.first()
}

object ProviderShowcaseHelper {

  const val DRAWABLE_URI_PREFIX = "drawable://"

  @DrawableRes
  fun resolveBundledDrawableRes(uri: String?): Int? {
    if (uri.isNullOrBlank() || !uri.startsWith(DRAWABLE_URI_PREFIX, ignoreCase = true)) {
      return null
    }
    val key = uri.substringAfter(DRAWABLE_URI_PREFIX).trim().lowercase()
    return when (key) {
      "img_showcase_eletrica" -> R.drawable.img_showcase_eletrica
      "img_showcase_pintura" -> R.drawable.img_showcase_pintura
      "img_showcase_redes" -> R.drawable.img_showcase_redes
      "img_avatar_roberto" -> R.drawable.img_avatar_roberto
      "img_avatar_anaclara" -> R.drawable.img_avatar_anaclara
      "resolveai_logo" -> R.drawable.resolveai_logo
      else -> null
    }
  }

  /**
   * Monta a vitrine promocional da tela de pedidos combinando:
   * 1) Fotos de conclusão dos serviços realizados (`ServiceStatus.CONCLUIDO` + `completionPhotoUri`)
   * 2) Portfólio de fotos de serviços cadastrados no perfil dos prestadores aprovados
   * mantendo cada prestador único com seu nome, foto de perfil e nota.
   */
  fun buildShowcaseEntries(
    users: List<User>,
    services: List<ServiceRequest>,
    categoryFilter: String = "Serviços"
  ): List<ProviderShowcaseEntry> {
    val approvedProviders = users.filter {
      it.role == UserRole.PRESTADOR && it.verificationStatus == ProviderVerificationStatus.APROVADO
    }

    val completedServicePhotosByProvider = mutableMapOf<String, MutableList<ProviderServicePhoto>>()
    val fallbackProviderMeta = mutableMapOf<String, ServiceRequest>()

    services
      .asSequence()
      .filter { it.status == ServiceStatus.CONCLUIDO && !it.acceptedProviderId.isNullOrBlank() }
      .sortedByDescending { it.completedAt ?: it.createdAt }
      .forEach { srv ->
        val providerId = srv.acceptedProviderId!!.trim()
        fallbackProviderMeta.putIfAbsent(providerId, srv)
        val servicePhoto = srv.completionPhotoUri?.takeIf { it.isNotBlank() }
          ?: srv.attachmentPhotoUri?.takeIf { it.isNotBlank() }
        if (!servicePhoto.isNullOrBlank()) {
          val matchingProvider = approvedProviders.find { it.ownsId(providerId) }
          val canonicalKey = matchingProvider?.id ?: providerId
          val list = completedServicePhotosByProvider.getOrPut(canonicalKey) { mutableListOf() }
          list += ProviderServicePhoto(
            id = "srv_photo_${srv.id}",
            title = srv.title,
            category = srv.category,
            photoUri = servicePhoto,
            createdAt = srv.completedAt ?: srv.createdAt
          )
        }
      }

    val entriesByProviderId = linkedMapOf<String, ProviderShowcaseEntry>()

    approvedProviders.forEach { provider ->
      val fromCompleted = completedServicePhotosByProvider[provider.id].orEmpty()
      val mergedPhotos = (fromCompleted + provider.portfolioPhotos)
        .filter { it.photoUri.isNotBlank() }
        .distinctBy { it.photoUri }
        .sortedByDescending { it.createdAt }
      if (mergedPhotos.isNotEmpty()) {
        entriesByProviderId[provider.id] = ProviderShowcaseEntry(
          providerId = provider.id,
          providerName = provider.name,
          providerCategory = provider.category.ifBlank { mergedPhotos.first().category },
          providerCity = provider.city,
          providerBio = provider.bio,
          providerPhotoUrl = provider.photoUrl,
          providerAvatarColorHex = provider.avatarColorHex,
          providerRating = provider.rating,
          completedJobs = maxOf(provider.completedJobs, fromCompleted.size),
          photos = mergedPhotos
        )
      }
    }

    completedServicePhotosByProvider.forEach { (providerId, srvPhotos) ->
      if (!entriesByProviderId.containsKey(providerId) && srvPhotos.isNotEmpty()) {
        val srv = fallbackProviderMeta[providerId]
        val cleanName = srv?.acceptedProviderName?.takeIf { it.isNotBlank() } ?: "Prestador ResolveAi"
        entriesByProviderId[providerId] = ProviderShowcaseEntry(
          providerId = providerId,
          providerName = cleanName,
          providerCategory = srv?.category ?: srvPhotos.first().category,
          providerCity = srv?.effectiveLocation?.city ?: "",
          providerBio = "Profissional com serviços concluídos e avaliados na plataforma.",
          providerPhotoUrl = srv?.acceptedProviderPhotoUrl,
          providerAvatarColorHex = srv?.acceptedProviderAvatarColorHex ?: 0xFF0D9488,
          providerRating = srv?.acceptedProviderRating ?: 5.0,
          completedJobs = srvPhotos.size,
          photos = srvPhotos.distinctBy { it.photoUri }
        )
      }
    }

    val normalizedFilter = categoryFilter.trim()
    val hasCategoryPriority = normalizedFilter.isNotBlank() &&
      normalizedFilter !in setOf("Serviços", "Todos", "Concluído", "Cancelado")

    return entriesByProviderId.values.sortedWith(
      compareByDescending<ProviderShowcaseEntry> { entry ->
        if (!hasCategoryPriority) 0
        else if (
          entry.providerCategory.contains(normalizedFilter, ignoreCase = true) ||
          entry.photos.any { it.category.contains(normalizedFilter, ignoreCase = true) }
        ) 1 else 0
      }
        .thenByDescending { it.providerRating }
        .thenByDescending { it.completedJobs }
    )
  }
}
