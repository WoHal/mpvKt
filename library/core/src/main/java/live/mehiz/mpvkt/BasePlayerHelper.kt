package live.mehiz.mpvkt

import android.app.Activity
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.WindowManager
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.ui.player.CustomKeyCodes
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.SingleActionGesture
import timber.log.Timber
import java.io.File

interface PlayerScreenObserver {
  fun onPlayerScreenCreated()
  fun onPlayerScreenPaused()
  fun onPlayerScreenResumed()
  fun onPlayerScreenStopped()
  fun onPlayerScreenDestroy()
}

class PlayerActivityHelper(
  private val activity: Activity,
  private val playerViewModel: PlayerViewModel,
) {
  val windowInsetsController by lazy { WindowCompat.getInsetsController(activity.window, activity.window.decorView) }
  val audioManager by lazy { activity.getSystemService(AUDIO_SERVICE) as AudioManager }
  var mediaSession: MediaSession? = null
  var audioFocusRequest: AudioFocusRequestCompat? = null
  private var restoreAudioFocus: () -> Unit = {}

  fun setupMPV() {
    Utils.copyAssets(activity)
    copyMPVConfigFiles()
    // fonts can be lazily loaded
    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
      copyMPVFonts()
    }
  }
  fun setupAudio() {
    playerViewModel.audioPreferences.audioChannels.get().let { MPVLib.setPropertyString(it.property, it.value) }

    val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN).also {
      it.setAudioAttributes(
        AudioAttributesCompat.Builder().setUsage(AudioAttributesCompat.USAGE_MEDIA)
          .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC).build(),
      )
      it.setOnAudioFocusChangeListener(audioFocusChangeListener)
    }.build()
    AudioManagerCompat.requestAudioFocus(playerViewModel.playerHelper.audioManager, request).let {
      if (it == AudioManager.AUDIOFOCUS_REQUEST_FAILED) return@let
      audioFocusRequest = request
    }
  }
  fun releaseAudio() {
    audioFocusRequest?.let {
      AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
    }
    audioFocusRequest = null
  }

  private fun copyMPVConfigFiles() {
    val applicationPath = activity.filesDir.path
    try {
      val mpvConf = playerViewModel.fileManager.fromUri(playerViewModel.advancedPreferences.mpvConfStorageUri.get().toUri())
        ?: error("User hasn't set any mpvConfig directory")
      if (!playerViewModel.fileManager.exists(mpvConf)) error("Couldn't access mpv configuration directory")
      playerViewModel.fileManager.copyDirectoryWithContent(mpvConf, playerViewModel.fileManager.fromPath(applicationPath), true)
    } catch (e: Exception) {
      File("$applicationPath/mpv.conf")
        .also { if (!it.exists()) it.createNewFile() }
        .writeText(playerViewModel.advancedPreferences.mpvConf.get())
      File("$applicationPath/input.conf")
        .also { if (!it.exists()) it.createNewFile() }
        .writeText(playerViewModel.advancedPreferences.inputConf.get())
      Timber.e("Couldn't copy mpv configuration files: ${e.message}")
    }
  }

  private fun copyMPVFonts() {
    try {
      val cachePath = activity.cacheDir.path
      val fontsDir = playerViewModel.fileManager.fromUri(playerViewModel.subtitlesPreferences.fontsFolder.get().toUri())
        ?: error("User hasn't set any fonts directory")
      if (!playerViewModel.fileManager.exists(fontsDir)) error("Couldn't access fonts directory")

      val destDir = playerViewModel.fileManager.fromPath("$cachePath/fonts")
      if (!playerViewModel.fileManager.exists(destDir)) playerViewModel.fileManager.createDir(playerViewModel.fileManager.fromPath(cachePath), "fonts")

      if (playerViewModel.fileManager.findFile(destDir, "subfont.ttf") == null) {
        activity.resources.assets.open("subfont.ttf")
          .copyTo(File("$cachePath/fonts/subfont.ttf").outputStream())
      }

      playerViewModel.fileManager.copyDirectoryWithContent(fontsDir, destDir, false)
    } catch (e: Exception) {
      Timber.e("Couldn't copy fonts to application directory: ${e.message}")
    }
  }

  private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    when (it) {
      AudioManager.AUDIOFOCUS_LOSS,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        val oldRestore = restoreAudioFocus
        val wasPlayerPaused = playerViewModel.paused ?: false
        playerViewModel.pause()
        restoreAudioFocus = {
          oldRestore()
          if (!wasPlayerPaused) playerViewModel.unpause()
        }
      }

      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        MPVLib.command("multiply", "volume", "0.5")
        restoreAudioFocus = {
          MPVLib.command("multiply", "volume", "2")
        }
      }

      AudioManager.AUDIOFOCUS_GAIN -> {
        restoreAudioFocus()
        restoreAudioFocus = {}
      }

      AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
        Timber.d("didn't get audio focus")
      }
    }
  }


  fun setupMediaSession() {
    val previousAction = playerViewModel.gesturePreferences.mediaPreviousGesture.get()
    val playAction = playerViewModel.gesturePreferences.mediaPlayGesture.get()
    val nextAction = playerViewModel.gesturePreferences.mediaNextGesture.get()

    mediaSession = MediaSession(activity, "PlayerActivity").apply {
      setCallback(
        object : MediaSession.Callback() {
          override fun onPlay() {
            when (playAction) {
              SingleActionGesture.None -> {}
              SingleActionGesture.Seek -> {}
              SingleActionGesture.PlayPause -> {
                super.onPlay()
                playerViewModel.unpause()
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
              }

              SingleActionGesture.Custom -> {
                MPVLib.command("keypress", CustomKeyCodes.MediaPlay.keyCode)
              }
            }
          }

          override fun onPause() {
            when (playAction) {
              SingleActionGesture.None -> {}
              SingleActionGesture.Seek -> {}
              SingleActionGesture.PlayPause -> {
                super.onPause()
                playerViewModel.pause()
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
              }

              SingleActionGesture.Custom -> {
                MPVLib.command("keypress", CustomKeyCodes.MediaPlay.keyCode)
              }
            }
          }

          override fun onSkipToPrevious() {
            when (previousAction) {
              SingleActionGesture.None -> {}
              SingleActionGesture.Seek -> {
                playerViewModel.leftSeek()
              }

              SingleActionGesture.PlayPause -> {
                playerViewModel.pauseUnpause()
              }

              SingleActionGesture.Custom -> {
                MPVLib.command("keypress", CustomKeyCodes.MediaPrevious.keyCode)
              }
            }
          }

          override fun onSkipToNext() {
            when (nextAction) {
              SingleActionGesture.None -> {}
              SingleActionGesture.Seek -> {
                playerViewModel.rightSeek()
              }

              SingleActionGesture.PlayPause -> {
                playerViewModel.pauseUnpause()
              }

              SingleActionGesture.Custom -> {
                MPVLib.command("keypress", CustomKeyCodes.MediaNext.keyCode)
              }
            }
          }

          override fun onStop() {
            super.onStop()
            isActive = false
          }
        },
      )
      setPlaybackState(
        PlaybackState.Builder()
          .setActions(
            PlaybackState.ACTION_PLAY or
              PlaybackState.ACTION_PAUSE or
              PlaybackState.ACTION_STOP or
              PlaybackState.ACTION_SKIP_TO_PREVIOUS or
              PlaybackState.ACTION_SKIP_TO_NEXT,
          )
          .build(),
      )
      isActive = true
    }
  }

  fun releaseMediaSession() {
    mediaSession?.release()
  }
}
