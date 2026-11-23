package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.widthIn
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.DocumentAttachmentHelper
import com.example.data.ProcessedDocumentAttachment
import com.example.data.ProcessedVideoAttachment
import com.example.data.ProviderShowcaseHelper
import com.example.model.ClientCompletedServicePhoto
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private open class SafeTakePictureContract : ActivityResultContracts.TakePicture() {
  override fun createIntent(context: Context, input: Uri): Intent {
    return super.createIntent(context, input).apply {
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
      clipData = ClipData.newRawUri("photo", input)
    }
  }
}

private class TakeSelfiePictureContract : SafeTakePictureContract() {
  override fun createIntent(context: Context, input: Uri): Intent {
    return super.createIntent(context, input).apply {
      putExtra("android.intent.extras.CAMERA_FACING", 1)
      putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
      putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
    }
  }
}

private fun grantCameraUriPermissions(context: Context, contentUri: Uri, intentAction: String) {
  try {
    val probe = Intent(intentAction)
    val handlers = context.packageManager.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
    for (resolveInfo in handlers) {
      val packageName = resolveInfo.activityInfo?.packageName ?: continue
      try {
        context.grantUriPermission(
          packageName,
          contentUri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
      } catch (_: Throwable) {
      }
    }
  } catch (_: Throwable) {
  }
}

@Composable
fun rememberSelfieCameraCaptureLauncher(
  docKey: String,
  onCaptured: (ProcessedDocumentAttachment) -> Unit,
  onPermissionDenied: () -> Unit = {},
  onCaptureFailed: () -> Unit = {}
): () -> Unit {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var pendingFilePath by rememberSaveable(docKey) { mutableStateOf<String?>(null) }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = TakeSelfiePictureContract()
  ) { success ->
    val resolvedPath = pendingFilePath ?: DocumentAttachmentHelper.consumePendingCapturePath(context, docKey)
    pendingFilePath = null
    DocumentAttachmentHelper.savePendingCapturePath(context, docKey, null)
    val file = resolvedPath?.let { File(it) }
    if (success && file != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processCapturedSelfieFile(
            context = context,
            captureFile = file,
            docKey = docKey
          )
        }
        if (processed != null) {
          onCaptured(processed)
        } else {
          Toast.makeText(context, "Não foi possível processar a foto capturada. Tente novamente.", Toast.LENGTH_SHORT).show()
          onCaptureFailed()
        }
      }
    } else {
      try {
        file?.delete()
      } catch (_: Throwable) {
      }
    }
  }

  fun startCameraCapture() {
    val target = DocumentAttachmentHelper.createSelfieCaptureTarget(context, docKey)
    if (target == null) {
      Toast.makeText(context, "Não foi possível abrir a câmera agora.", Toast.LENGTH_SHORT).show()
      onCaptureFailed()
      return
    }
    pendingFilePath = target.file.absolutePath
    DocumentAttachmentHelper.savePendingCapturePath(context, docKey, target.file.absolutePath)
    grantCameraUriPermissions(context, target.contentUri, android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    try {
      takePictureLauncher.launch(target.contentUri)
    } catch (_: Throwable) {
      pendingFilePath = null
      DocumentAttachmentHelper.savePendingCapturePath(context, docKey, null)
      try {
        target.file.delete()
      } catch (_: Throwable) {
      }
      Toast.makeText(context, "Nenhum aplicativo de câmera disponível neste aparelho.", Toast.LENGTH_LONG).show()
      onCaptureFailed()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      startCameraCapture()
    } else {
      Toast.makeText(
        context,
        "Permissão da câmera necessária para fotografar na hora.",
        Toast.LENGTH_LONG
      ).show()
      onPermissionDenied()
    }
  }

  return {
    try {
      val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
      if (hasCameraPermission) {
        startCameraCapture()
      } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    } catch (_: Throwable) {
      Toast.makeText(context, "Não foi possível solicitar acesso à câmera.", Toast.LENGTH_SHORT).show()
      onCaptureFailed()
    }
  }
}

