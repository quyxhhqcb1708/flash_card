package com.example.xq.flashcard.ui.main.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentPracticeBinding
import com.example.xq.flashcard.databinding.ItemPracticeScheduleCardBinding
import com.example.xq.flashcard.databinding.ItemPracticeSectionBinding
import com.example.xq.flashcard.ui.practice.PracticeOverviewBuilder
import com.example.xq.flashcard.ui.practice.PracticeUiFormatter
import com.example.xq.flashcard.ui.practice.ReviewSessionActivity

class PracticeFragment : BaseFragment<FragmentPracticeBinding>() {

    private val reviewLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            bindDashboard()
        }

    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPracticeBinding {
        return FragmentPracticeBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStartDueReview.setOnClickListener { openDueReview() }
        binding.btnPracticeAll.setOnClickListener { openAllReview() }
    }

    override fun onResume() {
        super.onResume()
        bindDashboard()
    }

    private fun bindDashboard() {
        val dashboard = PracticeOverviewBuilder.build(requireContext())
        val hasEnoughCards = dashboard.totalCards >= 4
        binding.tvDueTodayCount.text = getString(R.string.practice_due_count, dashboard.dueTodayCount)
        binding.tvTotalCardsCount.text = getString(R.string.practice_total_count, dashboard.totalCards)
        binding.tvLearningCount.text = getString(R.string.practice_learning_count, dashboard.learningCount)
        binding.tvMasteredCount.text = getString(R.string.practice_mastered_count, dashboard.masteredCount)

        val hasCards = dashboard.totalCards > 0
        val hasDueToday = dashboard.dueTodayCount > 0
        binding.emptyStateContainer.isVisible = !hasEnoughCards
        binding.scheduleSection.isVisible = hasEnoughCards
        binding.btnStartDueReview.isEnabled = hasEnoughCards
        binding.btnPracticeAll.isVisible = hasEnoughCards && hasDueToday
        binding.btnStartDueReview.text = getString(
            if (hasDueToday) R.string.practice_start_due else R.string.practice_start_all
        )
        binding.scheduleContainer.removeAllViews()

        if (!hasEnoughCards) {
            return
        }

        dashboard.buckets.forEach { bucket ->
            val sectionBinding = ItemPracticeSectionBinding.inflate(
                layoutInflater,
                binding.scheduleContainer,
                false
            )
            sectionBinding.tvSectionTitle.text = bucket.title
            sectionBinding.tvSectionSubtitle.text = getString(
                R.string.practice_section_count,
                bucket.entries.size
            )
            bucket.entries.forEach { entry ->
                val cardBinding = ItemPracticeScheduleCardBinding.inflate(
                    layoutInflater,
                    sectionBinding.cardsContainer,
                    false
                )
                cardBinding.tvDueChip.text = PracticeUiFormatter.formatDueLabel(
                    requireContext(),
                    entry.card,
                    System.currentTimeMillis()
                )
                cardBinding.tvTerm.text = entry.card.term
                cardBinding.tvDefinition.text = entry.card.definition
                cardBinding.tvCollectionName.text = entry.collectionName
                cardBinding.tvSrsMeta.text = PracticeUiFormatter.formatSrsMeta(
                    requireContext(),
                    entry.card
                )
                sectionBinding.cardsContainer.addView(cardBinding.root)
            }
            binding.scheduleContainer.addView(sectionBinding.root)
        }
    }

    private fun openDueReview() {
        val dashboard = PracticeOverviewBuilder.build(requireContext())
        if (dashboard.totalCards < 4) {
            Toast.makeText(requireContext(), R.string.practice_need_more_cards, Toast.LENGTH_SHORT).show()
            return
        }
        reviewLauncher.launch(
            if (dashboard.dueTodayCount > 0) {
                ReviewSessionActivity.createDueIntent(requireContext())
            } else {
                ReviewSessionActivity.createAllIntent(requireContext())
            }
        )
    }

    private fun openAllReview() {
        val dashboard = PracticeOverviewBuilder.build(requireContext())
        if (dashboard.totalCards < 4) {
            Toast.makeText(requireContext(), R.string.practice_need_more_cards, Toast.LENGTH_SHORT).show()
            return
        }
        reviewLauncher.launch(ReviewSessionActivity.createAllIntent(requireContext()))
    }
}
