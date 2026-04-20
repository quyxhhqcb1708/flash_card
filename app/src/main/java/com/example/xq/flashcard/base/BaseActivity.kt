package com.example.xq.flashcard.base

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.xq.flashcard.utils.locale.AppLanguageManager
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils
import java.util.Locale

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val updatedContext = updateBaseContextLocale(base)
        applyOverrideConfiguration(updatedContext.resources.configuration)
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val locale: Locale
        SharePreferUtils.init(context)
        val keyLang = AppLanguageManager.resolveStoredOrSystemLanguage(context)
        locale = Locale(keyLang)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    /*
     * Legacy network hook is intentionally disabled.
     * App now supports offline learning flows, so BaseActivity only keeps
     * shared immersive UI + common keyboard/locale behavior.
     */
    // companion object {
    //     const val ACTION_NETWORK_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"
    // }

    lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        fullStatusBar()
        super.onCreate(savedInstanceState)
        binding = inflateViewBinding(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        hideNavigationBar()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    open fun onBack() {
        finish()
    }

    abstract fun inflateViewBinding(layoutInflater: LayoutInflater): VB

    private fun fullStatusBar() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun hideNavigationBar() {
        val decorView: View = window.decorView
        val uiOptions: Int =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        try {
            if (event.action == MotionEvent.ACTION_UP) {
                val v = currentFocus
                if (v is EditText) {
                    val outRect = Rect()
                    v.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        v.clearFocus()
                        try {
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(v.windowToken, 0)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            return super.dispatchTouchEvent(event)
        } catch (e: Exception) {
            return false
        }
    }

    /*
     * Legacy navigation helper from older module-based flows.
     * Disabled to keep BaseActivity focused on reusable system UI behavior.
     */
    // fun gotoAndClear(pClass: Class<out Activity>) {
    //     val intent = Intent(this, pClass)
    //     intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    //     startActivity(intent)
    //     finish()
    // }

}
