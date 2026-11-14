# Fork changes

Based on MojoLauncher (branch `v3_openjdk`, LGPL-3.0). All changes were written without access to an Android SDK, so they are not compile-tested by the author. Build with GitHub Actions or a local Gradle build and report any errors.

## 1. Interface
- New palette: graphite surfaces with one accent color (`values/colors.xml`). Color names are unchanged, so every existing screen picks it up.
- Main menu buttons are rounded cards with spacing and a ripple effect, in portrait and landscape layouts.
- "Play" and all `MineButton`s are rounded and accent-colored with a muted disabled state.
- Main screen reorganized: the former Wiki button is now **Transfer**, the former social-media button is now **Help**. "Run .jar" and "Share log" were moved to Settings → Help and tools (the views still exist but are hidden, so no code path breaks).

## 2. Device profile, performance and "Restore optimal settings"
- `utils/DeviceProfile.java` detects SoC and GPU family (Adreno / Mali / other).
- On the first launch the launcher uses its per-device defaults (resolution scale, sustained performance, alternate surface, vsync, RAM) and, on Mali/Immortalis GPUs with mixed CPU core frequencies, also enables "Force big core" (never overriding a value the user already set). The big-core default is a hypothesis to measure, not a proven speedup.
- **Settings → Graphics → Restore optimal settings** (with a confirmation dialog) forgets the user's graphics and performance choices (renderer, resolution scale, sustained performance, alternate surface, vsync, ANGLE, Zink system driver, big-core affinity, Freedreno sysmem, UBWC workaround, RAM allocation) so the per-device defaults apply again, then re-applies the device profile and refreshes the screen.
- Settings → Experimental shows a "Device" row: SoC, GPU, OpenGL ES version, CPU cores, RAM.

## 3. Customization (Settings → Customization)
- Accent color: green (default), blue, red, purple, orange, pink, teal. Applied through a theme overlay in `BaseActivity` and through `AccentColor.buttonBackground()`.
- Icon style: white (default), accent color, gray. Applied to icons in settings and, via compound-drawable tint, to main menu buttons (the menu buttons depend on the internal ExtendedButton library and may not tint).
- Changing any of these recreates the activity so the new look applies immediately.

## 4. Languages (Settings → Customization → Language)
- System default, English (US), Español (EE. UU.), Русский, Українська, Қазақша, Azərbaycanca, 中文（简体）, Tiếng Việt, 한국어, Français.
- The chosen language overrides the old "Force English" switch (now hidden); with "System default" the old behavior is kept (`utils/LocaleUtils.java`).
- All new strings are translated into every language above. Existing project translations were reused; untranslated strings fall back to English. Kazakh previously had only 75 of 508 strings, so the main screen and settings strings were added; they are machine-translated and should be reviewed by a native speaker.

## 5. Transfer from other launchers (main screen → Transfer, or Settings → Transfer data)
- `tasks/FolderImporter.java` imports data from the game folder of **any** launcher (Pojav, Mojo, others) picked with the system folder picker.
- Choose what to transfer: worlds (saves, off by default), mods, resource packs, shader packs, screenshots, game settings and servers (options.txt, servers.dat, config), installed versions. Worlds are off by default because they can take a lot of space.
- It finds the real game folder inside what you picked (the folder itself, its `.minecraft`, or `files/.minecraft`).
- Files go into the currently selected profile (versions go into the shared versions folder). Existing files are never overwritten, free space is checked first, and progress is shown in the launcher's progress bar.
- For Pojav pick `games/PojavLauncher/.minecraft`; for Mojo pick the profile (instance) folder. The previous full copy between two Mojo installs (all data incl. accounts) is kept and moved to this screen as "Full copy from another Mojo".
- Not transferred: accounts of other launchers, `launcher_profiles.json`, libraries and assets (the launcher downloads them again).

## 6. Help
- On the very first launch a dialog offers a quick tour (shown once, `helpOfferShown` flag). The same tour is available any time in Settings → Help and tools → How to use the launcher (`utils/HelpGuide.java`).
- Settings → Help and tools also holds the guide, wiki, community link, share log and the .jar installer.

