import 'dart:convert';
import 'package:adhan/adhan.dart';

void main() {
  final places = {
    'jhelum': [32.9405, 73.7276], 'karachi': [24.8607, 67.0011], 'makkah': [21.4225, 39.8262],
    'london': [51.5072, -0.1276], 'oslo': [59.9139, 10.7522], 'newyork': [40.7128, -74.0060],
    'sydney': [-33.8688, 151.2093], 'reykjavik': [64.1466, -21.9426],
  };
  final dates = [[2026, 1, 1], [2026, 3, 20], [2026, 6, 21], [2026, 9, 23], [2026, 11, 23], [2028, 2, 29]];
  CalculationParameters params(String m) => m == 'jafari'
      ? CalculationParameters(fajrAngle: 16, ishaAngle: 14, maghribAngle: 4, method: CalculationMethod.other)
      : CalculationMethod.values.byName(m).getParameters();
  final methods = [...CalculationMethod.values.where((m) => m != CalculationMethod.other).map((m) => m.name), 'jafari'];
  final out = [];
  for (final p in places.entries) {
    for (final d in dates) {
      for (final m in methods) {
        for (final madhab in Madhab.values) {
          for (final rule in HighLatitudeRule.values) {
            final c = params(m)..madhab = madhab..highLatitudeRule = rule;
            try {
              final t = PrayerTimes(Coordinates(p.value[0], p.value[1]), DateComponents(d[0], d[1], d[2]), c);
              out.add([p.key, d.join('-'), m, madhab.name, rule.name,
                for (final x in [t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha]) x.millisecondsSinceEpoch]);
            } catch (e) {
              out.add([p.key, d.join('-'), m, madhab.name, rule.name, 'error']);
            }
          }
        }
      }
    }
  }
  final s = SunnahTimes(PrayerTimes(Coordinates(32.9405, 73.7276), DateComponents(2026, 11, 23), CalculationMethod.karachi.getParameters()..madhab = Madhab.hanafi));
  final q = Qibla(Coordinates(32.9405, 73.7276)).direction;
  print(jsonEncode({'cases': out, 'sunnah': [s.middleOfTheNight.millisecondsSinceEpoch, s.lastThirdOfTheNight.millisecondsSinceEpoch], 'qibla': q}));
}
