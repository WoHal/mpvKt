package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import dev.vivvvek.seeker.Segment
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton

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

  onOpenSheet: (Sheets) -> Unit,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.End,
  ) {
//    // speed
//    ControlsButton(
//      text = stringResource(R.string.player_speed, playbackSpeed),
//      onClick = { onPlaybackSpeedChange(if (playbackSpeed >= 2) 0.25f else playbackSpeed + 0.25f) },
//      onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
//    )

    // decoder
    ControlsButton(
      decoder.title,
      onClick = onDecoderClick,
      onLongClick = onDecoderLongClick,
    )

//    // subtitle
//    ControlsButton(
//      Icons.Default.Subtitles,
//      onClick = onSubtitlesClick,
//      onLongClick = onSubtitlesLongClick,
//    )
//
//    // audio
//    ControlsButton(
//      Icons.Default.Audiotrack,
//      onClick = onAudioClick,
//      onLongClick = onAudioLongClick,
//    )

//    // Chapter
//    AnimatedVisibility(
//      isChaptersVisible && currentChapter != null,
//      enter = fadeIn(),
//      exit = fadeOut(),
//    ) {
//      CurrentChapter(
//        chapter = currentChapter!!,
//        onClick = { onOpenSheet(Sheets.Chapters) }
//      )
//    }
  }
}
