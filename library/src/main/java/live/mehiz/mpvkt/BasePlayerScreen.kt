package live.mehiz.mpvkt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.Visibility
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.PlayerControls
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import timber.log.Timber
import androidx.core.view.isNotEmpty
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.ui.player.Sheets

@Composable
fun BasePlayerScreen(
  binding: PlayerLayoutBinding,
  playerHelper: BasePlayerHelper,
  onBackPress: () -> Unit,
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
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
            playerHelper.onCreated()
          }

          Lifecycle.Event.ON_PAUSE -> {
            playerHelper.onPaused()
          }

          Lifecycle.Event.ON_RESUME -> {
            playerHelper.onResumed()
          }

          Lifecycle.Event.ON_DESTROY -> {
            playerHelper.onDestroy()
            (binding.root.parent as ViewGroup).removeView(binding.root)
            viewModel.sheetShown.update { Sheets.None }
            viewModel.hideControls()
          }

          else -> {}
        }
      }
    },
  )

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
      FrameLayout(context).apply {
        addView(binding.root)

        // SurfaceView likely needs to be attached to the window before the visibility
        // toggle can trigger the necessary surface recreation,
        // using `post` to resolve it.
        binding.player.post {
          binding.player.visibility = View.GONE
          binding.player.visibility = View.VISIBLE
        }
        binding.controls.apply {
          setContent {
            MpvKtTheme {
              PlayerControls(
                viewModel = viewModel,
                onBackPress = onBackPress,
                modifier = modifier,
              )
            }
          }
        }
      }
    },

  )
}
