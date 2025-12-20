package wang.soian.automaterial3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor

@Composable
fun AutoIcon(
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  tint: Color = LocalContentColor.current
) {
  Icon(
    imageVector,
    contentDescription,
    modifier,
    tint,
  )
}
