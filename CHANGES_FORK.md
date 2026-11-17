# Fork changes (MojavLauncher)

Based on MojoLauncher (branch `v3_openjdk`, LGPL-3.0). The code was written without access to an Android SDK, so it is not compile-tested by the author. Build with GitHub Actions or a local Gradle build and report any errors.

## 1. Interface and branding
- New palette (graphite + one accent color), rounded card buttons with ripple in portrait and landscape layouts, rounded "Play"/MineButtons.
- Product name is **MojavLauncher** (all languages, window title, crash report). App icon, round icon and default profile icon come from `Mojav.png`. The application id `git.artdeell.mjlaunch` is unchanged on purpose.
- Main screen: **Transfer**, **Help**, **Presets** (new), Control layout, Open game folder. "Run .jar" and "Share log" moved to Settings > Help and tools.

## 2. Presets, device database, "Restore optimal settings"
- **Presets** (main screen button and Settings > Graphics): Auto, Max FPS, Balanced, Quality, Battery saver, Compatibility. Each preset sets resolution scale, sustained performance, vsync, big-core affinity and alternate surface using the tier of your chip (`utils/DeviceProfile.java`).
- **Device database** (`utils/DeviceDatabase.java`): about 60 chips (Snapdragon, Dimensity/Helio, Exynos, Tensor, Unisoc) with GPU and tier, and about 120 phones and tablets. The chip is detected from `Build.SOC_MODEL/HARDWARE/BOARD`, falling back to the GPU name. **Settings > Graphics > Select your device** opens a searchable list; picking a device stores it and applies balanced settings for its chip. The data is written from general knowledge and may contain mistakes: pick the closest device or one of the generic classes.
- **Restore optimal settings** resets graphics/performance options to the launcher's per-device defaults.
- Renderers: no new rendering engine was added. A renderer is native code (GL4ES, LTW, Zink, Freedreno, Mesa) built outside this repository and cannot be created in this environment. Choosing the renderer by device is left to the user (GL4ES for older versions, LTW for 1.17+), because the best choice also depends on the game version.

## 3. First launch
- **Quick setup** dialog (once): language, accent color, icon style and performance preset, plus "My device..." to pick the exact phone or tablet. Then the one-time offer of the tour.

## 4. Customization (Settings > Customization)
- Language (10 + system), accent color (7), icon style (3). Applied immediately.

## 5. Transfer from other launchers
- Main screen > Transfer, or Settings > Transfer data. Pick the game folder of Pojav or Mojo (or any launcher) and choose exactly what to copy: worlds (off by default), mods, resource packs, shader packs, screenshots, game options, saved servers, mod configs, maps and schematics, logs and crash reports, installed versions. "Select all" and "Clear selection" are provided. Existing files are never overwritten, free space is checked, progress is shown. The old full copy between two Mojo installs is kept on the same screen.

## 6. Help
- Guide dialog (Settings > Help and tools) with 9 steps, translated into all languages; wiki, community link, share log and .jar installer live there too.

## 7. Languages
- English (US), Español, Русский, Українська, Қазақша, Azərbaycanca, 中文（简体）, Tiếng Việt, 한국어, Français. All app strings were translated: every language now covers all user-facing strings except a few that are format-only or letters (A/B/X/Y buttons, `%d%%`, a joke string), and "Discord" in some languages. Kazakh and parts of the other languages are machine translations and should be reviewed by native speakers.

## 8. Settings cleanup (hidden, not deleted)
- Hidden with `app:isPreferenceVisible="false"`: Force English (replaced by Language), Run data migration (moved), Fast startup check, Verify manifest, Memory warning, Disable server blocklist, Dump shaders, Button all caps, Virtual mouse start, Keyboard auto-panning. Remove that attribute in `res/xml/pref_*.xml` to bring one back.

## 9. Build and signing
- Upstream keystores removed from the full archive; debug builds use the standard debug key. Builds without `.git` fall back to a `LOCAL-<date>` version name.
- Build: `./gradlew :app_mojo:assembleFullDebug` (JDK 17+), or run the `Android CI` workflow on GitHub (artifact `app-debug-full`).
- Upload note: GitHub's web upload accepts at most 100 files at a time, so the change archive is split into several folders.

