package com.satechdev.rafeeqapp.data.audio

/**
 * The ayah-by-ayah reciters from lib/data/reciters_list.dart (generated from
 * it), in the same order: the first is the default.
 */
enum class Reciter(val identifier: String, val nativeName: String, val englishName: String, val bitrate: Int) {
    Abdullahbasfar("ar.abdullahbasfar", "عبد الله بصفر", "Abdullah Basfar", 64),
    Abdurrahmaansudais("ar.abdurrahmaansudais", "عبدالرحمن السديس", "Abdurrahmaan As-Sudais", 64),
    Abdulsamad("ar.abdulsamad", "عبدالباسط عبدالصمد", "Abdul Samad", 64),
    Shaatree("ar.shaatree", "أبو بكر الشاطري", "Abu Bakr Ash-Shaatree", 64),
    Ahmedajamy("ar.ahmedajamy", "أحمد بن علي العجمي", "Ahmed ibn Ali al-Ajamy", 64),
    Alafasy("ar.alafasy", "مشاري العفاسي", "Alafasy", 64),
    Hanirifai("ar.hanirifai", "هاني الرفاعي", "Hani Rifai", 64),
    Husary("ar.husary", "محمود خليل الحصري", "Husary", 64),
    Husarymujawwad("ar.husarymujawwad", "محمود خليل الحصري (المجود)", "Husary (Mujawwad)", 64),
    Hudhaify("ar.hudhaify", "علي بن عبدالرحمن الحذيفي", "Hudhaify", 64),
    Ibrahimakhbar("ar.ibrahimakhbar", "إبراهيم الأخضر", "Ibrahim Akhdar", 32),
    Mahermuaiqly("ar.mahermuaiqly", "ماهر المعيقلي", "Maher Al Muaiqly", 64),
    Muhammadayyoub("ar.muhammadayyoub", "محمد أيوب", "Muhammad Ayyoub", 128),
    Muhammadjibreel("ar.muhammadjibreel", "محمد جبريل", "Muhammad Jibreel", 128),
    Saoodshuraym("ar.saoodshuraym", "سعود الشريم", "Saood bin Ibraaheem Ash-Shuraym", 64),
    Parhizgar("ar.parhizgar", "شهریار پرهیزگار", "Parhizgar", 48),
    Aymanswoaid("ar.aymanswoaid", "أيمن سويد", "Ayman Sowaid", 64),
    YasserAlDousaryAyahs("ar.yasseraldousary", "ياسر الدوسري", "Yasser Al Dousary", 64);

    /** The recording of one ayah ([ayah] is 1 to 6236 across the Quran). */
    fun ayahLink(ayah: Int, surah: Int, ayahInSurah: Int): String =
        if (this == YasserAlDousaryAyahs) "https://the-quran-project.github.io/Quran-Audio/Data/4/${surah}_$ayahInSurah.mp3"
        else "https://cdn.islamic.network/quran/audio/$bitrate/$identifier/$ayah.mp3"

    companion object {
        fun of(identifier: String?) = entries.firstOrNull { it.identifier == identifier } ?: entries.first()
    }
}
