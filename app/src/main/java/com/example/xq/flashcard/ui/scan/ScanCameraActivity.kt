package com.example.xq.flashcard.ui.scan

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityScanCameraBinding
import com.example.xq.flashcard.ui.translate.AppTranslationLanguage
import com.example.xq.flashcard.ui.translate.TranslateActivity
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
            if (selectedSourceText.isNotBlank()) {
                translateSelectedPhrase(selectedSourceText)
            }
        }
        binding.btnCapture.setOnClickListener { captureCurrentFrame() }
        binding.btnScanAgain.setOnClickListener { exitReviewMode() }
        binding.btnTranslateAll.setOnClickListener {
            if (currentFullText.isBlank()) {
                Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
            } else {
                openTranslateScreen(currentFullText)
            }
        }
        binding.btnGoToTranslate.setOnClickListener {
            if (selectedSourceText.isBlank()) {
                Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
            } else {
                openTranslateScreen(selectedSourceText, selectedTranslatedText)
            }
        }
        binding.ocrOverlay.onBlockTapped = { block ->
            if (isReviewMode) {
                translateSelectedPhrase(block.text)
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
        binding.ivCapturedPreview.setImageBitmap(bitmap)
        binding.ivCapturedPreview.isVisible = true
        binding.captureActions.isVisible = false
        binding.reviewActions.isVisible = true
        binding.cardSelection.isVisible = false
        binding.tvHint.text = getString(R.string.scan_review_hint)
        binding.progressProcessing.isVisible = true
        binding.ocrOverlay.clearBlocks()
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
                    binding.ocrOverlay.setInteractionEnabled(payload.blocks.isNotEmpty())
                    if (payload.blocks.isNotEmpty()) {
                        val firstBlock = payload.blocks.first()
                        binding.ocrOverlay.selectBlock(firstBlock.text)
                        translateSelectedPhrase(firstBlock.text)
                    }
                    if (payload.fullText.isBlank()) {
                        Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = {
                runOnUiThread {
                    binding.progressProcessing.isVisible = false
                    Toast.makeText(this, R.string.scan_no_text_found, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun exitReviewMode() {
        isReviewMode = false
        selectedSourceText = ""
        selectedTranslatedText = ""
        binding.ivCapturedPreview.isVisible = false
        binding.captureActions.isVisible = true
        binding.reviewActions.isVisible = false
        binding.cardSelection.isVisible = false
        binding.tvHint.text = getString(R.string.scan_live_hint)
        binding.progressProcessing.isVisible = false
        binding.ocrOverlay.clearBlocks()
        binding.ocrOverlay.setInteractionEnabled(false)
    }

    private fun translateSelectedPhrase(text: String) {
        selectedSourceText = text
        binding.ocrOverlay.selectBlock(text)
        binding.cardSelection.isVisible = true
        binding.tvSelectedSource.text = text
        binding.tvSelectedTranslation.text = getString(R.string.scan_translation_loading)
        translationManager.translateText(
            text = text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onSuccess = { translated ->
                selectedTranslatedText = translated
                binding.tvSelectedTranslation.text = translated.ifBlank { text }
            },
            onError = {
                selectedTranslatedText = ""
                binding.tvSelectedTranslation.text = getString(R.string.scan_translation_error)
            }
        )
    }

    private fun openTranslateScreen(sourceText: String, translatedText: String? = null) {
        val intent = Intent(this, TranslateActivity::class.java).apply {
            putExtra(TranslateActivity.EXTRA_SOURCE_TEXT, sourceText)
            putExtra(TranslateActivity.EXTRA_SOURCE_LANGUAGE, sourceLanguage.mlKitCode)
            putExtra(TranslateActivity.EXTRA_TARGET_LANGUAGE, targetLanguage.mlKitCode)
            if (!translatedText.isNullOrBlank()) {
                putExtra(TranslateActivity.EXTRA_RESULT_TEXT, translatedText)
            }
        }
        startActivity(intent)
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
