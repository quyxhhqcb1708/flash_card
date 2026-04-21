package com.example.xq.flashcard.sync

import android.content.Context
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.settings.AppSettingsStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

enum class CloudSyncEnableMode {
    ENABLED_ONLY,
    UPLOADED_LOCAL,
    RESTORED_FROM_CLOUD
}

private data class CloudLibrarySnapshot(
    val rawJson: String,
    val collectionCount: Int,
    val cardCount: Int,
    val updatedAt: Long
) {
    val hasData: Boolean
        get() = collectionCount > 0 || cardCount > 0
}

object StudyCloudSyncManager {

    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_STUDY_DATA = "studyData"
    private const val DOCUMENT_LIBRARY_SNAPSHOT = "librarySnapshot"
    private const val FIELD_VERSION = "version"
    private const val FIELD_COLLECTIONS_JSON = "collectionsJson"
    private const val FIELD_COLLECTION_COUNT = "collectionCount"
    private const val FIELD_CARD_COUNT = "cardCount"
    private const val FIELD_UPDATED_AT = "updatedAt"

    private var isUploadInProgress = false
    private var hasPendingUpload = false

    fun bootstrapAfterLogin(context: Context, onComplete: () -> Unit) {
        val appContext = context.applicationContext
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onComplete()
            return
        }
        val localSnapshot = FlashCardLibraryStore.getSnapshot(appContext)
        fetchRemoteSnapshot(
            userId = userId,
            onSuccess = { remoteSnapshot ->
                when {
                    remoteSnapshot == null || !remoteSnapshot.hasData -> {
                        if (AppSettingsStore.isCloudSyncEnabled(appContext) && localSnapshot.hasData) {
                            uploadLocalSnapshot(
                                context = appContext,
                                onSuccess = { onComplete() },
                                onError = { onComplete() }
                            )
                        } else {
                            onComplete()
                        }
                    }

                    !localSnapshot.hasData -> {
                        AppSettingsStore.setCloudSyncEnabled(appContext, true)
                        restoreSnapshot(
                            context = appContext,
                            remoteSnapshot = remoteSnapshot,
                            onComplete = onComplete
                        )
                    }

                    AppSettingsStore.isCloudSyncEnabled(appContext) &&
                        remoteSnapshot.updatedAt > localSnapshot.updatedAt -> {
                        restoreSnapshot(
                            context = appContext,
                            remoteSnapshot = remoteSnapshot,
                            onComplete = onComplete
                        )
                    }

                    AppSettingsStore.isCloudSyncEnabled(appContext) &&
                        localSnapshot.updatedAt >= remoteSnapshot.updatedAt -> {
                        uploadLocalSnapshot(
                            context = appContext,
                            onSuccess = { onComplete() },
                            onError = { onComplete() }
                        )
                    }

                    else -> onComplete()
                }
            },
            onError = { onComplete() }
        )
    }

    fun enableCloudSync(
        context: Context,
        onSuccess: (CloudSyncEnableMode) -> Unit,
        onError: () -> Unit
    ) {
        val appContext = context.applicationContext
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onError()
            return
        }
        val localSnapshot = FlashCardLibraryStore.getSnapshot(appContext)
        fetchRemoteSnapshot(
            userId = userId,
            onSuccess = { remoteSnapshot ->
                when {
                    remoteSnapshot != null &&
                        remoteSnapshot.hasData &&
                        (!localSnapshot.hasData || remoteSnapshot.updatedAt > localSnapshot.updatedAt) -> {
                        restoreSnapshot(
                            context = appContext,
                            remoteSnapshot = remoteSnapshot,
                            onComplete = { onSuccess(CloudSyncEnableMode.RESTORED_FROM_CLOUD) },
                            onError = onError
                        )
                    }

                    localSnapshot.hasData -> {
                        uploadLocalSnapshot(
                            context = appContext,
                            onSuccess = { onSuccess(CloudSyncEnableMode.UPLOADED_LOCAL) },
                            onError = onError
                        )
                    }

                    else -> onSuccess(CloudSyncEnableMode.ENABLED_ONLY)
                }
            },
            onError = onError
        )
    }

    fun syncNow(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        uploadLocalSnapshot(
            context = context.applicationContext,
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun restoreFromCloud(
        context: Context,
        onSuccess: (Boolean) -> Unit,
        onError: () -> Unit
    ) {
        val appContext = context.applicationContext
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onError()
            return
        }
        fetchRemoteSnapshot(
            userId = userId,
            onSuccess = { remoteSnapshot ->
                if (remoteSnapshot == null || !remoteSnapshot.hasData) {
                    onSuccess(false)
                } else {
                    restoreSnapshot(
                        context = appContext,
                        remoteSnapshot = remoteSnapshot,
                        onComplete = { onSuccess(true) },
                        onError = onError
                    )
                }
            },
            onError = onError
        )
    }

    fun syncLocalSnapshotInBackground(context: Context) {
        val appContext = context.applicationContext
        if (!AppSettingsStore.isCloudSyncEnabled(appContext)) return
        if (FirebaseAuth.getInstance().currentUser == null) return

        val shouldStartUpload = synchronized(this) {
            if (isUploadInProgress) {
                hasPendingUpload = true
                false
            } else {
                isUploadInProgress = true
                true
            }
        }
        if (!shouldStartUpload) return

        uploadLocalSnapshot(
            context = appContext,
            onSuccess = { onBackgroundUploadFinished(appContext) },
            onError = { onBackgroundUploadFinished(appContext) }
        )
    }

    private fun onBackgroundUploadFinished(context: Context) {
        val shouldRunAgain = synchronized(this) {
            if (hasPendingUpload) {
                hasPendingUpload = false
                true
            } else {
                isUploadInProgress = false
                false
            }
        }

        if (shouldRunAgain) {
            syncLocalSnapshotInBackground(context)
        }
    }

    private fun uploadLocalSnapshot(
        context: Context,
        onSuccess: (Long) -> Unit,
        onError: () -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onError()
            return
        }
        val localSnapshot = FlashCardLibraryStore.getSnapshot(context)
        val uploadedAt = maxOf(localSnapshot.updatedAt, System.currentTimeMillis())
        val payload = hashMapOf(
            FIELD_VERSION to 1,
            FIELD_COLLECTIONS_JSON to localSnapshot.rawJson,
            FIELD_COLLECTION_COUNT to localSnapshot.collectionCount,
            FIELD_CARD_COUNT to localSnapshot.cardCount,
            FIELD_UPDATED_AT to uploadedAt
        )

        document(userId)
            .set(payload)
            .addOnSuccessListener {
                AppSettingsStore.setCloudSyncLastSyncedAt(context, uploadedAt)
                onSuccess(uploadedAt)
            }
            .addOnFailureListener { onError() }
    }

    private fun restoreSnapshot(
        context: Context,
        remoteSnapshot: CloudLibrarySnapshot,
        onComplete: () -> Unit,
        onError: () -> Unit = onComplete
    ) {
        val restored = FlashCardLibraryStore.importCollectionsJson(
            context = context,
            rawJson = remoteSnapshot.rawJson,
            triggerCloudSync = false
        )
        if (restored) {
            AppSettingsStore.setCloudSyncLastSyncedAt(context, remoteSnapshot.updatedAt)
            onComplete()
        } else {
            onError()
        }
    }

    private fun fetchRemoteSnapshot(
        userId: String,
        onSuccess: (CloudLibrarySnapshot?) -> Unit,
        onError: () -> Unit
    ) {
        document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.toCloudLibrarySnapshot())
            }
            .addOnFailureListener { onError() }
    }

    private fun document(userId: String) = FirebaseFirestore.getInstance()
        .collection(COLLECTION_USERS)
        .document(userId)
        .collection(COLLECTION_STUDY_DATA)
        .document(DOCUMENT_LIBRARY_SNAPSHOT)

    private fun DocumentSnapshot.toCloudLibrarySnapshot(): CloudLibrarySnapshot? {
        if (!exists()) return null
        return CloudLibrarySnapshot(
            rawJson = getString(FIELD_COLLECTIONS_JSON).orEmpty().ifBlank { "[]" },
            collectionCount = getLong(FIELD_COLLECTION_COUNT)?.toInt() ?: 0,
            cardCount = getLong(FIELD_CARD_COUNT)?.toInt() ?: 0,
            updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L
        )
    }
}
