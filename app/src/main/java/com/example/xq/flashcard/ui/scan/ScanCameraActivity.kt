package com.example.xq.flashcard.ui.scan

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityScanCameraBinding
import com.example.xq.flashcard.ui.library.CreateCollectionActivity
import com.example.xq.flashcard.ui.library.SaveToFlashcardActivity
import com.example.xq.flashcard.ui.translate.AppTranslationLanguage
import com.example.xq.flashcard.ui.translate.TranslationManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanCameraActivity : BaseActivity<ActivityScanCameraBinding>() {

    private val recognitionCoordinator = TextRecognitionCoordinator()
    private val translationManager = TranslationManager()
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var isAnalyzingFrame = false
    private var isReviewMode = false
    private var isCapturingPhoto = false
    private var isTranslatedOverlayVisible = false
    private var translateOverlaySessionId = 0
    private var currentReviewBitmap: Bitmap? = null
    private var currentFullText = ""
    private var currentBlocks: List<OcrTextBlock> = emptyList()
    private var selectedSourceText = ""
    private var selectedTranslatedText = ""
    private var sourceLanguage = AppTranslationLanguage.ENGLISH
    private var targetLanguage = AppTranslationLanguage.VIETNAMESE

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCameraPreview()
        } else {
            Toast.makeText(this, R.string.scan_camera_permission_denied, Toast.LENGTH_SHORT).show()
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
        enterReviewMode(bitmap)
    }

    private val saveFlashcardLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(CreateCollectionActivity.EXTRA_RESULT_MESSAGE)?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
                clearCurrentSelection()
            }
        }

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityScanCameraBinding {
        return ActivityScanCameraBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
        updateLanguageUi()
        ensureCameraReady()
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        recognitionCoordinator.close()
        translationManager.close()
        super.onDestroy()
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSwapLanguage.setOnClickListener {
            val previousSource = sourceLanguage
            sourceLanguage = targetLanguage
            targetLanguage = previousSource
            updateLanguageUi()

            when {
                isTranslatedOverlayVisible && currentBlocks.isNotEmpty() -> translateAllOnImage()
                selectedSourceText.isNotBlank() -> translateSelectedPhrase(selectedSourceText)
            }
        }
        binding.btnCapture.setOnClickListener { captureCurrentFrame() }
        binding.btnScanAgain.setOnClickListener { exitReviewMode() }
        binding.btnTranslateAll.setOnClickListener {
            if (isTranslatedOverlayVisible) {
                showOriginalDetectedText()
            } else {
                translateAllOnImage()
            }
        }
        binding.btnSaveSelection.setOnClickListener { saveSelectedPhrase() }
        binding.ocrOverlay.onSelectionChanged = { selectedItems ->
            if (isReviewMode && !isTranslatedOverlayVisible) {
                if (selectedItems.isEmpty()) {
                    clearCurrentSelection()
                } else {
                    translateSelectedPhrase(selectedItems.joinToString(" ") { it.text })
                }
            }
        }
    }

    private fun ensureCameraReady() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startCameraPreview()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraPreview() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener(
            {
                try {
                    cameraProvider = providerFuture.get()
                    bindCameraUseCases()
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.scan_camera_unavailable, Toast.LENGTH_SHORT).show()
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build().apply {
            surfaceProvider = binding.previewView.surfaceProvider
        }
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isAnalyzingFrame || isReviewMode) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    isAnalyzingFrame = true
                    recognitionCoordinator.processMediaImage(
                        mediaImage = mediaImage,
                        rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                        onSuccess = { payload ->
                            runOnUiThread {
                                if (!isReviewMode) {
                                    currentFullText = payload.fullText
                                    currentBlocks = payload.blocks
                                    binding.tvHint.text = if (payload.blocks.isNotEmpty()) {
                                        getString(R.string.scan_live_detected_hint, payload.blocks.size)
                                    } else {
                                        getString(R.string.scan_live_hint)
                                    }
                                    binding.ocrOverlay.clearTranslatedBlocks()
                                    binding.ocrOverlay.setBlocks(
                                        payload.blocks,
                                        payload.imageWidth,
                                        payload.imageHeight
                                    )
                                }
                                isAnalyzingFrame = false
                                imageProxy.close()
                            }
                        },
                        onError = {
                            runOnUiThread {
                                isAnalyzingFrame = false
                                imageProxy.close()
                            }
                        }
                    )
                }
            }

        provider.unbindAll()
        try {
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                analyzer
            )
        } catch (_: Exception) {
            Toast.makeText(this, R.string.scan_camera_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureCurrentFrame() {
        if (isCapturingPhoto) return
        val capture = imageCapture
        if (capture == null) {
            val snapshot = binding.previewView.bitmap
            if (snapshot == null) {
                Toast.makeText(this, R.string.scan_capture_failed, Toast.LENGTH_SHORT).show()
                return
            }
            enterReviewMode(snapshot)
            return
        }
        isCapturingPhoto = true
        binding.progressProcessing.isVisible = true
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = ImageProxyBitmapConverter.toBitmap(image)
                    image.close()
                    runOnUiThread {
                        isCapturingPhoto = false
                        binding.progressProcessing.isVisible = false
                        if (bitmap == null) {
                            Toast.makeText(
                                this@ScanCameraActivity,
                                R.string.scan_capture_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }
                        enterReviewMode(bitmap)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        isCapturingPhoto = false
                        binding.progressProcessing.isVisible = false
                        Toast.makeText(
                            this@ScanCameraActivity,
                            R.string.scan_capture_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun enterReviewMode(bitmap: Bitmap) {
        isReviewMode = true
        isTranslatedOverlayVisible = false
        translateOverlaySessionId += 1
        currentReviewBitmap = bitmap
        currentFullText = ""
        currentBlocks = emptyList()
        binding.ivCapturedPreview.setImageBitmap(bitmap)
        binding.ivCapturedPreview.isVisible = true
        binding.captureActions.isVisible = false
        binding.reviewActions.isVisible = true
        binding.cardSelection.isVisible = false
        binding.tvHint.text = getString(R.string.scan_review_hint)
        binding.btnTranslateAll.text = getString(R.string.scan_translate_all)
        binding.btnTranslateAll.isEnabled = false
        binding.progressProcessing.isVisible = true
        binding.ocrOverlay.clearBlocks()
        binding.ocrOverlay.clearTranslatedBlocks()
        binding.ocrOverlay.setInteractionEnabled(false)
        recognitionCoordinator.processBitmap(
            bitmap = bitmap,
            onSuccess = { payload ->
                runOnUiThread {
                    binding.progressProcessing.isVisible = false
                    currentFullText = payload.fullText
                    currentBlocks = payload.blocks
                    binding.ocrOverlay.setBlocks(
                        payload.blocks,
                        payload.imageWidth,
                        payload.imageHeight
                    )
                    binding.btnTranslateAll.isEnabled = payload.blocks.isNotEmpty()
                    binding.ocrOverlay.setInteractionEnabled(payload.blocks.isNotEmpty())
                    if (payload.fullText.isBlank()) {
                        Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = {
                runOnUiThread {
                    binding.progressProcessing.isVisible = false
                    binding.btnTranslateAll.isEnabled = false
                    Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun exitReviewMode() {
        isReviewMode = false
        isTranslatedOverlayVisible = false
        translateOverlaySessionId += 1
        currentReviewBitmap = null
        currentFullText = ""
        currentBlocks = emptyList()
        binding.ivCapturedPreview.isVisible = false
        binding.captureActions.isVisible = true
        binding.reviewActions.isVisible = false
        binding.tvHint.text = getString(R.string.scan_live_hint)
        binding.progressProcessing.isVisible = false
        binding.ocrOverlay.clearBlocks()
        binding.ocrOverlay.clearTranslatedBlocks()
        binding.ocrOverlay.setInteractionEnabled(false)
        clearCurrentSelection()
    }

    private fun translateSelectedPhrase(text: String) {
        selectedSourceText = text
        binding.cardSelection.isVisible = true
        binding.tvSelectedSource.text = text
        binding.tvSelectedTranslation.text = getString(R.string.scan_translation_loading)
        binding.btnSaveSelection.isEnabled = false
        translationManager.translateText(
            text = text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onSuccess = { translated ->
                selectedTranslatedText = translated
                binding.tvSelectedTranslation.text = translated.ifBlank { text }
                binding.btnSaveSelection.isEnabled = true
            },
            onError = {
                selectedTranslatedText = ""
                binding.tvSelectedTranslation.text = getString(R.string.scan_translation_error)
                binding.btnSaveSelection.isEnabled = true
            }
        )
    }

    private fun clearCurrentSelection() {
        selectedSourceText = ""
        selectedTranslatedText = ""
        binding.cardSelection.isVisible = false
        binding.btnSaveSelection.isEnabled = false
        binding.ocrOverlay.selectBlocks(emptyList())
    }

    private fun saveSelectedPhrase() {
        if (selectedSourceText.isBlank()) {
            Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
            return
        }

        val translatedText = selectedTranslatedText
            .ifBlank { binding.tvSelectedTranslation.text?.toString().orEmpty() }
            .takeUnless {
                it == getString(R.string.scan_translation_loading) ||
                    it == getString(R.string.scan_translation_error)
            }
            .orEmpty()
        saveFlashcardLauncher.launch(
            SaveToFlashcardActivity.createIntent(
                context = this,
                term = selectedSourceText,
                definition = translatedText.ifBlank { selectedSourceText },
                sourceLanguageCode = sourceLanguage.mlKitCode,
                targetLanguageCode = targetLanguage.mlKitCode
            )
        )
    }

    private fun translateAllOnImage() {
        val groupedBlocks = OcrTextGrouping.buildTranslatedGroups(currentBlocks)
        if (groupedBlocks.isEmpty()) {
            Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
            return
        }

        val sessionId = ++translateOverlaySessionId
        isTranslatedOverlayVisible = false
        clearCurrentSelection()
        binding.progressProcessing.isVisible = true
        binding.btnTranslateAll.isEnabled = false
        binding.btnTranslateAll.text = getString(R.string.scan_translating_all)
        binding.tvHint.text = getString(R.string.scan_translation_overlay_processing_hint)
        binding.ocrOverlay.clearTranslatedBlocks()
        binding.ocrOverlay.setInteractionEnabled(false)
        translateGroupsSequentially(
            groups = groupedBlocks,
            index = 0,
            translated = mutableListOf(),
            sessionId = sessionId,
            cache = mutableMapOf()
        )
    }

    private fun translateGroupsSequentially(
        groups: List<OcrTextBlock>,
        index: Int,
        translated: MutableList<TranslatedOcrBlock>,
        sessionId: Int,
        cache: MutableMap<String, String>
    ) {
        if (sessionId != translateOverlaySessionId || isDestroyed || isFinishing) {
            return
        }
        if (index >= groups.size) {
            showTranslatedOverlay(translated)
            return
        }

        val group = groups[index]
        cache[group.text]?.let { cachedTranslation ->
            val overlayColors = ImageOverlayColorResolver.resolve(currentReviewBitmap, group.bounds)
            translated += TranslatedOcrBlock(
                sourceText = group.text,
                translatedText = cachedTranslation,
                bounds = RectF(group.bounds),
                backgroundColor = overlayColors.backgroundColor,
                textColor = overlayColors.textColor
            )
            translateGroupsSequentially(groups, index + 1, translated, sessionId, cache)
            return
        }

        translationManager.translateText(
            text = group.text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onSuccess = { result ->
                if (sessionId != translateOverlaySessionId || isDestroyed || isFinishing) return@translateText
                val translatedText = result.ifBlank { group.text }
                cache[group.text] = translatedText
                val overlayColors = ImageOverlayColorResolver.resolve(currentReviewBitmap, group.bounds)
                translated += TranslatedOcrBlock(
                    sourceText = group.text,
                    translatedText = translatedText,
                    bounds = RectF(group.bounds),
                    backgroundColor = overlayColors.backgroundColor,
                    textColor = overlayColors.textColor
                )
                translateGroupsSequentially(groups, index + 1, translated, sessionId, cache)
            },
            onError = {
                if (sessionId != translateOverlaySessionId || isDestroyed || isFinishing) return@translateText
                cache[group.text] = group.text
                val overlayColors = ImageOverlayColorResolver.resolve(currentReviewBitmap, group.bounds)
                translated += TranslatedOcrBlock(
                    sourceText = group.text,
                    translatedText = group.text,
                    bounds = RectF(group.bounds),
                    backgroundColor = overlayColors.backgroundColor,
                    textColor = overlayColors.textColor
                )
                translateGroupsSequentially(groups, index + 1, translated, sessionId, cache)
            }
        )
    }

    private fun showTranslatedOverlay(items: List<TranslatedOcrBlock>) {
        binding.progressProcessing.isVisible = false
        binding.btnTranslateAll.isEnabled = true
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.scan_translation_error, Toast.LENGTH_SHORT).show()
            return
        }

        isTranslatedOverlayVisible = true
        binding.ocrOverlay.showTranslatedBlocks(items)
        binding.tvHint.text = getString(R.string.scan_translation_overlay_hint)
        binding.btnTranslateAll.text = getString(R.string.scan_show_original)
    }

    private fun showOriginalDetectedText() {
        isTranslatedOverlayVisible = false
        binding.ocrOverlay.clearTranslatedBlocks()
        binding.ocrOverlay.setInteractionEnabled(currentBlocks.isNotEmpty())
        binding.tvHint.text = getString(R.string.scan_review_hint)
        binding.btnTranslateAll.text = getString(R.string.scan_translate_all)
        binding.btnTranslateAll.isEnabled = currentBlocks.isNotEmpty()
    }

    private fun updateLanguageUi() {
        binding.chipSourceLanguage.text = getString(sourceLanguage.labelRes)
        binding.chipTargetLanguage.text = getString(targetLanguage.labelRes)
        binding.chipSourceLanguage.setBackgroundResource(R.drawable.bg_scan_chip_selected)
        binding.chipTargetLanguage.setBackgroundResource(R.drawable.bg_scan_chip_selected)
        binding.chipSourceLanguage.setTextColor(getColor(R.color.auth_surface))
        binding.chipTargetLanguage.setTextColor(getColor(R.color.auth_surface))
    }
}