## 10. Changed and added files (128 files; only the upstream keystores were removed)
```
CHANGES_FORK.md
app_mojo/build.gradle
app_mojo/src/main/java/com/kdt/mcgui/LauncherMenuButton.java
app_mojo/src/main/java/com/kdt/mcgui/MineButton.java
app_mojo/src/main/java/git/artdeell/mojo/BaseActivity.java
app_mojo/src/main/java/git/artdeell/mojo/Tools.java
app_mojo/src/main/java/git/artdeell/mojo/fragments/MainMenuFragment.java
app_mojo/src/main/java/git/artdeell/mojo/game/GameActivity.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/LauncherPreferences.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/screens/LauncherPreferenceCustomizationFragment.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/screens/LauncherPreferenceExperimentalFragment.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/screens/LauncherPreferenceFragment.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/screens/LauncherPreferenceHelpFragment.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/screens/LauncherPreferenceMigrationFragment.java
app_mojo/src/main/java/git/artdeell/mojo/prefs/screens/LauncherPreferenceVideoFragment.java
app_mojo/src/main/java/git/artdeell/mojo/tasks/FolderImporter.java
app_mojo/src/main/java/git/artdeell/mojo/utils/AccentColor.java
app_mojo/src/main/java/git/artdeell/mojo/utils/DeviceDatabase.java
app_mojo/src/main/java/git/artdeell/mojo/utils/DevicePicker.java
app_mojo/src/main/java/git/artdeell/mojo/utils/DeviceProfile.java
app_mojo/src/main/java/git/artdeell/mojo/utils/FirstRunSetup.java
app_mojo/src/main/java/git/artdeell/mojo/utils/HelpGuide.java
app_mojo/src/main/java/git/artdeell/mojo/utils/LocaleUtils.java
app_mojo/src/main/java/git/artdeell/mojo/utils/PresetPicker.java
app_mojo/src/main/res/drawable/button_flat.xml
app_mojo/src/main/res/drawable/ic_mojo_full.webp
app_mojo/src/main/res/drawable/menu_card_background.xml
app_mojo/src/main/res/drawable/mine_button_background.xml
app_mojo/src/main/res/layout-land/fragment_launcher.xml
app_mojo/src/main/res/layout/fragment_launcher.xml
app_mojo/src/main/res/mipmap-hdpi/ic_launcher.webp
app_mojo/src/main/res/mipmap-hdpi/ic_launcher_foreground.webp
app_mojo/src/main/res/mipmap-hdpi/ic_launcher_round.webp
app_mojo/src/main/res/mipmap-mdpi/ic_launcher.webp
app_mojo/src/main/res/mipmap-mdpi/ic_launcher_foreground.webp
app_mojo/src/main/res/mipmap-mdpi/ic_launcher_round.webp
app_mojo/src/main/res/mipmap-xhdpi/ic_launcher.webp
app_mojo/src/main/res/mipmap-xhdpi/ic_launcher_foreground.webp
app_mojo/src/main/res/mipmap-xhdpi/ic_launcher_round.webp
app_mojo/src/main/res/mipmap-xxhdpi/ic_launcher.webp
app_mojo/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.webp
app_mojo/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp
app_mojo/src/main/res/mipmap-xxxhdpi/ic_launcher.webp
app_mojo/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp
app_mojo/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp
app_mojo/src/main/res/values-af/strings.xml
app_mojo/src/main/res/values-ar/strings.xml
app_mojo/src/main/res/values-az-rAZ/arrays_customization.xml
app_mojo/src/main/res/values-az-rAZ/arrays_presets.xml
app_mojo/src/main/res/values-az-rAZ/strings.xml
app_mojo/src/main/res/values-ba/strings.xml
app_mojo/src/main/res/values-bn-rBD/strings.xml
app_mojo/src/main/res/values-ca/strings.xml
app_mojo/src/main/res/values-cs/strings.xml
app_mojo/src/main/res/values-da/strings.xml
app_mojo/src/main/res/values-de/strings.xml
app_mojo/src/main/res/values-el/strings.xml
app_mojo/src/main/res/values-en-rGB/strings.xml
app_mojo/src/main/res/values-es/arrays_customization.xml
app_mojo/src/main/res/values-es/arrays_presets.xml
app_mojo/src/main/res/values-es/strings.xml
app_mojo/src/main/res/values-et-rEE/strings.xml
app_mojo/src/main/res/values-fa-rIR/strings.xml
app_mojo/src/main/res/values-fi/strings.xml
app_mojo/src/main/res/values-fil/strings.xml
app_mojo/src/main/res/values-fr/arrays_customization.xml
app_mojo/src/main/res/values-fr/arrays_presets.xml
app_mojo/src/main/res/values-fr/strings.xml
app_mojo/src/main/res/values-hi/strings.xml
app_mojo/src/main/res/values-hu/strings.xml
app_mojo/src/main/res/values-in/strings.xml
app_mojo/src/main/res/values-it/strings.xml
app_mojo/src/main/res/values-iw/strings.xml
app_mojo/src/main/res/values-ja/strings.xml
app_mojo/src/main/res/values-kk/arrays_customization.xml
app_mojo/src/main/res/values-kk/arrays_presets.xml
app_mojo/src/main/res/values-kk/strings.xml
app_mojo/src/main/res/values-ko/arrays_customization.xml
app_mojo/src/main/res/values-ko/arrays_presets.xml
app_mojo/src/main/res/values-ko/strings.xml
app_mojo/src/main/res/values-la/strings.xml
app_mojo/src/main/res/values-lol-rAA/strings.xml
app_mojo/src/main/res/values-lt/strings.xml
app_mojo/src/main/res/values-mn-rMN/strings.xml
app_mojo/src/main/res/values-ms/strings.xml
app_mojo/src/main/res/values-nl/strings.xml
app_mojo/src/main/res/values-no/strings.xml
app_mojo/src/main/res/values-pl/strings.xml
app_mojo/src/main/res/values-pt-rBR/strings.xml
app_mojo/src/main/res/values-pt/strings.xml
app_mojo/src/main/res/values-ro/strings.xml
app_mojo/src/main/res/values-rpr/strings.xml
app_mojo/src/main/res/values-ru/arrays_customization.xml
app_mojo/src/main/res/values-ru/arrays_presets.xml
app_mojo/src/main/res/values-ru/strings.xml
app_mojo/src/main/res/values-sk-rSK/strings.xml
app_mojo/src/main/res/values-sr/strings.xml
app_mojo/src/main/res/values-sv/strings.xml
app_mojo/src/main/res/values-th/strings.xml
app_mojo/src/main/res/values-tr/strings.xml
app_mojo/src/main/res/values-tt/strings.xml
app_mojo/src/main/res/values-uk/arrays_customization.xml
app_mojo/src/main/res/values-uk/arrays_presets.xml
app_mojo/src/main/res/values-uk/strings.xml
app_mojo/src/main/res/values-vi/arrays_customization.xml
app_mojo/src/main/res/values-vi/arrays_presets.xml
app_mojo/src/main/res/values-vi/strings.xml
app_mojo/src/main/res/values-zh-rCN/arrays_customization.xml
app_mojo/src/main/res/values-zh-rCN/arrays_presets.xml
app_mojo/src/main/res/values-zh-rCN/strings.xml
app_mojo/src/main/res/values-zh-rTW/strings.xml
app_mojo/src/main/res/values/arrays_customization.xml
app_mojo/src/main/res/values/arrays_customization_values.xml
app_mojo/src/main/res/values/arrays_language.xml
app_mojo/src/main/res/values/arrays_presets.xml
app_mojo/src/main/res/values/arrays_presets_values.xml
app_mojo/src/main/res/values/colors.xml
app_mojo/src/main/res/values/ic_launcher_background.xml
app_mojo/src/main/res/values/strings.xml
app_mojo/src/main/res/values/styles.xml
app_mojo/src/main/res/values/styles_accent.xml
app_mojo/src/main/res/xml/pref_control.xml
app_mojo/src/main/res/xml/pref_customization.xml
app_mojo/src/main/res/xml/pref_experimental.xml
app_mojo/src/main/res/xml/pref_help.xml
app_mojo/src/main/res/xml/pref_main.xml
app_mojo/src/main/res/xml/pref_migration.xml
app_mojo/src/main/res/xml/pref_misc.xml
app_mojo/src/main/res/xml/pref_video.xml
```
