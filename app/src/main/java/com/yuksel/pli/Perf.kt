package com.yuksel.pli

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.foundation.relocation.bringIntoViewRequester
import java.time.LocalDateTime

/**
 * True while the launcher is on screen. A launcher stays alive behind every app, so polling,
 * sensors and network refreshes wait for this instead of running all day in the background.
 */
@Composable
fun rememberOnScreen(): Boolean {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return state.isAtLeast(Lifecycle.State.RESUMED)
}

/** The current minute, ticking only while the launcher is on screen (and refreshed as soon as it comes back). */
@Composable
fun rememberMinute(): LocalDateTime {
    val onScreen = rememberOnScreen()
    val now by produceState(LocalDateTime.now(), onScreen) {
        value = LocalDateTime.now()
        while (onScreen) {
            delay(1000L * (60 - LocalDateTime.now().second).coerceAtLeast(1))
            value = LocalDateTime.now()
        }
    }
    return now
}

/** Once focused, scrolls the field into view again after the keyboard has finished opening. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.keepAboveKeyboard(): Modifier = composed {
    val requester = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    bringIntoViewRequester(requester).onFocusEvent { state ->
        if (state.isFocused) scope.launch { repeat(3) { delay(220); requester.bringIntoView() } }
    }
}
