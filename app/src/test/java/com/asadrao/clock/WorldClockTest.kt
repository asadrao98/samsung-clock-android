package com.asadrao.clock

import com.asadrao.clock.domain.worldclock.CityCatalog
import com.asadrao.clock.domain.worldclock.WorldClockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * World clock behaviour, with the emphasis on the cases a stored UTC offset would get wrong.
 */
class WorldClockTest {

    private val dubai = "Asia/Dubai"
    private val london = "Europe/London"
    private val newYork = "America/New_York"
    private val kolkata = "Asia/Kolkata"
    private val kathmandu = "Asia/Kathmandu"

    private fun at(
        year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: String = "UTC",
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of(zone)).toInstant()

    // ---- offsets across DST ---------------------------------------------------------------

    @Test
    fun the_offset_between_two_cities_changes_across_dst() {
        // This is the whole reason offsets are computed live rather than stored. London is 4 hours
        // behind Dubai in winter and 3 in summer; a stored "+4" would be wrong for half the year.
        val january = at(2026, 1, 15, 12, 0)
        val july = at(2026, 7, 15, 12, 0)

        assertEquals(
            "4 hours behind",
            WorldClockTime.offsetDescription(london, dubai, january),
        )
        assertEquals(
            "3 hours behind",
            WorldClockTime.offsetDescription(london, dubai, july),
        )
    }

    @Test
    fun a_city_ahead_of_home_reads_as_ahead() {
        val january = at(2026, 1, 15, 12, 0)
        assertEquals("4 hours ahead", WorldClockTime.offsetDescription(dubai, london, january))
    }

    @Test
    fun the_same_offset_reads_as_same_time_not_zero_hours() {
        val instant = at(2026, 1, 15, 12, 0)
        assertEquals("Same time", WorldClockTime.offsetDescription(london, "UTC", instant))
    }

    @Test
    fun half_and_quarter_hour_zones_are_reported_precisely() {
        // India is +5:30 and Nepal +5:45. Rounding to whole hours would misreport both.
        val instant = at(2026, 1, 15, 12, 0)
        assertEquals("5 hr 30 min ahead", WorldClockTime.offsetDescription(kolkata, "UTC", instant))
        assertEquals(
            "5 hr 45 min ahead",
            WorldClockTime.offsetDescription(kathmandu, "UTC", instant),
        )
    }

    @Test
    fun a_one_hour_difference_is_singular() {
        val january = at(2026, 1, 15, 12, 0)
        assertEquals(
            "1 hour ahead",
            WorldClockTime.offsetDescription("Europe/Paris", london, january),
        )
    }

    // ---- date differences ----------------------------------------------------------------

    @Test
    fun a_city_over_the_date_line_reads_as_tomorrow() {
        // 20:00 UTC on the 19th is already the 20th in Auckland.
        val instant = at(2026, 8, 19, 20, 0)
        assertEquals(1L, WorldClockTime.dayOffset("Pacific/Auckland", "UTC", instant))
    }

    @Test
    fun a_city_behind_can_read_as_yesterday() {
        // 02:00 UTC on the 19th is still the 18th in Los Angeles.
        val instant = at(2026, 8, 19, 2, 0)
        assertEquals(-1L, WorldClockTime.dayOffset("America/Los_Angeles", "UTC", instant))
    }

    @Test
    fun same_day_is_zero() {
        val instant = at(2026, 8, 19, 12, 0)
        assertEquals(0L, WorldClockTime.dayOffset(london, "UTC", instant))
    }

    // ---- current time --------------------------------------------------------------------

    @Test
    fun a_citys_local_time_is_correct_across_dst() {
        // 12:00 UTC is 13:00 in London in summer and 12:00 in winter.
        assertEquals(13, WorldClockTime.nowIn(london, at(2026, 7, 15, 12, 0)).hour)
        assertEquals(12, WorldClockTime.nowIn(london, at(2026, 1, 15, 12, 0)).hour)
    }

    @Test
    fun day_and_night_follow_local_hour() {
        // Midday in Dubai is 08:00 UTC.
        assertTrue(WorldClockTime.isDaytime(dubai, at(2026, 8, 19, 8, 0)))
        // Midnight in Dubai is 20:00 UTC the day before.
        assertFalse(WorldClockTime.isDaytime(dubai, at(2026, 8, 18, 20, 0)))
    }

    @Test
    fun the_day_night_boundary_is_inclusive_at_six_and_exclusive_at_eighteen() {
        // Documented heuristic, pinned so a change is deliberate.
        val sixAmDubai = at(2026, 8, 19, 2, 0)
        val sixPmDubai = at(2026, 8, 19, 14, 0)
        assertTrue(WorldClockTime.isDaytime(dubai, sixAmDubai))
        assertFalse(WorldClockTime.isDaytime(dubai, sixPmDubai))
    }

    // ---- the catalogue --------------------------------------------------------------------

    private val catalog = CityCatalog(locale = { Locale.UK })

    @Test
    fun the_catalogue_is_built_from_the_platform_time_zone_database() {
        // No bundled city list: the tz database is already on the device, is updated with the OS
        // and carries the DST rules.
        assertTrue("expected a substantial catalogue", catalog.cities.size > 200)
    }

    @Test
    fun zone_ids_are_unpacked_into_readable_city_names() {
        val hoChiMinh = catalog.cities.firstOrNull { it.zoneId == "Asia/Ho_Chi_Minh" }
        assertTrue("Asia/Ho_Chi_Minh should be present", hoChiMinh != null)
        assertEquals("Ho Chi Minh", hoChiMinh!!.cityName)
    }

    @Test
    fun technical_zones_are_excluded_from_the_catalogue() {
        // Etc/GMT+5 and the legacy SystemV set are not places and would read as nonsense rows.
        assertTrue(catalog.cities.none { it.zoneId.startsWith("Etc/") })
        assertTrue(catalog.cities.none { it.zoneId.startsWith("SystemV/") })
        // Single-word aliases like "Japan" and "UTC" are not cities either.
        assertTrue(catalog.cities.none { !it.zoneId.contains('/') })
    }

    @Test
    fun search_finds_a_city_by_name() {
        val results = catalog.search("dubai")
        assertTrue(results.any { it.zoneId == dubai })
    }

    @Test
    fun search_is_case_insensitive() {
        assertEquals(
            catalog.search("LONDON").map { it.zoneId },
            catalog.search("london").map { it.zoneId },
        )
    }

    @Test
    fun search_ranks_prefix_matches_first() {
        // Someone typing "lon" wants London before Colombo.
        val results = catalog.search("lon")
        val londonIndex = results.indexOfFirst { it.zoneId == london }
        val colomboIndex = results.indexOfFirst { it.zoneId == "Asia/Colombo" }
        assertTrue("London should be found", londonIndex >= 0)
        if (colomboIndex >= 0) {
            assertTrue("prefix match should rank first", londonIndex < colomboIndex)
        }
    }

    @Test
    fun search_matches_a_zone_id_too() {
        assertTrue(catalog.search("Asia/Kolkata").any { it.zoneId == kolkata })
    }

    @Test
    fun an_empty_query_returns_the_head_of_the_catalogue_rather_than_nothing() {
        assertTrue(catalog.search("").isNotEmpty())
    }

    @Test
    fun a_query_matching_nothing_returns_empty() {
        assertTrue(catalog.search("zzzzzznotacity").isEmpty())
    }

    @Test
    fun cities_are_sorted_by_name() {
        val names = catalog.cities.map { it.cityName.lowercase(Locale.UK) }
        assertEquals(names.sorted(), names)
    }
}
