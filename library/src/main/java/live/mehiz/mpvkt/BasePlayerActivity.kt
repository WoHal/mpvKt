package live.mehiz.mpvkt

import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.github.k1rakishou.fsaf.FileManager
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import live.mehiz.mpvkt.domain.playbackstate.repository.PlaybackStateRepository
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.ui.player.CustomKeyCodes
import live.mehiz.mpvkt.ui.player.MPVView
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.PlayerViewModelProviderFactory
import live.mehiz.mpvkt.ui.player.SingleActionGesture
import live.mehiz.mpvkt.ui.player.openContentFd
import live.mehiz.mpvkt.ui.player.resolveUri
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.util.UUID

abstract class BasePlayerActivity : ComponentActivity() {
  abstract val playerObserver: MPVLib.EventObserver
  abstract val playerHelper: BasePlayerHelper
  abstract var currentPlayerItem: MPVPlayerItem

  val playerLayoutBinding: PlayerLayoutBinding by lazy { PlayerLayoutBinding.inflate(layoutInflater) }
  val player: MPVView by lazy { playerLayoutBinding.player }

  val playerViewModel: PlayerViewModel by viewModels { PlayerViewModelProviderFactory(this as PlayerActivity) }
  private val playbackStateRepository: PlaybackStateRepository by inject()
  val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
  var mediaSession: MediaSession? = null
  val playerPreferences: PlayerPreferences by inject()
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val gesturePreferences: GesturePreferences by inject()
  private val fileManager: FileManager by inject()

  var audioFocusRequest: AudioFocusRequestCompat? = null
  private var restoreAudioFocus: () -> Unit = {}

  override fun finish() {
    setReturnIntent()
    super.finish()
  }

  private fun copyMPVAssets() {
    Utils.copyAssets(this@BasePlayerActivity)
    copyMPVScripts()
    copyMPVConfigFiles()
    // fonts can be lazily loaded
    lifecycleScope.launch(Dispatchers.IO) {
      copyMPVFonts()
    }
  }

  fun setupMPV() {
    copyMPVAssets()
    player.initialize(filesDir.path, cacheDir.path)
    MPVLib.addObserver(playerObserver)
  }

