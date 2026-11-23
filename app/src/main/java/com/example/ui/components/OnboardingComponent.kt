package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandTertiary
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch

enum class OnboardingSlideTopic {
  PROPOSALS,
  COUNTER_OFFERS,
  PRIVATE_CHAT
}

data class OnboardingSlide(
  val id: String,
  val topic: OnboardingSlideTopic,
  val stepLabel: String,
  val title: String,
  val description: String,
  val highlights: List<String>,
  val heroIcon: ImageVector,
  val accentColor: Color,
  val softAccentBackground: Color
)

object OnboardingSlidesCatalog {
  val slides: List<OnboardingSlide> = listOf(
    OnboardingSlide(
      id = "slide_proposals",
      topic = OnboardingSlideTopic.PROPOSALS,
      stepLabel = "1 DE 3 • PROPOSTAS",
      title = "Envie ou receba propostas sob medida",
      description = "Clientes publicam o pedido com fotos, vídeo e orçamento inicial. Prestadores avaliam os detalhes e enviam propostas com preço justo e prazo estimado.",
      highlights = listOf(
        "Pedidos detalhados com fotos, vídeo e nível de urgência",
        "Propostas claras com prazo em dias e avaliação do profissional",
        "Compare opções facilmente antes de confirmar"
      ),
      heroIcon = Icons.Default.RequestQuote,
      accentColor = InfoBlue,
      softAccentBackground = InfoBlueLight
    ),
    OnboardingSlide(
      id = "slide_counter_offers",
      topic = OnboardingSlideTopic.COUNTER_OFFERS,
      stepLabel = "2 DE 3 • CONTRAPROPOSTAS",
      title = "Negocie valores com contrapropostas",
      description = "Quer ajustar o orçamento ou o prazo? Cliente e prestador podem enviar contrapropostas diretamente no pedido até chegarem ao acordo ideal.",
      highlights = listOf(
        "Envie um novo valor com justificativa em poucos toques",
        "Acompanhe todo o histórico de negociação em tempo real",
        "Quando ambas as partes concordam, o serviço é confirmado na hora"
      ),
      heroIcon = Icons.Default.SwapHoriz,
      accentColor = WarningAmber,
      softAccentBackground = WarningAmberLight
    ),
    OnboardingSlide(
      id = "slide_private_chat",
      topic = OnboardingSlideTopic.PRIVATE_CHAT,
      stepLabel = "3 DE 3 • CHAT PRIVADO",
      title = "Chat privado com proteção de dados",
      description = "Após o aceite da proposta, o chat privado entre cliente e prestador é liberado automaticamente com bloqueio anti-vazamento de contatos externos.",
      highlights = listOf(
        "Proteção ativa contra compartilhamento de telefone, e-mail e links",
        "Notificações instantâneas e avisos automáticos de conclusão",
        "Histórico seguro para combinar horários e acompanhar o serviço"
      ),
      heroIcon = Icons.Default.Chat,
      accentColor = EmeraldWallet,
      softAccentBackground = EmeraldWalletLight
    )
  )
}

