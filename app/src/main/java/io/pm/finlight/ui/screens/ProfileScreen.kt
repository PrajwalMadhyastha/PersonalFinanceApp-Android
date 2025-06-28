// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/ProfileScreen.kt
// REASON: Updated to provide an explicit "Save" button after cropping an image,
// improving the user experience and making the action clearer.
// FIX: Added explicit theming to the CropImageOptions to ensure the cropper's
// toolbar icons (like the checkmark) are always visible.
// FIX 4: Corrected @Composable invocation error by moving all theme color access
// to the top-level composable scope, outside the clickable lambda.
// =================================================================================
package io.pm.finlight.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import io.pm.finlight.ProfileViewModel
import io.pm.finlight.R

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val savedProfilePictureUri by viewModel.profilePictureUri.collectAsState()
    val context = LocalContext.current

    // --- FIX 4: Resolve all colors from the theme in the correct composable scope ---
    val toolbarColor = MaterialTheme.colorScheme.primary.toArgb()
    val toolbarTintColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val activityBackgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val cropperLabelTextColor = MaterialTheme.colorScheme.onSurface.toArgb()


    // --- NEW: State to hold the newly cropped (but not yet saved) image URI ---
    var tempCroppedUri by remember { mutableStateOf<Uri?>(null) }

    val imageCropper = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // --- UPDATED: Don't save immediately. Store in temporary state. ---
            tempCroppedUri = result.uriContent
        } else {
            val exception = result.error
            Toast.makeText(context, "Image cropping failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- NEW: Determine which URI to display ---
    val displayUri = tempCroppedUri ?: savedProfilePictureUri

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Center the content
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = displayUri,
                        contentDescription = "User Profile Picture",
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                        error = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                // Now we use the pre-resolved color values
                                val cropOptions = CropImageContractOptions(
                                    uri = null,
                                    cropImageOptions = CropImageOptions(
                                        cropShape = CropImageView.CropShape.OVAL,
                                        aspectRatioX = 1,
                                        aspectRatioY = 1,
                                        fixAspectRatio = true,
                                        outputCompressQuality = 70,
                                        imageSourceIncludeGallery = true,
                                        imageSourceIncludeCamera = true,
                                        activityTitle = "Crop Profile Picture",
                                        activityMenuIconColor = toolbarTintColor,
                                        toolbarColor = toolbarColor,
                                        toolbarBackButtonColor = toolbarTintColor,
                                        activityBackgroundColor = activityBackgroundColor,
                                        cropperLabelTextColor = cropperLabelTextColor
                                    )
                                )
                                imageCropper.launch(cropOptions)
                            }
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(userName, style = MaterialTheme.typography.titleLarge)
                        Text("user.email@pm.com", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // --- NEW: Show Save/Cancel buttons only when there's a new image ---
                if (tempCroppedUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        OutlinedButton(onClick = { tempCroppedUri = null }) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            viewModel.saveProfilePictureUri(tempCroppedUri)
                            tempCroppedUri = null // Reset the temp state
                            Toast.makeText(context, "Profile picture saved!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Save Image")
                        }
                    }
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
