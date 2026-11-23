import 'dart:convert';
import 'package:hijri/hijri_calendar.dart';

/// Hijri dates from the Dart hijri package for every day of 2020 to 2035,
/// and the Gregorian start of every month 1440 to 1460 AH.
void main() {
  final days = [];
  for (var d = DateTime.utc(2020, 1, 1); d.year < 2036; d = d.add(const Duration(days: 1))) {
    final h = HijriCalendar.fromDate(d);
    days.add([d.year, d.month, d.day, h.hYear, h.hMonth, h.hDay, h.lengthOfMonth, h.wkDay]);
  }
  final starts = [];
  for (var y = 1440; y <= 1460; y++) {
    for (var m = 1; m <= 12; m++) {
      final g = HijriCalendar().hijriToGregorian(y, m, 1);
      starts.add([y, m, g.year, g.month, g.day, HijriCalendar().getDaysInMonth(y, m)]);
    }
  }
  print(jsonEncode({'days': days, 'starts': starts}));
}
