package live.mehiz.mpvkt.di

import io.github.wohal.quando.preference.AndroidPreferenceStore
import io.github.wohal.quando.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AppearancePreferences
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val PreferencesModule = module {
  single { AndroidPreferenceStore(androidContext()) }.bind(PreferenceStore::class)

  singleOf(::AppearancePreferences)
  singleOf(::PlayerPreferences)
  singleOf(::GesturePreferences)
  singleOf(::DecoderPreferences)
  singleOf(::SubtitlesPreferences)
  singleOf(::AudioPreferences)
  singleOf(::AdvancedPreferences)
}
