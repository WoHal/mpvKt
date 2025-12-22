package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.player.controls.components.TrackList
import live.mehiz.mpvkt.ui.player.controls.components.sheets.getTrackTitle
import live.mehiz.mpvkt.ui.player.modifier.handleDPadKeyEvents
import live.mehiz.mpvkt.ui.theme.spacing

@Composable
fun PlayerSheetControls(
  modifier: Modifier = Modifier,
  // speed
  playbackSpeed: Float,
  playbackSpeedPresets: Set<String>,
  onPlaybackSpeedChange: (Float) -> Unit,
  // decoder
  decoder: Decoder,
  onUpdateDecoder: (Decoder) -> Unit,
  // subtitles
  subtitles: ImmutableList<TrackNode>,
  onSelectSubtitle: (id: Int) -> Unit,
  // audio
  audioTracks: ImmutableList<TrackNode>,
  onSelectAudio: (id: Int) -> Unit,

  // chapter
  isChaptersVisible: Boolean = false,
  currentChapter: Segment? = null,

  onDismissRequest: () -> Unit,
  onOpenSheet: () -> Unit = {},
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    focusManager.moveFocus(FocusDirection.Next)
  }

  Row(
    horizontalArrangement = Arrangement.Center,
    modifier = modifier.padding(MaterialTheme.spacing.large)
      .clip(RoundedCornerShape(14.dp))
      .background(Color.White)
      .focusRequester(focusRequester)
      .focusable()
      .handleDPadKeyEvents(
        onLeft = {
          focusManager.moveFocus(FocusDirection.Left)
        },
        onRight = {
          focusManager.moveFocus(FocusDirection.Right)
        },
        onUp = {
          focusManager.moveFocus(FocusDirection.Up)
        },
        onDown = {
          focusManager.moveFocus(FocusDirection.Down)
        }
      ),
  ) {
    // subtitle
    TrackList(
      title = "Subtitle",
      tracks = subtitles,
      modifier = modifier.weight(1f),
      selector = { (it.mainSelection?.toInt() ?: -1) > -1 },
      onSelect = onSelectSubtitle,
    )

    // audio
    TrackList(
      title = "Audio Tracks",
      tracks = audioTracks,
      modifier = Modifier.weight(1f),
      selector = { it.isSelected },
      onSelect = onSelectAudio,
    )
  }

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
