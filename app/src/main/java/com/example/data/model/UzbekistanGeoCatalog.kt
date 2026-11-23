package com.example.data.model

import com.example.i18n.AppLanguage
import kotlin.math.hypot
import kotlin.math.roundToInt

data class UzCityNode(
    val id: String,
    val name: String,
    val regionId: String,
    val lat: Double,
    val lng: Double,
    val expressAvailable: Boolean,
    val etaUz: String,
    val etaRu: String,
    val etaEn: String,
    val streetUz: String,
    val streetRu: String,
    val streetEn: String,
    val pickupPoints: List<String>
) {
    fun localizedEta(language: AppLanguage): String = when (language) {
        AppLanguage.UZ -> etaUz
        AppLanguage.RU -> etaRu
        AppLanguage.EN -> etaEn
    }

    fun localizedStreet(language: AppLanguage): String = when (language) {
        AppLanguage.UZ -> streetUz
        AppLanguage.RU -> streetRu
        AppLanguage.EN -> streetEn
    }
}

data class UzRegion(
    val id: String,
    val nameUz: String,
    val nameRu: String,
    val nameEn: String,
    val cities: List<UzCityNode>
) {
    fun localizedName(language: AppLanguage): String = when (language) {
        AppLanguage.UZ -> nameUz
        AppLanguage.RU -> nameRu
        AppLanguage.EN -> nameEn
    }
}

object UzbekistanGeoCatalog {
    // Full bounding box of Republic of Uzbekistan
    const val UZ_MIN_LAT = 37.15
    const val UZ_MAX_LAT = 45.55
    const val UZ_MIN_LNG = 56.00
    const val UZ_MAX_LNG = 73.20

