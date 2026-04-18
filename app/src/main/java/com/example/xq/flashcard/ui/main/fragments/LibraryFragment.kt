package com.example.xq.flashcard.ui.main.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentLibraryBinding
import com.example.xq.flashcard.databinding.ItemLibraryCollectionBinding
import com.example.xq.flashcard.ui.library.CreateCollectionActivity
import com.example.xq.flashcard.ui.library.FlashCardDisplayFormatter
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.library.LibraryCollectionActivity
import com.example.xq.flashcard.ui.library.LibraryUiHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {

    private var searchQuery = ""

    private val createCollectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.getStringExtra(CreateCollectionActivity.EXTRA_RESULT_MESSAGE)?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
                bindCollections()
            }
        }

    private val collectionDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.getStringExtra(CreateCollectionActivity.EXTRA_RESULT_MESSAGE)?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
                bindCollections()
            }
        }

    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLibraryBinding {
        return FragmentLibraryBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAddCollection.setOnClickListener { openCreateCollection() }
        binding.btnCreateNow.setOnClickListener { openCreateCollection() }
        binding.etSearch.doAfterTextChanged {
            searchQuery = it?.toString().orEmpty()
            bindCollections()
        }
    }

    override fun onResume() {
        super.onResume()
        bindCollections()
    }

    private fun bindCollections() {
        val allCollections = FlashCardLibraryStore.getCollections(requireContext())
        val filteredCollections = if (searchQuery.isBlank()) {
            allCollections
        } else {
            allCollections.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        val totalCards = allCollections.sumOf { it.cards.size }
        val totalReadyCards = allCollections.sumOf {
            FlashCardLibraryStore.getPracticeSummary(it).readyToReviewCards
        }

        binding.collectionsContainer.removeAllViews()
        filteredCollections.forEach { collection ->
            val practiceSummary = FlashCardLibraryStore.getPracticeSummary(collection)
            val itemBinding = ItemLibraryCollectionBinding.inflate(layoutInflater, binding.collectionsContainer, false)
            itemBinding.coverFrame.setBackgroundResource(
                LibraryUiHelper.getCoverBackgroundRes(collection.id, collection.name)
            )
            itemBinding.tvCollectionInitial.text = LibraryUiHelper.getCollectionInitial(collection.name)
            itemBinding.tvCollectionName.text = collection.name
            itemBinding.tvCollectionCount.text = getString(R.string.library_count_label, collection.cards.size)
            itemBinding.tvPracticeSummary.text = getString(
                R.string.library_summary_ready_mastered,
                practiceSummary.readyToReviewCards,
                practiceSummary.masteredCards
            )
            itemBinding.tvLastPractice.text = getString(
                R.string.library_summary_last_practice,
                if (practiceSummary.lastPracticedAt > 0L) {
                    FlashCardDisplayFormatter.formatSavedAt(practiceSummary.lastPracticedAt)
                } else {
                    getString(R.string.library_summary_no_practice)
                }
            )
            itemBinding.root.setOnClickListener {
                collectionDetailLauncher.launch(
                    LibraryCollectionActivity.createIntent(requireContext(), collection.id)
                )
            }
            itemBinding.btnCollectionMenu.setOnClickListener { anchor ->
                showCollectionMenu(anchor, collection.id, collection.name)
            }
            binding.collectionsContainer.addView(itemBinding.root)
        }

        val hasCollections = allCollections.isNotEmpty()
        val hasFilteredResults = filteredCollections.isNotEmpty()
        binding.overviewCard.isVisible = hasCollections
        binding.topicsSectionContainer.isVisible = hasCollections && hasFilteredResults
        binding.tvOverviewTopicsValue.text = allCollections.size.toString()
        binding.tvOverviewCardsValue.text = totalCards.toString()
        binding.tvOverviewReadyValue.text = totalReadyCards.toString()
        binding.emptyStateContainer.isVisible = !hasCollections
        binding.tvSearchEmpty.isVisible = hasCollections && !hasFilteredResults
        binding.collectionsContainer.isVisible = hasFilteredResults
    }

    private fun showCollectionMenu(anchor: android.view.View, collectionId: Long, collectionName: String) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, 1, 0, getString(R.string.library_rename))
            menu.add(0, 2, 1, getString(R.string.library_delete))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        createCollectionLauncher.launch(
                            CreateCollectionActivity.createRenameIntent(
                                requireContext(),
                                collectionId = collectionId,
                                currentName = collectionName
                            )
                        )
                        true
                    }

                    2 -> {
                        confirmDeleteCollection(collectionId)
                        true
                    }

                    else -> false
                }
            }
        }.show()
    }

    private fun confirmDeleteCollection(collectionId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_collection_delete_title)
            .setMessage(R.string.library_collection_delete_message)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_confirm) { _, _ ->
                if (FlashCardLibraryStore.deleteCollection(requireContext(), collectionId)) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        R.string.library_collection_deleted,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    bindCollections()
                }
            }
            .show()
    }

    private fun openCreateCollection() {
        createCollectionLauncher.launch(CreateCollectionActivity.createIntent(requireContext()))
    }
}
