package com.example.xq.flashcard.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentSettingBinding
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.settings.AppSettingsStore
import com.example.xq.flashcard.ui.settings.UserProfileActivity
import com.example.xq.flashcard.ui.sync.CloudSyncEnableMode
import com.example.xq.flashcard.ui.sync.StudyCloudSyncManager
import com.example.xq.flashcard.utils.locale.AppLanguageManager
import com.example.xq.flashcard.utils.locale.AppLanguageOption
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    private var isBindingSwitchState = false
    private var isCloudActionRunning = false

    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.itemUser.setOnClickListener {
            startActivity(UserProfileActivity.createIntent(requireContext()))
        }
        binding.itemLanguage.setOnClickListener {
            showLanguageDialog()
        }
        binding.itemNotification.setOnClickListener {
            if (!isCloudActionRunning) {
                binding.switchNotifications.toggle()
            }
        }
        binding.itemSound.setOnClickListener {
            if (!isCloudActionRunning) {
                binding.switchSound.toggle()
            }
        }
        binding.itemCloudSync.setOnClickListener {
            if (!isCloudActionRunning && FirebaseAuth.getInstance().currentUser != null) {
                binding.switchCloudSync.toggle()
            } else if (FirebaseAuth.getInstance().currentUser == null) {
                showShortToast(R.string.setting_cloud_sync_sign_in_required)
            }
        }
        binding.itemStorage.setOnClickListener {
            showStorageDialog()
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingSwitchState) return@setOnCheckedChangeListener
            AppSettingsStore.setNotificationEnabled(requireContext(), isChecked)
            bindSettingStates()
        }
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingSwitchState) return@setOnCheckedChangeListener
            AppSettingsStore.setSoundEnabled(requireContext(), isChecked)
            bindSettingStates()
        }
        binding.switchCloudSync.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingSwitchState) return@setOnCheckedChangeListener
            handleCloudSyncToggle(isChecked)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.tvName.text = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email
            ?: getString(R.string.main_user_name_placeholder)
        binding.tvAccountHint.text = currentUser?.email ?: getString(R.string.setting_account_subtitle)
        bindSettingStates()
    }

    private fun bindSettingStates() {
        val context = requireContext()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val language = AppLanguageManager.getCurrentOption(context)
        val notificationsEnabled = AppSettingsStore.isNotificationEnabled(context)
        val soundEnabled = AppSettingsStore.isSoundEnabled(context)
        val cloudSyncEnabled = AppSettingsStore.isCloudSyncEnabled(context)
        val lastSyncedAt = AppSettingsStore.getCloudSyncLastSyncedAt(context)
        val collections = FlashCardLibraryStore.getCollections(context)
        val totalCards = collections.sumOf { it.cards.size }

        binding.tvLanguageValue.text = getString(language.labelRes)
        binding.tvNotificationSummary.text = getString(
            if (notificationsEnabled) {
                R.string.setting_notifications_enabled
            } else {
                R.string.setting_notifications_disabled
            }
        )
        binding.tvSoundSummary.text = getString(
            if (soundEnabled) {
                R.string.setting_sound_enabled
            } else {
                R.string.setting_sound_disabled
            }
        )
        binding.tvCloudSyncSummary.text = when {
            currentUser == null -> getString(R.string.setting_cloud_sync_sign_in_required)
            cloudSyncEnabled && lastSyncedAt > 0L -> getString(
                R.string.setting_cloud_sync_enabled_with_time,
                formatDateTime(lastSyncedAt)
            )
            cloudSyncEnabled -> getString(R.string.setting_cloud_sync_enabled)
            else -> getString(R.string.setting_cloud_sync_disabled)
        }
        binding.tvStorageSummary.text = getString(
            R.string.setting_storage_local_short,
            collections.size,
            totalCards
        )
        binding.tvStorageCaption.text = when {
            currentUser == null -> getString(R.string.setting_storage_caption_sign_in)
            cloudSyncEnabled && lastSyncedAt > 0L -> getString(
                R.string.setting_storage_caption_last_sync,
                formatDateTime(lastSyncedAt)
            )
            cloudSyncEnabled -> getString(R.string.setting_storage_caption_enabled)
            else -> getString(R.string.setting_storage_caption_disabled)
        }

        isBindingSwitchState = true
        binding.switchNotifications.isChecked = notificationsEnabled
        binding.switchSound.isChecked = soundEnabled
        binding.switchCloudSync.isChecked = cloudSyncEnabled && currentUser != null
        binding.switchNotifications.isEnabled = !isCloudActionRunning
        binding.switchSound.isEnabled = !isCloudActionRunning
        binding.switchCloudSync.isEnabled = !isCloudActionRunning && currentUser != null
        isBindingSwitchState = false

        binding.itemStorage.alpha = if (isCloudActionRunning) 0.7f else 1f
    }

    private fun handleCloudSyncToggle(isChecked: Boolean) {
        val context = requireContext()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showShortToast(R.string.setting_cloud_sync_sign_in_required)
            bindSettingStates()
            return
        }

        if (!isChecked) {
            AppSettingsStore.setCloudSyncEnabled(context, false)
            bindSettingStates()
            showShortToast(R.string.setting_cloud_sync_disabled_toast)
            return
        }

        AppSettingsStore.setCloudSyncEnabled(context, true)
        setCloudActionRunning(true)
        StudyCloudSyncManager.enableCloudSync(
            context = context,
            onSuccess = { mode ->
                setCloudActionRunning(false)
                bindSettingStates()
                val messageRes = when (mode) {
                    CloudSyncEnableMode.ENABLED_ONLY -> R.string.setting_cloud_sync_enabled_toast
                    CloudSyncEnableMode.UPLOADED_LOCAL -> R.string.setting_cloud_sync_uploaded_toast
                    CloudSyncEnableMode.RESTORED_FROM_CLOUD -> {
                        R.string.setting_cloud_sync_restored_toast
                    }
                }
                showShortToast(messageRes)
            },
            onError = {
                setCloudActionRunning(false)
                bindSettingStates()
                showShortToast(R.string.setting_cloud_sync_retry_toast)
            }
        )
    }

    private fun showLanguageDialog() {
        val options = AppLanguageOption.values()
        val currentOption = AppLanguageManager.getCurrentOption(requireContext())
        val checkedIndex = options.indexOfFirst { it == currentOption }.coerceAtLeast(0)
        val labels = options.map { getString(it.labelRes) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.setting_language_dialog_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selectedOption = options[which]
                AppLanguageManager.applyLanguage(requireContext(), selectedOption.code)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.library_cancel, null)
            .show()
    }

    private fun showStorageDialog() {
        val context = requireContext()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val collections = FlashCardLibraryStore.getCollections(context)
        val totalCards = collections.sumOf { it.cards.size }
        val cloudSyncEnabled = AppSettingsStore.isCloudSyncEnabled(context)

        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.setting_storage_dialog_title)
            .setMessage(
                getString(
                    R.string.setting_storage_dialog_message,
                    collections.size,
                    totalCards,
                    getCloudStatusLabel()
                )
            )
            .setNegativeButton(R.string.library_done, null)

        if (currentUser == null) {
            dialogBuilder.show()
            return
        }

        if (cloudSyncEnabled) {
            dialogBuilder
                .setPositiveButton(R.string.setting_cloud_sync_action_sync_now) { _, _ ->
                    syncNow()
                }
                .setNeutralButton(R.string.setting_cloud_sync_action_restore) { _, _ ->
                    confirmRestoreFromCloud()
                }
        } else {
            dialogBuilder
                .setPositiveButton(R.string.setting_cloud_sync_action_enable) { _, _ ->
                    binding.switchCloudSync.isChecked = true
                }
        }

        dialogBuilder.show()
    }

    private fun syncNow() {
        setCloudActionRunning(true)
        StudyCloudSyncManager.syncNow(
            context = requireContext(),
            onSuccess = {
                setCloudActionRunning(false)
                bindSettingStates()
                showShortToast(R.string.setting_cloud_sync_uploaded_toast)
            },
            onError = {
                setCloudActionRunning(false)
                bindSettingStates()
                showShortToast(R.string.setting_cloud_sync_retry_toast)
            }
        )
    }

    private fun confirmRestoreFromCloud() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.setting_cloud_sync_restore_title)
            .setMessage(R.string.setting_cloud_sync_restore_message)
            .setPositiveButton(R.string.setting_cloud_sync_action_restore) { _, _ ->
                restoreFromCloud()
            }
            .setNegativeButton(R.string.library_cancel, null)
            .show()
    }

    private fun restoreFromCloud() {
        setCloudActionRunning(true)
        StudyCloudSyncManager.restoreFromCloud(
            context = requireContext(),
            onSuccess = { restored ->
                setCloudActionRunning(false)
                bindSettingStates()
                showShortToast(
                    if (restored) {
                        R.string.setting_cloud_sync_restored_toast
                    } else {
                        R.string.setting_cloud_sync_no_cloud_data_toast
                    }
                )
            },
            onError = {
                setCloudActionRunning(false)
                bindSettingStates()
                showShortToast(R.string.setting_cloud_sync_retry_toast)
            }
        )
    }

    private fun setCloudActionRunning(isRunning: Boolean) {
        isCloudActionRunning = isRunning
        bindSettingStates()
    }

    private fun getCloudStatusLabel(): String {
        val context = requireContext()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cloudSyncEnabled = AppSettingsStore.isCloudSyncEnabled(context)
        return when {
            currentUser == null -> getString(R.string.setting_cloud_sync_sign_in_required_short)
            cloudSyncEnabled -> getString(R.string.setting_cloud_sync_status_connected)
            else -> getString(R.string.setting_cloud_sync_status_off)
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        val locale = Locale.forLanguageTag(AppLanguageManager.getCurrentOption(requireContext()).code)
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(Date(timestamp))
    }

    private fun showShortToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }
}
