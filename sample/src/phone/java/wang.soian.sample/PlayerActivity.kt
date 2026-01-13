package wang.soian.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import io.github.wohal.mpvplayer.PlayerActivity
import live.mehiz.mpvkt.BasePlayerEvent
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.model.MPVPlayerItem
import timber.log.Timber

class VideoPlayerActivity : PlayerActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    playerViewModel.play(
      listOf(
        MPVPlayerItem(
          mediaId = "123",
          mediaTitle = "Video 123",
          uri = "https://www.w3schools.com/tags/movie.mp4"
        ),
        MPVPlayerItem(
          mediaId = "456",
          mediaTitle = "Video 456",
          uri = "https://api.dogecloud.com/player/get.mp4?vcode=5ac682e6f8231991&userId=17&ext=.mp4"
        )
      )
    )

    setContent {
      PlayerScreen(
        onBackPress = ::finish
      )
    }
  }
}
