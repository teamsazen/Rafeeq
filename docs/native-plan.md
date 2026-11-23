# Rafeeq native: plan

Rafeeq moves from Flutter to a native Android app in Kotlin and Jetpack
Compose, the way 99 Names did (`../99-Names/native`). The design stays the
same screen for screen. The motion comes from 99 Names (theme reveal, hero,
favourite pop, springs) and is used in more places, because Rafeeq has more
screens.

The Flutter app stays in the repo and keeps shipping until the native app
matches it. The native app lives in `native/`, next to it.

## Why

- Faster start and scrolling, a smaller download, no shader jank.
- Android only, like 99 Names. The iOS app is not part of this.
- What we give up: Shorebird code push. Every fix goes out as a Play update.

## Ground rules

- Same stack as 99 Names: Kotlin 2.4.21, AGP 9.5.x, Gradle 9.9.2, Compose
  without Material, our own controls. No navigation library: a small back
  stack like 99 Names' `Navigator`.
- Same Play app: `com.satechdev.rafeeqapp`, signed with the same upload key,
  `versionCode` above the last Flutter release (52). Updating from the
  Flutter app must keep everything the user had.
- Debug builds use `com.satechdev.rafeeqapp.debug`, as the Flutter app does,
  so a native debug build installs over a Flutter debug build and must read
  its data. That is how migration gets tested.
- Colours only from `Palette` (light and dark), ported from `SettingsPalette`
  and the bottom bar. Every screen must look right in both.
- Fonts are the Flutter app's: GoogleSans, Jameel Noori Nastaleeq (Urdu),
  PDMS Saleem Quran, AlQuran Neo (16 line, not committed) and Surah Names.
- Icons are the same glyphs as in Flutter: Material Icons, Cupertino Icons and
  Flutter Islamic Icons are drawn from their own fonts by code point. All
  three licences allow it (Apache 2.0, MIT, BSD 2-Clause); credit them in
  Licences.
- Strings come from `lib/l10n/app_en.arb` and `app_ur.arb` into
  `values/strings.xml` and `values-ur/strings.xml`. Urdu is right to left
  in Jameel Noori Nastaleeq.
- Arabic text styles never get letter spacing (it breaks ligatures such as لا).

## Data the native app must keep

| What | Where Flutter keeps it | Native |
|---|---|---|
| Settings: theme, language, Hijri correction | `FlutterSharedPreferences`, key `flutter.app_settings` (JSON) | Read and write the same key and JSON |
| Prayer settings | `flutter.prayer_times_settings` (JSON) | Same |
| Quran view settings | `flutter.quran_view_settings` (JSON) | Same |
| Location | user location key (JSON) | Same |
| Bookmarks | `flutter.ayah_bookmarks` | Same |
| Reciter | `flutter.KEY_SELECTED_RECITER`, `flutter.KEY_SELECTED_SURAH_RECITER` | Same |
| Small flags | `about_opened_v1`, confetti dates, auto scroll | Same |
| Quran text cache | sqflite database, version 2 | Open the same file with `SQLiteOpenHelper`, version 2 |
| Scheduled azan alarms | flutter_local_notifications | Cancel the plugin's alarms once, then schedule our own |

Stored values use the shared_preferences encoding (lists and doubles have a
prefix). `Settings.kt` in 99 Names already reads and writes it; reuse that.

## Motion

From 99 Names, unchanged: `AppMotion` (easeOut, drawer, FAST 200, PAGE 380),
the page slide, `ThemeReveal`, `Modifier.hero`, `animateFloatAsState` for
favourites, springs for toggles and tab indicators, and reduced motion
(fade only when the system asks for less animation).

