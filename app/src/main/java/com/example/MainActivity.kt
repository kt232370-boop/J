package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FanSiteRepository
import com.example.ui.AuthScreen
import com.example.ui.FanSiteBrowserScreen
import com.example.ui.FanSiteViewModel
import com.example.ui.theme.FanSiteTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FanSiteTheme {
        FanSiteAppContent()
      }
    }
  }
}

@Composable
fun FanSiteAppContent() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var pendingHandle by rememberSaveable { mutableStateOf("") }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      pendingHandle = pendingHandle,
      onPendingHandleChanged = { pendingHandle = it },
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
    )
  } else {
    val uid = user.uid
    key(uid) {
      val viewModel: FanSiteViewModel =
        viewModel(
          key = uid,
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FanSiteViewModel(
                  repository = FanSiteRepository(db),
                  currentUserId = uid,
                  initialDisplayName = user.displayName ?: "FanSite Member",
                  initialEmail = user.email ?: "",
                  initialPhotoUrl = user.photoUrl?.toString(),
                  pendingHandle = pendingHandle.takeIf { it.isNotBlank() },
                )
              }
            },
        )
      FanSiteBrowserScreen(
        viewModel = viewModel,
        googleDisplayName = user.displayName ?: "FanSite Member",
        googleEmail = user.email ?: "",
        googlePhotoUrl = user.photoUrl?.toString(),
        onSignedOut = {
          pendingHandle = ""
          currentUser = Firebase.auth.currentUser
        },
      )
    }
  }
}
