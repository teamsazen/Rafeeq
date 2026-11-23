# Adhan reference times

Prints prayer times from the Dart `adhan` package (the one the Flutter app
uses) for 3,744 combinations of place, date, method, madhab and high
latitude rule. `AdhanTest` checks the Kotlin port against them.

    cd native/tool/adhan_reference
    dart pub get
    TZ=America/New_York dart run bin/main.dart > ../../app/src/test/resources/adhan_reference.json

The time zone matters: Dart's day of the year uses local time, so the test
runs the port in America/New_York too.
