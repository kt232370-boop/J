package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.signOut
import com.example.data.PendingDownloadRequest
import com.example.data.UiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FanSiteBrowserScreen(
  viewModel: FanSiteViewModel,
  googleDisplayName: String,
  googleEmail: String,
  googlePhotoUrl: String?,
  onSignedOut: () -> Unit,
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  val accountState by viewModel.accountState.collectAsStateWithLifecycle()
  val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
  val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
  val searchInput by viewModel.searchInput.collectAsStateWithLifecycle()
  val loadProgress by viewModel.loadProgress.collectAsStateWithLifecycle()
  val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
  val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
  val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()
  val pendingCameraRequest by viewModel.pendingCameraRequest.collectAsStateWithLifecycle()
  val cameraAuthorized by viewModel.cameraAuthorizedForSite.collectAsStateWithLifecycle()
  val downloadHistory by viewModel.downloadHistory.collectAsStateWithLifecycle()
  val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

  var showBrowserMenu by remember { mutableStateOf(false) }
  var showAccountSheet by remember { mutableStateOf(false) }
  var showBookmarksSheet by remember { mutableStateOf(false) }
  var showDownloadsSheet by remember { mutableStateOf(false) }

  val account = (accountState as? UiState.Success)?.data
  val bookmarks = (bookmarksState as? UiState.Success)?.data.orEmpty()
  val resolvedAvatarUrl = account?.photoUrl ?: googlePhotoUrl
  val resolvedDisplayName = account?.displayName?.ifBlank { googleDisplayName } ?: googleDisplayName
  val resolvedHandle = account?.fanHandle.orEmpty()

  val webViewRef = remember { mutableStateOf<WebView?>(null) }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      if (granted) {
        viewModel.resolvePendingCamera(
          granted = true,
          statusMsg = context.getString(R.string.camera_permission_granted_msg),
        )
      } else {
        viewModel.resolvePendingCamera(
          granted = false,
          statusMsg = context.getString(R.string.camera_permission_denied_msg),
        )
      }
    }

  LaunchedEffect(snackbarMessage) {
    val msg = snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeMessage()
    }
  }

  BackHandler(enabled = canGoBack) {
    webViewRef.value?.goBack()
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // White branded toolbar with FanSite logo and Google Firebase account avatar
        Surface(
          color = Color.White,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.size(40.dp).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
              ) {
                Image(
                  painter = painterResource(id = R.drawable.fansite_logo),
                  contentDescription = stringResource(R.string.fansite_logo_description),
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxSize().padding(4.dp).testTag("toolbar_fansite_logo"),
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF0F172A),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (resolvedHandle.isNotBlank()) {
                  Text(
                    text = resolvedHandle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0084B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            IconButton(
              onClick = { showAccountSheet = true },
              modifier = Modifier.size(48.dp).testTag("toolbar_account_button"),
            ) {
              Box(
                modifier =
                  Modifier.size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9F3FE))
                    .border(1.5.dp, Color(0xFF00AFF0), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                if (!resolvedAvatarUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = resolvedAvatarUrl,
                    contentDescription = stringResource(R.string.google_account_avatar_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                  )
                } else {
                  Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.google_account_avatar_description),
                    tint = Color(0xFF0084B8),
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
            }
          }
        }

        // FanSite Browser (Google Chrome) search & menu bar
        Surface(
          color = Color.White,
          tonalElevation = 0.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = searchInput,
                onValueChange = viewModel::onSearchInputChanged,
                placeholder = {
                  Text(
                    text = stringResource(R.string.browser_search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF0084B8),
                  )
                },
                trailingIcon = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchInput.isNotEmpty()) {
                      IconButton(
                        onClick = { viewModel.onSearchInputChanged("") },
                        modifier = Modifier.size(36.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Close,
                          contentDescription = "Clear search",
                          tint = Color(0xFF64748B),
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                    IconButton(
                      onClick = { viewModel.bookmarkCurrentPage() },
                      modifier = Modifier.size(40.dp).testTag("bookmark_current_page_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.BookmarkAdd,
                        contentDescription = "Bookmark current page",
                        tint = Color(0xFF0084B8),
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                  KeyboardActions(
                    onSearch = {
                      focusManager.clearFocus()
                      val targetUrl = viewModel.resolveSearchOrUrl(searchInput)
                      webViewRef.value?.loadUrl(targetUrl)
                    },
                  ),
                shape = RoundedCornerShape(28.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedBorderColor = Color(0xFF00AFF0),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                  ),
                modifier = Modifier.weight(1f).testTag("browser_search_input"),
              )
            }

            // Browser controls + FanSite Browser Menu
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { webViewRef.value?.goBack() },
                  enabled = canGoBack,
                  modifier = Modifier.size(48.dp).testTag("browser_back_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                  )
                }
                IconButton(
                  onClick = { webViewRef.value?.goForward() },
                  enabled = canGoForward,
                  modifier = Modifier.size(48.dp).testTag("browser_forward_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (canGoForward) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                  )
                }
                IconButton(
                  onClick = { webViewRef.value?.reload() },
                  modifier = Modifier.size(48.dp).testTag("browser_refresh_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reload page",
                    tint = Color(0xFF0F172A),
                  )
                }
                IconButton(
                  onClick = {
                    val home = viewModel.navigateHome()
                    webViewRef.value?.loadUrl(home)
                  },
                  modifier = Modifier.size(48.dp).testTag("browser_home_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = stringResource(R.string.browser_home_label),
                    tint = Color(0xFF0084B8),
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { viewModel.triggerMenuCameraAlert() },
                  modifier = Modifier.size(48.dp).testTag("browser_camera_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = stringResource(R.string.browser_camera_access_label),
                    tint = if (cameraAuthorized) Color(0xFF16A34A) else Color(0xFFD97706),
                  )
                }

                Box {
                  IconButton(
                    onClick = { showBrowserMenu = true },
                    modifier = Modifier.size(48.dp).testTag("browser_menu_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.MoreVert,
                      contentDescription = stringResource(R.string.browser_menu_title),
                      tint = Color(0xFF0F172A),
                    )
                  }
                  DropdownMenu(
                    expanded = showBrowserMenu,
                    onDismissRequest = { showBrowserMenu = false },
                  ) {
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.browser_home_label)) },
                      leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                      onClick = {
                        showBrowserMenu = false
                        val home = viewModel.navigateHome()
                        webViewRef.value?.loadUrl(home)
                      },
                    )
                    DropdownMenuItem(
                      text = { Text("Search Google") },
                      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                      onClick = {
                        showBrowserMenu = false
                        val googleUrl = viewModel.navigateToGoogleSearch()
                        webViewRef.value?.loadUrl(googleUrl)
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.browser_open_chrome_label)) },
                      leadingIcon = {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                      },
                      onClick = {
                        showBrowserMenu = false
                        openInChromeBrowser(context, currentUrl) { msg ->
                          viewModel.showMessage(msg)
                        }
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.browser_camera_access_label)) },
                      leadingIcon = { Icon(Icons.Rounded.Videocam, contentDescription = null) },
                      onClick = {
                        showBrowserMenu = false
                        viewModel.triggerMenuCameraAlert()
                      },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                      text = {
                        Text("${stringResource(R.string.browser_bookmarks_label)} (${bookmarks.size})")
                      },
                      leadingIcon = { Icon(Icons.Rounded.Bookmarks, contentDescription = null) },
                      onClick = {
                        showBrowserMenu = false
                        showBookmarksSheet = true
                      },
                    )
                    DropdownMenuItem(
                      text = {
                        Text("${stringResource(R.string.browser_downloads_label)} (${downloadHistory.size})")
                      },
                      leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                      onClick = {
                        showBrowserMenu = false
                        showDownloadsSheet = true
                      },
                    )
                  }
                }
              }
            }

            if (loadProgress in 1..99) {
              LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
              )
            } else {
              HorizontalDivider(color = Color(0xFFE2E8F0))
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams =
              ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
              )
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              useWideViewPort = true
              loadWithOverviewMode = true
              builtInZoomControls = true
              displayZoomControls = false
              mediaPlaybackRequiresUserGesture = false
              mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
              allowFileAccess = false
            }

            webViewClient =
              object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                  view: WebView?,
                  request: WebResourceRequest?,
                ): Boolean {
                  val urlStr = request?.url?.toString() ?: return false
                  return if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                    false
                  } else {
                    try {
                      ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)))
                    } catch (_: ActivityNotFoundException) {
                      viewModel.showMessage("Cannot open external link")
                    }
                    true
                  }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                  super.onPageStarted(view, url, favicon)
                  viewModel.onWebPageUpdated(
                    url = url,
                    title = view?.title,
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                  )
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                  super.onPageFinished(view, url)
                  viewModel.onWebPageUpdated(
                    url = url,
                    title = view?.title,
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                  )
                }
              }

            webChromeClient =
              object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                  viewModel.onProgressChanged(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                  viewModel.onWebPageUpdated(
                    url = view?.url,
                    title = title,
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                  )
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                  if (request == null) return
                  post { viewModel.onWebPermissionRequested(request) }
                }
              }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
              post {
                viewModel.onDownloadRequested(
                  url = url,
                  userAgent = userAgent,
                  contentDisposition = contentDisposition,
                  mimeType = mimetype,
                  contentLength = contentLength,
                )
              }
            }

            webViewRef.value = this
            loadUrl(currentUrl)
          }
        },
        modifier = Modifier.fillMaxSize().testTag("fansite_webview"),
      )

      DisposableEffect(Unit) {
        onDispose {
          webViewRef.value?.apply {
            stopLoading()
            destroy()
          }
          webViewRef.value = null
        }
      }
    }
  }

  pendingDownload?.let { request ->
    DownloadWarningDialog(
      request = request,
      onConfirm = {
        enqueueSystemDownload(context, request) { ok, errorMsg ->
          if (ok) {
            viewModel.recordDownloadStarted(request)
          } else {
            viewModel.dismissDownloadAlert()
            viewModel.showMessage(errorMsg ?: "Unable to start download")
          }
        }
      },
      onDismiss = viewModel::dismissDownloadAlert,
    )
  }

  pendingCameraRequest?.let { request ->
    CameraPermissionWarningDialog(
      request = request,
      onAllow = {
        val hasCameraPermission =
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
          viewModel.resolvePendingCamera(
            granted = true,
            statusMsg = context.getString(R.string.camera_permission_granted_msg),
          )
        } else {
          cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
      },
      onDeny = {
        viewModel.resolvePendingCamera(
          granted = false,
          statusMsg = context.getString(R.string.camera_permission_denied_msg),
        )
      },
    )
  }

  if (showAccountSheet) {
    AccountProfileBottomSheet(
      displayName = resolvedDisplayName,
      fanHandle = resolvedHandle,
      email = account?.email ?: googleEmail,
      photoUrl = resolvedAvatarUrl,
      isSaving = isSavingProfile,
      onSaveProfile = { newName, newHandle ->
        viewModel.updateFanProfile(newName, newHandle) { success ->
          if (success) showAccountSheet = false
        }
      },
      onSignOut = {
        showAccountSheet = false
        signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
      },
      onDismiss = { showAccountSheet = false },
    )
  }

  if (showBookmarksSheet) {
    BookmarksBottomSheet(
      bookmarks = bookmarks,
      onOpenBookmark = { url ->
        showBookmarksSheet = false
        val resolved = viewModel.resolveSearchOrUrl(url)
        webViewRef.value?.loadUrl(resolved)
      },
      onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
      onDismiss = { showBookmarksSheet = false },
    )
  }

  if (showDownloadsSheet) {
    DownloadHistoryBottomSheet(
      downloads = downloadHistory,
      onDismiss = { showDownloadsSheet = false },
    )
  }
}

private fun enqueueSystemDownload(
  context: Context,
  request: PendingDownloadRequest,
  onResult: (Boolean, String?) -> Unit,
) {
  try {
    val downloadManager =
      context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        ?: run {
          onResult(false, "Download manager unavailable")
          return
        }
    val dmRequest =
      DownloadManager.Request(Uri.parse(request.url)).apply {
        setMimeType(request.mimeType)
        addRequestHeader("User-Agent", request.userAgent)
        setTitle(request.fileName)
        setDescription("Downloaded via FanSite Browser")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, request.fileName)
        } else {
          setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, request.fileName)
        }
      }
    downloadManager.enqueue(dmRequest)
    onResult(true, null)
  } catch (e: Exception) {
    onResult(false, e.localizedMessage ?: "Download failed")
  }
}

private fun openInChromeBrowser(
  context: Context,
  url: String,
  onError: (String) -> Unit,
) {
  val uri = Uri.parse(url)
  val chromeIntent =
    Intent(Intent.ACTION_VIEW, uri).apply {
      setPackage("com.android.chrome")
    }
  try {
    context.startActivity(chromeIntent)
  } catch (_: ActivityNotFoundException) {
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
      onError("No browser installed on this device")
    }
  }
}
