package live.mehiz.mpvkt.player.controls.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.ListItem
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import kotlinx.collections.immutable.ImmutableList
import live.mehiz.mpvkt.player.TrackNode
import live.mehiz.mpvkt.player.controls.components.sheets.getTrackTitle
import live.mehiz.mpvkt.ui.theme.spacing

@Composable
fun TrackList(
  title: String,
  tracks: ImmutableList<TrackNode>,
  selector: (TrackNode) -> Boolean,
  modifier: Modifier = Modifier,
  onSelect: (Int) -> Unit = {},
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(MaterialTheme.spacing.medium)
    )
    var selectedId by remember { mutableIntStateOf(-1) }
    tracks.forEach {
      if (selector(it)) {
        selectedId = it.id
        return@forEach
      }
    }
    LazyColumn(
      modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
    ) {
      item {
        TrackItem(
          text = "None",
          selected = selectedId == -1,
          onClick = {
            selectedId = -1
            onSelect(-1)
          },
        )
      }
      items(tracks) {
        TrackItem(
          text = getTrackTitle(it),
          selected = selectedId == it.id,
          onClick = {
            selectedId = it.id
            onSelect(it.id)
          },
        )
      }
    }
  }
}

@Composable
fun TrackItem(
  text: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
  focusRequester: FocusRequester = remember { FocusRequester() },
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val isFocus by interactionSource.collectIsFocusedAsState()
  ListItem(
    modifier = modifier
      .focusRequester(focusRequester)
      .focusable(interactionSource = interactionSource),
    selected = isFocus,
    leadingContent = {
      RadioButton(
        selected = selected,
        onClick = null
      )
    },
    headlineContent = {
      Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    onClick = onClick,
  )
}
