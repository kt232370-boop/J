package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.BrowserBookmark
import com.example.data.DownloadRecord
import com.example.data.PendingCameraRequest
import com.example.data.PendingDownloadRequest
import java.text.DateFormat
import java.util.Date

@Composable
fun DownloadWarningDialog(
  request: PendingDownloadRequest,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.size(64.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_warning_alert),
          contentDescription = stringResource(R.string.warning_icon_description),
          contentScale = ContentScale.Fit,
          modifier = Modifier.padding(12.dp).testTag("download_warning_image"),
        )
      }
    },
    title = {
      Text(
        text = stringResource(R.string.download_alert_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.download_alert_message),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = request.fileName,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Source: ${request.host}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Type: ${request.mimeType}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
          ),
        modifier = Modifier.testTag("confirm_download_button"),
      ) {
        Text(stringResource(R.string.download_alert_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_download_button"),
      ) {
        Text(stringResource(R.string.download_alert_cancel))
      }
    },
  )
}

@Composable
fun CameraPermissionWarningDialog(
  request: PendingCameraRequest,
  onAllow: () -> Unit,
  onDeny: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDeny,
    icon = {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.size(64.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_warning_alert),
          contentDescription = stringResource(R.string.warning_icon_description),
          contentScale = ContentScale.Fit,
          modifier = Modifier.padding(12.dp).testTag("camera_warning_image"),
        )
      }
    },
    title = {
      Text(
        text = stringResource(R.string.camera_alert_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.camera_alert_message),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "Website Origin",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
              text = request.origin,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onAllow,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
          ),
        modifier = Modifier.testTag("confirm_camera_button"),
      ) {
        Text(stringResource(R.string.camera_alert_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDeny,
        modifier = Modifier.testTag("deny_camera_button"),
      ) {
        Text(stringResource(R.string.camera_alert_deny))
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileBottomSheet(
  displayName: String,
  fanHandle: String,
  email: String,
  photoUrl: String?,
  isSaving: Boolean,
  onSaveProfile: (String, String) -> Unit,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  var editedName by remember(displayName) { mutableStateOf(displayName) }
  var editedHandle by remember(fanHandle) { mutableStateOf(fanHandle) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier =
            Modifier.size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
              model = photoUrl,
              contentDescription = stringResource(R.string.google_account_avatar_description),
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(56.dp).clip(CircleShape),
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Person,
              contentDescription = stringResource(R.string.google_account_avatar_description),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(30.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.account_sheet_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = email.ifBlank { "Google Firebase Account" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      HorizontalDivider()

      OutlinedTextField(
        value = editedName,
        onValueChange = { if (it.length <= 80) editedName = it },
        label = { Text("Display Name") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
      )

      OutlinedTextField(
        value = editedHandle,
        onValueChange = { if (it.length <= 40) editedHandle = it },
        label = { Text("FanSite Handle") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("profile_handle_input"),
      )

      Button(
        onClick = { onSaveProfile(editedName, editedHandle) },
        enabled = !isSaving && editedName.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button"),
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.account_save_profile))
        }
      }

      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.account_sign_out))
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
  bookmarks: List<BrowserBookmark>,
  onOpenBookmark: (String) -> Unit,
  onDeleteBookmark: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.browser_bookmarks_label),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
      )
      if (bookmarks.isEmpty()) {
        Text(
          text = "No bookmarks yet. Tap the star in the search bar to save a FanSite link.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
          items(bookmarks, key = { it.id }) { bookmark ->
            ListItem(
              headlineContent = {
                Text(
                  text = bookmark.title,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  fontWeight = FontWeight.Medium,
                )
              },
              supportingContent = {
                Text(
                  text = bookmark.url,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              leadingContent = {
                Icon(
                  imageVector = Icons.Rounded.Bookmark,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
              trailingContent = {
                IconButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete bookmark",
                  )
                }
              },
              modifier = Modifier.clickable { onOpenBookmark(bookmark.url) },
            )
            HorizontalDivider()
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryBottomSheet(
  downloads: List<DownloadRecord>,
  onDismiss: () -> Unit,
) {
  val dateFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.browser_downloads_label),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
      )
      if (downloads.isEmpty()) {
        Text(
          text = "No files downloaded yet. Website file downloads trigger a warning alert before saving.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
          items(downloads, key = { it.id }) { item ->
            ListItem(
              headlineContent = {
                Text(text = item.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
              },
              supportingContent = {
                Text(
                  text = "${item.mimeType} • ${dateFormatter.format(Date(item.timestampMs))}",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              leadingContent = {
                Icon(
                  imageVector = Icons.Rounded.DownloadDone,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
            )
            HorizontalDivider()
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
