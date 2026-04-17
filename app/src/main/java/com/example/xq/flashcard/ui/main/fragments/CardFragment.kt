package com.example.xq.flashcard.ui.main.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentCardBinding
import com.example.xq.flashcard.databinding.ItemSavedFlashCardBinding
import com.example.xq.flashcard.ui.library.FlashCardDisplayFormatter
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.library.LibraryCollectionActivity

class CardFragment : BaseFragment<FragmentCardBinding>() {
    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCardBinding {
        return FragmentCardBinding.inflate(layoutInflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        bindSavedCards()
    }

    private fun bindSavedCards() {
        val items = FlashCardLibraryStore.getAllCards(requireContext())
        binding.cardPlaceholder.isVisible = items.isEmpty()
        binding.scrollCards.isVisible = items.isNotEmpty()
        binding.cardListContainer.removeAllViews()

        items.forEach { entry ->
            val itemBinding = ItemSavedFlashCardBinding.inflate(layoutInflater, binding.cardListContainer, false)
            itemBinding.tvSourceText.text = entry.card.term
            itemBinding.tvTranslatedText.text = entry.card.definition
            itemBinding.tvCollectionName.text = getString(R.string.main_card_saved_in, entry.collectionName)
            itemBinding.tvLanguagePair.text = FlashCardDisplayFormatter.getLanguagePair(requireContext(), entry.card)
            itemBinding.tvCreatedAt.text = getString(
                R.string.main_card_saved_at,
                FlashCardDisplayFormatter.formatSavedAt(entry.card.updatedAt)
            )
            itemBinding.root.setOnClickListener {
                startActivity(
                    Intent(requireContext(), LibraryCollectionActivity::class.java).apply {
                        putExtra(LibraryCollectionActivity.EXTRA_COLLECTION_ID, entry.collectionId)
                    }
                )
            }
            binding.cardListContainer.addView(itemBinding.root)
        }
    }
}
