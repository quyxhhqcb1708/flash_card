package com.example.xq.flashcard.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityCreateCollectionBinding

class CreateCollectionActivity : BaseActivity<ActivityCreateCollectionBinding>() {

    companion object {
        const val EXTRA_RESULT_MESSAGE = "extra_result_message"

        private const val EXTRA_MODE = "extra_mode"
        private const val EXTRA_COLLECTION_ID = "extra_collection_id"
        private const val EXTRA_CURRENT_NAME = "extra_current_name"
        private const val EXTRA_PENDING_TERM = "extra_pending_term"
        private const val EXTRA_PENDING_DEFINITION = "extra_pending_definition"
        private const val EXTRA_PENDING_SOURCE_LANGUAGE = "extra_pending_source_language"
        private const val EXTRA_PENDING_TARGET_LANGUAGE = "extra_pending_target_language"
        private const val EXTRA_PENDING_DIFFICULTY = "extra_pending_difficulty"

        private const val MODE_CREATE = "mode_create"
        private const val MODE_RENAME = "mode_rename"

        fun createIntent(context: Context): Intent {
            return Intent(context, CreateCollectionActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_CREATE)
            }
        }

        fun createIntentForSave(
            context: Context,
            term: String,
            definition: String,
            sourceLanguageCode: String,
            targetLanguageCode: String,
            manualDifficulty: FlashCardDifficulty
        ): Intent {
            return createIntent(context).apply {
                putExtra(EXTRA_PENDING_TERM, term)
                putExtra(EXTRA_PENDING_DEFINITION, definition)
                putExtra(EXTRA_PENDING_SOURCE_LANGUAGE, sourceLanguageCode)
                putExtra(EXTRA_PENDING_TARGET_LANGUAGE, targetLanguageCode)
                putExtra(EXTRA_PENDING_DIFFICULTY, manualDifficulty.persistedValue)
            }
        }

        fun createRenameIntent(context: Context, collectionId: Long, currentName: String): Intent {
            return Intent(context, CreateCollectionActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_RENAME)
                putExtra(EXTRA_COLLECTION_ID, collectionId)
                putExtra(EXTRA_CURRENT_NAME, currentName)
            }
        }
    }

    private val screenMode by lazy { intent.getStringExtra(EXTRA_MODE).orEmpty() }
    private val collectionId by lazy { intent.getLongExtra(EXTRA_COLLECTION_ID, -1L) }
    private val currentName by lazy { intent.getStringExtra(EXTRA_CURRENT_NAME).orEmpty() }
    private val pendingTerm by lazy { intent.getStringExtra(EXTRA_PENDING_TERM).orEmpty().trim() }
    private val pendingDefinition by lazy { intent.getStringExtra(EXTRA_PENDING_DEFINITION).orEmpty().trim() }
    private val pendingSourceLanguage by lazy { intent.getStringExtra(EXTRA_PENDING_SOURCE_LANGUAGE).orEmpty() }
    private val pendingTargetLanguage by lazy { intent.getStringExtra(EXTRA_PENDING_TARGET_LANGUAGE).orEmpty() }
    private val pendingDifficulty by lazy {
        FlashCardDifficulty.fromValue(intent.getStringExtra(EXTRA_PENDING_DIFFICULTY))
    }

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityCreateCollectionBinding {
        return ActivityCreateCollectionBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        val isRenameMode = screenMode == MODE_RENAME
        binding.tvTitle.text = getString(
            if (isRenameMode) R.string.library_rename_title else R.string.library_create_title
        )
        binding.btnCreateCollection.text = getString(
            if (isRenameMode) R.string.library_rename_action else R.string.library_create_action
        )
        binding.btnCancel.setOnClickListener { finish() }
        if (currentName.isNotBlank()) {
            binding.etCollectionName.setText(currentName)
            binding.etCollectionName.setSelection(currentName.length)
            updateCharacterCount(currentName)
            binding.btnCreateCollection.isEnabled = true
        }
        binding.etCollectionName.doAfterTextChanged { editable ->
            val value = editable?.toString().orEmpty()
            binding.tvNameError.isVisible = false
            binding.nameInputContainer.setBackgroundResource(R.drawable.bg_auth_input_normal)
            binding.btnCreateCollection.isEnabled = value.trim().isNotBlank()
            updateCharacterCount(value)
        }
        binding.btnCreateCollection.setOnClickListener { submit() }
    }

    private fun updateCharacterCount(value: String) {
        binding.tvCharacterCount.text = "${value.length}/100"
    }

    private fun submit() {
        val name = binding.etCollectionName.text?.toString().orEmpty().trim()
        if (name.isBlank()) {
            showNameError(getString(R.string.library_name_required))
            return
        }

        val isRenameMode = screenMode == MODE_RENAME
        val allCollections = FlashCardLibraryStore.getCollections(this)
        val hasDuplicate = allCollections.any {
            if (isRenameMode) {
                it.id != collectionId && it.name.equals(name, ignoreCase = true)
            } else {
                it.name.equals(name, ignoreCase = true)
            }
        }
        if (hasDuplicate) {
            showNameError(getString(R.string.library_name_duplicate))
            return
        }

        if (isRenameMode) {
            val updatedCollection = FlashCardLibraryStore.renameCollection(this, collectionId, name)
            if (updatedCollection == null) {
                showNameError(getString(R.string.library_name_duplicate))
                return
            }
            finishWithMessage(getString(R.string.library_collection_updated))
            return
        }

        val createdCollection = FlashCardLibraryStore.createCollection(this, name)
            val saveResult = if (pendingTerm.isNotBlank()) {
            FlashCardLibraryStore.saveCard(
                context = this,
                collectionId = createdCollection.id,
                term = pendingTerm,
                definition = pendingDefinition.ifBlank { pendingTerm },
                sourceLanguageCode = pendingSourceLanguage,
                targetLanguageCode = pendingTargetLanguage,
                manualDifficulty = pendingDifficulty
            )
        } else {
            null
        }

        val message = if (saveResult != null) {
            getString(R.string.library_save_success, saveResult.collectionName)
        } else {
            getString(R.string.library_collection_created)
        }
        finishWithMessage(message)
    }

    private fun showNameError(message: String) {
        binding.nameInputContainer.setBackgroundResource(R.drawable.bg_auth_input_error)
        binding.tvNameError.text = message
        binding.tvNameError.isVisible = true
    }

    private fun finishWithMessage(message: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT_MESSAGE, message))
        finish()
    }
}
