package io.github.wohal.mpvplayer.modifier

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent

private val DPadEventsKeyCodes = listOf(
  KeyEvent.KEYCODE_DPAD_LEFT,
  KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
  KeyEvent.KEYCODE_DPAD_RIGHT,
  KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
  KeyEvent.KEYCODE_DPAD_UP,
  KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
  KeyEvent.KEYCODE_DPAD_DOWN,
  KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
  KeyEvent.KEYCODE_DPAD_CENTER,
  KeyEvent.KEYCODE_ENTER,
  KeyEvent.KEYCODE_NUMPAD_ENTER,

  KeyEvent.KEYCODE_BACK,
)

@Suppress("CyclomaticComplexMethod")
fun Modifier.handleDPadKeyEvents(
  onLeft: (() -> Unit)? = null,
  onRight: (() -> Unit)? = null,
  onUp: (() -> Unit)? = null,
  onDown: (() -> Unit)? = null,
  onEnter: (() -> Unit)? = null,
  onBack: (() -> Unit)? = null,
  onKeyUp: (() -> Unit)? = null,
) = onKeyEvent {
  if (DPadEventsKeyCodes.contains(it.nativeKeyEvent.keyCode)) {
    when (it.nativeKeyEvent.action) {
      KeyEvent.ACTION_UP -> {
        onKeyUp?.invoke()?.also { return@onKeyEvent true }
      }
      KeyEvent.ACTION_DOWN -> {
        when (it.nativeKeyEvent.keyCode) {
          KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
            onLeft?.invoke().also { return@onKeyEvent true }
          }
          KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
            onRight?.invoke().also { return@onKeyEvent true }
          }
          KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
            onUp?.invoke().also { return@onKeyEvent true }
          }
          KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
            onDown?.invoke().also { return@onKeyEvent true }
          }
          KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
            onEnter?.invoke().also { return@onKeyEvent true }
          }
          KeyEvent.KEYCODE_BACK -> {
            onBack?.invoke().also { return@onKeyEvent true }
          }
        }
      }
      else -> {}
    }
  }
  false
}
