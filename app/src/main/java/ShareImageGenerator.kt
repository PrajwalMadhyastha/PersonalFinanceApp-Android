// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/utils/ShareImageGenerator.kt
// REASON: FIX - Resolved a runtime crash (IllegalStateException: Cannot locate
// windowRecomposer) by properly attaching the off-screen ComposeView to the
// Activity's window before rendering it to a bitmap. The view is now added to
// a temporary container within the root view hierarchy, measured, drawn, and
// then immediately removed in a 'finally' block to prevent memory leaks.
// =================================================================================
package io.pm.finlight.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import io.pm.finlight.BuildConfig
import io.pm.finlight.TransactionDetails
import io.pm.finlight.ui.components.ShareableField
import io.pm.finlight.ui.theme.PersonalFinanceAppTheme
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ShareImageGenerator {

    /**
     * Renders the TransactionSnapshotContent composable to a Bitmap.
     */
    private fun createBitmapFromComposable(
        context: Context,
        transactions: List<TransactionDetails>,
        fields: Set<ShareableField>
    ): Bitmap {
        // Ensure the context is an Activity to get the window
        val activity = context as? Activity
            ?: throw IllegalArgumentException("A valid Activity context is required to generate an image.")

        // The root view to which we'll temporarily attach our composable
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val composeView = ComposeView(context).apply {
            setContent {
                PersonalFinanceAppTheme { // Apply the app's theme
                    TransactionSnapshotContent(transactions = transactions, fields = fields)
                }
            }
        }

        // A temporary container for the ComposeView
        val container = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(composeView)
        }

        try {
            // Add the container to the view hierarchy to attach it to the window
            root.addView(container)

            // Measure and layout the container
            container.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            container.layout(0, 0, container.measuredWidth, container.measuredHeight)

            // Create the bitmap and draw the view onto it
            val bitmap = Bitmap.createBitmap(container.measuredWidth, container.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            container.draw(canvas)
            return bitmap
        } finally {
            // CRITICAL: Always remove the temporary view to avoid memory leaks
            root.removeView(container)
        }
    }

    /**
     * Saves the bitmap to a file and triggers the Android share sheet.
     */
    fun shareTransactionsAsImage(
        context: Context,
        transactions: List<TransactionDetails>,
        fields: Set<ShareableField>
    ) {
        // 1. Generate the Bitmap
        val bitmap = createBitmapFromComposable(context, transactions, fields)

        // 2. Save the Bitmap to a temporary file
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "transaction_snapshot.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        // 3. Get a content URI using FileProvider
        val contentUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)

        // 4. Trigger the share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Transactions"))
    }
}

/**
 * The Composable that defines the layout of the shareable image.
 */
@Composable
private fun TransactionSnapshotContent(
    transactions: List<TransactionDetails>,
    fields: Set<ShareableField>
) {
    val totalAmount = transactions.sumOf {
        if (it.transaction.transactionType == "income") it.transaction.amount else -it.transaction.amount
    }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .width(IntrinsicSize.Max) // Adjust width to content
    ) {
        // Header
        Text(
            "Transaction Summary",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Generated by Finlight",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fields.forEach { field ->
                Text(
                    text = field.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(if (field == ShareableField.Description) 1.5f else 1f),
                    textAlign = if (field == ShareableField.Amount) TextAlign.End else TextAlign.Start
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Table Rows
        transactions.forEach { details ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fields.forEach { field ->
                    val text = when (field) {
                        ShareableField.Date -> dateFormat.format(Date(details.transaction.date))
                        ShareableField.Description -> details.transaction.description
                        ShareableField.Amount -> currencyFormat.format(details.transaction.amount)
                        ShareableField.Category -> details.categoryName ?: "N/A"
                        ShareableField.Account -> details.accountName ?: "N/A"
                        ShareableField.Notes -> details.transaction.notes ?: ""
                        ShareableField.Tags -> "Tags..." // Placeholder for now
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(if (field == ShareableField.Description) 1.5f else 1f),
                        textAlign = if (field == ShareableField.Amount) TextAlign.End else TextAlign.Start,
                        fontSize = 12.sp
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Footer (Total)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "Total:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                currencyFormat.format(totalAmount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
