package com.example.data

import android.webkit.PermissionRequest
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FanSiteAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val fanHandle: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BrowserBookmark(
  @DocumentId val id: String = "",
  val title: String = "",
  val url: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class PendingDownloadRequest(
  val url: String,
  val userAgent: String,
  val contentDisposition: String,
  val mimeType: String,
  val contentLength: Long,
  val fileName: String,
  val host: String,
)

data class PendingCameraRequest(
  val origin: String,
  val resources: Array<String>,
  val permissionRequest: PermissionRequest?,
)

data class DownloadRecord(
  val id: String,
  val fileName: String,
  val url: String,
  val mimeType: String,
  val timestampMs: Long,
)

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}