  fun setupAudio() {
    audioPreferences.audioChannels.get().let { MPVLib.setPropertyString(it.property, it.value) }

    val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN).also {
      it.setAudioAttributes(
        AudioAttributesCompat.Builder().setUsage(AudioAttributesCompat.USAGE_MEDIA)
          .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC).build(),
      )
      it.setOnAudioFocusChangeListener(audioFocusChangeListener)
    }.build()
    AudioManagerCompat.requestAudioFocus(audioManager, request).let {
      if (it == AudioManager.AUDIOFOCUS_REQUEST_FAILED) return@let
      audioFocusRequest = request
    }
  }

  private fun copyMPVConfigFiles() {
    val applicationPath = filesDir.path
    try {
      val mpvConf = fileManager.fromUri(advancedPreferences.mpvConfStorageUri.get().toUri())
        ?: error("User hasn't set any mpvConfig directory")
      if (!fileManager.exists(mpvConf)) error("Couldn't access mpv configuration directory")
      fileManager.copyDirectoryWithContent(mpvConf, fileManager.fromPath(applicationPath), true)
    } catch (e: Exception) {
      File("$applicationPath/mpv.conf")
        .also { if (!it.exists()) it.createNewFile() }
        .writeText(advancedPreferences.mpvConf.get())
      File("$applicationPath/input.conf")
        .also { if (!it.exists()) it.createNewFile() }
        .writeText(advancedPreferences.inputConf.get())
      Timber.e("Couldn't copy mpv configuration files: ${e.message}")
    }
  }

  private fun copyMPVScripts() {
    val mpvktLua = assets.open("mpvkt.lua")
    val applicationPath = filesDir.path

    val scriptsDir = fileManager.createDir(fileManager.fromPath(applicationPath), "scripts")!!

    fileManager.deleteContent(scriptsDir)

    File("$scriptsDir/mpvkt.lua")
      .also { if (!it.exists()) it.createNewFile() }
      .writeText(mpvktLua.bufferedReader().readText())
  }

  fun setupCustomButtons(buttons: List<CustomButtonEntity>) {
    val applicationPath = filesDir.path

    val scriptsDir = fileManager.createDir(fileManager.fromPath(applicationPath), "scripts")!!

    val customButtonsContent = buildString {
      appendLine("local lua_modules = mp.find_config_file('scripts')")
      appendLine("if lua_modules then")
      appendLine("package.path = package.path .. ';' .. lua_modules .. '/?.lua;' .. lua_modules .. '/?/init.lua'")
      appendLine("end")
      appendLine("local mpvkt = require 'mpvkt'")
      buttons.forEach { button ->
        appendLine("function button${button.id}()")
        appendLine(button.content)
        appendLine("end")
        appendLine("mp.register_script_message('call_button_${button.id}', button${button.id})")
        appendLine("function button${button.id}long()")
        appendLine(button.longPressContent)
        appendLine("end")
        appendLine("mp.register_script_message('call_button_${button.id}_long', button${button.id}long)")
      }
    }

    val file = File("$scriptsDir/custombuttons.lua")
      .also { if (!it.exists()) it.createNewFile() }

    file.writeText(customButtonsContent)

    MPVLib.command("load-script", file.absolutePath)
  }

  private fun copyMPVFonts() {
    try {
      val cachePath = cacheDir.path
      val fontsDir = fileManager.fromUri(subtitlesPreferences.fontsFolder.get().toUri())
        ?: error("User hasn't set any fonts directory")
      if (!fileManager.exists(fontsDir)) error("Couldn't access fonts directory")

      val destDir = fileManager.fromPath("$cachePath/fonts")
      if (!fileManager.exists(destDir)) fileManager.createDir(fileManager.fromPath(cachePath), "fonts")

      if (fileManager.findFile(destDir, "subfont.ttf") == null) {
        resources.assets.open("subfont.ttf")
          .copyTo(File("$cachePath/fonts/subfont.ttf").outputStream())
      }

      fileManager.copyDirectoryWithContent(fontsDir, destDir, false)
    } catch (e: Exception) {
      Timber.e("Couldn't copy fonts to application directory: ${e.message}")
    }
  }

  private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    when (it) {
      AudioManager.AUDIOFOCUS_LOSS,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        -> {
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

  fun setIntentExtras(extras: Bundle?) {
    if (extras == null) return

    extras.getString("title")?.let { MPVLib.setPropertyString("force-media-title", it) }
    MPVLib.setPropertyInt("time-pos", extras.getInt("position", 0) / 1000)

    // subtitles
    if (extras.containsKey("subs")) {
      val subList = Utils.getParcelableArray<Uri>(extras, "subs")
      val subsToEnable = Utils.getParcelableArray<Uri>(extras, "subs.enable")

      for (suburi in subList) {
        val subfile = suburi.resolveUri(this) ?: continue
        val flag = if (subsToEnable.any { it == suburi }) "select" else "auto"

        Timber.v("Adding subtitles from intent extras: $subfile")
        MPVLib.command("sub-add", subfile, flag)
      }
    }

    extras.getStringArray("headers")?.let { headers ->
      if (headers[0].startsWith("User-Agent", true)) MPVLib.setPropertyString("user-agent", headers[1])
      val headersString = headers.asSequence().drop(2).chunked(2).associate { it[0] to it[1] }
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }.joinToString(",")
      MPVLib.setPropertyString("http-header-fields", headersString)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if (!isInPictureInPictureMode) {
        playerViewModel.changeVideoAspect(playerPreferences.videoAspect.get())
      } else {
        playerViewModel.hideControls()
      }
    }
    super.onConfigurationChanged(newConfig)
  }

  fun saveVideoPlaybackState(mediaId: String) {
    if (mediaId.isBlank()) return
    lifecycleScope.launch(Dispatchers.IO) {
      val oldState = playbackStateRepository.getVideoDataById(mediaId)
      Timber.d("Saving playback state, saveOnQuit: ${playerPreferences.savePositionOnQuit.get()}, pos: ${playerViewModel.pos}")
      playbackStateRepository.upsert(
        PlaybackStateEntity(
          mediaId = oldState?.mediaId ?: UUID.randomUUID().toString(),
          mediaTitle = oldState?.mediaTitle ?: currentPlayerItem.mediaTitle,
          lastPosition = if (playerPreferences.savePositionOnQuit.get()) {
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
        ),
      )
    }
  }

  suspend fun loadVideoPlaybackState(mediaId: String) {
    if (mediaId.isBlank()) return
    val state = playbackStateRepository.getVideoDataById(mediaId)
    val getDelay: (Int, Int?) -> Double = { preferenceDelay, stateDelay ->
      (stateDelay ?: preferenceDelay) / 1000.0
    }
    val subDelay = getDelay(subtitlesPreferences.defaultSubDelay.get(), state?.subDelay)
    val secondarySubDelay = getDelay(subtitlesPreferences.defaultSecondarySubDelay.get(), state?.secondarySubDelay)
    val audioDelay = getDelay(audioPreferences.defaultAudioDelay.get(), state?.audioDelay)
    state?.let {
      player?.apply {
        sid = it.sid
        secondarySid = it.secondarySid
        aid = it.aid
      }
      MPVLib.setPropertyDouble("sub-delay", subDelay)
      MPVLib.setPropertyDouble("secondary-sub-delay", secondarySubDelay)
      MPVLib.setPropertyDouble("speed", it.playbackSpeed)
      MPVLib.setPropertyDouble("audio-delay", audioDelay)
    }
    if (playerPreferences.savePositionOnQuit.get()) {
      state?.lastPosition?.let { if (it != 0) MPVLib.setPropertyInt("time-pos", it) }
    }
    MPVLib.setPropertyDouble("sub-speed", state?.subSpeed ?: subtitlesPreferences.defaultSubSpeed.get().toDouble())
  }

  private fun setReturnIntent() {
    Timber.d("setting return intent")
    setResult(
      RESULT_OK,
      Intent(RESULT_INTENT).apply {
        playerViewModel.pos?.let { putExtra("position", it * 1000) }
        playerViewModel.duration?.let { putExtra("duration", it * 1000) }
      },
    )
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> {
        playerViewModel.changeVolumeBy(1)
        playerViewModel.displayVolumeSlider()
      }

      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        playerViewModel.changeVolumeBy(-1)
        playerViewModel.displayVolumeSlider()
      }

      KeyEvent.KEYCODE_DPAD_RIGHT -> playerViewModel.handleLeftDoubleTap()
      KeyEvent.KEYCODE_DPAD_LEFT -> playerViewModel.handleRightDoubleTap()
      KeyEvent.KEYCODE_SPACE -> playerViewModel.pauseUnpause()
      KeyEvent.KEYCODE_MEDIA_STOP -> finishAndRemoveTask()

      KeyEvent.KEYCODE_MEDIA_REWIND -> playerViewModel.handleLeftDoubleTap()
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> playerViewModel.handleRightDoubleTap()

      // other keys should be bound by the user in input.conf ig
      else -> {
        event?.let { player?.onKey(it) }
        super.onKeyDown(keyCode, event)
      }
    }
    return true
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    if (player != null && player!!.onKey(event!!)) return true
    return super.onKeyUp(keyCode, event)
  }

  fun setupMediaSession() {
    val previousAction = gesturePreferences.mediaPreviousGesture.get()
    val playAction = gesturePreferences.mediaPlayGesture.get()
    val nextAction = gesturePreferences.mediaNextGesture.get()

    mediaSession = MediaSession(this, "PlayerActivity").apply {
      setCallback(
        object : MediaSession.Callback() {
          override fun onPlay() {
            when (playAction) {
              SingleActionGesture.None -> {}
              SingleActionGesture.Seek -> {}
              SingleActionGesture.PlayPause -> {
                super.onPlay()
                playerViewModel.unpause()
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
            this@BasePlayerActivity.onStop()
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

  companion object {
    // action of result intent
    private const val RESULT_INTENT = "live.mehiz.mpvkt.ui.player.PlayerActivity.result"
  }
}
