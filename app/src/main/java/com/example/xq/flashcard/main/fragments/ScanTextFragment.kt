package com.example.xq.flashcard.ui.main.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentScanTextBinding
import com.example.xq.flashcard.ui.scan.ScanCameraActivity
import com.example.xq.flashcard.translate.TranslateActivity

class ScanTextFragment : BaseFragment<FragmentScanTextBinding>() {
    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentScanTextBinding {
        return FragmentScanTextBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardScanText.setOnClickListener {
            startActivity(Intent(requireContext(), ScanCameraActivity::class.java))
        }
        binding.cardTranslate.setOnClickListener {
            startActivity(Intent(requireContext(), TranslateActivity::class.java))
        }
    }
}