    val regions: List<UzRegion> = listOf(
        UzRegion(
            id = "tashkent_city",
            nameUz = "Toshkent shahri",
            nameRu = "г. Ташкент",
            nameEn = "Tashkent City",
            cities = listOf(
                city("yunusobod", "Yunusobod", "tashkent_city", 41.3562, 69.2865, true, "Amir Temur shoh ko'chasi, Yunusobod 4", "Проспект Амира Темура, Юнусабад 4", "Amir Temur Ave, Yunusobod 4", listOf("TezBazar PVZ • Yunusobod Mega Planet", "TezBazar PVZ • Bodomzor metro")),
                city("chilonzor", "Chilonzor", "tashkent_city", 41.2858, 69.2036, true, "Bunyodkor shoh ko'chasi, Chilonzor 9", "Проспект Бунёдкор, Чиланзар 9", "Bunyodkor Ave, Chilanzar 9", listOf("TezBazar PVZ • Chilonzor Integro", "TezBazar PVZ • Novza metro")),
                city("mirzo_ulugbek", "Mirzo Ulug'bek", "tashkent_city", 41.3267, 69.3490, true, "Buyuk Ipak Yo'li ko'chasi, 45", "Улица Буюк Ипак Йули, 45", "Buyuk Ipak Yuli St, 45", listOf("TezBazar PVZ • Maksim Gorkiy", "TezBazar PVZ • TTZ Bozor")),
                city("shayxontohur", "Shayxontohur", "tashkent_city", 41.3210, 69.2421, true, "Navoiy shoh ko'chasi, Chorsu", "Проспект Навои, Чорсу", "Navoiy Ave, Chorsu", listOf("TezBazar PVZ • Tashkent City", "TezBazar PVZ • Chorsu Gum")),
                city("mirobod", "Mirobod", "tashkent_city", 41.2981, 69.2794, true, "Taras Shevchenko ko'chasi, 18", "Улица Тараса Шевченко, 18", "Taras Shevchenko St, 18", listOf("TezBazar PVZ • Oybek metro", "TezBazar PVZ • Gospital bozor")),
                city("yakkasaroy", "Yakkasaroy", "tashkent_city", 41.2886, 69.2665, true, "Shota Rustaveli ko'chasi, 28", "Улица Шота Руставели, 28", "Shota Rustaveli St, 28", listOf("TezBazar PVZ • Grand Mir", "TezBazar PVZ • Tekstil")),
                city("olmazor", "Olmazor", "tashkent_city", 41.3461, 69.2185, true, "Beruniy shoh ko'chasi, 12", "Проспект Беруни, 12", "Beruniy Ave, 12", listOf("TezBazar PVZ • Beruniy metro", "TezBazar PVZ • Qoraqamish 2/4")),
                city("sergeli", "Sergeli", "tashkent_city", 41.2248, 69.2225, true, "Yangi Sergeli yo'li, 7-mavze", "Улица Янги Сергели, 7-й квартал", "Yangi Sergeli Rd, Block 7", listOf("TezBazar PVZ • Sergeli-5", "TezBazar PVZ • Yangi Hayot")),
                city("uchtepa", "Uchtepa", "tashkent_city", 41.2965, 69.1742, true, "Lutfiy ko'chasi, 14-mavze", "Улица Лутфи, 14-й квартал", "Lutfiy St, Block 14", listOf("TezBazar PVZ • Farhod bozori")),
                city("yashnobod", "Yashnobod", "tashkent_city", 41.2932, 69.3385, true, "Maxtumquli ko'chasi, 79", "Улица Махтумкули, 79", "Makhtumkuli St, 79", listOf("TezBazar PVZ • Parkent bozori"))
            )
        ),
        UzRegion(
            id = "tashkent_region",
            nameUz = "Toshkent viloyati",
            nameRu = "Ташкентская область",
            nameEn = "Tashkent Region",
            cities = listOf(
                city("chirchiq", "Chirchiq", "tashkent_region", 41.4689, 69.5822, false, "Alisher Navoiy shoh ko'chasi, 112", "Проспект Алишера Навои, 112", "Alisher Navoiy Ave, 112", listOf("TezBazar PVZ • Chirchiq Markaz", "TezBazar PVZ • Troitskiy")),
                city("olmaliq", "Olmaliq", "tashkent_region", 40.8447, 69.5983, false, "Amir Temur ko'chasi, 34", "Улица Амира Темура, 34", "Amir Temur St, 34", listOf("TezBazar PVZ • Olmaliq Metallurg")),
                city("angren", "Angren", "tashkent_region", 41.0167, 70.1436, false, "Bunyodkor ko'chasi, 19", "Улица Бунёдкор, 19", "Bunyodkor St, 19", listOf("TezBazar PVZ • Angren Markaz")),
                city("nurafshon", "Nurafshon", "tashkent_region", 41.0315, 69.3422, false, "Toshkent yo'li ko'chasi, 5", "Улица Ташкент йули, 5", "Tashkent Yuli St, 5", listOf("TezBazar PVZ • Nurafshon City")),
                city("yangiyol", "Yangiyo'l", "tashkent_region", 41.1120, 69.0471, false, "Samarqand ko'chasi, 42", "Улица Самарканд, 42", "Samarkand St, 42", listOf("TezBazar PVZ • Yangiyo'l Markaz"))
            )
        ),
        UzRegion(
            id = "samarkand",
            nameUz = "Samarqand viloyati",
            nameRu = "Самаркандская область",
            nameEn = "Samarkand Region",
            cities = listOf(
                city("samarkand_city", "Samarqand sh.", "samarkand", 39.6542, 66.9597, true, "Registon ko'chasi, 15", "Улица Регистан, 15", "Registon St, 15", listOf("TezBazar PVZ • Registon Maydoni", "TezBazar PVZ • Mikrorayon", "TezBazar PVZ • Siyob")),
                city("urgut", "Urgut", "samarkand", 39.4022, 67.2431, false, "Navoiy shoh ko'chasi, 24", "Проспект Навои, 24", "Navoiy Ave, 24", listOf("TezBazar PVZ • Urgut Markaz")),
                city("kattaqorgon", "Kattaqo'rg'on", "samarkand", 39.8989, 66.2561, false, "Amir Temur ko'chasi, 8", "Улица Амира Темура, 8", "Amir Temur St, 8", listOf("TezBazar PVZ • Kattaqo'rg'on")),
                city("bulungur", "Bulung'ur", "samarkand", 39.7647, 67.2714, false, "Mustaqillik ko'chasi, 14", "Улица Мустакиллик, 14", "Mustaqillik St, 14", listOf("TezBazar PVZ • Bulung'ur Markaz"))
            )
        ),
        UzRegion(
            id = "fergana",
            nameUz = "Farg'ona viloyati",
            nameRu = "Ферганская область",
            nameEn = "Fergana Region",
            cities = listOf(
                city("fergana_city", "Farg'ona sh.", "fergana", 40.3842, 71.7843, true, "Al-Farg'oniy ko'chasi, 32", "Улица Аль-Фаргони, 32", "Al-Fergani St, 32", listOf("TezBazar PVZ • Al-Farg'oniy", "TezBazar PVZ • Asia.uz")),
                city("qoqon", "Qo'qon", "fergana", 40.5286, 70.9425, true, "Turon ko'chasi, 18", "Улица Турон, 18", "Turon St, 18", listOf("TezBazar PVZ • Qo'qon O'rda", "TezBazar PVZ • Yangi Bozor")),
                city("margilon", "Marg'ilon", "fergana", 40.4711, 71.7247, true, "Mustaqillik ko'chasi, 56", "Улица Мустакиллик, 56", "Mustaqillik St, 56", listOf("TezBazar PVZ • Marg'ilon Yodgorlik")),
                city("quva", "Quva", "fergana", 40.5220, 72.0729, false, "Quva Anor ko'chasi, 7", "Улица Кува Анор, 7", "Kuva Anor St, 7", listOf("TezBazar PVZ • Quva Markaz"))
            )
        ),
        UzRegion(
            id = "andijan",
            nameUz = "Andijon viloyati",
            nameRu = "Андижанская область",
            nameEn = "Andijan Region",
            cities = listOf(
                city("andijan_city", "Andijon sh.", "andijan", 40.7821, 72.3442, true, "Bobur shoh ko'chasi, 40", "Проспект Бобура, 40", "Babur Ave, 40", listOf("TezBazar PVZ • Bobur Bog'i", "TezBazar PVZ • Navruz Mall")),
                city("asaka", "Asaka", "andijan", 40.6415, 72.2387, false, "Imom Buxoriy ko'chasi, 16", "Улица Имама Бухари, 16", "Imom Bukhari St, 16", listOf("TezBazar PVZ • Asaka Markaz")),
                city("shahrixon", "Shahrixon", "andijan", 40.7133, 72.0571, false, "Nayman ko'chasi, 10", "Улица Найман, 10", "Nayman St, 10", listOf("TezBazar PVZ • Shahrixon"))
            )
        ),
        UzRegion(
            id = "namangan",
            nameUz = "Namangan viloyati",
            nameRu = "Наманганская область",
            nameEn = "Namangan Region",
            cities = listOf(
                city("namangan_city", "Namangan sh.", "namangan", 40.9983, 71.6726, true, "Nodira ko'chasi, 21", "Улица Нодира, 21", "Nodira St, 21", listOf("TezBazar PVZ • Chorsu Namangan", "TezBazar PVZ • Afsona")),
                city("chust", "Chust", "namangan", 41.0033, 71.2378, false, "Birlik ko'chasi, 9", "Улица Бирлик, 9", "Birlik St, 9", listOf("TezBazar PVZ • Chust Markaz")),
                city("chortoq", "Chortoq", "namangan", 41.0692, 71.8236, false, "Istiqlol ko'chasi, 15", "Улица Истиклол, 15", "Istiqlol St, 15", listOf("TezBazar PVZ • Chortoq"))
            )
        ),
        UzRegion(
            id = "bukhara",
            nameUz = "Buxoro viloyati",
            nameRu = "Бухарская область",
            nameEn = "Bukhara Region",
            cities = listOf(
                city("bukhara_city", "Buxoro sh.", "bukhara", 39.7681, 64.4556, true, "Bahouddin Naqshband ko'chasi, 88", "Улица Бахоуддина Накшбанда, 88", "Bahauddin Naqshband St, 88", listOf("TezBazar PVZ • Labi Hovuz", "TezBazar PVZ • Buxoro City")),
                city("kogon", "Kogon", "bukhara", 39.7228, 64.5517, false, "Do'stlik ko'chasi, 12", "Улица Дустлик, 12", "Dustlik St, 12", listOf("TezBazar PVZ • Kogon Vokzal")),
                city("gijduvon", "G'ijduvon", "bukhara", 40.1000, 64.6833, false, "Yusuf Hamadoniy ko'chasi, 18", "Улица Юсуфа Хамадони, 18", "Yusuf Hamadoni St, 18", listOf("TezBazar PVZ • G'ijduvon"))
            )
        ),
        UzRegion(
            id = "khorezm",
            nameUz = "Xorazm viloyati",
            nameRu = "Хорезмская область",
            nameEn = "Khorezm Region",
            cities = listOf(
                city("urganch", "Urganch", "khorezm", 41.5500, 60.6333, false, "Al-Xorazmiy ko'chasi, 54", "Улица Аль-Хорезми, 54", "Al-Khorezmi St, 54", listOf("TezBazar PVZ • Urganch Markaz", "TezBazar PVZ • Shovot kanali")),
                city("xiva", "Xiva", "khorezm", 41.3775, 60.3619, false, "Amir Temur ko'chasi, Ichan Qal'a", "Улица Амира Темура, Ичан-Кала", "Amir Temur St, Itchan Kala", listOf("TezBazar PVZ • Xiva Markaz"))
            )
        ),
        UzRegion(
            id = "kashkadarya",
            nameUz = "Qashqadaryo viloyati",
            nameRu = "Кашкадарьинская область",
            nameEn = "Kashkadarya Region",
            cities = listOf(
                city("qarshi", "Qarshi", "kashkadarya", 38.8606, 65.7891, false, "Islom Karimov ko'chasi, 102", "Улица Ислама Каримова, 102", "Islam Karimov St, 102", listOf("TezBazar PVZ • Qarshi Markaz", "TezBazar PVZ • Atlas")),
                city("shahrisabz", "Shahrisabz", "kashkadarya", 39.0578, 66.8342, false, "Ipak Yo'li ko'chasi, 30", "Улица Ипак Йули, 30", "Ipak Yuli St, 30", listOf("TezBazar PVZ • Oqsaroy"))
            )
        ),
        UzRegion(
            id = "surkhandarya",
            nameUz = "Surxondaryo viloyati",
            nameRu = "Сурхандарьинская область",
            nameEn = "Surkhandarya Region",
            cities = listOf(
                city("termiz", "Termiz", "surkhandarya", 37.2242, 67.2783, false, "At-Termiziy shoh ko'chasi, 19", "Проспект Ат-Термизи, 19", "At-Termiziy Ave, 19", listOf("TezBazar PVZ • Termiz Markaz", "TezBazar PVZ • Yubileyniy")),
                city("denov", "Denov", "surkhandarya", 38.2675, 67.8989, false, "Sharof Rashidov ko'chasi, 44", "Улица Шарафа Рашидова, 44", "Sharof Rashidov St, 44", listOf("TezBazar PVZ • Denov"))
            )
        ),
        UzRegion(
            id = "navoi",
            nameUz = "Navoiy viloyati",
            nameRu = "Навоийская область",
            nameEn = "Navoi Region",
            cities = listOf(
                city("navoiy_city", "Navoiy sh.", "navoi", 40.0844, 65.3792, false, "Galaba shoh ko'chasi, 65", "Проспект Галаба, 65", "Galaba Ave, 65", listOf("TezBazar PVZ • Navoiy TSUM", "TezBazar PVZ • Zarafshon")),
                city("zarafshon", "Zarafshon", "navoi", 41.5761, 64.2050, false, "3-mikrorayon, 12", "3-й микрорайон, 12", "3rd Microdistrict, 12", listOf("TezBazar PVZ • Zarafshon"))
            )
        ),
        UzRegion(
            id = "jizzakh",
            nameUz = "Jizzax viloyati",
            nameRu = "Джизакская область",
            nameEn = "Jizzakh Region",
            cities = listOf(
                city("jizzakh_city", "Jizzax sh.", "jizzakh", 40.1158, 67.8422, false, "Sharof Rashidov shoh ko'chasi, 77", "Проспект Шарафа Рашидова, 77", "Sharof Rashidov Ave, 77", listOf("TezBazar PVZ • Jizzax Markaz", "TezBazar PVZ • Zargarlik")),
                city("zomin", "Zomin", "jizzakh", 39.9606, 68.3958, false, "Mustaqillik ko'chasi, 14", "Улица Мустакиллик, 14", "Mustaqillik St, 14", listOf("TezBazar PVZ • Zomin"))
            )
        ),
        UzRegion(
            id = "syrdarya",
            nameUz = "Sirdaryo viloyati",
            nameRu = "Сырдарьинская область",
            nameEn = "Syrdarya Region",
            cities = listOf(
                city("guliston", "Guliston", "syrdarya", 40.4897, 68.7842, false, "O'zbekiston shoh ko'chasi, 25", "Проспект Узбекистан, 25", "Uzbekistan Ave, 25", listOf("TezBazar PVZ • Guliston Markaz")),
                city("yangiyer", "Yangiyer", "syrdarya", 40.2750, 68.8225, false, "Paxtakor ko'chasi, 8", "Улица Пахтакор, 8", "Pakhtakor St, 8", listOf("TezBazar PVZ • Yangiyer"))
            )
        ),
        UzRegion(
            id = "karakalpakstan",
            nameUz = "Qoraqalpog'iston Resp.",
            nameRu = "Респ. Каракалпакстан",
            nameEn = "Rep. of Karakalpakstan",
            cities = listOf(
                city("nukus", "Nukus", "karakalpakstan", 42.4531, 59.6103, false, "Do'stlik guzari, 48", "Улица Дустлик гузары, 48", "Dustlik Guzari St, 48", listOf("TezBazar PVZ • Nukus Savitskiy", "TezBazar PVZ • Nukus Mega")),
                city("beruniy", "Beruniy", "karakalpakstan", 41.6911, 60.7525, false, "Xalqlar do'stligi ko'chasi, 17", "Улица Дружбы народов, 17", "Xalqlar Dustligi St, 17", listOf("TezBazar PVZ • Beruniy")),
                city("tortkol", "To'rtko'l", "karakalpakstan", 41.5542, 61.0033, false, "To'rtko'l shoh ko'chasi, 11", "Проспект Турткуль, 11", "Turtkul Ave, 11", listOf("TezBazar PVZ • To'rtko'l"))
            )
        )
    )

