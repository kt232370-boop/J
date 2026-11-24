package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.auth.attemptAutoSignIn

@Composable
fun AuthScreen(
  pendingHandle: String,
  onPendingHandleChanged: (String) -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  val backgroundBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          MaterialTheme.colorScheme.background,
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(backgroundBrush)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp, vertical = 20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier =
        Modifier.widthIn(max = 460.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = Color.White,
          shadowElevation = 6.dp,
          modifier = Modifier.size(104.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.fansite_logo),
            contentDescription = stringResource(R.string.fansite_logo_description),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier.fillMaxSize()
                .padding(16.dp)
                .clip(CircleShape)
                .testTag("auth_fansite_logo"),
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(R.string.auth_create_account_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )
          Text(
            text = stringResource(R.string.auth_create_account_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }

        OutlinedTextField(
          value = pendingHandle,
          onValueChange = { if (it.length <= 40) onPendingHandleChanged(it) },
          label = { Text(stringResource(R.string.auth_handle_label)) },
          placeholder = { Text(stringResource(R.string.auth_handle_placeholder)) },
          leadingIcon = {
            Icon(imageVector = Icons.Rounded.AlternateEmail, contentDescription = null)
          },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth().testTag("auth_handle_input"),
        )

        if (!errorMessage.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        GoogleSignInButton(
          onAuthSuccess = {
            errorMessage = null
            onAuthSuccess()
          },
          onAuthError = { msg -> errorMessage = msg },
        )

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Protected by Google Firebase Authentication & Safe Browser Alerts",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