| Where | What |
|---|---|
| Light, dark or system theme | Circle of the new theme grows from the tap (`ThemeReveal`) |
| Every push and back | Page slide, predictive back on Android 14+ |
| Bottom bar | Indicator slides with a spring; icon swaps to its filled form |
| Settings toggles | Spring thumb, colour fade |
| Picker rows (theme, language, methods) | Selected card colour fades, check pops in |
| Home prayer card to prayer times | Hero |
| Next prayer countdown, Hijri date | `AnimatedContent` roll |
| Surah list to reader, juz list to 16 line page | Hero on the surah name |
| Ayah bookmark, dua favourite | Favourite pop |
| Asma-ul-Husna card to detail | Hero on the Arabic name, as in 99 Names |
| Namaz guide step to detail | Hero on the step card |
| Ayah to share card | Hero on the ayah text |
| Qibla needle | Spring that follows the compass |
| Quran mini player | Slides up from the bottom bar |
| Toasts | 99 Names' `ToastHost` |

## Flutter package to native

| Flutter | Native |
|---|---|
| Riverpod, hooks | Compose state, `ViewModel` and `StateFlow` |
| shared_preferences, rx_shared_preferences | `SharedPreferences` in the Flutter format |
| sqflite | `SQLiteOpenHelper` on the same file |
| just_audio, just_audio_background | Media3 ExoPlayer and `MediaSessionService` |
| flutter_local_notifications, azan | `AlarmManager` exact alarms, notification channel with the azan sound |
| adhan | adhan-kotlin (same library, same results) |
| geolocator, geocoding | Fused location provider, `android.location.Geocoder` |
| flutter_qiblah_advanced | `SensorManager` rotation vector |
| hijri | Port the Dart algorithm and test it against the Flutter output |
| dio, http, cached_network_image, flutter_cache_manager | `HttpURLConnection` and a file cache (ask before adding OkHttp or Coil) |
| share_plus, url_launcher, app_settings | Android intents |
| in_app_update | `app-update-ktx`, as in 99 Names |
| firebase_analytics, firebase_crashlytics | Firebase BoM, release builds only |
| confetti, shimmer, flutter_spinkit, flutter_animate | Compose animation |
| flutter_svg | Vector drawables |

Ask before adding any library not listed here.

## Phases

Each phase ends with a debug build on the Samsung, installed over the
previous build, with nothing lost.

### Phase 1: foundation (this phase)

- `native/` Gradle project, set up like 99 Names.
- `Palette`, fonts, icon fonts, `AppMotion`, text styles.
- `Settings` that reads and writes `flutter.app_settings`.
- `AppRoot`: back stack, page slide, hero and the theme reveal.
- App shell with the four tabs and the bottom bar as in Flutter. Home, Quran
  and Qibla show a placeholder until their phase.
- Settings screen laid out in full. Theme and Language work; the other rows
  open a placeholder.
- English and Urdu strings for these screens, right to left in Urdu.
- Check: installs over the Flutter debug build and keeps its theme and
  language.

### Phase 2: home, prayer times and azan

Home screen and its scene artwork, location
(GPS and manual), calculation, juristic and high latitude settings, Hijri
correction, time format, prayer times screen, notification settings and
sound, exact azan alarms that survive reboot, and onboarding.

### Phase 3: Quran

Quran home (surah, juz and bookmarks tabs), reader with translation and
transliteration, view settings, 16 line Quran, search, audio with the mini
player and background playback, reciter choice, juz downloads and the text
cache, and the ayah focus and text share.

### Phase 4: everything else

Qibla, Hijri calendar, Ramadan calendar, namaz guide and wudu, duas and
their categories, Asma-ul-Husna, ayah picture share card, about, legal,
licences, version and the developer panel.

### Phase 5: release

Firebase, in-app update, Paparazzi screenshots against the Flutter ones,
store listing, an internal testing release, then production. After that the
Flutter app comes out of the repo, as in 99 Names.

## Status

| Phase | State |
|---|---|
| 1. Foundation | Done (2026-11-23), see notes |
| 2. Home, prayer times, azan | Done (2026-11-23), see notes |
| 3. Quran | Done (2026-11-23), see notes |
| 4. Everything else | Done (2026-11-23), see notes |
| 5. Release | Not started |

### Phase 1 notes

