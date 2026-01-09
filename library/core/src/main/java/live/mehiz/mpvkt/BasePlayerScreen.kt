package live.mehiz.mpvkt

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.theme.MpvKtTheme

@Suppress("ViewModelForwarding")
@Composable
fun BasePlayerScreen(
  playerHelper: PlayerScreenObserver,
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
  playerControlContent: @Composable () -> Unit = {},
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  lifecycleOwner.lifecycle.addObserver(
    object : LifecycleEventObserver {
      override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
      ) {
        when (event) {
          Lifecycle.Event.ON_CREATE -> {
            playerHelper.onPlayerScreenCreated()
          }

          Lifecycle.Event.ON_PAUSE -> {
            playerHelper.onPlayerScreenPaused()
          }

          Lifecycle.Event.ON_RESUME -> {
            playerHelper.onPlayerScreenResumed()
          }

          Lifecycle.Event.ON_DESTROY -> {
            playerHelper.onPlayerScreenDestroy()
            viewModel.sheetShown.update { Sheets.None }
            viewModel.hideControls()
          }

          else -> {}
        }
      }
    },
  )

  MpvKtTheme {
    Box(
      modifier = modifier.fillMaxSize()
    ) {
      AndroidView(
        factory = {
          // SurfaceView likely needs to be attached to the window before the visibility
          // toggle can trigger the necessary surface recreation,
          // using `post` to resolve it.
          viewModel.player.apply {
            post {
              visibility = View.GONE
              visibility = View.VISIBLE
            }
          }
        }
      )
      playerControlContent()
    }
  }
}
