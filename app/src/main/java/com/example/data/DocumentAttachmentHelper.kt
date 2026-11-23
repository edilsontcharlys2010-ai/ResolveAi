package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ProcessedDocumentAttachment(
  val fileName: String,
  val attachmentUri: String,
  val localFileUri: String = attachmentUri
)

data class ProcessedVideoAttachment(
  val fileName: String,
  val attachmentUri: String,
  val thumbnailUri: String? = null,
  val durationMs: Long = 0L
)

object DocumentAttachmentHelper {

  private const val MAX_DIMENSION_PX = 760
  private const val MAX_PREVIEW_DECODE_PX = 1024
  private const val MAX_BASE64_DATA_URI_LENGTH = 120_000
  private const val PENDING_CAPTURE_PREFS = "resolveai_pending_camera_captures"

  data class PendingAuthReturnState(
    val modeName: String,
    val stepName: String,
    val roleName: String? = null,
    val googleUid: String? = null,
    val googleEmail: String? = null,
    val googleName: String? = null,
    val googlePhoto: String? = null,
    val socialProviderLabel: String? = null,
    val pendingPhone: String? = null
  )

  private const val PENDING_AUTH_TTL_MS = 15L * 60L * 1000L

  fun savePendingAuthReturn(
    context: Context,
    modeName: String,
    stepName: String,
    roleName: String? = null,
    googleUid: String? = null,
    googleEmail: String? = null,
    googleName: String? = null,
    googlePhoto: String? = null,
    socialProviderLabel: String? = null,
    pendingPhone: String? = null
  ) {
    try {
      val prefs = context.applicationContext.getSharedPreferences(PENDING_CAPTURE_PREFS, Context.MODE_PRIVATE)
      prefs.edit()
        .putString("pending_auth_mode", modeName)
        .putString("pending_auth_step", stepName)
        .putString("pending_auth_role", roleName)
        .putString("pending_auth_google_uid", googleUid)
        .putString("pending_auth_google_email", googleEmail)
        .putString("pending_auth_google_name", googleName)
        .putString("pending_auth_google_photo", googlePhoto)
        .putString("pending_auth_social_provider", socialProviderLabel)
        .putString("pending_auth_phone", pendingPhone)
        .putLong("pending_auth_saved_at", System.currentTimeMillis())
        .commit()
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao salvar estado de retorno do cadastro: ${t.message}")
    }
  }

  fun peekPendingAuthReturn(context: Context): PendingAuthReturnState? {
    return try {
      val prefs = context.applicationContext.getSharedPreferences(PENDING_CAPTURE_PREFS, Context.MODE_PRIVATE)
      val savedAt = prefs.getLong("pending_auth_saved_at", 0L)
      if (savedAt <= 0L || (System.currentTimeMillis() - savedAt) > PENDING_AUTH_TTL_MS) {
        return null
      }
      val modeName = prefs.getString("pending_auth_mode", null)?.takeIf { it.isNotBlank() } ?: return null
      val stepName = prefs.getString("pending_auth_step", null)?.takeIf { it.isNotBlank() } ?: "FORM"
      PendingAuthReturnState(
        modeName = modeName,
        stepName = stepName,
        roleName = prefs.getString("pending_auth_role", null),
        googleUid = prefs.getString("pending_auth_google_uid", null),
        googleEmail = prefs.getString("pending_auth_google_email", null),
        googleName = prefs.getString("pending_auth_google_name", null),
        googlePhoto = prefs.getString("pending_auth_google_photo", null),
        socialProviderLabel = prefs.getString("pending_auth_social_provider", null),
        pendingPhone = prefs.getString("pending_auth_phone", null)
      )
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao ler estado de retorno do cadastro: ${t.message}")
      null
    }
  }

  fun clearPendingAuthReturn(context: Context) {
    try {
      val prefs = context.applicationContext.getSharedPreferences(PENDING_CAPTURE_PREFS, Context.MODE_PRIVATE)
      prefs.edit()
        .remove("pending_auth_mode")
        .remove("pending_auth_step")
        .remove("pending_auth_role")
        .remove("pending_auth_google_uid")
        .remove("pending_auth_google_email")
        .remove("pending_auth_google_name")
        .remove("pending_auth_google_photo")
        .remove("pending_auth_social_provider")
        .remove("pending_auth_phone")
        .remove("pending_auth_saved_at")
        .apply()
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao limpar estado de retorno do cadastro: ${t.message}")
    }
  }