Checked on a Samsung S21 (Android 15), installed over the Flutter debug
build: theme, language and location carried over; theme and language
changes save in the Flutter format with every other field kept; the theme
reveal, page slide, sliding bottom bar indicator and picker check animate;
Urdu lays out right to left in Jameel Noori Nastaleeq.

Build and install: `cd native && ./gradlew :app:assembleDebug`, then
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Left for later phases:

- Predictive back (Android 14+) on page changes.
- The bottom bar has no backdrop blur; it uses Flutter's translucent
  gradient only. Compose has no backdrop blur before Android 12.
- Material Icons ships as the full font (1.6 MB); subset the icon fonts to
  the glyphs used before release (phase 5).
- Credit the icon fonts in Licences (phase 4).
- `strings.xml` is generated: after changing `lib/l10n/*.arb`, run
  `python3 scripts/arb_to_android_strings.py`. Native-only strings live in
  `native_strings.xml`.

### Phase 4 notes

- Qibla: a new dial over Flutter's (the rose turns under a fixed mark, an arc
  shows what is left to turn), filtered heading with the declination
  correction, a detent tick every 3 degrees and a firm click on the Qibla
  (owner asked for these, 2026-11-23). `QiblaTest` checks the bearing.
- The Hijri correction also moves the calendar's Gregorian dates.
- Data from the Flutter sources (99 names, namaz, wudu, licences) is
  generated by `python3 scripts/flutter_data_to_native.py`.
- Licences lists what the native app ships (AndroidX, Media3, Kotlin, the
  icon fonts, the adhan and hijri ports, the 16 line Quran credits) rather
  than Flutter's Dart packages.
- Left out: the Ramadan calendar (only the dropped event cards opened it),
  the namaz prayer detail screen (nothing opened it), and the developer
  panel's event previews; its update dialog test comes with in-app update.
- Before release: the Version screen still shows the 1.2.21 notes; write
  the 2.0.0 notes (phase 5).

### Phase 3 notes

- Recitation runs in a Media3 `MediaSessionService` (`data/audio`), so it
  keeps playing with the screen off and shows on the lock screen. Reciters
  and `KEY_SELECTED_RECITER` match Flutter, as does
  `global_last_played_index`.
- The reader keeps Flutter's `isQuranAutoScrollEnabled` (on unless set off).
- Left out because nothing in the Flutter app opens them: the Listen Quran
  home, the unified/transliteration search and `ayah_text_share.dart`. The
  ayah share button opens the Ayah Picture maker, which comes in phase 4.
- The 16 line index search folds spellings as Flutter does (`SearchKeyTest`).

### Phase 2 notes (so far)

- Prayer times and Hijri dates are ports of the Dart `adhan` and `hijri`
  packages, checked against them by `AdhanTest` and `HijriTest` (data from
  `native/tool/reference`).
- Reminders (`data/prayer/Reminders.kt`) keep Flutter's IDs, ten days and
  silent/default channels. The azan has new channels (`prayer_azan_alarm`,
  `prayer_azan_notification`) that name `raw/azan1` instead of a resource
  number; the Flutter azan channels are deleted, so anyone who changed those
  channel settings in Android will need to again.
- New over Flutter: a daily refresh alarm, rescheduling after reboot, update
  and time or time zone changes, and the Flutter plugin's alarms are cleared
  once.
- Debug builds can fire a test reminder:
  `adb shell am broadcast -n com.satechdev.rafeeqapp.debug/com.satechdev.rafeeqapp.DebugReminderReceiver -a test --ei seconds 60`.
- The Friday, Ramadan and Eid event cards and dialogs are left out on
  purpose (owner, 2026-11-23): they will come back later in a better form.
  Their `eventPreviews` and seen markers in the settings are kept untouched.
- Onboarding uses the Flutter flag `onboarding_completed_v2`, so people who
  finished it in Flutter go straight in. Steps slide forward and back
  instead of Flutter's fade. Back on the home tab asks before leaving.
- To check: the R-Debug icon showed as the default robot on one Samsung
  settings screen (likely its icon cache from the Flutter debug build);
  look at the launcher icon before release.
