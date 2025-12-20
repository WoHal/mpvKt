package live.mehiz.mpvkt.model

data class MPVPlaybackState(
  val mediaId: String,
  val mediaTitle: String,
  val lastPosition: Int = 0, // in seconds
  val playbackSpeed: Double = 1.0,
  val sid: Int = 0,
  val subDelay: Int = 0,
  val subSpeed: Double = 1.0,
  val secondarySid: Int,
  val secondarySubDelay: Int = 0,
  val aid: Int = 0,
  val audioDelay: Int = 0,
)
