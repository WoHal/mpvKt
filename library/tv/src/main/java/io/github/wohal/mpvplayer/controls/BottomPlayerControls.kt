package io.github.wohal.mpvplayer.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wohal.mpvplayer.controls.components.ControlsButton

@Composable
fun BottomPlayerControls(
  modifier: Modifier = Modifier,
  onSelectSubtitle: () -> Unit = {},
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.End,
  ) {
    // subtitle
    ControlsButton(
      Icons.Default.Subtitles,
      onClick = onSelectSubtitle,
    )
//
//    // audio
//    ControlsButton(
//      Icons.Default.Audiotrack,
//      onClick = onAudioClick,
//      onLongClick = onAudioLongClick,
//    )
  }
}
