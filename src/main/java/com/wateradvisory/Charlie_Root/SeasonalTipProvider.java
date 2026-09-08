package com.wateradvisory.Charlie_Root;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;

/**
 * Supplies a short, seasonal water-saving tip that changes once per calendar day.
 *
 * <p>Hemisphere is inferred from the user's region string; the season is taken from
 * the current month (Southern-Hemisphere calendar, flipped for the Northern
 * Hemisphere). The tip for a given day is picked deterministically as
 * {@code dayOfYear % tipsForSeason.size()} so it rotates predictably rather than
 * randomly -- the same day always yields the same tip.</p>
 */
public class SeasonalTipProvider {

    public enum Season { SUMMER, AUTUMN, WINTER, SPRING }

    /** True when the configured region is in the Southern Hemisphere. */
    private final boolean isSouthernHemisphere;

    public SeasonalTipProvider() {
        this(null);
    }

    public SeasonalTipProvider(String userRegion) {
        this.isSouthernHemisphere = isSouthern(userRegion);
    }

    public boolean isSouthernHemisphere() {
        return isSouthernHemisphere;
    }

    /**
     * Region strings mentioning "AU", "Australia", "NZ" or "New Zealand" are treated as
     * Southern Hemisphere; everything else (and null) defaults to Northern Hemisphere.
     */
    static boolean isSouthern(String region) {
        if (region == null) {
            return false;
        }
        String r = region.toLowerCase(Locale.ROOT);
        return r.contains("au")
            || r.contains("australia")
            || r.contains("nz")
            || r.contains("new zealand");
    }

    /**
     * Today's tip for the given region.
     *
     * @param userRegion region string; may be null to use this provider's own hemisphere
     */
    public String getTipForToday(String userRegion) {
        boolean southern = (userRegion == null) ? isSouthernHemisphere : isSouthern(userRegion);
        return tipFor(LocalDate.now(), southern);
    }

    /** Deterministic tip for a specific date + hemisphere. Package-private so tests can pin the date. */
    String tipFor(LocalDate date, boolean southern) {
        List<String> tips = tipsForSeason(seasonFor(date.getMonth(), southern));
        int index = date.getDayOfYear() % tips.size();
        return tips.get(index);
    }

    /** Season for a month on the Southern-Hemisphere calendar, flipped for the Northern Hemisphere. */
    Season seasonFor(Month month, boolean southern) {
        Season southernSeason = switch (month) {
            case DECEMBER, JANUARY, FEBRUARY  -> Season.SUMMER;
            case MARCH, APRIL, MAY            -> Season.AUTUMN;
            case JUNE, JULY, AUGUST           -> Season.WINTER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> Season.SPRING;
        };
        return southern ? southernSeason : opposite(southernSeason);
    }

    private static Season opposite(Season season) {
        return switch (season) {
            case SUMMER -> Season.WINTER;
            case WINTER -> Season.SUMMER;
            case AUTUMN -> Season.SPRING;
            case SPRING -> Season.AUTUMN;
        };
    }

    /** At least three short (< 100 char), specific, actionable tips per season. */
    private static List<String> tipsForSeason(Season season) {
        return switch (season) {
            case SUMMER -> List.of(
                "Run sprinklers before 9am - midday watering loses up to 30% to evaporation.",
                "Check outdoor taps for drips - heat causes fittings to expand and leak.",
                "Mulch garden beds to reduce watering frequency by up to 50%.");
            case AUTUMN -> List.of(
                "Reduce irrigation as temperatures drop - lawns need less water now.",
                "Clear gutters before wet season so rainwater reaches your garden.",
                "Fix leaking taps before winter - a slow drip wastes 20,000 L/year.");
            case WINTER -> List.of(
                "Insulate exposed pipes to prevent frost cracks and hidden leaks.",
                "Take shorter warm showers to offset higher hot-water energy and water use.",
                "Check your toilet for silent leaks - place a dye tablet in the cistern.");
            case SPRING -> List.of(
                "Test irrigation systems before summer - one blocked head wastes thousands of litres.",
                "Plant drought-tolerant species now so they establish roots before summer heat.",
                "Install a rainwater tank now to be ready to collect winter rain runoff.");
        };
    }
}