    val allCities: List<UzCityNode> = regions.flatMap { it.cities }

    // Regional capital / major hub pins shown on the National Map of Uzbekistan
    val regionalHubs: List<UzCityNode> = regions.map { it.cities.first() }

    private fun city(
        id: String,
        name: String,
        regionId: String,
        lat: Double,
        lng: Double,
        express: Boolean,
        streetUz: String,
        streetRu: String,
        streetEn: String,
        pvzs: List<String>
    ): UzCityNode = UzCityNode(
        id = id,
        name = name,
        regionId = regionId,
        lat = lat,
        lng = lng,
        expressAvailable = express,
        etaUz = if (express) "15–30 daq • Ertaga PVZ bepul" else "Ertaga bepul (TezBazar PVZ)",
        etaRu = if (express) "15–30 мин • Завтра в ПВЗ бесплатно" else "Завтра бесплатно (ПВЗ TezBazar)",
        etaEn = if (express) "15–30 min • Free Pickup Tomorrow" else "Free Next-Day Pickup (PVZ)",
        streetUz = streetUz,
        streetRu = streetRu,
        streetEn = streetEn,
        pickupPoints = pvzs
    )

    fun findCity(cityNameOrId: String): UzCityNode {
        return allCities.firstOrNull {
            it.name.equals(cityNameOrId, ignoreCase = true) ||
                it.id.equals(cityNameOrId, ignoreCase = true)
        } ?: allCities.first()
    }

