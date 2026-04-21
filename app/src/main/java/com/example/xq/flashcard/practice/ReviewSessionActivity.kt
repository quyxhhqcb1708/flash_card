package com.example.xq.flashcard.ui.practice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityReviewSessionBinding
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.google.android.material.button.MaterialButton
import java.io.Serializable

class ReviewSessionActivity : BaseActivity<ActivityReviewSessionBinding>() {

    companion object {
        private const val EXTRA_COLLECTION_ID = "extra_collection_id"
        private const val EXTRA_DUE_ONLY = "extra_due_only"

        private const val STATE_QUESTIONS = "state_questions"
        private const val STATE_CURRENT_INDEX = "state_current_index"
        private const val STATE_SELECTED_ANSWER = "state_selected_answer"
        private const val STATE_HAS_ANSWERED = "state_has_answered"
        private const val STATE_CORRECT_COUNT = "state_correct_count"
        private const val STATE_WRONG_COUNT = "state_wrong_count"
        private const val STATE_COMPLETED = "state_completed"

        fun createDueIntent(context: Context): Intent {
            return Intent(context, ReviewSessionActivity::class.java).apply {
                putExtra(EXTRA_DUE_ONLY, true)
            }
        }

        fun createAllIntent(context: Context): Intent {
            return Intent(context, ReviewSessionActivity::class.java).apply {
                putExtra(EXTRA_DUE_ONLY, false)
            }
        }

        fun createCollectionIntent(context: Context, collectionId: Long): Intent {
            return Intent(context, ReviewSessionActivity::class.java).apply {
                putExtra(EXTRA_COLLECTION_ID, collectionId)
                putExtra(EXTRA_DUE_ONLY, false)
            }
        }
    }

    private val collectionId: Long? by lazy {
        intent.getLongExtra(EXTRA_COLLECTION_ID, -1L).takeIf { it > 0L }
    }
    private val dueOnly: Boolean by lazy { intent.getBooleanExtra(EXTRA_DUE_ONLY, true) }

    private val optionButtons: List<MaterialButton> by lazy {
        listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
    }

    private var questions: List<PracticeQuestion> = emptyList()
    private var currentIndex: Int = 0
    private var selectedAnswer: String? = null
    private var hasAnswered: Boolean = false
    private var correctCount: Int = 0
    private var wrongCount: Int = 0
    private var isCompleted: Boolean = false

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityReviewSessionBinding {
        return ActivityReviewSessionBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreState(savedInstanceState)
        setupUi()
        bindSession()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(STATE_QUESTIONS, ArrayList(questions) as Serializable)
        outState.putInt(STATE_CURRENT_INDEX, currentIndex)
        outState.putString(STATE_SELECTED_ANSWER, selectedAnswer)
        outState.putBoolean(STATE_HAS_ANSWERED, hasAnswered)
        outState.putInt(STATE_CORRECT_COUNT, correctCount)
        outState.putInt(STATE_WRONG_COUNT, wrongCount)
        outState.putBoolean(STATE_COMPLETED, isCompleted)
    }

