package wang.soian.sample

import android.os.Bundle
import io.github.wohal.mpvplayer.PlayerActivity
import live.mehiz.mpvkt.BasePlayerEvent
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.model.MPVPlayerItem
import timber.log.Timber

class VideoPlayerActivity : PlayerActivity(), BasePlayerEvent {
  override lateinit var currentPlayerItem: MPVPlayerItem
  fun initCurrentPlayerItem() {
    currentPlayerItem = MPVPlayerItem(
      mediaId = "ee853737-ad2b-4975-a71c-900bcb43cdfd",
      mediaTitle = "Example Video",
      uri = "https://api.dogecloud.com/player/get.mp4?vcode=5ac682e6f8231991&userId=17&ext=.mp4"
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initCurrentPlayerItem()

    play(currentPlayerItem)
  }

  override fun onPlayEnd() {
    super.onPlayEnd()
    Timber.v("play end")
  }

  override fun savePlaybackState(state: PlaybackStateEntity) {
    Timber.d("state: $state")
  }

  override suspend fun loadVideoPlaybackStateById(mediaId: String): PlaybackStateEntity? {
    return null
  }
}
