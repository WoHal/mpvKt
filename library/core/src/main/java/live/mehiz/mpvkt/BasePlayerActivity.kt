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
import live.mehiz.mpvkt.domain.playbackstate.repository.PlaybackStateRepository
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.ui.player.MPVView
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.PlayerViewModelProviderFactory
import org.koin.android.ext.android.inject
import timber.log.Timber

@Suppress("TooManyFunctions")
abstract class BasePlayerActivity : ComponentActivity(),
  BasePlayerEvent, MPVLib.EventObserver, PlayerScreenObserver {
  var currentVideoPlaybackState: PlaybackStateEntity? = null

  val playbackStateRepository: PlaybackStateRepository by inject()

  val playerViewModel: PlayerViewModel by viewModels { PlayerViewModelProviderFactory(this) }
  val windowInsetsController by lazy { playerViewModel.playerHelper.windowInsetsController }

  // a bunch of observers
  override fun eventProperty(property: String, value: Long) {
    if (playerViewModel.player.isExiting) return
  }

  override fun eventProperty(property: String) {
    if (playerViewModel.player.isExiting) return
  }

  override fun eventProperty(property: String, value: Boolean) {
    if (playerViewModel.player.isExiting) return
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
          onPlayReachedEnd()
        }
      }
    }
  }

  override fun eventProperty(property: String, value: String) {
    if (playerViewModel.player.isExiting) return
    // Custom Buttons Event
  }

  override fun eventProperty(property: String, value: MPVNode) {
    if (playerViewModel.player.isExiting) return
  }

  @SuppressLint("NewApi")
  override fun eventProperty(property: String, value: Double) {
    if (playerViewModel.player.isExiting) return
  }

  override fun event(eventId: Int, data: MPVNode) {
    if (playerViewModel.player.isExiting) return
    when (eventId) {
      MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
        playerViewModel.setMpvExtras(playerViewModel.currentPlayItem)

        MPVLib.setPropertyString("media-title", playerViewModel.currentPlayItem.mediaTitle)
        lifecycleScope.launch(Dispatchers.IO) {
          loadVideoPlaybackState(playerViewModel.currentPlayItem.mediaId)
        }
        playerViewModel.changeVideoAspect(playerViewModel.playerPreferences.videoAspect.get())
      }

      MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> playerViewModel.player.isExiting = false
    }
  }

  override fun onPlayerScreenCreated() {
    playerViewModel.playerHelper.setupAudio()
    playerViewModel.playerHelper.setupMediaSession()
    playerViewModel.player.isExiting = false
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
    playerViewModel.playerHelper.releaseAudio()
    playerViewModel.playerHelper.releaseMediaSession()

    playerViewModel.player.isExiting = true
    if (isFinishing) {
      MPVLib.command("stop")
    }
  }

  override fun onPlayReachedEnd() {
    if (playerViewModel.canPlayNext) {
      playerViewModel.playNext()
    } else if (playerViewModel.playerPreferences.closeAfterReachingEndOfVideo.get()) {
      onBackPressedDispatcher.onBackPressed()
    }
  }

  fun saveVideoPlaybackState() {
    lifecycleScope.launch(Dispatchers.IO) {
      val oldState = currentVideoPlaybackState
      val newState = PlaybackStateEntity(
        mediaId = oldState?.mediaId ?: playerViewModel.currentPlayItem.mediaId,
        mediaTitle = oldState?.mediaTitle ?: playerViewModel.currentPlayItem.mediaTitle,
        lastPosition = if (playerViewModel.playerPreferences.savePositionOnQuit.get()) {
          val pos = playerViewModel.pos ?: 0
          val duration = playerViewModel.duration ?: 0
          if (pos < duration - 1) pos else 0
        } else {
          oldState?.lastPosition ?: 0
        },
        playbackSpeed = MPVLib.getPropertyDouble("speed")!!,
        sid = playerViewModel.player.sid,
        subDelay = (MPVLib.getPropertyDouble("sub-delay")!! * 1000).toInt(),
        subSpeed = MPVLib.getPropertyDouble("sub-speed")!!,
        secondarySid = playerViewModel.player.secondarySid,
        secondarySubDelay = (MPVLib.getPropertyDouble("secondary-sub-delay")!! * 1000).toInt(),
        aid = playerViewModel.player.aid,
        audioDelay = (MPVLib.getPropertyDouble("audio-delay")!! * 1000).toInt(),
      )
      playbackStateRepository.upsert(newState)
      currentVideoPlaybackState = newState
    }
  }

  suspend fun loadVideoPlaybackState(mediaId: String) {
    if (mediaId.isBlank()) return
    val state = playbackStateRepository.getVideoDataById(mediaId)
    val getDelay: (Int, Int?) -> Double = { preferenceDelay, stateDelay ->
      (stateDelay ?: preferenceDelay) / 1000.0
    }
    val subDelay = getDelay(playerViewModel.subtitlesPreferences.defaultSubDelay.get(), state?.subDelay)
    val secondarySubDelay = getDelay(playerViewModel.subtitlesPreferences.defaultSecondarySubDelay.get(), state?.secondarySubDelay)
    val audioDelay = getDelay(playerViewModel.audioPreferences.defaultAudioDelay.get(), state?.audioDelay)
    Timber.d("loaded playback state: $state")
    state?.let {
      playerViewModel.player.sid = it.sid
      playerViewModel.player.secondarySid = it.secondarySid
      playerViewModel.player.aid = it.aid
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
    if (playerViewModel.player.onKey(event!!)) return true
    return super.onKeyUp(keyCode, event)
  }
}