@Composable
fun rememberCameraPhotoCaptureLauncher(
  docKey: String,
  fileNamePrefix: String = "foto_camera",
  permissionDeniedMessage: String = "Permissão da câmera necessária para tirar a foto.",
  onBeforeLaunchCamera: () -> Unit = {},
  onCaptured: (ProcessedDocumentAttachment) -> Unit,
  onPermissionDenied: () -> Unit = {},
  onCaptureFailed: () -> Unit = {}
): () -> Unit {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var pendingFilePath by rememberSaveable(docKey) { mutableStateOf<String?>(null) }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = SafeTakePictureContract()
  ) { success ->
    val resolvedPath = pendingFilePath ?: DocumentAttachmentHelper.consumePendingCapturePath(context, docKey)
    pendingFilePath = null
    DocumentAttachmentHelper.savePendingCapturePath(context, docKey, null)
    val file = resolvedPath?.let { File(it) }
    if (success && file != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processCapturedPhotoFile(
            context = context,
            captureFile = file,
            docKey = docKey,
            fileNamePrefix = fileNamePrefix
          )
        }
        if (processed != null) {
          onCaptured(processed)
        } else {
          Toast.makeText(context, "Não foi possível salvar a foto tirada. Tente novamente.", Toast.LENGTH_SHORT).show()
          onCaptureFailed()
        }
      }
    } else {
      try {
        file?.delete()
      } catch (_: Throwable) {
      }
    }
  }

  fun startCameraCapture() {
    val target = DocumentAttachmentHelper.createSelfieCaptureTarget(context, docKey)
    if (target == null) {
      Toast.makeText(context, "Não foi possível abrir a câmera agora.", Toast.LENGTH_SHORT).show()
      onCaptureFailed()
      return
    }
    pendingFilePath = target.file.absolutePath
    DocumentAttachmentHelper.savePendingCapturePath(context, docKey, target.file.absolutePath)
    onBeforeLaunchCamera()
    grantCameraUriPermissions(context, target.contentUri, android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    try {
      takePictureLauncher.launch(target.contentUri)
    } catch (_: Throwable) {
      pendingFilePath = null
      DocumentAttachmentHelper.savePendingCapturePath(context, docKey, null)
      try {
        target.file.delete()
      } catch (_: Throwable) {
      }
      Toast.makeText(context, "Nenhum aplicativo de câmera disponível neste aparelho.", Toast.LENGTH_LONG).show()
      onCaptureFailed()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      startCameraCapture()
    } else {
      Toast.makeText(
        context,
        permissionDeniedMessage,
        Toast.LENGTH_LONG
      ).show()
      onPermissionDenied()
    }
  }

  return {
    try {
      val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
      if (hasCameraPermission) {
        startCameraCapture()
      } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    } catch (_: Throwable) {
      Toast.makeText(context, "Não foi possível solicitar acesso à câmera.", Toast.LENGTH_SHORT).show()
      onCaptureFailed()
    }
  }
}

@Composable
fun rememberCameraVideoCaptureLauncher(
  docKey: String,
  fileNamePrefix: String = "video_camera",
  permissionDeniedMessage: String = "Permissão da câmera necessária para gravar o vídeo.",
  onCaptured: (ProcessedVideoAttachment) -> Unit,
  onPermissionDenied: () -> Unit = {},
  onCaptureFailed: () -> Unit = {}
): () -> Unit {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var pendingFilePath by rememberSaveable(docKey) { mutableStateOf<String?>(null) }

  val captureVideoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CaptureVideo()
  ) { success ->
    val resolvedPath = pendingFilePath ?: DocumentAttachmentHelper.consumePendingCapturePath(context, docKey)
    pendingFilePath = null
    DocumentAttachmentHelper.savePendingCapturePath(context, docKey, null)
    val file = resolvedPath?.let { File(it) }
    if (success && file != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processCapturedVideoFile(
            context = context,
            captureFile = file,
            docKey = docKey,
            fileNamePrefix = fileNamePrefix
          )
        }
        if (processed != null) {
          onCaptured(processed)
        } else {
          Toast.makeText(context, "Não foi possível processar o vídeo gravado.", Toast.LENGTH_SHORT).show()
          onCaptureFailed()
        }
      }
    } else {
      try {
        file?.delete()
      } catch (_: Throwable) {
      }
    }
  }

  fun startVideoCapture() {
    val target = DocumentAttachmentHelper.createVideoCaptureTarget(context, docKey)
    if (target == null) {
      Toast.makeText(context, "Não foi possível abrir a câmera de vídeo agora.", Toast.LENGTH_SHORT).show()
      onCaptureFailed()
      return
    }
    pendingFilePath = target.file.absolutePath
    DocumentAttachmentHelper.savePendingCapturePath(context, docKey, target.file.absolutePath)
    grantCameraUriPermissions(context, target.contentUri, android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
    try {
      captureVideoLauncher.launch(target.contentUri)
    } catch (_: Throwable) {
      pendingFilePath = null
      DocumentAttachmentHelper.savePendingCapturePath(context, docKey, null)
      try {
        target.file.delete()
      } catch (_: Throwable) {
      }
      Toast.makeText(context, "Nenhum aplicativo de vídeo disponível neste aparelho.", Toast.LENGTH_LONG).show()
      onCaptureFailed()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      startVideoCapture()
    } else {
      Toast.makeText(
        context,
        permissionDeniedMessage,
        Toast.LENGTH_LONG
      ).show()
      onPermissionDenied()
    }
  }

  return {
    try {
      val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
      if (hasCameraPermission) {
        startVideoCapture()
      } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    } catch (_: Throwable) {
      Toast.makeText(context, "Não foi possível solicitar acesso à câmera.", Toast.LENGTH_SHORT).show()
      onCaptureFailed()
    }
  }
}