    fun findRegionForCity(cityName: String): UzRegion {
        val node = findCity(cityName)
        return regions.firstOrNull { it.id == node.regionId } ?: regions.first()
    }

    fun coordinatesForCity(cityName: String): Pair<Double, Double> {
        val node = findCity(cityName)
        return node.lat to node.lng
    }

    fun nearestCity(lat: Double, lng: Double): UzCityNode {
        return allCities.minByOrNull { hypot(lat - it.lat, lng - it.lng) } ?: allCities.first()
    }

    fun pickupPointsFor(cityName: String): List<String> {
        val node = findCity(cityName)
        return node.pickupPoints.ifEmpty { listOf("TezBazar PVZ • ${node.name} Markaz") }
    }

    fun fallbackStreet(lat: Double, lng: Double, language: AppLanguage): String {
        val nearest = nearestCity(lat, lng)
        val metersOffset = (hypot(lat - nearest.lat, lng - nearest.lng) * 111_000).roundToInt()
        return if (metersOffset < 1200) {
            nearest.localizedStreet(language)
        } else {
            val houseNum = ((lat * 1000).toInt() + (lng * 1000).toInt()).let { (kotlin.math.abs(it) % 85) + 3 }
            when (language) {
                AppLanguage.UZ -> "${nearest.name} hududi, Markaziy ko'cha, $houseNum-uy"
                AppLanguage.RU -> "${nearest.name}, Центральная улица, дом $houseNum"
                AppLanguage.EN -> "${nearest.name}, Central Ave, bldg $houseNum"
            }
        }
    }

