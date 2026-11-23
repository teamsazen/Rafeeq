# Reference data from the Flutter app's packages

The native app ports two Dart packages the Flutter app uses. These scripts
print what the Dart packages give, and the unit tests check the Kotlin ports
against it.

    cd native/tool/reference
    dart pub get
    TZ=America/New_York dart run bin/main.dart > ../../app/src/test/resources/adhan_reference.json
    dart run bin/hijri.dart > ../../app/src/test/resources/hijri_reference.json

- `bin/main.dart`: prayer times from `adhan` for 3,744 combinations of
  place, date, method, madhab and high latitude rule (`AdhanTest`). The
  time zone matters: Dart's day of the year uses local time, so the test
  runs the port in America/New_York too.
- `bin/hijri.dart`: Hijri dates from `hijri` for every day of 2020 to 2035
  and the start of every month 1440 to 1460 AH (`HijriTest`).