  fun savePendingCapturePath(context: Context, docKey: String, filePath: String?) {
    try {
      val prefs = context.applicationContext.getSharedPreferences(PENDING_CAPTURE_PREFS, Context.MODE_PRIVATE)
      if (filePath.isNullOrBlank()) {
        prefs.edit().remove("pending_$docKey").apply()
      } else {
        prefs.edit().putString("pending_$docKey", filePath).commit()
      }
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao salvar caminho temporário ($docKey): ${t.message}")
    }
  }

  fun consumePendingCapturePath(context: Context, docKey: String): String? {
    return try {
      val prefs = context.applicationContext.getSharedPreferences(PENDING_CAPTURE_PREFS, Context.MODE_PRIVATE)
      val path = prefs.getString("pending_$docKey", null)
      prefs.edit().remove("pending_$docKey").apply()
      path?.takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao ler caminho temporário ($docKey): ${t.message}")
      null
    }
  }

  /**
   * Converts a compact local file:// image URI (safe to keep in rememberSaveable) into a
   * compressed data:image/jpeg;base64,... URI right before persisting to Firestore / repository.
   * Returns the input URI unchanged if it is already a data/http URI or if the file is not an image.
   */
  fun resolveForPersistence(context: Context, uri: String?): String? {
    if (uri.isNullOrBlank()) return uri
    if (uri.startsWith("data:", ignoreCase = true) ||
      uri.startsWith("http://", ignoreCase = true) ||
      uri.startsWith("https://", ignoreCase = true)
    ) {
      return uri
    }
    return try {
      val filePath = when {
        uri.startsWith("file://", ignoreCase = true) -> Uri.parse(uri).path
        uri.startsWith("/") -> uri
        else -> null
      } ?: return uri

      val file = File(filePath)
      if (!file.exists() || file.length() <= 0L) return uri
      if (file.extension.equals("pdf", ignoreCase = true)) {
        return renderPdfFirstPageToDataUri(file) ?: uri
      }
      val bitmap = decodeSampledBitmapFromFile(file.absolutePath, MAX_DIMENSION_PX) ?: return uri
      try {
        encodeBitmapToJpegDataUri(bitmap) ?: uri
      } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
      }
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao preparar anexo para persistência: ${t.message}")
      uri
    }
  }

  fun processPickedAttachment(
    context: Context,
    sourceUri: Uri,
    docKey: String
  ): ProcessedDocumentAttachment? {
    var tempFile: File? = null
    return try {
      val appContext = context.applicationContext
      val mimeType = appContext.contentResolver.getType(sourceUri).orEmpty().lowercase()
      val originalName = resolveDisplayName(appContext, sourceUri)
        ?: defaultNameFor(docKey, mimeType.contains("pdf"))

      val docsDir = File(appContext.filesDir, "provider_docs").apply {
        if (!exists()) mkdirs()
      }

      if (mimeType.contains("pdf") || originalName.endsWith(".pdf", ignoreCase = true)) {
        val localPdf = File(docsDir, "${docKey}_${System.currentTimeMillis()}.pdf")
        val copied = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
          FileOutputStream(localPdf).use { output -> input.copyTo(output) }
          true
        } ?: false
        if (!copied || !localPdf.exists() || localPdf.length() <= 0L) {
          localPdf.delete()
          return null
        }
        val localUri = "file://${localPdf.absolutePath}"
        val renderedPageDataUri = renderPdfFirstPageToDataUri(localPdf)
        ProcessedDocumentAttachment(
          fileName = originalName,
          attachmentUri = renderedPageDataUri ?: localUri,
          localFileUri = localUri
        )
      } else {
        // Stream to a temporary file on disk to avoid loading multi-MB images into RAM via readBytes()
        val cacheDir = File(appContext.cacheDir, "camera_captures").apply {
          if (!exists()) mkdirs()
        }
        val stagedFile = File(cacheDir, "${docKey}_picked_${System.currentTimeMillis()}.tmp")
        tempFile = stagedFile
        val copied = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
          FileOutputStream(stagedFile).use { output -> input.copyTo(output) }
          true
        } ?: false
        if (!copied || !stagedFile.exists() || stagedFile.length() <= 0L) {
          return null
        }

        val bitmap = decodeSampledBitmapFromFile(stagedFile.absolutePath, MAX_DIMENSION_PX)
        if (bitmap != null) {
          try {
            val localImg = File(docsDir, "${docKey}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(localImg).use { out ->
              bitmap.compress(Bitmap.CompressFormat.JPEG, 78, out)
            }
            val localUri = "file://${localImg.absolutePath}"
            val dataUri = encodeBitmapToJpegDataUri(bitmap)
            ProcessedDocumentAttachment(
              fileName = originalName,
              attachmentUri = dataUri ?: localUri,
              localFileUri = localUri
            )
          } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
          }
        } else {
          val localFile = File(docsDir, "${docKey}_${System.currentTimeMillis()}_${originalName.takeLast(20)}")
          FileInputStream(stagedFile).use { input ->
            FileOutputStream(localFile).use { output -> input.copyTo(output) }
          }
          val localUri = "file://${localFile.absolutePath}"
          ProcessedDocumentAttachment(
            fileName = originalName,
            attachmentUri = localUri,
            localFileUri = localUri
          )
        }
      }
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao processar anexo $docKey: ${t.message}")
      null
    } finally {
      try {
        tempFile?.delete()
      } catch (_: Throwable) {
      }
    }
  }

  data class SelfieCaptureTarget(
    val file: File,
    val contentUri: Uri
  )

  fun createSelfieCaptureTarget(
    context: Context,
    docKey: String
  ): SelfieCaptureTarget? {
    return try {
      val appContext = context.applicationContext
      val captureDir = File(appContext.cacheDir, "camera_captures").apply {
        if (!exists()) mkdirs()
      }
      val captureFile = File(captureDir, "${docKey}_camera_${System.currentTimeMillis()}.jpg")
      val authority = "${appContext.packageName}.fileprovider"
      val uri = FileProvider.getUriForFile(appContext, authority, captureFile)
      SelfieCaptureTarget(file = captureFile, contentUri = uri)
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao preparar captura de câmera para $docKey: ${t.message}")
      null
    }
  }

  fun processCapturedSelfieFile(
    context: Context,
    captureFile: File,
    docKey: String
  ): ProcessedDocumentAttachment? = processCapturedPhotoFile(
    context = context,
    captureFile = captureFile,
    docKey = docKey,
    fileNamePrefix = "selfie_camera"
  )

  fun processCapturedPhotoFile(
    context: Context,
    captureFile: File,
    docKey: String,
    fileNamePrefix: String = "foto_camera"
  ): ProcessedDocumentAttachment? {
    return try {
      if (!captureFile.exists() || captureFile.length() <= 0L) return null
      val appContext = context.applicationContext
      val docsDir = File(appContext.filesDir, "provider_docs").apply {
        if (!exists()) mkdirs()
      }
      val ts = System.currentTimeMillis()
      val displayName = "${fileNamePrefix}_$ts.jpg"

      // Decode directly from file with inSampleSize + EXIF rotation (never captureFile.readBytes()!)
      val bitmap = decodeSampledBitmapFromFile(captureFile.absolutePath, MAX_DIMENSION_PX)
      val localImg = File(docsDir, "${docKey}_$ts.jpg")
      val localUri = "file://${localImg.absolutePath}"

      val attachmentUri = if (bitmap != null) {
        try {
          FileOutputStream(localImg).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 78, out)
          }
          encodeBitmapToJpegDataUri(bitmap) ?: localUri
        } finally {
          if (!bitmap.isRecycled) bitmap.recycle()
        }
      } else {
        FileInputStream(captureFile).use { input ->
          FileOutputStream(localImg).use { output -> input.copyTo(output) }
        }
        localUri
      }

      try {
        captureFile.delete()
      } catch (_: Throwable) {
      }

      ProcessedDocumentAttachment(
        fileName = displayName,
        attachmentUri = attachmentUri,
        localFileUri = localUri
      )
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao processar foto capturada ($docKey): ${t.message}")
      null
    }
  }

  data class VideoCaptureTarget(
    val file: File,
    val contentUri: Uri
  )

  fun createVideoCaptureTarget(
    context: Context,
    docKey: String
  ): VideoCaptureTarget? {
    return try {
      val appContext = context.applicationContext
      val captureDir = File(appContext.cacheDir, "camera_captures").apply {
        if (!exists()) mkdirs()
      }
      val captureFile = File(captureDir, "${docKey}_video_${System.currentTimeMillis()}.mp4")
      val authority = "${appContext.packageName}.fileprovider"
      val uri = FileProvider.getUriForFile(appContext, authority, captureFile)
      VideoCaptureTarget(file = captureFile, contentUri = uri)
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao preparar gravação de vídeo para $docKey: ${t.message}")
      null
    }
  }

  fun processPickedVideo(
    context: Context,
    sourceUri: Uri,
    docKey: String
  ): ProcessedVideoAttachment? {
    return try {
      val appContext = context.applicationContext
      val originalName = resolveDisplayName(appContext, sourceUri)
        ?.let { name -> if (name.contains('.')) name else "$name.mp4" }
        ?: "video_${docKey}_${System.currentTimeMillis()}.mp4"

      val docsDir = File(appContext.filesDir, "provider_docs").apply {
        if (!exists()) mkdirs()
      }
      val localVideo = File(docsDir, "${docKey}_video_${System.currentTimeMillis()}.mp4")
      val copied = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(localVideo).use { output ->
          input.copyTo(output)
        }
        true
      } ?: false

      if (!copied || !localVideo.exists() || localVideo.length() <= 0L) {
        localVideo.delete()
        return null
      }

      val (thumbnailUri, durationMs) = extractVideoThumbnailAndDuration(appContext, localVideo.absolutePath)
      ProcessedVideoAttachment(
        fileName = originalName,
        attachmentUri = "file://${localVideo.absolutePath}",
        thumbnailUri = thumbnailUri,
        durationMs = durationMs
      )
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao processar vídeo selecionado ($docKey): ${t.message}")
      null
    }
  }

  fun processCapturedVideoFile(
    context: Context,
    captureFile: File,
    docKey: String,
    fileNamePrefix: String = "video_camera"
  ): ProcessedVideoAttachment? {
    return try {
      if (!captureFile.exists() || captureFile.length() <= 0L) return null
      val appContext = context.applicationContext
      val docsDir = File(appContext.filesDir, "provider_docs").apply {
        if (!exists()) mkdirs()
      }
      val ts = System.currentTimeMillis()
      val localVideo = File(docsDir, "${docKey}_video_$ts.mp4")
      FileInputStream(captureFile).use { input ->
        FileOutputStream(localVideo).use { output ->
          input.copyTo(output)
        }
      }
      captureFile.delete()
      if (!localVideo.exists() || localVideo.length() <= 0L) {
        localVideo.delete()
        return null
      }
      val (thumbnailUri, durationMs) = extractVideoThumbnailAndDuration(appContext, localVideo.absolutePath)
      ProcessedVideoAttachment(
        fileName = "${fileNamePrefix}_$ts.mp4",
        attachmentUri = "file://${localVideo.absolutePath}",
        thumbnailUri = thumbnailUri,
        durationMs = durationMs
      )
    } catch (t: Throwable) {
      Log.w("DocAttachment", "Erro ao processar vídeo gravado ($docKey): ${t.message}")
      null
    }
  }

  fun extractVideoThumbnailAndDuration(
    context: Context,
    videoPath: String
  ): Pair<String?, Long> {
    val retriever = MediaMetadataRetriever()
    return try {
      try {
        retriever.setDataSource(videoPath)
      } catch (_: Exception) {
        retriever.setDataSource(context, Uri.fromFile(File(videoPath)))
      }
      val durationMs = retriever
        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        ?.toLongOrNull()
        ?.coerceAtLeast(0L)
        ?: 0L
      val targetTimeUs = if (durationMs > 0L) {
        minOf(500_000L, (durationMs * 1000L) / 3L)
      } else {
        0L
      }
      val frame = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: retriever.getFrameAtTime(-1L)
      val thumbnailUri = frame?.let { bmp ->
        val encoded = encodeBitmapToJpegDataUri(bmp)
        if (!bmp.isRecycled) bmp.recycle()
        encoded
      }
      thumbnailUri to durationMs
    } catch (_: Throwable) {
      null to 0L
    } finally {
      try {
        retriever.release()
      } catch (_: Throwable) {
      }
    }
  }

  fun formatVideoDuration(durationMs: Long?): String {
    val totalSec = ((durationMs ?: 0L) / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d", mins, secs)
  }

  fun decodeBitmap(context: Context, attachmentUri: String?): Bitmap? {
    if (attachmentUri.isNullOrBlank()) return null
    return try {
      when {
        attachmentUri.startsWith("data:image", ignoreCase = true) -> {
          val base64Part = attachmentUri.substringAfter("base64,", "")
          if (base64Part.isBlank()) return null
          val bytes = Base64.decode(base64Part, Base64.DEFAULT)
          decodeSampledBitmapFromBytes(bytes, MAX_PREVIEW_DECODE_PX)
        }
        attachmentUri.startsWith("file://") -> {
          val path = Uri.parse(attachmentUri).path ?: return null
          decodeSampledBitmapFromFile(path, MAX_PREVIEW_DECODE_PX)
        }
        attachmentUri.startsWith("/") -> {
          decodeSampledBitmapFromFile(attachmentUri, MAX_PREVIEW_DECODE_PX)
        }
        attachmentUri.startsWith("content://") -> {
          context.contentResolver.openInputStream(Uri.parse(attachmentUri))?.use { stream ->
            BitmapFactory.decodeStream(stream)
          }
        }
        else -> null
      }
    } catch (_: Throwable) {
      null
    }
  }

  private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return try {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
          } else null
        } ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
      null
    }
  }

  private fun defaultNameFor(docKey: String, isPdf: Boolean): String {
    val ext = if (isPdf) "pdf" else "jpg"
    return "${docKey}_${System.currentTimeMillis()}.$ext"
  }

  private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var sampleSize = 1
    var w = width
    var h = height
    while (w / 2 >= maxDim || h / 2 >= maxDim) {
      w /= 2
      h /= 2
      sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
  }

  private fun decodeSampledBitmapFromFile(filePath: String, maxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(filePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
    val opts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    val decoded = BitmapFactory.decodeFile(filePath, opts) ?: return null
    val scaled = scaleDownIfNeeded(decoded, maxDim)
    if (scaled !== decoded && !decoded.isRecycled) {
      decoded.recycle()
    }
    val rotated = applyExifOrientationIfNeeded(filePath, scaled)
    if (rotated !== scaled && !scaled.isRecycled) {
      scaled.recycle()
    }
    return rotated
  }

  private fun applyExifOrientationIfNeeded(filePath: String, source: Bitmap): Bitmap {
    return try {
      val exif = ExifInterface(filePath)
      val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
      )
      val matrix = Matrix()
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
          matrix.postRotate(90f)
          matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
          matrix.postRotate(270f)
          matrix.postScale(-1f, 1f)
        }
        else -> return source
      }
      Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    } catch (_: Throwable) {
      source
    }
  }

  private fun decodeSampledBitmapFromBytes(bytes: ByteArray, maxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
    val opts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
    val scaled = scaleDownIfNeeded(decoded, maxDim)
    if (scaled !== decoded && !decoded.isRecycled) {
      decoded.recycle()
    }
    return scaled
  }

  private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val maxCurrent = maxOf(bitmap.width, bitmap.height)
    if (maxCurrent <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / maxCurrent.toFloat()
    val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  private fun encodeBitmapToJpegDataUri(source: Bitmap): String? {
    var working = scaleDownIfNeeded(source, MAX_DIMENSION_PX)
    var ownsWorking = working !== source
    try {
      var quality = 72
      while (quality >= 35) {
        val baos = ByteArrayOutputStream()
        working.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64"
        if (dataUri.length <= MAX_BASE64_DATA_URI_LENGTH) {
          return dataUri
        }
        quality -= 12
        if (quality < 45 && maxOf(working.width, working.height) > 480) {
          val smaller = scaleDownIfNeeded(working, 480)
          if (ownsWorking && smaller !== working && !working.isRecycled) {
            working.recycle()
          }
          working = smaller
          ownsWorking = smaller !== source
        }
      }
      return null
    } finally {
      if (ownsWorking && !working.isRecycled) {
        working.recycle()
      }
    }
  }

  private fun renderPdfFirstPageToDataUri(pdfFile: File): String? {
    return try {
      ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
          if (renderer.pageCount <= 0) return null
          renderer.openPage(0).use { page ->
            val scale = (MAX_DIMENSION_PX.toFloat() / maxOf(page.width, page.height)).coerceAtMost(2.0f)
            val w = (page.width * scale).toInt().coerceAtLeast(120)
            val h = (page.height * scale).toInt().coerceAtLeast(120)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            try {
              bitmap.eraseColor(Color.WHITE)
              page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
              encodeBitmapToJpegDataUri(bitmap)
            } finally {
              if (!bitmap.isRecycled) bitmap.recycle()
            }
          }
        }
      }
    } catch (_: Throwable) {
      null
    }
  }
}
