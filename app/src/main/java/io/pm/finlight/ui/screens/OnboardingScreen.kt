// FILE: app/src/main/java/io/pm/finlight/ui/screens/OnboardingScreen.kt

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
    // Page count is 8 to include the new SMS info page
    val pagerState = rememberPagerState { 8 }
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
                viewModel = viewModel, // Pass ViewModel to control button state
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
            userScrollEnabled = false // Prevent swiping to enforce button logic
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> UserNamePage(viewModel)
                2 -> CategorySetupPage(viewModel)
                3 -> BudgetSetupPage(viewModel)
                4 -> SmsPermissionPage(onPermissionResult = onNextClicked)
                5 -> SmsScanningInfoPage()
                6 -> NotificationPermissionPage(onPermissionResult = onNextClicked)
                7 -> CompletionPage()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingBottomBar(
    pagerState: PagerState,
    viewModel: OnboardingViewModel, // ViewModel is needed to check the user's name
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
                    pagerState.currentPage != 4 && // Hide on SMS Permission Page
                    pagerState.currentPage != 6    // Hide on Notification Permission Page

            // --- UPDATED: Check if the user is on the name page and if the name is blank ---
            val isNextEnabled = if (pagerState.currentPage == 1) {
                userName.isNotBlank()
            } else {
                true
            }

            if (isNextButtonVisible) {
                Button(
                    onClick = onNextClicked,
                    enabled = isNextEnabled // Button is disabled if name is blank on the name page
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
