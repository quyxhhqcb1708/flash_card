package com.example.xq.flashcard.utils.ex

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun LifecycleOwner.launchWhenResumed(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            block()
            this@launch.cancel()
        }
    }
}

fun LifecycleOwner.launchWithIO(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch(Dispatchers.IO) {
        block()
    }
}

fun LifecycleOwner.launchWithDefault(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch(Dispatchers.Default) {
        block()
    }
}

fun LifecycleOwner.launchWithMain(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch(Dispatchers.Main) {
        block()
    }
}