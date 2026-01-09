package live.mehiz.mpvkt.ui.player.controls

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun BottomRightPlayerControls(
  isPipAvailable: Boolean,
  onAspectClick: () -> Unit,
  onPipClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier) {
    if (isPipAvailable) {
      ControlsButton(
        Icons.Default.PictureInPictureAlt,
        onClick = onPipClick,
      )
    }

    ControlsButton(
      Icons.Default.AspectRatio,
      onClick = onAspectClick,
    )
  }
}
