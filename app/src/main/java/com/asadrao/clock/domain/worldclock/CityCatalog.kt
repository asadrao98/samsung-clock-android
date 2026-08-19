package com.asadrao.clock.domain.worldclock

import java.time.ZoneId
import java.util.Locale

/** A searchable city, derived from a time zone. */
data class City(
    val zoneId: String,
    val cityName: String,
    val countryName: String,
    val region: String,
) {
    val displayName: String get() = cityName
}

/**
 * The list of cities the user can add, built from the **platform's own time-zone database**.
 *
 * There is deliberately no bundled city file. The tz database is already on the device, is updated
 * with the OS, and carries the DST rules — so deriving the catalogue from it is both smaller and
 * more correct than shipping a list that would slowly go stale. It also means the World clock works
 * with no network, which is a requirement.
 *
 * Zone ids are not city names, so each one is unpacked: `Asia/Ho_Chi_Minh` becomes
 * "Ho Chi Minh" in the region "Asia". The country comes from ICU's zone-to-region mapping, which
 * is on-device too.
 *
 * Zones that are not places are filtered out — `Etc/GMT+5`, the deprecated single-word aliases like
 * `Japan`, and the `SystemV` legacy set. They would show up as nonsense entries in a city list.
 */
class CityCatalog(
    private val zoneIds: () -> Set<String> = { ZoneId.getAvailableZoneIds() },
    private val regionOf: (String) -> String? = ::icuRegionOf,
    private val locale: () -> Locale = { Locale.getDefault() },
) {

    /** Built once and cached: several hundred zones, and the list never changes at runtime. */
    val cities: List<City> by lazy {
        val displayLocale = locale()
        zoneIds()
            .asSequence()
            .filter(::isPlaceZone)
            .map { id -> toCity(id, displayLocale) }
            .sortedBy { it.cityName.lowercase(displayLocale) }
            .toList()
    }

    /**
     * Matches on city, country **or** zone id, so "Dubai", "United Arab Emirates" and "Asia/Dubai"
     * all find the same row.
     *
     * Prefix matches on the city name are ranked first, because someone typing "lon" wants London
     * before Colombo.
     */
    fun search(query: String, limit: Int = 60): List<City> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return cities.take(limit)
        val needle = trimmed.lowercase(locale())

        val prefix = ArrayList<City>()
        val contains = ArrayList<City>()
        for (city in cities) {
            val name = city.cityName.lowercase(locale())
            when {
                name.startsWith(needle) -> prefix += city
                name.contains(needle) ||
                    city.countryName.lowercase(locale()).contains(needle) ||
                    city.zoneId.lowercase(locale()).contains(needle) -> contains += city
            }
        }
        return (prefix + contains).take(limit)
    }

    private fun toCity(zoneId: String, displayLocale: Locale): City {
        val parts = zoneId.split('/')
        val region = parts.first().replace('_', ' ')
        val cityName = parts.last().replace('_', ' ')
        val country = regionOf(zoneId)
            ?.takeIf { it.length == 2 }
            ?.let { Locale.Builder().setRegion(it).build().getDisplayCountry(displayLocale) }
            ?.takeIf { it.isNotBlank() }
            ?: region
        return City(
            zoneId = zoneId,
            cityName = cityName,
            countryName = country,
            region = region,
        )
    }

    private fun isPlaceZone(zoneId: String): Boolean {
        if (!zoneId.contains('/')) return false
        val prefix = zoneId.substringBefore('/')
        return prefix !in NON_PLACE_PREFIXES
    }

    private companion object {
        /** Prefixes that are technical groupings rather than places. */
        val NON_PLACE_PREFIXES = setOf("Etc", "SystemV", "US", "Canada", "Brazil", "Mexico", "Chile")
    }
}

/**
 * ICU's zone-to-country mapping, which ships with Android.
 *
 * Wrapped in a runCatching because it is not present on every JVM — the unit tests run on a desktop
 * JDK, where this class is absent, and the catalogue must still build (falling back to the region
 * name) rather than crash.
 */
private fun icuRegionOf(zoneId: String): String? = runCatching {
    val clazz = Class.forName("android.icu.util.TimeZone")
    val method = clazz.getMethod("getRegion", String::class.java)
    method.invoke(null, zoneId) as? String
}.getOrNull()
