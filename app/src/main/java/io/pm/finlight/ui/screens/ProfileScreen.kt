// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ProfileScreen.kt
// REASON: Simplified the screen by removing direct editing functionality. The main
// profile card is now clickable, navigating the user to a dedicated
// "Edit Profile" screen.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.pm.finlight.ProfileViewModel
import io.pm.finlight.R

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val savedProfilePictureUri by viewModel.profilePictureUri.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // --- UPDATED: The entire card is now clickable ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("edit_profile") },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = savedProfilePictureUri,
                        contentDescription = "User Profile Picture",
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                        error = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userName, style = MaterialTheme.typography.titleLarge)
                        Text("user.email@pm.com", style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ListItem(
            headlineContent = { Text("App Settings") },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            trailingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable {
                navController.navigate("settings_screen")
            }
        )
        HorizontalDivider()
    }
}
