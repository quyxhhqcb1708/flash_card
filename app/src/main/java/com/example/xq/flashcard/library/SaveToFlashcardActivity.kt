package com.example.xq.flashcard.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivitySaveToFlashcardBinding
import com.example.xq.flashcard.databinding.ItemSaveTargetCollectionBinding

class SaveToFlashcardActivity : BaseActivity<ActivitySaveToFlashcardBinding>() {

    companion object {
        private const val EXTRA_TERM = "extra_term"
        private const val EXTRA_DEFINITION = "extra_definition"
        private const val EXTRA_SOURCE_LANGUAGE = "extra_source_language"
        private const val EXTRA_TARGET_LANGUAGE = "extra_target_language"

        fun createIntent(
            context: Context,
            term: String,
            definition: String,
            sourceLanguageCode: String,
            targetLanguageCode: String
        ): Intent {
            return Intent(context, SaveToFlashcardActivity::class.java).apply {
                putExtra(EXTRA_TERM, term)
                putExtra(EXTRA_DEFINITION, definition)
                putExtra(EXTRA_SOURCE_LANGUAGE, sourceLanguageCode)
                putExtra(EXTRA_TARGET_LANGUAGE, targetLanguageCode)
            }
        }
    }

    private val createCollectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK, result.data)
                finish()
            }
        }

    private val term by lazy { intent.getStringExtra(EXTRA_TERM).orEmpty().trim() }
    private val definition by lazy { intent.getStringExtra(EXTRA_DEFINITION).orEmpty().trim() }
    private val sourceLanguageCode by lazy { intent.getStringExtra(EXTRA_SOURCE_LANGUAGE).orEmpty() }
    private val targetLanguageCode by lazy { intent.getStringExtra(EXTRA_TARGET_LANGUAGE).orEmpty() }
    private var selectedDifficulty: FlashCardDifficulty = FlashCardDifficulty.MEDIUM

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivitySaveToFlashcardBinding {
        return ActivitySaveToFlashcardBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (term.isBlank()) {
            Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setupUi()
        bindCollections()
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnClose.setOnClickListener { finish() }
        binding.btnDifficultyEasy.setOnClickListener { updateDifficultySelection(FlashCardDifficulty.EASY) }
        binding.btnDifficultyMedium.setOnClickListener { updateDifficultySelection(FlashCardDifficulty.MEDIUM) }
        binding.btnDifficultyHard.setOnClickListener { updateDifficultySelection(FlashCardDifficulty.HARD) }
        updateDifficultySelection(selectedDifficulty)
        binding.rowCreateCollection.setOnClickListener {
            createCollectionLauncher.launch(
                CreateCollectionActivity.createIntentForSave(
                    context = this,
                    term = term,
                    definition = definition.ifBlank { term },
                    sourceLanguageCode = sourceLanguageCode,
                    targetLanguageCode = targetLanguageCode,
                    manualDifficulty = selectedDifficulty
                )
            )
        }
    }

    private fun updateDifficultySelection(difficulty: FlashCardDifficulty) {
        selectedDifficulty = difficulty
        bindDifficultyButton(
            button = binding.btnDifficultyEasy,
            isSelected = difficulty == FlashCardDifficulty.EASY,
            selectedBackgroundRes = R.drawable.bg_difficulty_easy,
            selectedTextColorRes = R.color.library_success_text
        )
        bindDifficultyButton(
            button = binding.btnDifficultyMedium,
            isSelected = difficulty == FlashCardDifficulty.MEDIUM,
            selectedBackgroundRes = R.drawable.bg_difficulty_medium,
            selectedTextColorRes = R.color.practice_medium_text
        )
        bindDifficultyButton(
            button = binding.btnDifficultyHard,
            isSelected = difficulty == FlashCardDifficulty.HARD,
            selectedBackgroundRes = R.drawable.bg_difficulty_hard,
            selectedTextColorRes = R.color.practice_hard_text
        )
    }

    private fun bindDifficultyButton(
        button: com.google.android.material.button.MaterialButton,
        isSelected: Boolean,
        selectedBackgroundRes: Int,
        selectedTextColorRes: Int
    ) {
        button.setBackgroundResource(
            if (isSelected) selectedBackgroundRes else R.drawable.bg_difficulty_unselected
        )
        button.setTextColor(
            ContextCompat.getColor(
                this,
                if (isSelected) selectedTextColorRes else R.color.auth_text_secondary
            )
        )
    }

    private fun bindCollections() {
        binding.collectionsContainer.removeAllViews()
        FlashCardLibraryStore.getCollections(this).forEach { collection ->
            val itemBinding = ItemSaveTargetCollectionBinding.inflate(layoutInflater, binding.collectionsContainer, false)
            itemBinding.coverFrame.setBackgroundResource(
                LibraryUiHelper.getCoverBackgroundRes(collection.id, collection.name)
            )
            itemBinding.tvCollectionInitial.text = LibraryUiHelper.getCollectionInitial(collection.name)
            itemBinding.tvCollectionName.text = collection.name
            itemBinding.root.setOnClickListener {
                val result = FlashCardLibraryStore.saveCard(
                    context = this,
                    collectionId = collection.id,
                    term = term,
                    definition = definition.ifBlank { term },
                    sourceLanguageCode = sourceLanguageCode,
                    targetLanguageCode = targetLanguageCode,
                    manualDifficulty = selectedDifficulty
                )
                if (result == null) {
                    Toast.makeText(this, R.string.scan_translation_error, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(
                        CreateCollectionActivity.EXTRA_RESULT_MESSAGE,
                        getString(R.string.library_save_success, result.collectionName)
                    )
                )
                finish()
            }
            binding.collectionsContainer.addView(itemBinding.root)
        }
    }
}
