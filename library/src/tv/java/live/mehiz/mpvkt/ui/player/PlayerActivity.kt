package live.mehiz.mpvkt.ui.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media.AudioManagerCompat
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.BasePlayerActivity
import live.mehiz.mpvkt.BasePlayerHelper
import live.mehiz.mpvkt.BasePlayerScreen
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import timber.log.Timber

abstract class PlayerActivity : BasePlayerActivity() {
  abstract fun initCurrentPlayerItem()
  abstract fun onPlayEnd()

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

            // TODO: set mpv configurations
            setIntentExtras(intent.extras)

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

      initCurrentPlayerItem()

      play()
    }

    override fun onPaused() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        !isInPictureInPictureMode &&
        !playerPreferences.automaticBackgroundPlayback.get()
      ) {
        playerViewModel.pause()
      }
      saveVideoPlaybackState(currentPlayerItem.mediaId)
    }

    override fun onResumed() {
      playerViewModel.currentVolume.update {
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).also {
          if (it < playerViewModel.maxVolume) playerViewModel.changeMPVVolumeTo(100)
        }
      }
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
      audioFocusRequest?.let {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
      }
      audioFocusRequest = null
      mediaSession?.release()

      player.isExiting = true
      if (isFinishing) {
        MPVLib.command("stop")
      }
      MPVLib.removeObserver(playerObserver)
      MPVLib.destroy()
    }
  }

  fun play(playerItem: MPVPlayerItem? = null) {
    playerItem?.let {
      currentPlayerItem = it
    }

    player.playFile(currentPlayerItem.uri)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    setupMPV()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    runOnUiThread {
      when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> playerViewModel.showControls()
        KeyEvent.KEYCODE_DPAD_DOWN -> {
          if (playerViewModel.controlsShown.value) {
            playerViewModel.hideControls()
          }
        }
        KeyEvent.KEYCODE_DPAD_CENTER -> {
          if (playerViewModel.controlsShown.value) {
            playerViewModel.hideControls()
          } else {
            playerViewModel.showControls()
          }
          playerViewModel.pauseUnpause()
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
          val pos = playerViewModel.pos
          val duration = playerViewModel.duration
          Timber.d("pos: $pos, duration: $duration")
          if (pos != null && duration != null) {
            playerViewModel.seekTo(pos + 10)
          }
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> playerViewModel.handleLeftDoubleTap()

        // other keys should be bound by the user in input.conf ig
        else -> {
          event?.let { player.onKey(it) }
          super.onKeyDown(keyCode, event)
        }
      }
    }
    return true
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    if (player.onKey(event!!)) return true
    return super.onKeyUp(keyCode, event)
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