    @Suppress("UNCHECKED_CAST")
    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        val restoredQuestions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState.getSerializable(STATE_QUESTIONS, ArrayList::class.java) as? ArrayList<PracticeQuestion>
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState.getSerializable(STATE_QUESTIONS) as? ArrayList<PracticeQuestion>
        }
        questions = restoredQuestions.orEmpty()
        currentIndex = savedInstanceState.getInt(STATE_CURRENT_INDEX, 0)
        selectedAnswer = savedInstanceState.getString(STATE_SELECTED_ANSWER)
        hasAnswered = savedInstanceState.getBoolean(STATE_HAS_ANSWERED, false)
        correctCount = savedInstanceState.getInt(STATE_CORRECT_COUNT, 0)
        wrongCount = savedInstanceState.getInt(STATE_WRONG_COUNT, 0)
        isCompleted = savedInstanceState.getBoolean(STATE_COMPLETED, false)
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finishWithRefresh() }
        binding.btnDone.setOnClickListener { finishWithRefresh() }
        binding.btnContinue.setOnClickListener { moveToNextQuestion() }
        optionButtons.forEach { button ->
            button.setOnClickListener {
                handleOptionSelected(button.text?.toString().orEmpty())
            }
        }
    }

    private fun bindSession() {
        if (questions.isEmpty()) {
            questions = PracticeSessionBuilder.buildSession(
                context = this,
                collectionId = collectionId,
                dueOnly = dueOnly
            )
        }

        if (questions.isEmpty()) {
            Toast.makeText(this, R.string.practice_need_more_cards, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvReviewScope.text = when {
            collectionId != null -> FlashCardLibraryStore.getCollection(this, collectionId!!)?.name.orEmpty()
            dueOnly -> getString(R.string.practice_review_scope_due)
            else -> getString(R.string.practice_review_scope_all)
        }

        if (isCompleted) {
            showCompleteState()
        } else {
            showQuestionState()
        }
    }

    private fun showQuestionState() {
        binding.reviewContent.isVisible = true
        binding.completeContent.isVisible = false
        bindCurrentQuestion()
    }

    private fun bindCurrentQuestion() {
        val question = questions.getOrNull(currentIndex) ?: return
        binding.tvProgress.text = getString(
            R.string.practice_progress,
            currentIndex + 1,
            questions.size
        )
        binding.progressIndicator.max = questions.size
        binding.progressIndicator.progress = currentIndex + 1
        binding.tvCollectionName.text = question.collectionName
        binding.tvPrompt.text = question.prompt
        binding.tvLanguagePair.text = question.languagePair
        binding.tvFeedback.isVisible = false
        binding.tvCorrectAnswer.isVisible = false
        binding.btnContinue.isEnabled = hasAnswered
        binding.btnContinue.alpha = if (hasAnswered) 1f else 0.55f

        optionButtons.forEachIndexed { index, button ->
            button.text = question.options.getOrNull(index).orEmpty()
            applyOptionState(button, OptionState.DEFAULT)
        }

        if (hasAnswered) {
            renderAnsweredState(question)
        }
    }

    private fun handleOptionSelected(answer: String) {
        if (hasAnswered) return
        val question = questions.getOrNull(currentIndex) ?: return
        selectedAnswer = answer
        hasAnswered = true

        val currentCard = FlashCardLibraryStore.getCard(this, question.collectionId, question.cardId)
        val isCorrect = answer == question.correctAnswer
        val quality = PracticeReviewScorer.qualityForAnswer(
            card = currentCard ?: return,
            isCorrect = isCorrect
        )
        FlashCardLibraryStore.recordReviewQuality(
            context = this,
            collectionId = question.collectionId,
            cardId = question.cardId,
            quality = quality
        )

        if (isCorrect) {
            correctCount += 1
        } else {
            wrongCount += 1
        }

        renderAnsweredState(question)
    }

    private fun renderAnsweredState(question: PracticeQuestion) {
        val answer = selectedAnswer.orEmpty()
        val isCorrect = answer == question.correctAnswer
        optionButtons.forEach { button ->
            val optionText = button.text?.toString().orEmpty()
            val state = when {
                optionText == question.correctAnswer -> OptionState.CORRECT
                optionText == answer && !isCorrect -> OptionState.INCORRECT
                else -> OptionState.DEFAULT
            }
            applyOptionState(button, state)
        }

        binding.tvFeedback.isVisible = true
        binding.tvFeedback.text = getString(
            if (isCorrect) R.string.practice_answer_correct else R.string.practice_answer_wrong
        )
        binding.tvFeedback.setTextColor(
            ContextCompat.getColor(
                this,
                if (isCorrect) R.color.library_success_text else R.color.practice_hard_text
            )
        )
        binding.tvCorrectAnswer.isVisible = !isCorrect
        if (!isCorrect) {
            binding.tvCorrectAnswer.text = getString(
                R.string.practice_correct_answer,
                question.correctAnswer
            )
        }
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = 1f
    }

    private fun moveToNextQuestion() {
        if (!hasAnswered) return
        if (currentIndex < questions.lastIndex) {
            currentIndex += 1
            selectedAnswer = null
            hasAnswered = false
            bindCurrentQuestion()
            return
        }

        isCompleted = true
        showCompleteState()
    }

    private fun showCompleteState() {
        binding.reviewContent.isVisible = false
        binding.completeContent.isVisible = true
        binding.tvProgress.text = getString(
            R.string.practice_progress,
            questions.size,
            questions.size
        )
        binding.progressIndicator.max = questions.size.coerceAtLeast(1)
        binding.progressIndicator.progress = questions.size
        val reviewedCount = correctCount + wrongCount
        val accuracy = if (reviewedCount == 0) 0 else (correctCount * 100) / reviewedCount
        binding.tvReviewedCount.text = getString(R.string.practice_complete_reviewed, reviewedCount)
        binding.tvCorrectCount.text = getString(R.string.practice_complete_correct, correctCount)
        binding.tvWrongCount.text = getString(R.string.practice_complete_wrong, wrongCount)
        binding.tvAccuracy.text = getString(R.string.practice_complete_accuracy, accuracy)
        binding.tvDueLeft.text = getString(
            R.string.practice_complete_due_left,
            PracticeOverviewBuilder.buildReviewQueue(
                context = this,
                collectionId = collectionId,
                dueOnly = true
            ).size
        )
    }

    private fun applyOptionState(button: MaterialButton, state: OptionState) {
        when (state) {
            OptionState.DEFAULT -> {
                button.setBackgroundResource(R.drawable.bg_library_secondary_button)
                button.setTextColor(ContextCompat.getColor(this, R.color.auth_text_primary))
            }

            OptionState.CORRECT -> {
                button.setBackgroundResource(R.drawable.bg_difficulty_easy)
                button.setTextColor(ContextCompat.getColor(this, R.color.library_success_text))
            }

            OptionState.INCORRECT -> {
                button.setBackgroundResource(R.drawable.bg_difficulty_hard)
                button.setTextColor(ContextCompat.getColor(this, R.color.practice_hard_text))
            }
        }
    }

    private fun finishWithRefresh() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private enum class OptionState {
        DEFAULT,
        CORRECT,
        INCORRECT
    }
}
