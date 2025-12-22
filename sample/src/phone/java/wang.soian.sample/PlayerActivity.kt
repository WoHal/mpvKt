package wang.soian.sample

import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.BasePlayerEvent
import live.mehiz.mpvkt.model.MPVPlayerItem
import timber.log.Timber

class VideoPlayerActivity : PlayerActivity(), BasePlayerEvent {
  override lateinit var currentPlayerItem: MPVPlayerItem
  override fun initCurrentPlayerItem() {
    currentPlayerItem = MPVPlayerItem(
      mediaId = "123",
      mediaTitle = "Example Video",
      uri = "https://api.dogecloud.com/player/get.mp4?vcode=5ac682e6f8231991&userId=17&ext=.mp4"
    )
  }

  override fun onPlayEnd() {
    super.onPlayEnd()
    Timber.v("play end")
  }
}
