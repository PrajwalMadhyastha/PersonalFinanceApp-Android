// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/EditProfileScreen.kt
// REASON: FIX - Replaced the `Uri.parse(it)` call with the more idiomatic
// `it.toUri()` KTX extension function. This resolves the "AndroidLintUseKtx"
// warning and improves code readability.
// FIX - Removed a redundant `.let` call for converting the saved URI string,
// making the code more concise.
// =================================================================================
package io.pm.finlight.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import io.pm.finlight.ProfileViewModel
import io.pm.finlight.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // --- FIX: Removed redundant .let call ---
    val displayUri = croppedImageUri ?: savedProfilePictureUri?.toUri()

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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraImageUri?.let { uri ->
                val cropOptions = CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = createCropOptions(toolbarColor, toolbarTintColor, activityBackgroundColor)
                )
                imageCropper.launch(cropOptions)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val cropOptions = CropImageContractOptions(
                uri = it,
                cropImageOptions = createCropOptions(toolbarColor, toolbarTintColor, activityBackgroundColor)
            )
            imageCropper.launch(cropOptions)
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Change Profile Picture") },
            text = { Text("Choose a source for your new image.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        // --- FIX: Create the temp file and get its secure URI before launching the camera ---
                        val tempFile = createTempImageFile(context)
                        val newTempUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            tempFile
                        )
                        tempCameraImageUri = newTempUri
                        cameraLauncher.launch(newTempUri)
                    }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
        )
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
                    showImageSourceDialog = true
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

/**
 * Helper function to create the crop options for the image cropper.
 */
private fun createCropOptions(toolbarColor: Int, toolbarTintColor: Int, activityBackgroundColor: Int): CropImageOptions {
    return CropImageOptions(
        cropShape = CropImageView.CropShape.OVAL,
        aspectRatioX = 1,
        aspectRatioY = 1,
        fixAspectRatio = true,
        outputCompressQuality = 70,
        imageSourceIncludeGallery = false, // We handle this ourselves now
        imageSourceIncludeCamera = false, // We handle this ourselves now
        activityTitle = "Crop Profile Picture",
        activityMenuIconColor = toolbarTintColor,
        toolbarColor = toolbarColor,
        toolbarBackButtonColor = toolbarTintColor,
        activityBackgroundColor = activityBackgroundColor
    )
}

/**
 * Helper function to create a temporary image file in the app's external files directory.
 */
private fun createTempImageFile(context: Context): File {
    // --- FIX: Use the external files directory for better compatibility with the camera intent ---
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}
