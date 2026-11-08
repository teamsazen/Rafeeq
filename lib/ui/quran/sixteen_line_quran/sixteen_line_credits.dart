import 'package:flutter/foundation.dart';

/// Shown in Licenses under this name.
const sixteenLineQuranTextCredit = 'IndoPak Quran text and font (QuranWBW)';
const sixteenLineQuranLayoutCredit = '16 line mushaf layout (QUL)';

/// The credits QuranWBW asks for, from the README in their IndoPak v5.4
/// package, with the permission Rafeeq was given.
const _quranWbwCredits = '''
IndoPak Quran Text v5.4 (Hanafi) and the AlQuran Neo v5.x font, by Ayman Siddiqui & R. Siddiqua.
https://www.quranwbw.com

Used in Rafeeq with prior written permission from QuranWBW to use the original, unmodified Quran text and font. The permission is restricted to displaying the Quran text only.

Copyright holders and sources:
© Ayman Siddiqui
© Al-Qalam and Ghandhara Software
© King Fahd Glorious Quran Printing Complex (KFGQPC)
All rights remain with their respective owners.

Quran text sources:
• King Fahd Glorious Quran Printing Complex (KFGQPC)
• TypeMyBook.com (Ata Rafi)
• InPage Quran Publishing Resources

Font development:
• Ayman Siddiqui & R. Siddiqua
• Al-Qalam Font Project
• Ghandhara Software
• King Fahd Glorious Quran Printing Complex (KFGQPC)

Verification and quality assurance:
• Hafiz Muhammad Zain Viqar, Content Lead at Deen App, and his team of Huffaz:
  Hafiz Muavia bin Muhammad Haneef, Hafiz Ali Ahmed bin Aftab Ahmed, Hafiz Abdul Rehman bin Ramzan

Special thanks:
• Abdul Majeed Khan (Al Qalam Quran Majeed font)
• Arif Karim (Al Qalam Quran Majeed font)
• Shakir-ul-Qadree (Al Qalam Quran Majeed font)
• Jawad (Al Qalam Quran Majeed font)
• And all contributors who directly or indirectly assisted in the digitization, preservation, verification, testing, and enhancement of Quranic text and typography.

Distributed as Sadaqah Jariyah. For permissions and corrections: quranwbw@gmail.com''';

const _qulCredits = '''
The IndoPak 16 line layout (Taj Company) and the surah name font v2 come from the Quranic Universal Library by Tarteel.
https://qul.tarteel.ai''';

/// Lists the 16 Line Quran's text, font and layout in Licenses.
void registerSixteenLineQuranLicenses() {
  LicenseRegistry.addLicense(() async* {
    yield const LicenseEntryWithLineBreaks([
      sixteenLineQuranTextCredit,
    ], _quranWbwCredits);
    yield const LicenseEntryWithLineBreaks([
      sixteenLineQuranLayoutCredit,
    ], _qulCredits);
  });
}
