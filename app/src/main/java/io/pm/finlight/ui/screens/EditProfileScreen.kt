// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/EditProfileScreen.kt
// REASON: New screen to allow users to update their profile information
// (name and picture) after the initial onboarding.
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val currentName by viewModel.userName.collectAsState()
    val savedProfilePictureUri by viewModel.profilePictureUri.collectAsState()
    val context = LocalContext.current

    var editedName by remember(currentName) { mutableStateOf(currentName) }
    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }

    val displayUri = croppedImageUri ?: savedProfilePictureUri?.let { Uri.parse(it) }

    val toolbarColor = MaterialTheme.colorScheme.primary.toArgb()
    val toolbarTintColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val activityBackgroundColor = MaterialTheme.colorScheme.background.toArgb()

    val imageCropper = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            croppedImageUri = result.uriContent
        } else {
            val exception = result.error
            Toast.makeText(context, "Image cropping failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AsyncImage(
            model = displayUri,
            contentDescription = "User Profile Picture",
            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
            error = painterResource(id = R.drawable.ic_launcher_foreground),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable {
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
                            activityBackgroundColor = activityBackgroundColor
                        )
                    )
                    imageCropper.launch(cropOptions)
                }
        )

        OutlinedTextField(
            value = editedName,
            onValueChange = { editedName = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.updateUserName(editedName)
                    croppedImageUri?.let { viewModel.saveProfilePictureUri(it) }
                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f),
                enabled = editedName.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