    fun uzLatLngToNormalized(lat: Double, lng: Double): Pair<Float, Float> {
        val nx = ((lng - UZ_MIN_LNG) / (UZ_MAX_LNG - UZ_MIN_LNG)).toFloat().coerceIn(0.04f, 0.96f)
        val ny = (1.0 - ((lat - UZ_MIN_LAT) / (UZ_MAX_LAT - UZ_MIN_LAT))).toFloat().coerceIn(0.06f, 0.94f)
        return nx to ny
    }

    fun uzNormalizedToLatLng(nx: Float, ny: Float): Pair<Double, Double> {
        val clampedX = nx.coerceIn(0.02f, 0.98f).toDouble()
        val clampedY = ny.coerceIn(0.02f, 0.98f).toDouble()
        val lng = UZ_MIN_LNG + clampedX * (UZ_MAX_LNG - UZ_MIN_LNG)
        val lat = UZ_MIN_LAT + (1.0 - clampedY) * (UZ_MAX_LAT - UZ_MIN_LAT)
        return lat to lng
    }

    // Local ~14km x 14km city street grid centered on a specific city
    private const val CITY_HALF_SPAN_LAT = 0.065
    private const val CITY_HALF_SPAN_LNG = 0.085

    fun cityLatLngToNormalized(lat: Double, lng: Double, centerLat: Double, centerLng: Double): Pair<Float, Float> {
        val minLat = centerLat - CITY_HALF_SPAN_LAT
        val maxLat = centerLat + CITY_HALF_SPAN_LAT
        val minLng = centerLng - CITY_HALF_SPAN_LNG
        val maxLng = centerLng + CITY_HALF_SPAN_LNG
        val nx = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f)
        val ny = (1.0 - ((lat - minLat) / (maxLat - minLat))).toFloat().coerceIn(0.05f, 0.95f)
        return nx to ny
    }

    fun cityNormalizedToLatLng(nx: Float, ny: Float, centerLat: Double, centerLng: Double): Pair<Double, Double> {
        val minLat = centerLat - CITY_HALF_SPAN_LAT
        val maxLat = centerLat + CITY_HALF_SPAN_LAT
        val minLng = centerLng - CITY_HALF_SPAN_LNG
        val maxLng = centerLng + CITY_HALF_SPAN_LNG
        val clampedX = nx.coerceIn(0.03f, 0.97f).toDouble()
        val clampedY = ny.coerceIn(0.03f, 0.97f).toDouble()
        val lng = minLng + clampedX * (maxLng - minLng)
        val lat = minLat + (1.0 - clampedY) * (maxLat - minLat)
        return lat to lng
    }
}
