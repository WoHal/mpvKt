package io.github.wohal.mpvplayer.controls.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.ui.player.controls.LocalPlayerButtonsClickEvent
import live.mehiz.mpvkt.ui.theme.spacing

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ControlsButton(
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  title: String? = null,
  color: Color = Color.White,
  focusRequester: FocusRequester = remember { FocusRequester() },
  focusInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current

  Box(
    modifier = modifier
      .combinedClickable(
        onClick = {
          clickEvent()
          onClick()
        },
        onLongClick = onLongClick,
        interactionSource = interactionSource,
        indication = null,
      )
      .clip(CircleShape)
      .indication(
        interactionSource,
        ripple()
      )
      .padding(MaterialTheme.spacing.medium)
      .focusRequester(focusRequester)
      .focusable(interactionSource = focusInteractionSource),
  ) {
    Icon(
      icon,
      title,
      tint = color,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  color: Color = Color.White,
  focusRequester: FocusRequester = remember { FocusRequester() },
  focusInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val interactionSource = remember { MutableInteractionSource() }

  val clickEvent = LocalPlayerButtonsClickEvent.current
  val isFocus by focusInteractionSource.collectIsFocusedAsState()

  Box(
    modifier = modifier
      .combinedClickable(
        onClick = {
          clickEvent()
          onClick()
        },
        onLongClick = onLongClick,
        interactionSource = interactionSource,
        indication = null,
      )
      .clip(CircleShape)
      .background(if (isFocus) Color.White.copy(0.35f) else Color.Transparent)
      .indication(
        interactionSource,
        ripple()
      )
      .padding(MaterialTheme.spacing.medium)
      .focusRequester(focusRequester)
      .focusable(interactionSource = focusInteractionSource),
  ) {
    Text(
      text,
      color = color,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Preview
@Composable
private fun PreviewControlsButton() {
  ControlsButton(
    Icons.Default.CatchingPokemon,
    onClick = {},
  )
}
