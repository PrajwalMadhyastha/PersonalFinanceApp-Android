package io.pm.finlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.pm.finlight.ui.theme.FinlightTheme
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import io.pm.finlight.ui.viewmodel.DashboardViewModel
import io.pm.finlight.ui.viewmodel.DashboardViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinlightTheme {
                // A very simple placeholder UI to ensure the build passes.
                // We will restore the full navigation and UI next.
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(application)
                )
                Greeting(name = "Finlight User")
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FinlightTheme {
        Greeting("Android")
    }
}
