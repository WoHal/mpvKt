package live.mehiz.mpvkt.model

val UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0"

data class SubtitleItem(
  val url: String,
  val enable: Boolean
)

data class MPVPlayerItem(
  val mediaId: String,
  val mediaTitle: String,
  val uri: String,
  val position: Int = 0,
  val audioFiles: List<String> = emptyList(),
  val subtitles: List<SubtitleItem> = emptyList(),
  val userAgent: String = UA,
  val headers: Map<String, String> = mapOf(),
)
