package ru.coffeemaniavpn.app.ui

object FlagUtils {
    private val regionalIndicatorBase = 0x1F1E6

    /** Запасной флаг, если страны в имени нет. */
    const val DEFAULT_FLAG_CODE = "eu"
    const val DEFAULT_FLAG_EMOJI = "🇪🇺"

    /**
     * Названия стран / распространённые алиасы → ISO 3166-1 alpha-2.
     * Ключи — lowercase; длинные имена проверяются первыми.
     */
    private val countryNameToCode: Map<String, String> = linkedMapOf(
        // --- Europe ---
        "netherlands" to "nl", "nederland" to "nl", "голландия" to "nl", "нидерланды" to "nl",
        "amsterdam" to "nl",
        "germany" to "de", "deutschland" to "de", "германия" to "de", "frankfurt" to "de", "berlin" to "de",
        "france" to "fr", "франция" to "fr", "paris" to "fr",
        "united kingdom" to "gb", "great britain" to "gb", "britain" to "gb", "england" to "gb",
        "london" to "gb", "uk" to "gb", "великобритания" to "gb", "англия" to "gb",
        "switzerland" to "ch", "schweiz" to "ch", "швейцария" to "ch", "zurich" to "ch", "geneva" to "ch",
        "sweden" to "se", "швеция" to "se", "stockholm" to "se",
        "norway" to "no", "норвегия" to "no", "oslo" to "no",
        "finland" to "fi", "финляндия" to "fi", "helsinki" to "fi",
        "denmark" to "dk", "дания" to "dk", "copenhagen" to "dk",
        "poland" to "pl", "польша" to "pl", "warsaw" to "pl", "варшава" to "pl",
        "czech" to "cz", "czechia" to "cz", "czech republic" to "cz", "чехия" to "cz", "prague" to "cz",
        "austria" to "at", "австрия" to "at", "vienna" to "at",
        "belgium" to "be", "бельгия" to "be", "brussels" to "be",
        "spain" to "es", "испания" to "es", "madrid" to "es", "barcelona" to "es",
        "italy" to "it", "италия" to "it", "milan" to "it", "rome" to "it",
        "portugal" to "pt", "португалия" to "pt", "lisbon" to "pt",
        "ireland" to "ie", "ирландия" to "ie", "dublin" to "ie",
        "romania" to "ro", "румыния" to "ro", "bucharest" to "ro",
        "bulgaria" to "bg", "болгария" to "bg", "sofia" to "bg",
        "greece" to "gr", "греция" to "gr", "athens" to "gr",
        "hungary" to "hu", "венгрия" to "hu", "budapest" to "hu",
        "slovakia" to "sk", "словакия" to "sk",
        "slovenia" to "si", "словения" to "si",
        "croatia" to "hr", "хорватия" to "hr",
        "serbia" to "rs", "сербия" to "rs", "belgrade" to "rs",
        "ukraine" to "ua", "украина" to "ua", "kyiv" to "ua", "kiev" to "ua", "киев" to "ua",
        "belarus" to "by", "беларусь" to "by", "минск" to "by",
        "moldova" to "md", "молдова" to "md",
        "lithuania" to "lt", "литва" to "lt", "vilnius" to "lt",
        "latvia" to "lv", "латвия" to "lv", "riga" to "lv",
        "estonia" to "ee", "эстония" to "ee", "tallinn" to "ee",
        "iceland" to "is", "исландия" to "is",
        "luxembourg" to "lu", "люксембург" to "lu",
        "monaco" to "mc", "монако" to "mc",
        "malta" to "mt", "мальта" to "mt",
        "cyprus" to "cy", "кипр" to "cy",
        "turkey" to "tr", "türkiye" to "tr", "turkiye" to "tr", "турция" to "tr", "istanbul" to "tr",
        "russia" to "ru", "российская федерация" to "ru", "россия" to "ru", "moscow" to "ru", "москва" to "ru",
        "georgia" to "ge", "грузия" to "ge", "tbilisi" to "ge",
        "armenia" to "am", "армения" to "am",
        "azerbaijan" to "az", "азербайджан" to "az",
        "kazakhstan" to "kz", "казахстан" to "kz", "almaty" to "kz", "astana" to "kz",
        "uzbekistan" to "uz", "узбекистан" to "uz",
        // --- Americas ---
        "united states" to "us", "united states of america" to "us", "usa" to "us", "america" to "us",
        "сша" to "us", "америка" to "us", "new york" to "us", "los angeles" to "us", "miami" to "us",
        "chicago" to "us", "seattle" to "us", "dallas" to "us", "ashburn" to "us",
        "canada" to "ca", "канада" to "ca", "toronto" to "ca", "montreal" to "ca", "vancouver" to "ca",
        "mexico" to "mx", "мексика" to "mx",
        "brazil" to "br", "brasil" to "br", "бразилия" to "br", "sao paulo" to "br",
        "argentina" to "ar", "аргентина" to "ar", "buenos aires" to "ar",
        "chile" to "cl", "чили" to "cl",
        "colombia" to "co", "колумбия" to "co",
        "peru" to "pe", "перу" to "pe",
        // --- Asia / Pacific ---
        "japan" to "jp", "япония" to "jp", "tokyo" to "jp", "osaka" to "jp",
        "south korea" to "kr", "korea" to "kr", "южная корея" to "kr", "корея" to "kr", "seoul" to "kr",
        "china" to "cn", "китай" to "cn", "hong kong" to "hk", "гонконг" to "hk",
        "taiwan" to "tw", "тайвань" to "tw", "taipei" to "tw",
        "singapore" to "sg", "сингапур" to "sg",
        "india" to "in", "индия" to "in", "mumbai" to "in", "delhi" to "in",
        "indonesia" to "id", "индонезия" to "id", "jakarta" to "id",
        "thailand" to "th", "таиланд" to "th", "bangkok" to "th",
        "vietnam" to "vn", "вьетнам" to "vn",
        "malaysia" to "my", "малайзия" to "my",
        "philippines" to "ph", "филиппины" to "ph",
        "israel" to "il", "израиль" to "il", "tel aviv" to "il",
        "uae" to "ae", "united arab emirates" to "ae", "оаэ" to "ae", "dubai" to "ae", "abu dhabi" to "ae",
        "saudi arabia" to "sa", "саудовская аравия" to "sa",
        "qatar" to "qa", "катар" to "qa",
        "australia" to "au", "австралия" to "au", "sydney" to "au", "melbourne" to "au",
        "new zealand" to "nz", "новая зеландия" to "nz", "auckland" to "nz",
        // --- Africa ---
        "south africa" to "za", "юар" to "za", "южная африка" to "za", "johannesburg" to "za",
        "egypt" to "eg", "египет" to "eg",
        "nigeria" to "ng", "нигерия" to "ng",
        "morocco" to "ma", "марокко" to "ma",
        // --- Regions / meta ---
        "europe" to "eu", "европа" to "eu", "european union" to "eu", "ес" to "eu",
        "авто" to "eu", "auto" to "eu", "автовыбор" to "eu",
    ).entries
        .sortedByDescending { it.key.length }
        .associate { it.key to it.value }

