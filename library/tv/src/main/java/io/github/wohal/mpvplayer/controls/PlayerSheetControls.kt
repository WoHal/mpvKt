package io.github.wohal.mpvplayer.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import io.github.wohal.mpvplayer.controls.components.TrackList
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.theme.spacing

@Composable
fun PlayerSheetControls(
  // speed
//  playbackSpeed: Float,
//  playbackSpeedPresets: Set<String>,
//  onPlaybackSpeedChange: (Float) -> Unit,
//  // decoder
//  decoder: Decoder,
//  onUpdateDecoder: (Decoder) -> Unit,
  // subtitles
  subtitles: ImmutableList<TrackNode>,
  audioTracks: ImmutableList<TrackNode>,
  modifier: Modifier = Modifier,

  onSelectSubtitle: (id: Int) -> Unit = {},
  onSelectAudio: (id: Int) -> Unit = {},

//  // chapter
//  isChaptersVisible: Boolean = false,
//  currentChapter: Segment? = null,
//
//  onDismissRequest: () -> Unit,
//  onOpenSheet: () -> Unit = {},
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
      .focusable(),
  ) {
    // subtitle
    TrackList(
      title = "Subtitle",
      tracks = subtitles,
      modifier = Modifier.weight(1f),
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
