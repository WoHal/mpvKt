package wang.soian.sample

import android.app.Application
import live.mehiz.mpvkt.di.AppModule
import live.mehiz.mpvkt.di.FileManagerModule
import live.mehiz.mpvkt.di.MpvKtDatabaseModule
import live.mehiz.mpvkt.di.PreferencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration
import timber.log.Timber

@OptIn(KoinExperimentalAPI::class)
class SampleApplication : Application(), KoinStartup {
  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

  }

  override fun onKoinStartup() = koinConfiguration {
    androidContext(this@SampleApplication)
    modules(
      AppModule,
      MpvKtDatabaseModule,
      PreferencesModule,
      FileManagerModule,
    )
  }
}
