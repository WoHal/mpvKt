package live.mehiz.mpvkt.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.player.controls.LocalPlayerButtonsClickEvent
import live.mehiz.mpvkt.ui.theme.spacing

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class)
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
  val isFocused by focusInteractionSource.collectIsFocusedAsState()
// 跳动动画
  val jumpAnim = remember { Animatable(1f) }

  // 监听焦点变化
  LaunchedEffect(isFocused) {
    if (isFocused) {
      jumpAnim.animateTo(
        targetValue = 1.3f, // 放大倍数
        animationSpec = infiniteRepeatable(
          animation = keyframes {
            durationMillis = 500 // 持续时间
            1f at 0 using FastOutSlowInEasing // 初始大小
            1.3f at 100 // 放大
            1f at 300 using FastOutSlowInEasing // 恢复大小
          }
        )
      )
    } else {
      jumpAnim.snapTo(1f) // 当失去焦点时恢复到正常大小
    }
  }

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
      .graphicsLayer(scaleX = jumpAnim.value, scaleY = jumpAnim.value)
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
