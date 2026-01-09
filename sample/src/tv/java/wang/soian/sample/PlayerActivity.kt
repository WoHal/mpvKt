package wang.soian.sample

import live.mehiz.mpvkt.model.MPVPlayerItem
import io.github.wohal.mpvplayer.PlayerActivity
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import timber.log.Timber

open class VideoPlayerActivity : PlayerActivity() {
  override lateinit var currentPlayerItem: MPVPlayerItem
  override fun initCurrentPlayerItem() {
    currentPlayerItem = MPVPlayerItem(
      mediaId = "123",
      mediaTitle = "Example Video",
      uri = "https://api.dogecloud.com/player/get.mp4?vcode=5ac682e6f8231991&userId=17&ext=.mp4"
    )
  }

  override fun onPlayerScreenCreated() {
    super.onPlayerScreenCreated()

    initCurrentPlayerItem()

    play(currentPlayerItem)
  }

  override fun onPlayEnd() {
    Timber.v("play end")
  }

  override fun savePlaybackState(state: PlaybackStateEntity) {
    Timber.d("state: $state")
  }

  override suspend fun loadVideoPlaybackStateById(mediaId: String): PlaybackStateEntity? {
    return null
  }
}
