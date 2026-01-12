package io.github.wohal.mpvplayer

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.github.wohal.mpvplayer.controls.PlayerControls
import `is`.xyz.mpv.MPVLib
import live.mehiz.mpvkt.BasePlayerActivity
import live.mehiz.mpvkt.BasePlayerScreen
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.ui.player.Sheets
import timber.log.Timber

abstract class PlayerActivity : BasePlayerActivity() {
  override fun onPlayerScreenCreated() {
    super.onPlayerScreenCreated()

    MPVLib.addObserver(this)
  }

  override fun onPlayerScreenDestroy() {
    super.onPlayerScreenDestroy()

    MPVLib.removeObserver(this)
    playerViewModel.player.apply {
      destroy()
      initialize(filesDir.path, cacheDir.path)
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    playerViewModel.player.destroy()
  }

  @Suppress("CyclomaticComplexMethod")
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    runOnUiThread {
      when (keyCode) {
        KeyEvent.KEYCODE_BACK -> {
          if (playerViewModel.sheetShown.value != Sheets.None) {
            playerViewModel.hideSheet()
          } else if (playerViewModel.controlsShown.value) {
            playerViewModel.hideControls()
          } else {
            event?.let { playerViewModel.player.onKey(it) }
            super.onKeyDown(keyCode, event)
          }
        }
        KeyEvent.KEYCODE_DPAD_UP -> {
          if (!playerViewModel.controlsShown.value) {
            playerViewModel.showControls()
          } else if (playerViewModel.sheetShown.value != Sheets.None) {
            playerViewModel.hideSheet()
          }
        }
        KeyEvent.KEYCODE_DPAD_DOWN -> {
          if (playerViewModel.controlsShown.value) {
            playerViewModel.hideControls()
          } else if (playerViewModel.sheetShown.value == Sheets.None) {
            playerViewModel.showSheet()
          }
        }
        KeyEvent.KEYCODE_DPAD_CENTER -> {
          playerViewModel.showControls()
          playerViewModel.pauseUnpause()
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> playerViewModel.handleRightDoubleTap()
        KeyEvent.KEYCODE_DPAD_LEFT -> playerViewModel.handleLeftDoubleTap()

        // other keys should be bound by the user in input.conf ig
        else -> {
          event?.let { playerViewModel.player.onKey(it) }
          super.onKeyDown(keyCode, event)
        }
      }
    }
    return true
  }

  @Composable
  fun PlayerScreen(
    onBackPress: () -> Unit = {},
  ) {
    BasePlayerScreen(
      playerHelper = this@PlayerActivity,
      viewModel = playerViewModel,
    ) {
      PlayerControls(
        viewModel = playerViewModel,
        onBackPress = onBackPress,
      )
    }
  }
}
