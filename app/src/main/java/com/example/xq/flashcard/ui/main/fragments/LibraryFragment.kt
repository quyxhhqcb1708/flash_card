package com.example.xq.flashcard.ui.main.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentLibraryBinding

class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLibraryBinding {
        return FragmentLibraryBinding.inflate(layoutInflater, container, false)
    }
}
