package live.mehiz.mpvkt

interface BasePlayerHelper {
  fun onCreated()
  fun onPaused()
  fun onResumed()
  fun onStopped()
  fun onDestroy()
}
