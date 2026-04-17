package com.example.xq.flashcard.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityLibraryCollectionBinding
import com.example.xq.flashcard.databinding.ItemLibraryFlashcardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LibraryCollectionActivity : BaseActivity<ActivityLibraryCollectionBinding>() {

    companion object {
        const val EXTRA_COLLECTION_ID = "extra_collection_id"

        fun createIntent(context: Context, collectionId: Long): Intent {
            return Intent(context, LibraryCollectionActivity::class.java).apply {
                putExtra(EXTRA_COLLECTION_ID, collectionId)
            }
        }
    }

    private val collectionId by lazy { intent.getLongExtra(EXTRA_COLLECTION_ID, -1L) }

    private val renameLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(CreateCollectionActivity.EXTRA_RESULT_MESSAGE)?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
                bindCollection()
            }
        }

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityLibraryCollectionBinding {
        return ActivityLibraryCollectionBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.setOnClickListener { anchor -> showCollectionMenu(anchor) }
    }

    override fun onResume() {
        super.onResume()
        bindCollection()
    }

    private fun bindCollection() {
        val collection = FlashCardLibraryStore.getCollection(this, collectionId)
        if (collection == null) {
            finish()
            return
        }

        binding.tvCollectionTitle.text = collection.name
        binding.tvCollectionCount.text = getString(R.string.library_count_label, collection.cards.size)
        binding.flashcardsContainer.removeAllViews()
        collection.cards.forEach { card ->
            val itemBinding = ItemLibraryFlashcardBinding.inflate(layoutInflater, binding.flashcardsContainer, false)
            itemBinding.tvTerm.text = card.term
            itemBinding.tvDefinition.text = card.definition
            itemBinding.tvLanguagePair.text = FlashCardDisplayFormatter.getLanguagePair(this, card)
            itemBinding.tvSavedAt.text = getString(
                R.string.library_saved_at,
                FlashCardDisplayFormatter.formatSavedAt(card.updatedAt)
            )
            binding.flashcardsContainer.addView(itemBinding.root)
        }
        binding.tvEmptyState.isVisible = collection.cards.isEmpty()
        binding.flashcardsContainer.isVisible = collection.cards.isNotEmpty()
    }

    private fun showCollectionMenu(anchor: android.view.View) {
        val collection = FlashCardLibraryStore.getCollection(this, collectionId) ?: return
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, getString(R.string.library_rename))
            menu.add(0, 2, 1, getString(R.string.library_delete))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        renameLauncher.launch(
                            CreateCollectionActivity.createRenameIntent(
                                context = this@LibraryCollectionActivity,
                                collectionId = collection.id,
                                currentName = collection.name
                            )
                        )
                        true
                    }

                    2 -> {
                        confirmDeleteCollection()
                        true
                    }

                    else -> false
                }
            }
        }.show()
    }

    private fun confirmDeleteCollection() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.library_collection_delete_title)
            .setMessage(R.string.library_collection_delete_message)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_confirm) { _, _ ->
                if (FlashCardLibraryStore.deleteCollection(this, collectionId)) {
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(
                            CreateCollectionActivity.EXTRA_RESULT_MESSAGE,
                            getString(R.string.library_collection_deleted)
                        )
                    )
                    finish()
                }
            }
            .show()
    }
}