@Composable
fun ResolveAiOnboardingComponent(
  onFinish: () -> Unit,
  onSkip: () -> Unit = onFinish,
  slides: List<OnboardingSlide> = OnboardingSlidesCatalog.slides,
  modifier: Modifier = Modifier
) {
  val safeSlides = remember(slides) { slides.ifEmpty { OnboardingSlidesCatalog.slides } }
  val pagerState = rememberPagerState(pageCount = { safeSlides.size })
  val coroutineScope = rememberCoroutineScope()
  val isLastSlide = pagerState.currentPage == safeSlides.lastIndex

  BackHandler {
    if (pagerState.currentPage > 0) {
      coroutineScope.launch {
        pagerState.animateScrollToPage(pagerState.currentPage - 1)
      }
    } else {
      onSkip()
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier
      .fillMaxSize()
      .testTag("onboarding_overlay")
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter
    ) {
      val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }
      val isCompactHeight = maxHeight < 660.dp
      val currentSlide = safeSlides[pagerState.currentPage]

      Column(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
      ) {
        // 1. Top Bar: Brand Identity & Skip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.horizontalPadding, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color.White,
              shadowElevation = 2.dp,
              modifier = Modifier.size(34.dp)
            ) {
              Image(
                painter = painterResource(id = R.drawable.resolveai_logo),
                contentDescription = "Logo ResolveAi",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxSize()
                  .clip(RoundedCornerShape(10.dp))
              )
            }
            Column {
              Text(
                text = "ResolveAi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Guia Rápido",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          TextButton(
            onClick = onSkip,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("btn_onboarding_skip")
          ) {
            Text(
              text = stringResource(R.string.onboarding_skip),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // 2. Swipeable 3-Slide HorizontalPager
        HorizontalPager(
          state = pagerState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("onboarding_pager")
        ) { pageIndex ->
          val slide = safeSlides[pageIndex]
          OnboardingSlidePage(
            slide = slide,
            pageIndex = pageIndex,
            horizontalPadding = spec.horizontalPadding,
            isCompactHeight = isCompactHeight
          )
        }

        // 3. Bottom Navigation Controls & Page Indicators
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = spec.horizontalPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Animated Dot Indicators
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              safeSlides.forEachIndexed { index, slide ->
                val isSelected = pagerState.currentPage == index
                val indicatorWidth by animateDpAsState(
                  targetValue = if (isSelected) 28.dp else 9.dp,
                  animationSpec = spring(stiffness = 450f),
                  label = "onboarding_dot_width"
                )
                Box(
                  modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(9.dp)
                    .width(indicatorWidth)
                    .clip(CircleShape)
                    .background(
                      if (isSelected) currentSlide.accentColor
                      else MaterialTheme.colorScheme.outlineVariant
                    )
                    .clickable {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                      }
                    }
                    .testTag("onboarding_indicator_$index")
                )
              }
            }

            // Action Buttons Row (48.dp touch targets)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn(),
                exit = fadeOut()
              ) {
                OutlinedButton(
                  onClick = {
                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                    coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                  },
                  shape = RoundedCornerShape(14.dp),
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_onboarding_prev")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.onboarding_previous),
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              Button(
                onClick = {
                  if (isLastSlide) {
                    onFinish()
                  } else {
                    coroutineScope.launch {
                      pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = currentSlide.accentColor),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .testTag("btn_onboarding_next")
              ) {
                Text(
                  text = if (isLastSlide) {
                    stringResource(R.string.onboarding_finish)
                  } else {
                    stringResource(R.string.onboarding_next)
                  },
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = if (isLastSlide) {
                    Icons.Default.CheckCircle
                  } else {
                    Icons.AutoMirrored.Filled.ArrowForward
                  },
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingSlidePage(
  slide: OnboardingSlide,
  pageIndex: Int,
  horizontalPadding: androidx.compose.ui.unit.Dp,
  isCompactHeight: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = horizontalPadding, vertical = 8.dp)
      .testTag("onboarding_slide_$pageIndex"),
    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 12.dp else 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Visual Hero Artwork Card (Layered Asymmetry + Custom Canvas Backdrop)
    OnboardingSlideHeroArtwork(
      slide = slide,
      compact = isCompactHeight
    )

    // Step Pill
    Surface(
      color = slide.softAccentBackground,
      shape = RoundedCornerShape(20.dp),
      border = BorderStroke(1.dp, slide.accentColor.copy(alpha = 0.35f))
    ) {
      Text(
        text = slide.stepLabel,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.8.sp
        ),
        color = slide.accentColor,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
      )
    }

    // Title & Description
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = slide.title,
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.ExtraBold,
          lineHeight = 28.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )
      Text(
        text = slide.description,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }

    // Feature Highlights List
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
      shape = RoundedCornerShape(18.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        slide.highlights.forEach { bullet ->
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = slide.accentColor,
              modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
            )
            Text(
              text = bullet,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
  }
}

@Composable
private fun OnboardingSlideHeroArtwork(
  slide: OnboardingSlide,
  compact: Boolean
) {
  val heroHeight = if (compact) 168.dp else 208.dp
  val gradientColors = when (slide.topic) {
    OnboardingSlideTopic.PROPOSALS -> listOf(BrandPrimaryVariant, BrandPrimary, InfoBlue)
    OnboardingSlideTopic.COUNTER_OFFERS -> listOf(Color(0xFF78350F), Color(0xFFB45309), BrandTertiary)
    OnboardingSlideTopic.PRIVATE_CHAT -> listOf(Color(0xFF064E3B), EmeraldWallet, BrandSecondary)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(heroHeight)
      .clip(RoundedCornerShape(24.dp))
      .background(Brush.linearGradient(colors = gradientColors))
  ) {
    // Decorative asymmetrical Canvas geometry
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = size.minDimension * 0.48f,
        center = Offset(size.width * 0.88f, size.height * 0.16f)
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.08f),
        radius = size.minDimension * 0.34f,
        center = Offset(size.width * 0.10f, size.height * 0.90f)
      )
    }

    // Topic-specific interactive-style visual mockup preview
    when (slide.topic) {
      OnboardingSlideTopic.PROPOSALS -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
              .align(Alignment.CenterStart)
              .fillMaxWidth(0.84f)
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Handshake,
                  contentDescription = null,
                  tint = InfoBlue,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "Nova Proposta Enviada",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF0F172A)
                )
              }
              Text(
                text = "R$ 220,00 • Prazo: 1 dia",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = EmeraldWallet
              )
              Text(
                text = "Inclui vistoria completa e garantia do serviço.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Surface(
            color = InfoBlueLight,
            shape = CircleShape,
            shadowElevation = 4.dp,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .offset(x = (-4).dp, y = (-4).dp)
              .size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = slide.heroIcon,
                contentDescription = "Ilustração de Propostas",
                tint = InfoBlue,
                modifier = Modifier.size(28.dp)
              )
            }
          }
        }
      }

      OnboardingSlideTopic.COUNTER_OFFERS -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              color = Color.White.copy(alpha = 0.94f),
              shape = RoundedCornerShape(14.dp),
              shadowElevation = 4.dp,
              modifier = Modifier.fillMaxWidth(0.82f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Proposta inicial",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFF475569)
                )
                Text(
                  text = "R$ 260,00",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF0F172A)
                )
              }
            }

            Surface(
              color = Color.White,
              shape = RoundedCornerShape(14.dp),
              shadowElevation = 6.dp,
              modifier = Modifier
                .align(Alignment.End)
                .fillMaxWidth(0.86f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Ilustração de Contrapropostas",
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Contraproposta acordada",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = WarningAmber
                  )
                }
                Text(
                  text = "R$ 230,00",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = EmeraldWallet
                )
              }
            }
          }
        }
      }

      OnboardingSlideTopic.PRIVATE_CHAT -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              color = Color.White.copy(alpha = 0.96f),
              shape = RoundedCornerShape(14.dp),
              shadowElevation = 4.dp,
              modifier = Modifier.fillMaxWidth(0.88f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = "Proteção anti-vazamento no chat",
                  tint = EmeraldWallet,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Chat privado liberado • Anti-vazamento ativo",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF065F46),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Surface(
              color = Color.White,
              shape = RoundedCornerShape(14.dp),
              shadowElevation = 6.dp,
              modifier = Modifier
                .align(Alignment.End)
                .fillMaxWidth(0.84f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = EmeraldWallet,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "\"Combinado! Chego amanhã às 09h pelo endereço do pedido.\"",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = Color(0xFF1E293B),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          Surface(
            color = EmeraldWalletLight,
            shape = CircleShape,
            shadowElevation = 3.dp,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = EmeraldWallet,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}
