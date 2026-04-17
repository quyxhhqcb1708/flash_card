package com.example.xq.flashcard.ui.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityTranslateBinding
import com.example.xq.flashcard.ui.library.CreateCollectionActivity
import com.example.xq.flashcard.ui.library.SaveToFlashcardActivity
import com.example.xq.flashcard.ui.scan.ImageBitmapLoader
import com.example.xq.flashcard.ui.scan.TextRecognitionCoordinator

class TranslateActivity : BaseActivity<ActivityTranslateBinding>() {

    companion object {
        const val EXTRA_SOURCE_TEXT = "extra_source_text"
        const val EXTRA_RESULT_TEXT = "extra_result_text"
        const val EXTRA_SOURCE_LANGUAGE = "extra_source_language"
        const val EXTRA_TARGET_LANGUAGE = "extra_target_language"
    }

    private val translationManager = TranslationManager()
    private val recognitionCoordinator = TextRecognitionCoordinator()
    private val handler = Handler(Looper.getMainLooper())

    private var pendingTranslate: Runnable? = null
    private var sourceLanguage = AppTranslationLanguage.ENGLISH
    private var targetLanguage = AppTranslationLanguage.VIETNAMESE
    private var internalTextChange = false
    private var hasTranslationResult = false

    private val saveFlashcardLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.getStringExtra(CreateCollectionActivity.EXTRA_RESULT_MESSAGE)?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val bitmap = ImageBitmapLoader.loadBitmap(this, uri)
        if (bitmap == null) {
            Toast.makeText(this, R.string.scan_gallery_empty, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        binding.progressTranslate.isVisible = true
        recognitionCoordinator.processBitmap(
            bitmap = bitmap,
            onSuccess = { payload ->
                runOnUiThread {
                    binding.progressTranslate.isVisible = false
                    if (payload.fullText.isBlank()) {
                        Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    setSourceText(payload.fullText, triggerTranslate = true)
                }
            },
            onError = {
                runOnUiThread {
                    binding.progressTranslate.isVisible = false
                    Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityTranslateBinding {
        return ActivityTranslateBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resolveInitialLanguages()
        setupUi()
        consumeIncomingPayload()
    }

    override fun onDestroy() {
        cancelPendingTranslation()
        translationManager.close()
        recognitionCoordinator.close()
        super.onDestroy()
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnClearSource.setOnClickListener {
            setSourceText("", triggerTranslate = false)
            showPlaceholderResult()
        }
        binding.btnPickImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSwapLanguage.setOnClickListener { swapLanguages() }
        binding.btnFlashCard.setOnClickListener {
            saveCurrentFlashcard()
        }
        binding.btnCopyResult.setOnClickListener { copyResultToClipboard() }
        binding.etSource.doAfterTextChanged { editable ->
            if (internalTextChange) return@doAfterTextChanged
            scheduleTranslation(editable?.toString().orEmpty())
        }
        updateLanguageUi()
        showPlaceholderResult()
    }

    private fun resolveInitialLanguages() {
        sourceLanguage = AppTranslationLanguage.fromCode(intent.getStringExtra(EXTRA_SOURCE_LANGUAGE))
        targetLanguage = AppTranslationLanguage.fromCode(intent.getStringExtra(EXTRA_TARGET_LANGUAGE))
        if (sourceLanguage == targetLanguage) {
            targetLanguage = if (sourceLanguage == AppTranslationLanguage.ENGLISH) {
                AppTranslationLanguage.VIETNAMESE
            } else {
                AppTranslationLanguage.ENGLISH
            }
        }
    }

    private fun consumeIncomingPayload() {
        val sourceText = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val translatedText = intent.getStringExtra(EXTRA_RESULT_TEXT).orEmpty()
        if (sourceText.isNotBlank()) {
            setSourceText(sourceText, triggerTranslate = translatedText.isBlank())
        }
        if (translatedText.isNotBlank()) {
            setResultText(translatedText)
        }
    }

    private fun scheduleTranslation(text: String) {
        cancelPendingTranslation()
        val runnable = Runnable { translateSourceText(text) }
        pendingTranslate = runnable
        handler.postDelayed(runnable, 300)
    }

    private fun translateSourceText(text: String) {
        if (text.isBlank()) {
            showPlaceholderResult()
            return
        }
        binding.progressTranslate.isVisible = true
        translationManager.translateText(
            text = text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onSuccess = { translated ->
                binding.progressTranslate.isVisible = false
                setResultText(translated.ifBlank { text })
            },
            onError = {
                binding.progressTranslate.isVisible = false
                Toast.makeText(this, R.string.scan_translation_error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setSourceText(text: String, triggerTranslate: Boolean) {
        internalTextChange = true
        binding.etSource.setText(text)
        binding.etSource.setSelection(binding.etSource.text?.length ?: 0)
        internalTextChange = false
        if (triggerTranslate) {
            scheduleTranslation(text)
        } else {
            cancelPendingTranslation()
        }
    }

    private fun setResultText(text: String) {
        hasTranslationResult = true
        binding.tvResult.text = text
        binding.tvResult.setTextColor(getColor(R.color.auth_text_primary))
    }

    private fun showPlaceholderResult() {
        hasTranslationResult = false
        binding.tvResult.text = getString(R.string.scan_result_hint)
        binding.tvResult.setTextColor(getColor(R.color.auth_text_hint))
    }

    private fun swapLanguages() {
        val previousSource = sourceLanguage
        sourceLanguage = targetLanguage
        targetLanguage = previousSource
        updateLanguageUi()
        val newSource = if (hasTranslationResult) {
            binding.tvResult.text.toString()
        } else {
            binding.etSource.text?.toString().orEmpty()
        }
        setSourceText(newSource, triggerTranslate = true)
    }

    private fun updateLanguageUi() {
        binding.chipSourceLanguage.text = getString(sourceLanguage.labelRes)
        binding.chipTargetLanguage.text = getString(targetLanguage.labelRes)
        binding.chipSourceLanguage.setBackgroundResource(R.drawable.bg_scan_chip_selected)
        binding.chipTargetLanguage.setBackgroundResource(R.drawable.bg_scan_chip_selected)
        binding.chipSourceLanguage.setTextColor(getColor(R.color.auth_surface))
        binding.chipTargetLanguage.setTextColor(getColor(R.color.auth_surface))
    }

    private fun copyResultToClipboard() {
        if (!hasTranslationResult) {
            Toast.makeText(this, R.string.scan_input_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", binding.tvResult.text))
        Toast.makeText(this, R.string.scan_copy_done, Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentFlashcard() {
        val sourceText = binding.etSource.text?.toString().orEmpty().trim()
        val translatedText = binding.tvResult.text?.toString().orEmpty().trim()
        if (sourceText.isBlank() || !hasTranslationResult) {
            Toast.makeText(this, R.string.scan_input_empty, Toast.LENGTH_SHORT).show()
            return
        }
        saveFlashcardLauncher.launch(
            SaveToFlashcardActivity.createIntent(
                context = this,
                term = sourceText,
                definition = translatedText.ifBlank { sourceText },
                sourceLanguageCode = sourceLanguage.mlKitCode,
                targetLanguageCode = targetLanguage.mlKitCode
            )
        )
    }

    private fun cancelPendingTranslation() {
        pendingTranslate?.let { handler.removeCallbacks(it) }
        pendingTranslate = null
    }
}