## 7. Settings cleanup (hidden, not deleted)
Hidden with `app:isPreferenceVisible="false"`; their defaults stay in effect and the code that reads them is untouched. To bring an item back, remove that attribute in the matching `res/xml/pref_*.xml`.
- Main: Force English (replaced by Language).
- Miscellaneous: Run data migration (moved to Transfer), Fast startup check, Verify manifest, Memory warning, Disable server blocklist.
- Experimental: Dump shaders.
- Controls: Button all caps, Virtual mouse start, Keyboard auto-panning.
- Left visible on purpose: items whose visibility is controlled by code (ANGLE, Vulkan drivers, gyro options, notch).

## 8. Build and signing
- The upstream signing keystores (`mojo_release_next.jks`, `mojo_upload_next.jks`) were removed from the full archive. Debug builds use the standard debug keystore. Create your own keystore for a release.
- Builds from a ZIP have no `.git` folder, so `app_mojo/build.gradle` (`getVersionName`) was patched to fall back to `LOCAL-<date>` without Git info. Builds from a Git clone or GitHub Actions do not need this patch.
- Build command: `./gradlew :app_mojo:assembleFullDebug` (JDK 17+, Android SDK platform 36, build-tools 36.0.0, NDK 29.0.14206865, CMake 3.22.1), or run the `Android CI` workflow on GitHub, which produces `app-debug-full.apk`.
- The archives contain no Minecraft worlds or other world data.

## 9. Branding
- Product name is now **MojavLauncher** (`app_short_name` and every user-facing "MJLauncher" string in all languages, window title, crash report header, file-picker root name).
- New app icon from the supplied `Mojav.png`: adaptive icon (foreground + background color `#0D1117`), legacy square and round icons for all densities (`res/mipmap-*`), saved as WebP under the original file names so they simply replace the old icons, and the default profile icon in the bottom bar (`res/drawable/ic_mojo_full.webp`).
- The application id (`git.artdeell.mjlaunch`) is unchanged on purpose: the Mojo-to-Mojo full copy and the storage provider depend on it. Debug builds carry a `.debug` suffix, so they install next to the original app.
- LGPL-3.0 still applies: keep the license text and the credit to the MojoLauncher and PojavLauncher authors, and publish your changes to the LGPL-covered code if you distribute the app.

## 10. Changed and added files (114 files; only the upstream keystores were removed)
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
app_mojo/src/main/java/git/artdeell/mojo/utils/DeviceProfile.java
app_mojo/src/main/java/git/artdeell/mojo/utils/HelpGuide.java
app_mojo/src/main/java/git/artdeell/mojo/utils/LocaleUtils.java
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
app_mojo/src/main/res/values-es/strings.xml
app_mojo/src/main/res/values-et-rEE/strings.xml
app_mojo/src/main/res/values-fa-rIR/strings.xml
app_mojo/src/main/res/values-fi/strings.xml
app_mojo/src/main/res/values-fil/strings.xml
app_mojo/src/main/res/values-fr/arrays_customization.xml
app_mojo/src/main/res/values-fr/strings.xml
app_mojo/src/main/res/values-hi/strings.xml
app_mojo/src/main/res/values-hu/strings.xml
app_mojo/src/main/res/values-in/strings.xml
app_mojo/src/main/res/values-it/strings.xml
app_mojo/src/main/res/values-iw/strings.xml
app_mojo/src/main/res/values-ja/strings.xml
app_mojo/src/main/res/values-kk/arrays_customization.xml
app_mojo/src/main/res/values-kk/strings.xml
app_mojo/src/main/res/values-ko/arrays_customization.xml
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
app_mojo/src/main/res/values-ru/strings.xml
app_mojo/src/main/res/values-sk-rSK/strings.xml
app_mojo/src/main/res/values-sr/strings.xml
app_mojo/src/main/res/values-sv/strings.xml
app_mojo/src/main/res/values-th/strings.xml
app_mojo/src/main/res/values-tr/strings.xml
app_mojo/src/main/res/values-tt/strings.xml
app_mojo/src/main/res/values-uk/arrays_customization.xml
app_mojo/src/main/res/values-uk/strings.xml
app_mojo/src/main/res/values-vi/arrays_customization.xml
app_mojo/src/main/res/values-vi/strings.xml
app_mojo/src/main/res/values-zh-rCN/arrays_customization.xml
app_mojo/src/main/res/values-zh-rCN/strings.xml
app_mojo/src/main/res/values-zh-rTW/strings.xml
app_mojo/src/main/res/values/arrays_customization.xml
app_mojo/src/main/res/values/arrays_customization_values.xml
app_mojo/src/main/res/values/arrays_language.xml
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
