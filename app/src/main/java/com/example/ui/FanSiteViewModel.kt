package com.example.ui

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrowserBookmark
import com.example.data.DownloadRecord
import com.example.data.FanSiteAccount
import com.example.data.FanSiteRepository
import com.example.data.PendingCameraRequest
import com.example.data.PendingDownloadRequest
import com.example.data.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FanSiteViewModel(
  private val repository: FanSiteRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
  private val initialEmail: String,
  private val initialPhotoUrl: String?,
  pendingHandle: String?,
) : ViewModel() {

  val accountState: StateFlow<UiState<FanSiteAccount?>> =
    repository
      .observeAccount(currentUserId)
      .map<FanSiteAccount?, UiState<FanSiteAccount?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(UiState.Error(rootError.message ?: "Failed to load FanSite account"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val bookmarksState: StateFlow<UiState<List<BrowserBookmark>>> =
    repository
      .observeBookmarks(currentUserId)
      .map<List<BrowserBookmark>, UiState<List<BrowserBookmark>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(UiState.Error(rootError.message ?: "Failed to load bookmarks"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _currentUrl = MutableStateFlow(FANSITE_HOME_URL)
  val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

  private val _searchInput = MutableStateFlow(FANSITE_HOME_URL)
  val searchInput: StateFlow<String> = _searchInput.asStateFlow()

  private val _pageTitle = MutableStateFlow("FanSite")
  val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

  private val _loadProgress = MutableStateFlow(0)
  val loadProgress: StateFlow<Int> = _loadProgress.asStateFlow()

  private val _canGoBack = MutableStateFlow(false)
  val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

  private val _canGoForward = MutableStateFlow(false)
  val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

  private val _pendingDownload = MutableStateFlow<PendingDownloadRequest?>(null)
  val pendingDownload: StateFlow<PendingDownloadRequest?> = _pendingDownload.asStateFlow()

  private val _pendingCameraRequest = MutableStateFlow<PendingCameraRequest?>(null)
  val pendingCameraRequest: StateFlow<PendingCameraRequest?> = _pendingCameraRequest.asStateFlow()

  private val _cameraAuthorizedForSite = MutableStateFlow(false)
  val cameraAuthorizedForSite: StateFlow<Boolean> = _cameraAuthorizedForSite.asStateFlow()

  private val _downloadHistory = MutableStateFlow<List<DownloadRecord>>(emptyList())
  val downloadHistory: StateFlow<List<DownloadRecord>> = _downloadHistory.asStateFlow()

  private val _isSavingProfile = MutableStateFlow(false)
  val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureOrUpdateAccount(
        userId = currentUserId,
        displayName = initialDisplayName,
        fanHandle = pendingHandle ?: "",
        email = initialEmail,
        photoUrl = initialPhotoUrl,
        overwriteExistingHandle = !pendingHandle.isNullOrBlank(),
      )
    }
  }

  fun onSearchInputChanged(value: String) {
    _searchInput.value = value
  }

  fun resolveSearchOrUrl(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return FANSITE_HOME_URL
    val resolved =
      when {
        trimmed.startsWith("http://", ignoreCase = true) ||
          trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        !trimmed.contains(" ") && trimmed.contains(".") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
      }
    _currentUrl.value = resolved
    _searchInput.value = resolved
    return resolved
  }

  fun navigateHome(): String {
    _currentUrl.value = FANSITE_HOME_URL
    _searchInput.value = FANSITE_HOME_URL
    return FANSITE_HOME_URL
  }

  fun navigateToGoogleSearch(): String {
    val googleUrl = "https://www.google.com"
    _currentUrl.value = googleUrl
    _searchInput.value = googleUrl
    return googleUrl
  }

  fun onWebPageUpdated(
    url: String?,
    title: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
  ) {
    if (!url.isNullOrBlank()) {
      _currentUrl.value = url
      _searchInput.value = url
    }
    if (!title.isNullOrBlank()) {
      _pageTitle.value = title
    }
    _canGoBack.value = canGoBack
    _canGoForward.value = canGoForward
  }

  fun onProgressChanged(progress: Int) {
    _loadProgress.value = progress
  }

  fun onDownloadRequested(
    url: String,
    userAgent: String,
    contentDisposition: String,
    mimeType: String,
    contentLength: Long,
  ) {
    val guessedName =
      URLUtil.guessFileName(url, contentDisposition, mimeType).ifBlank { "downloaded_file" }
    val host = Uri.parse(url).host ?: "unknown website"
    _pendingDownload.value =
      PendingDownloadRequest(
        url = url,
        userAgent = userAgent,
        contentDisposition = contentDisposition,
        mimeType = mimeType.ifBlank { "application/octet-stream" },
        contentLength = contentLength,
        fileName = guessedName,
        host = host,
      )
  }

  fun dismissDownloadAlert() {
    _pendingDownload.value = null
  }

  fun recordDownloadStarted(request: PendingDownloadRequest) {
    _pendingDownload.value = null
    val entry =
      DownloadRecord(
        id = "${System.currentTimeMillis()}_${request.fileName}",
        fileName = request.fileName,
        url = request.url,
        mimeType = request.mimeType,
        timestampMs = System.currentTimeMillis(),
      )
    _downloadHistory.value = listOf(entry) + _downloadHistory.value
    _snackbarMessage.value = "Downloading ${request.fileName}…"
  }

  fun onWebPermissionRequested(request: PermissionRequest) {
    val origin = request.origin?.toString() ?: _currentUrl.value
    _pendingCameraRequest.value =
      PendingCameraRequest(
        origin = origin,
        resources = request.resources,
        permissionRequest = request,
      )
  }

  fun triggerMenuCameraAlert() {
    val currentHost = Uri.parse(_currentUrl.value).host ?: _currentUrl.value
    _pendingCameraRequest.value =
      PendingCameraRequest(
        origin = currentHost,
        resources = arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
        permissionRequest = null,
      )
  }

  fun resolvePendingCamera(granted: Boolean, statusMsg: String? = null) {
    val pending = _pendingCameraRequest.value
    _pendingCameraRequest.value = null
    if (pending != null) {
      if (granted) {
        try {
          pending.permissionRequest?.grant(pending.resources)
        } catch (_: IllegalStateException) {
          // Request already resolved or detached
        }
        _cameraAuthorizedForSite.value = true
      } else {
        try {
          pending.permissionRequest?.deny()
        } catch (_: IllegalStateException) {
          // Request already resolved or detached
        }
        _cameraAuthorizedForSite.value = false
      }
    }
    if (!statusMsg.isNullOrBlank()) {
      _snackbarMessage.value = statusMsg
    }
  }

  fun updateFanProfile(displayName: String, fanHandle: String, onDone: (Boolean) -> Unit) {
    viewModelScope.launch {
      _isSavingProfile.value = true
      val result =
        repository.ensureOrUpdateAccount(
          userId = currentUserId,
          displayName = displayName,
          fanHandle = fanHandle,
          email = initialEmail,
          photoUrl = initialPhotoUrl,
          overwriteExistingHandle = true,
        )
      _isSavingProfile.value = false
      result
        .onSuccess {
          _snackbarMessage.value = "FanSite profile updated"
          onDone(true)
        }
        .onFailure { err ->
          _snackbarMessage.value = err.localizedMessage ?: "Failed to save profile"
          onDone(false)
        }
    }
  }

  fun bookmarkCurrentPage() {
    val url = _currentUrl.value
    val title = _pageTitle.value.ifBlank { url }
    viewModelScope.launch {
      repository
        .addBookmark(currentUserId, title, url)
        .onSuccess { _snackbarMessage.value = "Saved to FanSite bookmarks" }
        .onFailure { err ->
          _snackbarMessage.value = err.localizedMessage ?: "Failed to save bookmark"
        }
    }
  }

  fun deleteBookmark(bookmarkId: String) {
    viewModelScope.launch {
      repository
        .deleteBookmark(currentUserId, bookmarkId)
        .onFailure { err ->
          _snackbarMessage.value = err.localizedMessage ?: "Failed to remove bookmark"
        }
    }
  }

  fun showMessage(msg: String) {
    _snackbarMessage.value = msg
  }

  fun consumeMessage() {
    _snackbarMessage.value = null
  }

  companion object {
    const val FANSITE_HOME_URL = "https://share.google/s45hVkei7sq90GoI5"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
