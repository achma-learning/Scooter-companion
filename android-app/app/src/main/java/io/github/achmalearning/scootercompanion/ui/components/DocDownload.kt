package io.github.achmalearning.scootercompanion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.R
import java.util.Locale

/** "12.3 MB" (Western digits in every language, like the rest of the app's numbers). */
fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(Locale.US, bytes / 1_000_000.0)
    bytes >= 1_000 -> "%d KB".format(Locale.US, bytes / 1_000)
    else -> "$bytes B"
}

/**
 * A PDF that isn't on the phone yet: what it is, its size, and a Download button. The app goes
 * online only when the owner taps it.
 */
@Composable
fun DocDownloadCard(title: String, bytes: Long, progress: Float?, error: String?, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    Panel(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.doc_download_text, formatSize(bytes)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )
            if (progress != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.doc_downloading, (progress * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Button(onClick = onDownload) {
                    Icon(Icons.Filled.Download, null)
                    Text(" " + stringResource(R.string.doc_download, formatSize(bytes)))
                }
            }
            error?.let {
                Text(
                    stringResource(R.string.doc_failed, it), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
