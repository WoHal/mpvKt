package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.preferences.preference.minusAssign
import live.mehiz.mpvkt.preferences.preference.plusAssign
import live.mehiz.mpvkt.ui.player.Decoder.Companion.getDecoderFromValue
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.collectAsState
import live.mehiz.mpvkt.ui.player.controls.components.MultipleSpeedPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.TextPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.sheets.toFixed
import live.mehiz.mpvkt.ui.player.modifier.handleDPadKeyEvents
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.math.abs

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3Api::class)
@Composable
@Suppress("CyclomaticComplexMethod", "ViewModelForwarding")
fun PlayerControls(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
  onBackPress: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  val focusInteractionSource = remember { MutableInteractionSource() }
  val spacing = MaterialTheme.spacing
  val playerPreferences = koinInject<PlayerPreferences>()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val sheetShown by viewModel.sheetShown.collectAsState()
  val pausedForCache by MPVLib.propBoolean["paused-for-cache"].collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val position by MPVLib.propInt["time-pos"].collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControls by remember { mutableStateOf(true) }
  val currentChapter by MPVLib.propInt["chapter"].collectAsState()
  val mpvDecoder by MPVLib.propString["hwdec-current"].collectAsState()
  val decoder by remember { derivedStateOf { getDecoderFromValue(mpvDecoder ?: "auto") } }
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val chapters by viewModel.chapters.collectAsState(persistentListOf())

  val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
  val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
  val speedPresets by playerPreferences.speedPresets.collectAsState()

  val onOpenSheet: (Sheets) -> Unit = {
    viewModel.sheetShown.update { _ -> it }
    if (it == Sheets.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.panelShown.update { Panels.None }
    }
  }
  val onOpenPanel: (Panels) -> Unit = {
    viewModel.panelShown.update { _ -> it }
    if (it == Panels.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.sheetShown.update { Sheets.None }
    }
  }

  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControls,
  ) {
    if (controlsShown && paused == false && !isSeeking) {
      delay(playerTimeToDisappear.toLong())
      viewModel.hideControls()
    }
  }
  val transparentOverlay by animateFloatAsState(
    if (controlsShown) .8f else 0f,
    animationSpec = playerControlsExitAnimationSpec(),
    label = "controls_transparent_overlay",
  )
  CompositionLocalProvider(
    LocalRippleConfiguration provides playerRippleConfiguration,
    LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
    LocalContentColor provides Color.White,
  ) {
    CompositionLocalProvider(
      LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
      ConstraintLayout(
        modifier = modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              Pair(0f, Color.Black),
              Pair(.2f, Color.Transparent),
              Pair(.7f, Color.Transparent),
              Pair(1f, Color.Black),
            ),
            alpha = transparentOverlay,
          )
          .padding(horizontal = MaterialTheme.spacing.medium)
          .focusRequester(focusRequester)
          .focusable()
          .handleDPadKeyEvents(
            onLeft = {
              viewModel.showControls()
              focusManager.moveFocus(FocusDirection.Left)
            },
            onRight = {
              viewModel.showControls()
              focusManager.moveFocus(FocusDirection.Right)
            }
          ),
      ) {
        val (topControls, bottomControls) = createRefs()
        val playerPauseButton = createRef()
        val (playerUpdates) = createRefs()

        val reduceMotion by playerPreferences.reduceMotion.collectAsState()

        val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
        val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
        val aspectRatio by playerPreferences.videoAspect.collectAsState()
        LaunchedEffect(currentPlayerUpdate, aspectRatio) {
          if (currentPlayerUpdate is PlayerUpdates.MultipleSpeed || currentPlayerUpdate is PlayerUpdates.None) {
            return@LaunchedEffect
          }
          delay(2000)
          viewModel.playerUpdate.update { PlayerUpdates.None }
        }

        // top left status
        AnimatedVisibility(
          currentPlayerUpdate !is PlayerUpdates.None,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerUpdates) {
            linkTo(parent.start, parent.end)
            linkTo(parent.top, parent.bottom, bias = 0.2f)
          },
        ) {
          when (currentPlayerUpdate) {
            is PlayerUpdates.MultipleSpeed -> MultipleSpeedPlayerUpdate(currentSpeed = holdForMultipleSpeed)
            is PlayerUpdates.AspectRatio -> TextPlayerUpdate(stringResource(aspectRatio.titleRes))
            is PlayerUpdates.ShowText -> TextPlayerUpdate((currentPlayerUpdate as PlayerUpdates.ShowText).value)
            else -> {}
          }
        }

        // Top
        val mediaTitle by MPVLib.propString["media-title"].collectAsState()
        AnimatedVisibility(
          controlsShown,
          enter = if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(topControls) {
            top.linkTo(parent.top, spacing.medium)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          },
        ) {
          Text(
            mediaTitle ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
          )
        }

        // Play Button, Seeking Text
        AnimatedVisibility(
          visible = (controlsShown || gestureSeekAmount != null) || pausedForCache == true,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerPauseButton) {
            end.linkTo(parent.absoluteRight)
            start.linkTo(parent.absoluteLeft)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) {
          val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()
          val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
          val interaction = remember { MutableInteractionSource() }
          val isFocus by focusInteractionSource.collectIsFocusedAsState()
          when {
            gestureSeekAmount != null -> {
              Text(
                stringResource(
                  R.string.player_gesture_seek_indicator,
                  if (gestureSeekAmount!!.second >= 0) '+' else '-',
                  Utils.prettyTime(abs(gestureSeekAmount!!.second)),
                  Utils.prettyTime(gestureSeekAmount!!.first + gestureSeekAmount!!.second),
                ),
                style = MaterialTheme.typography.headlineMedium.copy(
                  shadow = Shadow(Color.Black, blurRadius = 5f),
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
            }

            pausedForCache == true && showLoadingCircle -> {
              CircularProgressIndicator(
                Modifier.size(96.dp),
                strokeWidth = 6.dp,
              )
            }

            controlsShown -> {
              val focusRequester = remember { FocusRequester() }
              val focusInteractionSource = remember { MutableInteractionSource() }
              Image(
                painter = rememberAnimatedVectorPainter(icon, paused == false),
                modifier = Modifier
                  .size(96.dp)
                  .clip(CircleShape)
                  .clickable(
                    interaction,
                    ripple(),
                    onClick = viewModel::pauseUnpause,
                  )
                  .background(color = if (isFocus) Color.White.copy(alpha = 0.35f) else Color.Transparent)
                  .padding(MaterialTheme.spacing.medium)
                  .focusRequester(focusRequester)
                  .focusable(interactionSource = focusInteractionSource),
                contentDescription = null,
              )
            }
          }

          LaunchedEffect(controlsShown) {
            if (controlsShown) {
              focusRequester.requestFocus()
              focusManager.moveFocus(FocusDirection.Next)
            }
          }
        }

        // Bottom Controls
        AnimatedVisibility(
          visible = controlsShown,
          enter = if (!reduceMotion) {
            slideInVertically(playerControlsEnterAnimationSpec()) { it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutVertically(playerControlsExitAnimationSpec()) { it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(bottomControls) {
            linkTo(
              start = parent.start,
              end = parent.end,
              startMargin = spacing.medium,
              endMargin = spacing.medium
            )
            bottom.linkTo(parent.bottom)
          },
        ) {
          val invertDuration by playerPreferences.invertDuration.collectAsState()
          val readAhead by MPVLib.propFloat["demuxer-cache-time"].collectAsState()
          val remaining by MPVLib.propFloat["playtime-remaining"].collectAsState()
          val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
          val showChaptersButton by playerPreferences.showChaptersButton.collectAsState()

          Row(
            modifier = Modifier.fillMaxWidth(),
          ) {
            SeekbarWithTimers(
              modifier = Modifier.weight(1f).focusProperties { canFocus = false },
              position = position?.toFloat() ?: 0f,
              duration = duration?.toFloat() ?: 0f,
              remaining = remaining ?: 0f,
              readAheadValue = readAhead ?: 0f,
              onValueChange = {
                isSeeking = true
                viewModel.seekTo(it.toInt(), preciseSeeking)
              },
              onValueChangeFinished = { isSeeking = false },
              timersInverted = Pair(false, invertDuration),
              durationTimerOnCLick = { playerPreferences.invertDuration.set(!invertDuration) },
              positionTimerOnClick = {},
              chapters = chapters,
            )

            BottomPlayerControls(
              modifier = Modifier.padding(start = 50.dp),
              // speed
              playbackSpeed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
              onPlaybackSpeedChange = {
                MPVLib.setPropertyFloat("speed", it)
                playerPreferences.defaultSpeed.set(it)
              },
              // decoder
              decoder = decoder,
              onDecoderClick = { viewModel.cycleDecoders() },
              onDecoderLongClick = { onOpenSheet(Sheets.Decoders) },
              // subtitle
              onSubtitlesClick = { onOpenSheet(Sheets.SubtitleTracks) },
              onSubtitlesLongClick = { onOpenPanel(Panels.SubtitleSettings) },
              // audio
              onAudioClick = { onOpenSheet(Sheets.AudioTracks) },
              onAudioLongClick = { onOpenPanel(Panels.AudioDelay) },
              // chapter
              isChaptersVisible = showChaptersButton && chapters.isNotEmpty(),
              currentChapter = chapters.getOrNull(currentChapter ?: 0),

              onOpenSheet = onOpenSheet,
            )
          }
        }

        // more settings
        AnimatedVisibility(
          visible = sheetShown != Sheets.None,
          enter = slideInVertically(playerControlsEnterAnimationSpec()) +
            fadeIn(playerControlsEnterAnimationSpec()),
          exit = slideOutVertically(playerControlsExitAnimationSpec()) +
            fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerPauseButton) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) {
          PlayerSheetControls(
            modifier = Modifier.fillMaxSize(),
            // decoder
            decoder = decoder,
            onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) },
            // speed
            playbackSpeed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
            playbackSpeedPresets = speedPresets,
            onPlaybackSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) },
            // subtitle
            subtitles = subtitles,
            onSelectSubtitle = viewModel::selectSub,
            // audio
            audioTracks = audioTracks,
            onSelectAudio = { id ->
              if (id < 0) {
                MPVLib.setPropertyBoolean("aid", false)
              } else if (MPVLib.getPropertyInt("aid") != id) {
                MPVLib.setPropertyInt("aid", id)
              }
            },

            onDismissRequest = { onOpenSheet(Sheets.None) },
          )
        }
      }
    }
