package io.pm.finlight.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.pm.finlight.DailyReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(
    navController: NavController,
    viewModel: DailyReportViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val transactions by viewModel.dailyTransactions.collectAsState()
    val weeklyChartData by viewModel.weeklyBarChartData.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = dateFormatter.format(selectedDate.time),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row {
                        IconButton(onClick = { viewModel.selectPreviousDay() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                        }
                        IconButton(onClick = { viewModel.selectNextDay() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Daily Report Screen Content Goes Here")
        }
    }
}