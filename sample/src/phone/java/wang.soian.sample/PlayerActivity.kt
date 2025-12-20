package wang.soian.sample

import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.ui.player.MPVView
import timber.log.Timber

class VideoPlayerActivity : PlayerActivity() {
  override lateinit var currentPlayerItem: MPVPlayerItem
  override fun initCurrentPlayerItem() {
    currentPlayerItem = MPVPlayerItem(
      mediaId = "123",
      mediaTitle = "Example Video",
      uri = "https://api.dogecloud.com/player/get.mp4?vcode=5ac682e6f8231991&userId=17&ext=.mp4"
    )
  }

  override fun onPlayEnd() {
    Timber.v("play end")
  }
}
