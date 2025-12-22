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
  private var pipRect: Rect? = null
  val isPipSupported by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      false
    } else {
      packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
  }
  private var pipReceiver: BroadcastReceiver? = null

  private var mediaPlaybackService: MediaPlaybackService? = null
  var serviceBound = false

  private val noisyReceiver = object : BroadcastReceiver() {
    var initialized = false
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
        playerViewModel.pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }

  private fun setupNoisyReceiver() {
    val filter = IntentFilter().apply { addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY) }
    registerReceiver(noisyReceiver, filter)
    noisyReceiver.initialized = true
  }

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

    @SuppressLint("NewApi")
    override fun eventProperty(property: String, value: Double) {
      if (player.isExiting) return
      runOnUiThread {
        when (property) {
          "video-params/aspect" -> if (isPipSupported) createPipParams()
        }
      }
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
            setOrientation()
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
      setupNoisyReceiver()

      initCurrentPlayerItem()

      play()

      setOrientation()
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
      if (!serviceBound && playerPreferences.automaticBackgroundPlayback.get()) {
        startBackgroundPlayback()
      } else {
        playerViewModel.pause()
        if (serviceBound) {
          unbindService(serviceConnection)
          serviceBound = false
        }
      }
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
      if (noisyReceiver.initialized) {
        unregisterReceiver(noisyReceiver)
        noisyReceiver.initialized = false
      }

      player.isExiting = true
      if (isFinishing) {
        MPVLib.command("stop")
      }
      MPVLib.removeObserver(playerObserver)
      MPVLib.destroy()
    }
  }

  val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      val binder = service as MediaPlaybackService.MediaPlaybackBinder
      mediaPlaybackService = binder.getService()
      serviceBound = true

      val artist = MPVLib.getPropertyString("metadata/artist") ?: ""
      Timber.d("on service connected")
      mediaPlaybackService?.setMediaInfo(
        title = currentPlayerItem.mediaTitle,
        artist = artist,
        thumbnail = MPVLib.grabThumbnail(1080)
      )
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      mediaPlaybackService = null
      serviceBound = false
    }
  }

  fun play(playerItem: MPVPlayerItem? = null) {
    playerItem?.let {
      currentPlayerItem = it
    }

    player.playFile(currentPlayerItem.uri)
  }
  fun startBackgroundPlayback() {
    val intent = Intent(this, MediaPlaybackService::class.java)
    startService(intent)
    bindService(intent, serviceConnection, BIND_AUTO_CREATE)
  }

  fun endBackgroundPlayback() {
    stopService(Intent(this, MediaPlaybackService::class.java))
    mediaPlaybackService = null
    serviceBound = false
  }

  @SuppressLint("NewApi")
  override fun onUserLeaveHint() {
    if (isPipSupported && playerViewModel.paused == false && playerPreferences.automaticallyEnterPip.get()) {
      enterPictureInPictureMode()
    }
    super.onUserLeaveHint()
  }

  @SuppressLint("NewApi")
  override fun onBackPressed() {
    if (isPipSupported && playerViewModel.paused == false && playerPreferences.automaticallyEnterPip.get()) {
      if (playerViewModel.sheetShown.value == Sheets.None && playerViewModel.panelShown.value == Panels.None) {
        enterPictureInPictureMode()
      }
    } else {
      super.onBackPressed()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    setupMPV()

    setContent {
      MpvKtTheme {
        BasePlayerScreen(
          binding = playerLayoutBinding,
          playerHelper = playerHelper,
          onBackPress = ::finish,
          viewModel = playerViewModel,
          modifier = Modifier.onGloballyPositioned {
            pipRect = run {
              val boundsInWindow = it.boundsInWindow()
              Rect(
                boundsInWindow.left.toInt(), boundsInWindow.top.toInt(),
                boundsInWindow.right.toInt(), boundsInWindow.bottom.toInt()
              )
            }
          }
        )
      }
    }
  }

  override fun onStart() {
    super.onStart()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported) {
      setPictureInPictureParams(createPipParams())
    }
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    playerLayoutBinding.root.systemUiVisibility =
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LOW_PROFILE
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

    if (serviceBound) {
      endBackgroundPlayback()
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun createPipParams(): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      builder.setTitle(currentPlayerItem.mediaTitle)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val autoEnter = playerPreferences.automaticallyEnterPip.get()
      builder.setAutoEnterEnabled(playerViewModel.paused == false && autoEnter)
      builder.setSeamlessResizeEnabled(playerViewModel.paused == false && autoEnter)
    }
    builder.setActions(createPipActions(this, playerViewModel.paused == true))
    builder.setSourceRectHint(pipRect)
    MPVLib.getPropertyInt("video-params/h")?.let {
      val height = it
      val width = it * player.getVideoOutAspect()!!
      val rational = Rational(height, width.toInt()).toFloat()
      if (rational in 0.42..2.38) builder.setAspectRatio(Rational(width.toInt(), height))
    }
    return builder.build()
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
    if (!isInPictureInPictureMode) {
      pipReceiver?.let {
        unregisterReceiver(pipReceiver)
        pipReceiver = null
      }
      super.onPictureInPictureModeChanged(false, newConfig)
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setPictureInPictureParams(createPipParams())
    }
    playerViewModel.hideControls()
    playerViewModel.hideSeekBar()
    playerViewModel.isBrightnessSliderShown.update { false }
    playerViewModel.isVolumeSliderShown.update { false }
    playerViewModel.sheetShown.update { Sheets.None }
    pipReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action != PIP_INTENTS_FILTER) return
        when (intent.getIntExtra(PIP_INTENT_ACTION, 0)) {
          PIP_PAUSE -> playerViewModel.pause()
          PIP_PLAY -> playerViewModel.unpause()
          PIP_FF -> playerViewModel.handleRightDoubleTap()
          PIP_FR -> playerViewModel.handleLeftDoubleTap()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          setPictureInPictureParams(createPipParams())
        }
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(pipReceiver, IntentFilter(PIP_INTENTS_FILTER), RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(pipReceiver, IntentFilter(PIP_INTENTS_FILTER))
    }
    super.onPictureInPictureModeChanged(true, newConfig)
  }

  private fun setOrientation() {
    requestedOrientation = when (playerPreferences.orientation.get()) {
      PlayerOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
      PlayerOrientation.Video -> if ((player.getVideoOutAspect() ?: 0.0) > 1.0) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      }

      PlayerOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      PlayerOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      PlayerOrientation.SensorPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      PlayerOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      PlayerOrientation.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      PlayerOrientation.SensorLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
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

      KeyEvent.KEYCODE_SPACE -> playerViewModel.pauseUnpause()
      KeyEvent.KEYCODE_MEDIA_STOP -> finishAndRemoveTask()

      KeyEvent.KEYCODE_MEDIA_REWIND -> playerViewModel.handleLeftDoubleTap()
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> playerViewModel.handleRightDoubleTap()

      // other keys should be bound by the user in input.conf ig
      else -> {
        event?.let { player.onKey(it) }
        super.onKeyDown(keyCode, event)
      }
    }
    return true
  }
}
