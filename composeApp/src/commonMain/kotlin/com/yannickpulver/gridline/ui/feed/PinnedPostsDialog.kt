package com.yannickpulver.gridline.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Instagram's API doesn't tell which posts are pinned, so the user pastes their links. */
@Composable
fun PinnedPostsDialog(
    links: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val fields = remember(links) {
        mutableStateListOf(*Array(MAX_PINNED_POSTS) { links.getOrElse(it) { "" } })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pinned posts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste the links of the posts pinned on your profile. They show first in the grid.",
                    style = MaterialTheme.typography.bodySmall
                )
                fields.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { fields[index] = it },
                        placeholder = { Text("instagram.com/p/…", maxLines = 1) },
                        singleLine = true,
                        isError = value.isNotBlank() && shortcodeOf(value) == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(fields.toList()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
