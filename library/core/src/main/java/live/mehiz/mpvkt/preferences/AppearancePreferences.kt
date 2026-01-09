package live.mehiz.mpvkt.preferences

import android.os.Build
import live.mehiz.mpvkt.ui.theme.DarkMode
import io.github.wohal.quando.preference.PreferenceStore
import io.github.wohal.quando.preference.getEnum

class AppearancePreferences(preferenceStore: PreferenceStore) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val materialYou = preferenceStore.getBoolean("material_you", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
}