    /** ISO 3166-1 alpha-2 из emoji-флага (🇳🇱 → nl). */
    fun emojiToCountryCode(flag: String): String? {
        val codePoints = flag.trim()
            .codePoints()
            .toArray()
            .filter { it != 0xFE0F && it != 0x200D }
        if (codePoints.size < 2) return null

        var i = 0
        while (i <= codePoints.size - 2) {
            val first = codePoints[i] - regionalIndicatorBase
            val second = codePoints[i + 1] - regionalIndicatorBase
            if (first in 0..25 && second in 0..25) {
                return buildString {
                    append(('a'.code + first).toChar())
                    append(('a'.code + second).toChar())
                }
            }
            i++
        }
        return null
    }

    /** Нормализует emoji / ISO-код / неизвестное → lowercase ISO или null. */
    fun resolveCountryCode(flagOrCode: String): String? {
        val trimmed = flagOrCode.trim()
        if (trimmed.isEmpty() || isGlobeOrUnknownRaw(trimmed)) return null

        emojiToCountryCode(trimmed)?.let { return it }

        val ascii = trimmed.lowercase()
        if (ascii.length == 2 && ascii.all { it in 'a'..'z' }) {
            return ascii
        }
        return null
    }

    /**
     * Ищет страну в произвольном тексте (название сервера / города / страны).
     * Сначала emoji/ISO, затем совпадение по названию (longest-first).
     */
    fun resolveCountryCodeFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        resolveCountryCode(trimmed)?.let { return it }

        val lower = trimmed.lowercase()
            .replace('ё', 'е')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace('|', ' ')
            .replace('·', ' ')
            .replace(',', ' ')

        // Exact / contains match against known names (longest keys first)
        for ((name, code) in countryNameToCode) {
            if (lower == name) return code
            if (lower.contains(name)) {
                // Prefer word-boundary-ish matches to avoid "in" inside "finland" false positives —
                // keys are sorted longest-first so "finland" wins before "in" isn't in map as short token.
                val idx = lower.indexOf(name)
                val beforeOk = idx == 0 || !lower[idx - 1].isLetter()
                val afterIdx = idx + name.length
                val afterOk = afterIdx >= lower.length || !lower[afterIdx].isLetter()
                if (beforeOk && afterOk) return code
            }
        }

        // Token scan: "NL-AMS-01", "de1", "US East"
        val tokens = Regex("""[a-zа-я]{2,}""", RegexOption.IGNORE_CASE)
            .findAll(lower)
            .map { it.value }
            .toList()
        for (token in tokens) {
            countryNameToCode[token]?.let { return it }
            if (token.length == 2 && token.all { it in 'a'..'z' }) {
                // Only accept common ISO tokens that we ship as assets
                if (token != "in" && token != "to" && token != "as" && token != "is") {
                    return token
                }
            }
        }

        return null
    }

    /** Код страны или ЕС, если определить не удалось. */
    fun resolveCountryCodeOrDefault(flagOrCode: String): String =
        resolveCountryCode(flagOrCode)
            ?: resolveCountryCodeFromText(flagOrCode)
            ?: DEFAULT_FLAG_CODE

    /** Локальный asset; всегда есть (fallback — ЕС). */
    fun flagAssetPath(flagOrCode: String): String {
        val code = resolveCountryCodeOrDefault(flagOrCode)
        return "file:///android_asset/flags/$code.png"
    }

    fun flagCdnUrl(flagOrCode: String, widthPx: Int = 160): String {
        val code = resolveCountryCodeOrDefault(flagOrCode)
        val width = when {
            widthPx <= 40 -> 40
            widthPx <= 80 -> 80
            widthPx <= 160 -> 160
            widthPx <= 320 -> 320
            else -> 640
        }
        return "https://flagcdn.com/w$width/$code.png"
    }

    fun isGlobeOrUnknown(flagOrCode: String): Boolean {
        val t = flagOrCode.trim()
        if (isGlobeOrUnknownRaw(t)) return true
        return resolveCountryCode(t) == null && resolveCountryCodeFromText(t) == null
    }

    private fun isGlobeOrUnknownRaw(t: String): Boolean =
        t == "🌐" || t == "🌍" || t == "🌎" || t == "🌏" || t == "🗺"
}
