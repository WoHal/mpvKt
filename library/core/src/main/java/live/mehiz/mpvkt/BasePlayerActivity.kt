package live.mehiz.mpvkt

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media.AudioManagerCompat
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.ui.player.MPVView
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.PlayerViewModelProviderFactory
import timber.log.Timber

@Suppress("TooManyFunctions")
abstract class BasePlayerActivity : ComponentActivity(),
  BasePlayerEvent, MPVLib.EventObserver, PlayerScreenObserver {
  abstract var currentPlayerItem: MPVPlayerItem
  var currentVideoPlaybackState: PlaybackStateEntity? = null

  val playerViewModel: PlayerViewModel by viewModels { PlayerViewModelProviderFactory(this) }
  lateinit var player: MPVView
  val windowInsetsController by lazy { playerViewModel.playerHelper.windowInsetsController }

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
          if (playerViewModel.playerPreferences.closeAfterReachingEndOfVideo.get()) {
            finishAndRemoveTask()
          } else {
            onPlayEnd()
          }
        }
      }
    }
  }

  override fun eventProperty(property: String, value: String) {
    if (player.isExiting) return
    // Custom Buttons Event
  }

  override fun eventProperty(property: String, value: MPVNode) {
    if (player.isExiting) return
  }

  @SuppressLint("NewApi")
  override fun eventProperty(property: String, value: Double) {
    if (player.isExiting) return
  }

  override fun event(eventId: Int, data: MPVNode) {
    if (player.isExiting) return
    when (eventId) {
      MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
        setMpvExtras(currentPlayerItem)

        MPVLib.setPropertyString("media-title", currentPlayerItem.mediaTitle)
        lifecycleScope.launch(Dispatchers.IO) {
          loadVideoPlaybackState(currentPlayerItem.mediaId)
        }
        playerViewModel.changeVideoAspect(playerViewModel.playerPreferences.videoAspect.get())
      }

      MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> player.isExiting = false
    }
  }

  override fun onPlayerScreenCreated() {
    playerViewModel.playerHelper.setupAudio()
    playerViewModel.playerHelper.setupMediaSession()
  }

  override fun onPlayerScreenPaused() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
      !isInPictureInPictureMode &&
      !playerViewModel.playerPreferences.automaticBackgroundPlayback.get()
    ) {
      playerViewModel.pause()
    }
    saveVideoPlaybackState()
  }

  override fun onPlayerScreenResumed() {
    playerViewModel.currentVolume.update {
      playerViewModel.playerHelper.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).also {
        if (it < playerViewModel.maxVolume) playerViewModel.changeMPVVolumeTo(100)
      }
    }
  }

  override fun onPlayerScreenStopped() {
    saveVideoPlaybackState()
  }

  override fun onPlayerScreenDestroy() {
    Timber.d("Exiting")
    playerViewModel.playerHelper.audioFocusRequest?.let {
      AudioManagerCompat.abandonAudioFocusRequest(playerViewModel.playerHelper.audioManager, it)
    }
    playerViewModel.playerHelper.audioFocusRequest = null
    playerViewModel.playerHelper.releaseAudio()
    playerViewModel.playerHelper.releaseMediaSession()

    player.isExiting = true
    if (isFinishing) {
      MPVLib.command("stop")
    }
    MPVLib.destroy()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    player = playerViewModel.player

    playerViewModel.playerHelper.setupMPV()
  }

  override fun onPlayEnd() {
    playerViewModel.seekTo(0)
  }

  fun setRequestHeaders(playerItem: MPVPlayerItem) {
    if (playerItem.userAgent.isNotEmpty()) {
      MPVLib.setPropertyString("user-agent", playerItem.userAgent)
    }

    if (playerItem.headers.isNotEmpty()) {
      val headersString = playerItem.headers.map {
        "${it.key}: ${it.value.replace(",", "\\,")}"
      }.joinToString(",")
      MPVLib.setPropertyString("http-header-fields", headersString)
    }
  }

  fun setMpvExtras(playerItem: MPVPlayerItem) {
    MPVLib.setPropertyString("force-media-title", playerItem.mediaTitle)
    MPVLib.setPropertyInt("time-pos", playerItem.position / 1000)

    playerItem.audioFiles.forEach { url ->
      playerViewModel.addAudio(url.toUri())
    }

    playerItem.subtitles.forEach { subtitle ->
      Timber.v("Adding subtitles from intent extras: ${subtitle.url}")
      MPVLib.command("sub-add", subtitle.url, if (subtitle.enable) "select" else "auto")
    }
  }

  abstract fun savePlaybackState(state: PlaybackStateEntity)
  fun saveVideoPlaybackState() {
    lifecycleScope.launch(Dispatchers.IO) {
      val oldState = currentVideoPlaybackState
      val newState = PlaybackStateEntity(
        mediaId = oldState?.mediaId ?: currentPlayerItem.mediaId,
        mediaTitle = oldState?.mediaTitle ?: currentPlayerItem.mediaTitle,
        lastPosition = if (playerViewModel.playerPreferences.savePositionOnQuit.get()) {
          val pos = playerViewModel.pos ?: 0
          val duration = playerViewModel.duration ?: 0
          if (pos < duration - 1) pos else 0
        } else {
          oldState?.lastPosition ?: 0
        },
        playbackSpeed = MPVLib.getPropertyDouble("speed")!!,
        sid = player.sid,
        subDelay = (MPVLib.getPropertyDouble("sub-delay")!! * 1000).toInt(),
        subSpeed = MPVLib.getPropertyDouble("sub-speed")!!,
        secondarySid = player.secondarySid,
        secondarySubDelay = (MPVLib.getPropertyDouble("secondary-sub-delay")!! * 1000).toInt(),
        aid = player.aid,
        audioDelay = (MPVLib.getPropertyDouble("audio-delay")!! * 1000).toInt(),
      )
      savePlaybackState(newState)
      currentVideoPlaybackState = newState
    }
  }

  abstract suspend fun loadVideoPlaybackStateById(mediaId: String): PlaybackStateEntity?
  suspend fun loadVideoPlaybackState(mediaId: String) {
    if (mediaId.isBlank()) return
    val state = loadVideoPlaybackStateById(mediaId)
    val getDelay: (Int, Int?) -> Double = { preferenceDelay, stateDelay ->
      (stateDelay ?: preferenceDelay) / 1000.0
    }
    val subDelay = getDelay(playerViewModel.subtitlesPreferences.defaultSubDelay.get(), state?.subDelay)
    val secondarySubDelay = getDelay(playerViewModel.subtitlesPreferences.defaultSecondarySubDelay.get(), state?.secondarySubDelay)
    val audioDelay = getDelay(playerViewModel.audioPreferences.defaultAudioDelay.get(), state?.audioDelay)
    Timber.d("loaded playback state: $state")
    state?.let {
      player.sid = it.sid
      player.secondarySid = it.secondarySid
      player.aid = it.aid
      MPVLib.setPropertyDouble("sub-delay", subDelay)
      MPVLib.setPropertyDouble("secondary-sub-delay", secondarySubDelay)
      MPVLib.setPropertyDouble("speed", it.playbackSpeed)
      MPVLib.setPropertyDouble("audio-delay", audioDelay)
    }
    if (playerViewModel.playerPreferences.savePositionOnQuit.get()) {
      state?.lastPosition?.let { if (it != 0) MPVLib.setPropertyInt("time-pos", it) }
    }
    MPVLib.setPropertyDouble("sub-speed", state?.subSpeed ?: playerViewModel.subtitlesPreferences.defaultSubSpeed.get().toDouble())

    currentVideoPlaybackState = state
  }


  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    if (player.onKey(event!!)) return true
    return super.onKeyUp(keyCode, event)
  }
}
