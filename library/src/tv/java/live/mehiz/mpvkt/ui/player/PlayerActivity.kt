package live.mehiz.mpvkt.ui.player

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.BasePlayerActivity
import live.mehiz.mpvkt.BasePlayerHelper
import live.mehiz.mpvkt.model.MPVPlayerItem
import timber.log.Timber

abstract class PlayerActivity : BasePlayerActivity() {
  abstract fun initCurrentPlayerItem()

  override val playerObserver = object : MPVLib.EventObserver {
    // a bunch of observers
    override fun eventProperty(property: String, value: Long) {
      if (player.isExiting) return
    }

    override fun eventProperty(property: String) {
      if (player.isExiting) return
    }

    override fun eventProperty(property: String, value: Boolean) {
      if (player.isExiting) return
      runOnUiThread {
        when (property) {
          "pause" -> {
            if (value) {
              window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
              window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
          }
          "eof-reached" if value -> {
            if (playerPreferences.closeAfterReachingEndOfVideo.get()) {
              finishAndRemoveTask()
            } else {
              playerViewModel.seekTo(0)
              onPlayEnd()
            }
          }
        }
      }
    }

    override fun eventProperty(property: String, value: String) {
      if (player.isExiting) return
      // Custom Buttons Event
      runOnUiThread {
        when (property.substringBeforeLast("/")) {
          "user-data/mpvkt" -> playerViewModel.handleLuaInvocation(property, value)
        }
      }
    }

    override fun eventProperty(property: String, value: MPVNode) {
      if (player.isExiting) return
    }

    override fun eventProperty(property: String, value: Double) {
      if (player.isExiting) return
    }

    override fun event(eventId: Int, data: MPVNode) {
      if (player.isExiting) return
      runOnUiThread {
        when (eventId) {
          MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
            setMpvExtras(currentPlayerItem)

            MPVLib.setPropertyString("media-title", currentPlayerItem.mediaTitle)
            lifecycleScope.launch(Dispatchers.IO) {
              loadVideoPlaybackState(currentPlayerItem.mediaId)
            }
            playerViewModel.changeVideoAspect(playerPreferences.videoAspect.get())
          }

          MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> player.isExiting = false
        }
      }
    }
  }

  override val playerHelper = object : BasePlayerHelper {
    override fun onCreated() {
      setupAudio()
      setupMediaSession()
    }

    override fun onPaused() {
      saveVideoPlaybackState(currentPlayerItem.mediaId)
    }

    override fun onResumed() {
      playerViewModel.showControls()
    }

    override fun onStopped() {
      saveVideoPlaybackState(currentPlayerItem.mediaId)
      playerViewModel.pause()

      window.attributes.screenBrightness.let {
        if (playerPreferences.rememberBrightness.get() && it != -1f) {
          playerPreferences.defaultBrightness.set(it)
        }
      }
    }

    override fun onDestroy() {
      Timber.d("Exiting")
      playerViewModel.pause()

      releaseAudio()
      releaseMediaSession()
    }
  }

  fun play(playerItem: MPVPlayerItem) {
    currentPlayerItem = playerItem

    setRequestHeaders(currentPlayerItem)

    player.playFile(currentPlayerItem.uri)

    playerViewModel.unpause()
    playerViewModel.showControls()
  }

  override fun onDestroy() {
    super.onDestroy()

    player.isExiting = true
    if (isFinishing) {
      MPVLib.command("stop")
    }
    MPVLib.removeObserver(playerObserver)

    player.destroy()
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
            event?.let { player.onKey(it) }
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

  override fun onStart() {
    super.onStart()

    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode = if (playerPreferences.drawOverDisplayCutout.get()) {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      } else {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
      }
    }

    if (playerPreferences.rememberBrightness.get()) {
      playerPreferences.defaultBrightness.get().let {
        if (it != -1f) playerViewModel.changeBrightnessTo(it)
      }
    }
  }
}
