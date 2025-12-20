package live.mehiz.mpvkt

import live.mehiz.mpvkt.ui.player.MPVView

interface BasePlayerHelper {
  fun onCreated()
  fun onPaused()
  fun onResumed()
  fun onStopped()
  fun onDestroy()
}
