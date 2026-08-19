package com.docket.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.docket.R
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.TintedButton
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketExtendedColors
import kotlinx.coroutines.launch

private data class OnboardingSlide(val title: String, val body: String)

/** Illustration card shadow — matches the handoff's `rgba(33,33,33,.14)` literally; a genuine
 *  one-off, not a shared elevation token (see Elevation.kt's docketCardShadow/docketNeutralShadow
 *  for the shared cases). */
private val IllustrationShadowColor = Color(0x24212121)

@Composable
private fun onboardingSlides(): List<OnboardingSlide> = listOf(
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide1_title),
        body = stringResource(R.string.onboarding_slide1_body)
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide2_title),
        body = stringResource(R.string.onboarding_slide2_body)
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide3_title),
        body = stringResource(R.string.onboarding_slide3_body)
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val onboardingSlides = onboardingSlides()
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                OnboardingSlideContent(onboardingSlides[page])
            }

            OnboardingDots(
                pageCount = onboardingSlides.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DocketSpacing.space24, end = DocketSpacing.space24, bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12)
            ) {
                TintedButton(text = stringResource(R.string.onboarding_skip), onClick = onFinished, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = stringResource(R.string.onboarding_next),
                    onClick = {
                        if (pagerState.currentPage == onboardingSlides.lastIndex) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustration()
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DocketSpacing.space24)
        )
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = DocketSpacing.space12)
                .widthIn(max = 300.dp)
        )
    }
}

@Composable
private fun OnboardingDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .background(
                        // surfaceContainerHighest is DocketSurfaceContainerHighestLight (#E0E0E0)
                        // — the handoff's exact inactive-dot gray, one step darker than the
                        // #EEEEEE "line" divider color outlineVariant maps to.
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun OnboardingIllustration() {
    val extended = docketExtendedColors()
    Box(
        modifier = Modifier.size(width = 270.dp, height = 290.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 16.dp)
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 2.dp, bottom = 38.dp)
                .size(18.dp)
                .background(extended.amber.copy(alpha = 0.5f), CircleShape)
        )
        Column(
            modifier = Modifier
                .size(width = 146.dp, height = 222.dp)
                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = IllustrationShadowColor, spotColor = IllustrationShadowColor)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Chip(MaterialTheme.colorScheme.primaryContainer)
                Chip(extended.coralContainer)
                Chip(extended.safeContainer)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Chip(extended.warningContainer)
                Chip(MaterialTheme.colorScheme.primaryContainer)
                Chip(MaterialTheme.colorScheme.surfaceContainerLowest)
            }
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .width(68.dp)
                    .height(5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
            )
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(10.dp))
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun RowScope.Chip(color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(26.dp)
            .background(color, RoundedCornerShape(9.dp))
    )
}
