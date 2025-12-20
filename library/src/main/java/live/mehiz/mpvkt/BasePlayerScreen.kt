package live.mehiz.mpvkt

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import live.mehiz.mpvkt.databinding.PlayerLayoutBinding
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.PlayerControls
import live.mehiz.mpvkt.ui.theme.MpvKtTheme
import timber.log.Timber

@Composable
fun BasePlayerScreen(
  binding: PlayerLayoutBinding,
  playerHelper: BasePlayerHelper,
  onBackPress: () -> Unit,
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
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
        }
        else -> {}
      }
    }
  })

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = {
      FrameLayout(it).apply {
          addView(binding.root)
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
          binding.player
        }

    }
  )
}
