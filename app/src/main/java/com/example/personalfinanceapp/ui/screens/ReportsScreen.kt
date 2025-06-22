package com.example.personalfinanceapp.com.example.personalfinanceapp.ui.screens

import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.personalfinanceapp.ReportsViewModel
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components.ChartLegend
import com.example.personalfinanceapp.com.example.personalfinanceapp.ui.components.GroupedBarChart
import com.github.mikephil.charting.charts.PieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: ReportsViewModel = viewModel()) {
    val pieData by viewModel.spendingByCategoryPieData.collectAsState(initial = null)
    val trendDataPair by viewModel.monthlyTrendData.collectAsState(initial = null)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Pie Chart Card (Unchanged) ---
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spending by Category for ${viewModel.monthYear}", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (pieData == null || pieData?.entryCount == 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Text("No expense data for this month.")
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    PieChart(context).apply {
                                        description.isEnabled = false; isDrawHoleEnabled = true; setHoleColor(
                                        Color.TRANSPARENT); setEntryLabelColor(Color.BLACK); setEntryLabelTextSize(12f); legend.isEnabled = false
                                    }
                                },
                                update = { chart -> chart.data = pieData; chart.invalidate() },
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ChartLegend(pieData)
                        }
                    }
                }
            }

            // --- Bar Chart Card (REFINED) ---
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income vs. Expense Trend", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (trendDataPair != null && trendDataPair!!.first.entryCount > 0) {
                            // This is the refined BarChart implementation
                            GroupedBarChart(trendDataPair!!)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                                Text("Not enough data for trend analysis.")
                            }
                        }
                    }
                }
            }
        }
    }
}