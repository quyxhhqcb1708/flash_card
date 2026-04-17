package com.example.xq.flashcard.ui.main.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentCardBinding

class CardFragment : BaseFragment<FragmentCardBinding>() {
    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCardBinding {
        return FragmentCardBinding.inflate(layoutInflater, container, false)
    }
}