//    val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
//    val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
//    val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
//    val speedPresets by playerPreferences.speedPresets.collectAsState()
//    PlayerSheets(
//      sheetShown = sheetShown,
//      subtitles = subtitles,
//      onAddSubtitle = viewModel::addSubtitle,
//      onSelectSubtitle = viewModel::selectSub,
//      audioTracks = audioTracks,
//      onAddAudio = viewModel::addAudio,
//      onSelectAudio = {
//        if (MPVLib.getPropertyInt("aid") == it.id) {
//          MPVLib.setPropertyBoolean("aid", false)
//        } else {
//          MPVLib.setPropertyInt("aid", it.id)
//        }
//      },
//      chapter = chapters.getOrNull(currentChapter ?: 0),
//      chapters = chapters,
//      onSeekToChapter = {
//        MPVLib.setPropertyInt("chapter", it)
//        viewModel.unpause()
//      },
//      decoder = decoder,
//      onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) },
//      speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
//      onSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) },
//      onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) },
//      onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() },
//      onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() },
//      onResetSpeedPresets = playerPreferences.speedPresets::delete,
//      speedPresets = speedPresets.map { it.toFloat() }.sorted(),
//      onResetDefaultSpeed = {
//        MPVLib.setPropertyFloat("speed", playerPreferences.defaultSpeed.deleteAndGet().toFixed(2))
//      },
//
//      sleepTimerTimeRemaining = sleepTimerTimeRemaining,
//      onStartSleepTimer = viewModel::startTimer,
//      buttons = emptyList<CustomButtonEntity>().toImmutableList(),
//      onOpenPanel = onOpenPanel,
//      onDismissRequest = { onOpenSheet(Sheets.None) },
//    )
  }
}

fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 300,
  easing = FastOutSlowInEasing,
)

fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 100,
  easing = LinearOutSlowInEasing,
)
