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
