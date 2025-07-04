// =================================================================================
// FILE: app/src/main/java/io/pm/finlight/ui/screens/OnboardingScreen.kt
// REASON: BUG FIX - The PagerState is now passed down to the individual page
// composables (`UserNamePage`, `BudgetSetupPage`). This allows each page to
// know when it is the `currentPage` and request focus only when it is active,
// preventing focus from being stolen by off-screen pages.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.pm.finlight.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onOnboardingFinished: () -> Unit) {
    val pagerState = rememberPagerState { 7 }
    val scope = rememberCoroutineScope()

    val onNextClicked: () -> Unit = {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Scaffold(
        bottomBar = {
            OnboardingBottomBar(
                pagerState = pagerState,
                viewModel = viewModel,
                onNextClicked = onNextClicked,
                onFinishClicked = {
                    viewModel.finishOnboarding()
                    onOnboardingFinished()
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                // --- FIX: Pass pagerState to pages that need to manage focus ---
                1 -> UserNamePage(viewModel = viewModel, pagerState = pagerState)
                2 -> BudgetSetupPage(viewModel = viewModel, pagerState = pagerState)
                3 -> SmsPermissionPage(onPermissionResult = onNextClicked)
                4 -> SmsScanningInfoPage()
                5 -> NotificationPermissionPage(onPermissionResult = onNextClicked)
                6 -> CompletionPage()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingBottomBar(
    pagerState: PagerState,
    viewModel: OnboardingViewModel,
    onNextClicked: () -> Unit,
    onFinishClicked: () -> Unit
) {
    val userName by viewModel.userName.collectAsState()

    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PageIndicator(pageCount = pagerState.pageCount, currentPage = pagerState.currentPage)

            val isNextButtonVisible = pagerState.currentPage < pagerState.pageCount - 1 &&
                    pagerState.currentPage != 3 && // Hide on SMS Permission Page
                    pagerState.currentPage != 5    // Hide on Notification Permission Page

            val isNextEnabled = if (pagerState.currentPage == 1) {
                userName.isNotBlank()
            } else {
                true
            }

            if (isNextButtonVisible) {
                Button(
                    onClick = onNextClicked,
                    enabled = isNextEnabled
                ) {
                    Text("Next")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                }
            } else if (pagerState.currentPage == pagerState.pageCount - 1) {
                Button(onClick = onFinishClicked) {
                    Text("Finish Setup")
                }
            } else {
                Spacer(modifier = Modifier.width(0.dp))
            }
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { iteration ->
            val color = if (currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
