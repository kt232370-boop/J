package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FanSiteRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context,
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeAccount(userId: String): Flow<FanSiteAccount?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch" }
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              FanSiteAccount::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        },
    )
  }

  fun observeBookmarks(userId: String): Flow<List<BrowserBookmark>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch" }
    val path = "users/$uid/bookmarks"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("bookmarks")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            BrowserBookmark::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        },
    )
  }

  suspend fun ensureOrUpdateAccount(
    userId: String,
    displayName: String,
    fanHandle: String,
    email: String,
    photoUrl: String?,
    overwriteExistingHandle: Boolean = true,
  ): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val docRef = db.collection("users").document(uid)
      docPath = docRef.path

      val cleanDisplayName = displayName.trim().ifBlank { "FanSite Member" }.take(80)
      val normalizedHandle = normalizeHandle(fanHandle, cleanDisplayName)
      val cleanEmail = email.trim().take(160)
      val cleanPhotoUrl = photoUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() }

      val existingSnapshot = docRef.get().await()
      if (!existingSnapshot.exists()) {
        val createPayload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", cleanDisplayName)
            put("fanHandle", normalizedHandle)
            put("email", cleanEmail)
            if (cleanPhotoUrl != null) {
              put("photoUrl", cleanPhotoUrl)
            }
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(createPayload).await()
      } else {
        val existingHandle = existingSnapshot.getString("fanHandle")
        val targetHandle =
          if (overwriteExistingHandle || existingHandle.isNullOrBlank()) {
            normalizedHandle
          } else {
            existingHandle
          }
        val updatePayload =
          buildMap<String, Any> {
            put("displayName", cleanDisplayName)
            put("fanHandle", targetHandle)
            put("email", cleanEmail)
            if (cleanPhotoUrl != null) {
              put("photoUrl", cleanPhotoUrl)
            }
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.update(updatePayload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addBookmark(userId: String, title: String, url: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/bookmarks"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val cleanTitle = title.trim().ifBlank { "FanSite Link" }.take(120)
      val cleanUrl = url.trim().take(1000)
      require(cleanUrl.isNotEmpty()) { "Bookmark URL must not be empty." }

      val docRef = db.collection("users").document(uid).collection("bookmarks").document()
      docPath = docRef.path
      val payload =
        mapOf(
          "title" to cleanTitle,
          "url" to cleanUrl,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteBookmark(userId: String, bookmarkId: String): Result<Unit> {
    var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/bookmarks/$bookmarkId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      require(bookmarkId.isNotBlank()) { "Bookmark ID must not be blank." }
      val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmarkId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  private fun normalizeHandle(rawHandle: String, fallbackName: String): String {
    val trimmed = rawHandle.trim()
    val base =
      if (trimmed.isNotEmpty()) {
        trimmed
      } else {
        fallbackName.lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank { "fan" }
      }
    val prefixed = if (base.startsWith("@")) base else "@$base"
    return prefixed.take(40).let { if (it.length < 2) "@fan" else it }
  }
}
