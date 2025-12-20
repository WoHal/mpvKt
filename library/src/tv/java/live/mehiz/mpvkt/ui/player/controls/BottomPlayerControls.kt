package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import dev.vivvvek.seeker.Segment
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.CurrentChapter

@Composable
fun BottomPlayerControls(
  modifier: Modifier = Modifier,
  // speed
  playbackSpeed: Float,
  onPlaybackSpeedChange: (Float) -> Unit,
  // decoder
  decoder: Decoder,
  onDecoderClick: () -> Unit,
  onDecoderLongClick: () -> Unit,
  // subtitles
  onSubtitlesClick: () -> Unit,
  onSubtitlesLongClick: () -> Unit,
  // audio
  onAudioClick: () -> Unit,
  onAudioLongClick: () -> Unit,

  // chapter
  isChaptersVisible: Boolean = false,
  currentChapter: Segment? = null,

  focusRequester: FocusRequester = remember { FocusRequester() },
  focusInteraction: MutableInteractionSource = remember { MutableInteractionSource() },
  onOpenSheet: (Sheets) -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // speed
    ControlsButton(
      text = stringResource(R.string.player_speed, playbackSpeed),
      onClick = { onPlaybackSpeedChange(if (playbackSpeed >= 2) 0.25f else playbackSpeed + 0.25f) },
      onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
      modifier = Modifier.focusRequester(focusRequester)
        .focusable(enabled = true, interactionSource = focusInteraction),
    )

    // decoder
    ControlsButton(
      decoder.title,
      onClick = onDecoderClick,
      onLongClick = onDecoderLongClick,
      modifier = Modifier.focusRequester(focusRequester)
        .focusable(enabled = true, interactionSource = focusInteraction),
    )

    // subtitle
    ControlsButton(
      Icons.Default.Subtitles,
      onClick = onSubtitlesClick,
      onLongClick = onSubtitlesLongClick,
      modifier = Modifier.focusRequester(focusRequester)
        .focusable(enabled = true, interactionSource = focusInteraction),
    )

    // audio
    ControlsButton(
      Icons.Default.Audiotrack,
      onClick = onAudioClick,
      onLongClick = onAudioLongClick,
      modifier = Modifier.focusRequester(focusRequester)
        .focusable(enabled = true, interactionSource = focusInteraction),
    )

    // Chapter controls
    AnimatedVisibility(
      isChaptersVisible && currentChapter != null,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      CurrentChapter(
        chapter = currentChapter!!,
        onClick = { onOpenSheet(Sheets.Chapters) }
      )
    }
  }
}
