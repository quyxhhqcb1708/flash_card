package com.example.xq.flashcard.utils.ex

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import com.example.xq.flashcard.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.tabs.TabLayout

var isAvailableClick = true
fun handleAvailableClick(time: Long) {
    Handler(Looper.getMainLooper()).postDelayed({
        isAvailableClick = true
    }, time)
}

fun View.clickSafe(time: Long = 200, action: () -> Unit) {
    this.setOnClickListener {
        if (isAvailableClick) {
            isAvailableClick = false
            handleAvailableClick(time)
            action.invoke()
        }
    }
}

fun Context.openActivity(pClass: Class<out Activity>, bundle: Bundle?) {
    val intent = Intent(this, pClass)
    if (bundle != null) {
        intent.putExtras(bundle)
    }
    startActivity(intent)
}

fun Context.openActivity(pClass: Class<out Activity>, bundle: Bundle?, isFinish: Boolean = false) {
    openActivity(pClass, bundle)
    if (isFinish) {
        (this as Activity).finish()
    }
}

fun Context.openActivity(pClass: Class<out Activity>, isFinish: Boolean = false) {
    openActivity(pClass, null)
    if (isFinish) {
        (this as Activity).finish()
    }
}

fun Activity.openActivity(pClass: Class<out Activity>, isFinish: Boolean = false) {
    openActivity(pClass, null)
    overridePendingTransition(R.anim.anim_in, R.anim.anim_out)
    if (isFinish) {
        finish()
    }
}

fun View.hide() {
    this.visibility = View.INVISIBLE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.isShow(): Boolean {
    return visibility == View.VISIBLE
}

fun View.showOrGone(isShow: Boolean) {
    if (isShow) {
        show()
    } else {
        gone()
    }
}

fun View.showOrHide(isShow: Boolean) {
    if (isShow) {
        show()
    } else {
        hide()
    }
}

fun ImageView.setTint() {
    val matrix = ColorMatrix()
    matrix.setSaturation(0f)

    val filter = ColorMatrixColorFilter(matrix)
    this.colorFilter = filter
}

fun ImageView.clearTint() {
    this.colorFilter = null
}

fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val p = layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(left, top, right, bottom)
        requestLayout()
    }
}

fun TabLayout.createTab(tabName: String): TabLayout.Tab {
    return newTab().setText(tabName)
}

val Int.toDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Context.getBatteryLevel(): Int {
    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

fun ImageView.loadGlide(src: Any) {
    Glide.with(this.context)
        .load(src)
        .into(this)
}

fun ImageView.loadGlide(src: Any, error: Any) {
    Glide.with(this.context)
        .load(src)
        .error(error)
        .into(this)
}

fun ImageView.loadGlideGif(src: Any) {
    Glide.with(this.context)
        .asGif()
        .load(src)
        .into(this)
}

fun ImageView.loadGlideAsBitmap(src: Any) {
    Glide.with(this.context)
        .asBitmap()
        .load(src)
        .into(this)
}

fun ImageView.loadGlideForSize(src: Any) {
    Glide.with(this.context)
        .load(src)
        .override(this.width, this.height)
        .into(this)
}

fun TextView.changeTextColor(color: Int) {
    this.setTextColor(
        ContextCompat.getColor(
            this.context,
            color
        )
    )
}

fun TextView.changeFont(font: Int) {
    this.setTypeface(
        ResourcesCompat.getFont(this.context, font)
    )
}


fun ImageView.setTintColor(colorResId: Int) {
    val color = ContextCompat.getColor(this.context, colorResId)
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
}

fun ImageView.setTintByColor(color: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
}

fun ImageView.clearTintColor() {
    ImageViewCompat.setImageTintList(this, null)
}

fun ImageView.loadGlideFailGone(src: Any) {
    show()
    Glide.with(this.context)
        .asBitmap()
        .load(src)
        .listener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>,
                isFirstResource: Boolean,
            ): Boolean {
                gone()
                return false
            }

            override fun onResourceReady(
                resource: Bitmap,
                model: Any,
                target: Target<Bitmap>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                show()
                return false
            }
        })
        .into(this)
}

@SuppressLint("CheckResult")
fun ImageView.loadGlideListener(
    src: Any,
    progress: ProgressBar,
    actionReady: (() -> Unit)? = null,
    actionFail: (() -> Unit)? = null,
) {
    progress.show()
    Glide.with(this.context)
        .asBitmap()
        .load(src)
        .listener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>,
                isFirstResource: Boolean,
            ): Boolean {
                actionFail?.invoke()
                return false
            }

            override fun onResourceReady(
                resource: Bitmap,
                model: Any,
                target: Target<Bitmap>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                actionReady?.invoke()
                progress.gone()
                return false
            }
        })
        .into(this)
}

fun ImageView.loadGlideListener(src: Any, actionReady: () -> Unit, actionFail: () -> Unit) {
    Glide.with(this.context)
        .load(src)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                actionFail()
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                actionReady()
                return false
            }

        })
        .into(this)
}

@SuppressLint("ClickableViewAccessibility")
fun View.setOnTouchScale(
    scale: Float = 0.95f,
    disView: Boolean = true,
    time: Long = 200L,
    action: () -> Unit,
) {
    var isClick = true
    this.setOnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                isClick = true
                view.scaleX = scale
                view.scaleY = scale
            }

            MotionEvent.ACTION_MOVE -> {
                if (motionEvent.x < 0 || motionEvent.x > this.width || motionEvent.y < 0 || motionEvent.y > this.height) {
                    isClick = false
                    if (disView) {
                        view.scaleX = 1f
                        view.scaleY = 1f
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isClick) {
                    if (isAvailableClick) {
                        isAvailableClick = false
                        handleAvailableClick(time)
                        action()
                    }
                }
                view.scaleX = 1f
                view.scaleY = 1f
            }

            MotionEvent.ACTION_CANCEL -> {
                view.scaleX = 1f
                view.scaleY = 1f
            }
        }
        true
    }
}


fun SeekBar.onSeekChange(action: (progress: Int) -> Unit) {
    this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean,
        ) {
            if (fromUser) {
                action(progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

        }
    })
}


fun NestedScrollView.onScrollBack(action: () -> Unit) {

    val sX: Int = this.scrollX
    val sY: Int = this.scrollY

    action.invoke()

    this.getViewTreeObserver()
        .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@onScrollBack.scrollTo(sX, sY)
                this@onScrollBack.getViewTreeObserver().removeOnGlobalLayoutListener(this)
            }
        })
}

private fun isBackgroundDark(color: Int): Boolean {
    val r = (color shr 16) and 0xff
    val g = (color shr 8) and 0xff
    val b = color and 0xff
    val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
    return brightness < 128
}

fun TextView.setTextColorBackOrWhite(color: Int) {
    if (isBackgroundDark(color)) {
        setTextColor(Color.WHITE)
    } else {
        setTextColor(Color.BLACK)
    }
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
