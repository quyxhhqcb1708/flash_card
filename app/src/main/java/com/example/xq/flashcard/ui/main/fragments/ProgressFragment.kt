package com.example.xq.flashcard.ui.main.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentProgressBinding
import com.example.xq.flashcard.databinding.ItemProgressCollectionBinding
import com.example.xq.flashcard.ui.library.LibraryCollectionActivity
import com.example.xq.flashcard.ui.library.LibraryUiHelper
import com.example.xq.flashcard.ui.progress.CollectionProgressSummary
import com.example.xq.flashcard.ui.progress.LearningProgressBuilder
import com.example.xq.flashcard.ui.progress.ProgressChartEntry
import com.example.xq.flashcard.ui.progress.ProgressUiFormatter
import kotlin.math.roundToInt

class ProgressFragment : BaseFragment<FragmentProgressBinding>() {

    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProgressBinding {
        return FragmentProgressBinding.inflate(layoutInflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        bindDashboard()
    }

    private fun bindDashboard() {
        val context = requireContext()
        val dashboard = LearningProgressBuilder.build(context)

        binding.tvTotalSavedValue.text = dashboard.totalSavedCount.toString()
        binding.tvMasteredValue.text = dashboard.masteredCount.toString()
        binding.tvDueTodayValue.text = dashboard.dueTodayCount.toString()
        binding.tvAccuracyValue.text = ProgressUiFormatter.formatPercent(dashboard.averageAccuracyRate)
        binding.tvCollectionsValue.text = dashboard.totalCollections.toString()
        binding.tvPracticedValue.text = dashboard.practicedCount.toString()
        binding.tvFocusMessage.text = ProgressUiFormatter.buildFocusMessage(context, dashboard)

        binding.progressChart.setEntries(
            listOf(
                ProgressChartEntry(
                    label = getString(R.string.progress_chart_due),
                    value = dashboard.distribution.dueCount,
                    color = ContextCompat.getColor(context, R.color.progress_due)
                ),
                ProgressChartEntry(
                    label = getString(R.string.progress_chart_learning),
                    value = dashboard.distribution.learningCount,
                    color = ContextCompat.getColor(context, R.color.progress_learning)
                ),
                ProgressChartEntry(
                    label = getString(R.string.progress_chart_upcoming),
                    value = dashboard.distribution.upcomingCount,
                    color = ContextCompat.getColor(context, R.color.progress_upcoming)
                ),
                ProgressChartEntry(
                    label = getString(R.string.progress_chart_stable),
                    value = dashboard.distribution.stableCount,
                    color = ContextCompat.getColor(context, R.color.progress_stable)
                )
            )
        )
        binding.progressChart.contentDescription =
            ProgressUiFormatter.buildChartDescription(context, dashboard)

        bindCollections(dashboard.collections)
    }

    private fun bindCollections(collections: List<CollectionProgressSummary>) {
        binding.collectionsContainer.removeAllViews()
        binding.tvCollectionsEmpty.isVisible = collections.isEmpty()
        collections.forEach { summary ->
            val itemBinding = ItemProgressCollectionBinding.inflate(
                layoutInflater,
                binding.collectionsContainer,
                false
            )
            itemBinding.coverFrame.setBackgroundResource(
                LibraryUiHelper.getCoverBackgroundRes(summary.collectionId, summary.name)
            )
            itemBinding.tvCollectionInitial.text = LibraryUiHelper.getCollectionInitial(summary.name)
            itemBinding.tvCollectionName.text = summary.name
            itemBinding.tvCollectionMeta.text = getString(
                R.string.progress_collection_meta,
                summary.totalCards,
                summary.practicedCount
            )
            itemBinding.tvDueChip.isVisible = summary.dueTodayCount > 0
            itemBinding.tvDueChip.text = getString(
                R.string.progress_collection_due,
                summary.dueTodayCount
            )
            itemBinding.tvLearningChip.isVisible =
                summary.learningCount > 0 || summary.upcomingCount > 0
            itemBinding.tvLearningChip.text = getString(
                if (summary.learningCount > 0) {
                    R.string.progress_collection_learning
                } else {
                    R.string.progress_collection_upcoming
                },
                if (summary.learningCount > 0) summary.learningCount else summary.upcomingCount
            )
            itemBinding.tvMasteredChip.isVisible = summary.masteredCount > 0
            itemBinding.tvMasteredChip.text = getString(
                R.string.progress_collection_mastered,
                summary.masteredCount
            )
            itemBinding.progressMastery.progress = (summary.masteryRate * 100f).roundToInt()
            itemBinding.tvMasteryValue.text = getString(
                R.string.progress_collection_mastery,
                ProgressUiFormatter.formatPercent(summary.masteryRate)
            )
            itemBinding.tvAccuracyValue.text = getString(
                R.string.progress_collection_accuracy,
                ProgressUiFormatter.formatPercent(summary.accuracyRate)
            )
            itemBinding.root.setOnClickListener {
                startActivity(
                    LibraryCollectionActivity.createIntent(
                        requireContext(),
                        summary.collectionId
                    )
                )
            }
            binding.collectionsContainer.addView(itemBinding.root)
        }
    }
}
