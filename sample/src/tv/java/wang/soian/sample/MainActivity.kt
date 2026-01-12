package wang.soian.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import live.mehiz.mpvkt.model.MPVPlayerItem
import live.mehiz.mpvkt.ui.theme.MpvKtTheme

class MainActivity : VideoPlayerActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MpvKtTheme {
        Surface {
          Scaffold { paddingValues ->
            val mainNavController = rememberNavController()
            NavHost(navController = mainNavController, startDestination = "home") {
              composable("home") {
                Column(
                  modifier = Modifier.fillMaxSize().padding(paddingValues),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Button(
                    onClick = {
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
                      mainNavController.navigate("video")
                    }
                  ) {
                    Text(
                      text = "Play"
                    )
                  }
                }
              }
              composable("video") {
                PlayerScreen(
                  onBackPress = {
                    mainNavController.navigateUp()
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}