@Composable
fun DocumentAttachmentPreviewImage(
  attachmentUri: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
  fallbackIcon: ImageVector = Icons.Default.Description,
  accentColor: Color = MaterialTheme.colorScheme.primary
) {
  val context = LocalContext.current
  val bundledDrawableRes = remember(attachmentUri) {
    ProviderShowcaseHelper.resolveBundledDrawableRes(attachmentUri)
  }
  val isRemoteUrl = !attachmentUri.isNullOrBlank() &&
    (attachmentUri.startsWith("http://", ignoreCase = true) || attachmentUri.startsWith("https://", ignoreCase = true))

  val decodedBitmap by produceState<ImageBitmap?>(initialValue = null, attachmentUri) {
    value = if (attachmentUri.isNullOrBlank() || isRemoteUrl || bundledDrawableRes != null) {
      null
    } else {
      withContext(Dispatchers.IO) {
        DocumentAttachmentHelper.decodeBitmap(context, attachmentUri)?.asImageBitmap()
      }
    }
  }

  when {
    bundledDrawableRes != null -> {
      Image(
        painter = painterResource(id = bundledDrawableRes),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
      )
    }
    isRemoteUrl -> {
      AsyncImage(
        model = attachmentUri,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
      )
    }
    decodedBitmap != null -> {
      Image(
        bitmap = decodedBitmap!!,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
      )
    }
    else -> {
      Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = fallbackIcon,
            contentDescription = contentDescription,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

@Composable
fun ProviderDocumentAttachmentCard(
  title: String,
  subtitle: String,
  fileName: String,
  attachmentUri: String?,
  buttonLabel: String,
  buttonTag: String,
  onAttachClick: () -> Unit,
  onClearClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonIcon: ImageVector = Icons.Default.UploadFile,
  reattachLabel: String = "Trocar Anexo",
  helperBadgeText: String? = null
) {
  val isAttached = !attachmentUri.isNullOrBlank()

  Surface(
    color = if (isAttached) EmeraldWalletLight.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      width = 1.dp,
      color = if (isAttached) EmeraldWallet.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (isAttached) {
          DocumentAttachmentPreviewImage(
            attachmentUri = attachmentUri,
            contentDescription = "Prévia do anexo $title",
            accentColor = EmeraldWallet,
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(8.dp))
          )
        } else {
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (isAttached) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldWallet,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Anexado: ${fileName.ifBlank { "documento" }}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF065F46),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          } else {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (isAttached) {
          IconButton(
            onClick = onClearClick,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover anexo $title",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      if (!helperBadgeText.isNullOrBlank()) {
        Surface(
          color = InfoBlueLight.copy(alpha = 0.7f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = InfoBlue,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = helperBadgeText,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = InfoBlue,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      OutlinedButton(
        onClick = onAttachClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
          1.dp,
          if (isAttached) EmeraldWallet else MaterialTheme.colorScheme.primary
        ),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = if (isAttached) EmeraldWallet else MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag(buttonTag)
      ) {
        Icon(
          imageVector = buttonIcon,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isAttached) reattachLabel else buttonLabel,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun ServicePhotoAttachmentPickerCard(
  title: String,
  subtitle: String,
  docKey: String,
  fileNamePrefix: String,
  photoUri: String?,
  photoName: String,
  galleryButtonTag: String,
  cameraButtonTag: String,
  removeButtonTag: String,
  previewTag: String,
  onPhotoChanged: (uri: String?, name: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var cameraPermissionDenied by remember { mutableStateOf(false) }
  var showZoomDialog by remember { mutableStateOf(false) }

  val galleryPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      DocumentAttachmentHelper.processPickedAttachment(context, uri, docKey)?.let { processed ->
        cameraPermissionDenied = false
        onPhotoChanged(processed.attachmentUri, processed.fileName)
      } ?: Toast.makeText(
        context,
        "Não foi possível processar a imagem selecionada.",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  val launchCamera = rememberCameraPhotoCaptureLauncher(
    docKey = docKey,
    fileNamePrefix = fileNamePrefix,
    permissionDeniedMessage = "Permissão da câmera negada. Escolha uma foto pela Galeria.",
    onCaptured = { processed ->
      cameraPermissionDenied = false
      onPhotoChanged(processed.attachmentUri, processed.fileName)
    },
    onPermissionDenied = { cameraPermissionDenied = true },
    onCaptureFailed = {
      Toast.makeText(
        context,
        "Não foi possível capturar a foto agora. Tente novamente ou use a Galeria.",
        Toast.LENGTH_SHORT
      ).show()
    }
  )

  val hasPhoto = !photoUri.isNullOrBlank()

  Surface(
    color = if (hasPhoto) EmeraldWalletLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(
      width = 1.dp,
      color = if (hasPhoto) EmeraldWallet.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (hasPhoto) {
              "Foto anexada: ${photoName.ifBlank { "imagem.jpg" }}"
            } else {
              subtitle
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (hasPhoto) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (hasPhoto) {
          IconButton(
            onClick = { onPhotoChanged(null, "") },
            modifier = Modifier
              .size(48.dp)
              .testTag(removeButtonTag)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover foto anexada",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      if (hasPhoto) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { showZoomDialog = true }
            .testTag(previewTag)
        ) {
          DocumentAttachmentPreviewImage(
            attachmentUri = photoUri,
            contentDescription = "Prévia da foto anexada",
            contentScale = ContentScale.Crop,
            accentColor = EmeraldWallet,
            modifier = Modifier.fillMaxSize()
          )
          Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Ampliar",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            galleryPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag(galleryButtonTag)
        ) {
          Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Galeria",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = { launchCamera() },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, EmeraldWallet),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldWallet),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag(cameraButtonTag)
        ) {
          Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Abrir Câmera",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (cameraPermissionDenied) {
        Text(
          text = "Permissão da câmera negada. Selecione uma imagem pela Galeria ou libere a câmera nas configurações.",
          style = MaterialTheme.typography.labelSmall,
          color = WarningAmber
        )
      }
    }
  }

  if (showZoomDialog && hasPhoto) {
    ServicePhotoFullScreenDialog(
      title = title,
      photoUri = photoUri!!,
      photoName = photoName,
      onDismiss = { showZoomDialog = false }
    )
  }
}

@Composable
fun ServicePhotoFullScreenDialog(
  title: String,
  photoUri: String,
  photoName: String? = null,
  subtitleNote: String? = null,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("service_photo_fullscreen_dialog")
      ) {
        DocumentAttachmentPreviewImage(
          attachmentUri = photoUri,
          contentDescription = title,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 380.dp)
            .clip(RoundedCornerShape(12.dp))
        )
        if (!photoName.isNullOrBlank()) {
          Text(
            text = photoName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (!subtitleNote.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = subtitleNote,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar", fontWeight = FontWeight.Bold)
      }
    }
  )
}

@Composable
fun ServiceVideoAttachmentPickerCard(
  title: String,
  subtitle: String,
  docKey: String,
  fileNamePrefix: String,
  videoUri: String?,
  videoName: String,
  videoThumbnailUri: String?,
  videoDurationMs: Long?,
  galleryButtonTag: String,
  cameraButtonTag: String,
  removeButtonTag: String,
  previewTag: String,
  onVideoChanged: (uri: String?, name: String, thumbnailUri: String?, durationMs: Long?) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var cameraPermissionDenied by remember { mutableStateOf(false) }
  var showPlayerDialog by remember { mutableStateOf(false) }

  val galleryVideoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      DocumentAttachmentHelper.processPickedVideo(context, uri, docKey)?.let { processed ->
        cameraPermissionDenied = false
        onVideoChanged(
          processed.attachmentUri,
          processed.fileName,
          processed.thumbnailUri,
          processed.durationMs
        )
      } ?: Toast.makeText(
        context,
        "Não foi possível processar o vídeo selecionado.",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  val launchVideoCamera = rememberCameraVideoCaptureLauncher(
    docKey = docKey,
    fileNamePrefix = fileNamePrefix,
    permissionDeniedMessage = "Permissão da câmera negada. Selecione um vídeo pela Galeria.",
    onCaptured = { processed ->
      cameraPermissionDenied = false
      onVideoChanged(
        processed.attachmentUri,
        processed.fileName,
        processed.thumbnailUri,
        processed.durationMs
      )
    },
    onPermissionDenied = { cameraPermissionDenied = true },
    onCaptureFailed = {
      Toast.makeText(
        context,
        "Não foi possível gravar o vídeo agora. Tente novamente ou use a Galeria.",
        Toast.LENGTH_SHORT
      ).show()
    }
  )

  val hasVideo = !videoUri.isNullOrBlank()
  val durationLabel = remember(videoDurationMs) {
    DocumentAttachmentHelper.formatVideoDuration(videoDurationMs)
  }

  Surface(
    color = if (hasVideo) InfoBlueLight.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(
      width = 1.dp,
      color = if (hasVideo) InfoBlue.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (hasVideo) {
              "Vídeo anexado: ${videoName.ifBlank { "video.mp4" }} ($durationLabel)"
            } else {
              subtitle
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (hasVideo) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (hasVideo) {
          IconButton(
            onClick = { onVideoChanged(null, "", null, null) },
            modifier = Modifier
              .size(48.dp)
              .testTag(removeButtonTag)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover vídeo anexado",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      if (hasVideo) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .clickable { showPlayerDialog = true }
            .testTag(previewTag)
        ) {
          if (!videoThumbnailUri.isNullOrBlank()) {
            DocumentAttachmentPreviewImage(
              attachmentUri = videoThumbnailUri,
              contentDescription = "Prévia do vídeo anexado",
              contentScale = ContentScale.Crop,
              accentColor = InfoBlue,
              modifier = Modifier.fillMaxSize()
            )
          }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
          ) {
            Surface(
              color = Color.Black.copy(alpha = 0.65f),
              shape = CircleShape,
              modifier = Modifier.size(48.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Reproduzir vídeo",
                  tint = Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }
            }
          }
          Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Assistir ($durationLabel)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            galleryVideoPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag(galleryButtonTag)
        ) {
          Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Vídeo Galeria",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = { launchVideoCamera() },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, InfoBlue),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoBlue),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag(cameraButtonTag)
        ) {
          Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Gravar Vídeo",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (cameraPermissionDenied) {
        Text(
          text = "Permissão da câmera negada. Selecione um vídeo pela Galeria ou libere a câmera nas configurações.",
          style = MaterialTheme.typography.labelSmall,
          color = WarningAmber
        )
      }
    }
  }

  if (showPlayerDialog && hasVideo) {
    ServiceVideoPlayerDialog(
      title = title,
      videoUri = videoUri!!,
      videoName = videoName,
      thumbnailUri = videoThumbnailUri,
      durationMs = videoDurationMs,
      onDismiss = { showPlayerDialog = false }
    )
  }
}

@Composable
fun ServiceVideoPlayerDialog(
  title: String,
  videoUri: String,
  videoName: String? = null,
  thumbnailUri: String? = null,
  durationMs: Long? = null,
  subtitleNote: String? = null,
  onDismiss: () -> Unit
) {
  val durationLabel = remember(durationMs) {
    DocumentAttachmentHelper.formatVideoDuration(durationMs)
  }
  val isPlayableSource = remember(videoUri) {
    when {
      videoUri.startsWith("http://", ignoreCase = true) ||
        videoUri.startsWith("https://", ignoreCase = true) ||
        videoUri.startsWith("content://", ignoreCase = true) -> true
      videoUri.startsWith("file://", ignoreCase = true) -> {
        val path = Uri.parse(videoUri).path
        path != null && File(path).let { it.exists() && it.length() > 0L }
      }
      videoUri.startsWith("/") -> {
        File(videoUri).let { it.exists() && it.length() > 0L }
      }
      else -> false
    }
  }
  var playbackError by remember { mutableStateOf(false) }
  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

  DisposableEffect(videoUri) {
    onDispose {
      videoViewRef?.stopPlayback()
      videoViewRef = null
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    title = {
      Text(
        text = "$title ($durationLabel)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("service_video_player_dialog")
      ) {
        if (isPlayableSource && !playbackError) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black),
            contentAlignment = Alignment.Center
          ) {
            AndroidView(
              factory = { ctx ->
                VideoView(ctx).apply {
                  val controller = MediaController(ctx)
                  controller.setAnchorView(this)
                  setMediaController(controller)
                  setVideoURI(Uri.parse(videoUri))
                  setOnPreparedListener { mp ->
                    mp.isLooping = false
                    start()
                  }
                  setOnErrorListener { _, _, _ ->
                    playbackError = true
                    true
                  }
                  videoViewRef = this
                }
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(190.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
          ) {
            if (!thumbnailUri.isNullOrBlank()) {
              DocumentAttachmentPreviewImage(
                attachmentUri = thumbnailUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                accentColor = InfoBlue,
                modifier = Modifier.fillMaxSize()
              )
            }
            Surface(
              color = Color.Black.copy(alpha = 0.72f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.padding(14.dp)
            ) {
              Text(
                text = "Vídeo registrado ($durationLabel). O arquivo completo permanece salvo no dispositivo de origem.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
        }

        if (!videoName.isNullOrBlank()) {
          Text(
            text = videoName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (!subtitleNote.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = subtitleNote,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar", fontWeight = FontWeight.Bold)
      }
    }
  )
}

@Composable
fun ClientProfilePhotoPickerCard(
  photoUri: String?,
  onPhotoSelected: (String) -> Unit,
  onClearPhoto: () -> Unit,
  title: String = "Foto de Perfil do Cliente (Opcional)",
  subtitle: String = "Escolha uma foto da galeria ou tire na hora com a câmera.",
  onBeforeLaunchPicker: () -> Unit = {},
  onLaunchCameraOverride: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processPickedAttachment(context, uri, "client_profile")
        }
        if (processed != null) {
          onPhotoSelected(processed.localFileUri)
        } else {
          Toast.makeText(context, "Não foi possível carregar a foto selecionada.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  val defaultLaunchCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "client_profile_cam",
    fileNamePrefix = "perfil_cliente",
    permissionDeniedMessage = "Permissão da câmera necessária para tirar sua foto de perfil.",
    onBeforeLaunchCamera = onBeforeLaunchPicker,
    onCaptured = { processed ->
      onPhotoSelected(processed.localFileUri)
    }
  )
  val launchCamera = onLaunchCameraOverride ?: defaultLaunchCamera

  Surface(
    color = if (!photoUri.isNullOrBlank()) EmeraldWalletLight.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(
      1.dp,
      if (!photoUri.isNullOrBlank()) EmeraldWallet.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("client_profile_photo_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          if (!photoUri.isNullOrBlank()) {
            DocumentAttachmentPreviewImage(
              attachmentUri = photoUri,
              contentDescription = "Prévia da foto de perfil do cliente",
              contentScale = ContentScale.Crop,
              fallbackIcon = Icons.Default.Person,
              accentColor = EmeraldWallet,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Sem foto de perfil",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (!photoUri.isNullOrBlank()) {
          IconButton(
            onClick = onClearPhoto,
            modifier = Modifier.testTag("btn_client_profile_clear")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover foto de perfil",
              tint = DangerRed
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            try {
              onBeforeLaunchPicker()
              galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            } catch (_: Throwable) {
              Toast.makeText(context, "Não foi possível abrir a galeria neste aparelho.", Toast.LENGTH_SHORT).show()
            }
          },
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_client_profile_gallery")
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Galeria", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }

        Button(
          onClick = { launchCamera() },
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_client_profile_camera")
        ) {
          Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Câmera", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}

@Composable
fun ProviderCameraProfilePhotoCard(
  photoUri: String?,
  onPhotoCaptured: (String) -> Unit,
  onClearPhoto: () -> Unit = {},
  onBeforeLaunchCamera: () -> Unit = {},
  onLaunchCameraOverride: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val defaultLaunchCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "provider_profile_cam",
    fileNamePrefix = "perfil_prestador",
    permissionDeniedMessage = "Permissão da câmera necessária para tirar a foto de perfil obrigatória do prestador.",
    onBeforeLaunchCamera = onBeforeLaunchCamera,
    onCaptured = { processed ->
      onPhotoCaptured(processed.localFileUri)
    }
  )
  val launchCamera = onLaunchCameraOverride ?: defaultLaunchCamera

  val hasPhoto = !photoUri.isNullOrBlank()
  Surface(
    color = if (hasPhoto) EmeraldWalletLight.copy(alpha = 0.65f) else WarningAmberLight.copy(alpha = 0.7f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(
      1.2.dp,
      if (hasPhoto) EmeraldWallet else WarningAmber
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("provider_camera_profile_photo_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (hasPhoto) EmeraldWalletLight else Color.White),
          contentAlignment = Alignment.Center
        ) {
          if (hasPhoto) {
            DocumentAttachmentPreviewImage(
              attachmentUri = photoUri,
              contentDescription = "Foto de perfil do prestador capturada na câmera",
              contentScale = ContentScale.Crop,
              fallbackIcon = Icons.Default.PhotoCamera,
              accentColor = EmeraldWallet,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              imageVector = Icons.Default.PhotoCamera,
              contentDescription = "Foto de perfil obrigatória na câmera",
              tint = WarningAmber,
              modifier = Modifier.size(26.dp)
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Foto de Perfil do Prestador *",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (hasPhoto) EmeraldWallet else Color(0xFF92400E)
          )
          Text(
            text = if (hasPhoto) {
              "✓ Foto tirada na câmera no ato do cadastro."
            } else {
              "Obrigatória no cadastro • Somente câmera na hora."
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (hasPhoto) EmeraldWallet else Color(0xFF78350F)
          )
        }

        if (hasPhoto) {
          IconButton(
            onClick = onClearPhoto,
            modifier = Modifier.testTag("btn_provider_profile_clear")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover foto de perfil do prestador",
              tint = DangerRed
            )
          }
        }
      }

      Surface(
        color = Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Somente câmera ao vivo • Galeria bloqueada para validação do perfil",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = Color(0xFF78350F)
          )
        }
      }

      Button(
        onClick = { launchCamera() },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (hasPhoto) EmeraldWallet else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_provider_profile_camera")
      ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (hasPhoto) "Tirar Nova Foto de Perfil" else "Abrir Câmera e Tirar Foto de Perfil *",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun AdminAttachProfilePhotoCard(
  photoUri: String?,
  isRequired: Boolean = false,
  onPhotoAttached: (String) -> Unit,
  onClearPhoto: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processPickedAttachment(context, uri, "admin_profile")
        }
        if (processed != null) {
          onPhotoAttached(processed.localFileUri)
        } else {
          Toast.makeText(context, "Não foi possível anexar a foto selecionada.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val hasPhoto = !photoUri.isNullOrBlank()
  Surface(
    color = when {
      hasPhoto -> EmeraldWalletLight.copy(alpha = 0.65f)
      isRequired -> WarningAmberLight.copy(alpha = 0.65f)
      else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    },
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(
      1.dp,
      when {
        hasPhoto -> EmeraldWallet
        isRequired -> WarningAmber
        else -> MaterialTheme.colorScheme.outlineVariant
      }
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("admin_attach_profile_photo_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(if (hasPhoto) EmeraldWalletLight else MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          if (hasPhoto) {
            DocumentAttachmentPreviewImage(
              attachmentUri = photoUri,
              contentDescription = "Prévia da foto de perfil anexada",
              contentScale = ContentScale.Crop,
              fallbackIcon = Icons.Default.Person,
              accentColor = EmeraldWallet,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Sem foto de perfil anexada",
              tint = if (isRequired) WarningAmber else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isRequired) "Foto de Perfil *" else "Foto de Perfil (Opcional)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = when {
              hasPhoto -> EmeraldWallet
              isRequired -> Color(0xFF92400E)
              else -> MaterialTheme.colorScheme.onSurface
            }
          )
          Text(
            text = if (hasPhoto) {
              "✓ Foto de perfil anexada ao cadastro."
            } else {
              "Anexe uma foto de perfil da galeria ou arquivos do aparelho."
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = when {
              hasPhoto -> EmeraldWallet
              isRequired -> Color(0xFF78350F)
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }

        if (hasPhoto) {
          IconButton(
            onClick = onClearPhoto,
            modifier = Modifier.testTag("btn_admin_attach_profile_clear")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover foto de perfil anexada",
              tint = DangerRed
            )
          }
        }
      }

      Button(
        onClick = {
          try {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          } catch (_: Throwable) {
            Toast.makeText(context, "Não foi possível abrir o seletor de fotos.", Toast.LENGTH_SHORT).show()
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (hasPhoto) EmeraldWallet else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("btn_admin_attach_profile_photo")
      ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (hasPhoto) {
            "Trocar Foto Anexada"
          } else if (isRequired) {
            "Anexar Foto de Perfil *"
          } else {
            "Anexar Foto de Perfil"
          },
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun ClientCompletedPhotosSection(
  photos: List<ClientCompletedServicePhoto>,
  canEdit: Boolean,
  onAddPhoto: (photoUri: String, photoName: String, caption: String) -> Unit,
  onRemovePhoto: (photoId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var previewPhoto by remember { mutableStateOf<ClientCompletedServicePhoto?>(null) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      DocumentAttachmentHelper.processPickedAttachment(context, uri, "client_completed")?.let { processed ->
        onAddPhoto(processed.attachmentUri, processed.fileName, "")
      }
    }
  }
  val launchCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "client_completed_cam",
    fileNamePrefix = "servico_concluido_cliente",
    permissionDeniedMessage = "Permissão da câmera necessária para fotografar o serviço concluído.",
    onCaptured = { processed ->
      onAddPhoto(processed.attachmentUri, processed.fileName, "")
    }
  )

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.35f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("client_completed_photos_section")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "📸 Fotos do Cliente após Conclusão (${photos.size}/6)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = EmeraldWallet
          )
          Text(
            text = if (canEdit) {
              "O prestador finalizou o serviço. Adicione fotos do resultado final pela galeria ou câmera."
            } else {
              "Registros fotográficos adicionados pelo cliente após a conclusão."
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (photos.isEmpty()) {
        Surface(
          color = InfoBlueLight.copy(alpha = 0.65f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (canEdit) {
              "Nenhuma foto enviada ainda. Use os botões abaixo para registrar o serviço concluído."
            } else {
              "O cliente ainda não adicionou fotos deste serviço concluído."
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = InfoBlue,
            modifier = Modifier.padding(10.dp)
          )
        }
      } else {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(photos, key = { it.id }) { photo ->
            Box(
              modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { previewPhoto = photo }
                .testTag("client_completed_photo_item_${photo.id}")
            ) {
              DocumentAttachmentPreviewImage(
                attachmentUri = photo.photoUri,
                contentDescription = photo.photoName.ifBlank { "Foto do serviço concluído" },
                contentScale = ContentScale.Crop,
                fallbackIcon = Icons.Default.PhotoLibrary,
                accentColor = EmeraldWallet,
                modifier = Modifier.fillMaxSize()
              )
              if (canEdit) {
                Surface(
                  color = Color.Black.copy(alpha = 0.65f),
                  shape = CircleShape,
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clickable { onRemovePhoto(photo.id) }
                    .testTag("btn_remove_client_photo_${photo.id}")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Remover foto do serviço",
                      tint = Color.White,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (canEdit && photos.size < 6) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_client_completed_photo_gallery")
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Galeria", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
          }

          Button(
            onClick = { launchCamera() },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_client_completed_photo_camera")
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Abrir Câmera", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }
      }
    }
  }

  previewPhoto?.let { activePhoto ->
    AlertDialog(
      onDismissRequest = { previewPhoto = null },
      properties = DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .widthIn(max = 520.dp),
      title = {
        Text(
          text = activePhoto.photoName.ifBlank { "Foto do Serviço Concluído" },
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      },
      text = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          DocumentAttachmentPreviewImage(
            attachmentUri = activePhoto.photoUri,
            contentDescription = activePhoto.photoName,
            contentScale = ContentScale.Fit,
            fallbackIcon = Icons.Default.PhotoLibrary,
            accentColor = EmeraldWallet,
            modifier = Modifier.fillMaxSize()
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { previewPhoto = null }) {
          Text("Fechar", fontWeight = FontWeight.Bold)
        }
      }
    )
  }
}
