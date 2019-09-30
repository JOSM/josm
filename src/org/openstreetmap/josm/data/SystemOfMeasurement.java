// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A system of units used to express length and area measurements.
 * <p>
 * This class also manages one globally set system of measurement stored in the {@code ProjectionPreference}
 * @since 3406 (creation)
 * @since 6992 (extraction in this package)
 */
public class SystemOfMeasurement {

    /**
     * Interface to notify listeners of the change of the system of measurement.
     * @since 8554
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface SoMChangeListener {
        /**
         * The current SoM has changed.
         * @param oldSoM The old system of measurement
         * @param newSoM The new (current) system of measurement
         */
        void systemOfMeasurementChanged(String oldSoM, String newSoM);
    }

    /**
     * Metric system (international standard).
     * @since 3406
     */
    public static final SystemOfMeasurement METRIC = new SystemOfMeasurement(marktr("Metric"), 1, "m", 1000, "km", "km/h", 3.6, 10_000, "ha");

    /**
     * Chinese system.
     * See <a href="https://en.wikipedia.org/wiki/Chinese_units_of_measurement#Chinese_length_units_effective_in_1930">length units</a>,
     * <a href="https://en.wikipedia.org/wiki/Chinese_units_of_measurement#Chinese_area_units_effective_in_1930">area units</a>
     * @since 3406
     */
    public static final SystemOfMeasurement CHINESE = new SystemOfMeasurement(marktr("Chinese"),
            1.0/3.0, "\u5e02\u5c3a" /* chi */, 500, "\u5e02\u91cc" /* li */, "km/h", 3.6, 666.0 + 2.0/3.0, "\u4ea9" /* mu */);

    /**
     * Imperial system (British Commonwealth and former British Empire).
     * @since 3406
     */
    public static final SystemOfMeasurement IMPERIAL = new SystemOfMeasurement(marktr("Imperial"),
            0.3048, "ft", 1609.344, "mi", "mph", 2.23694, 4046.86, "ac");

    /**
     * Nautical mile system (navigation, polar exploration).
     * @since 5549
     */
    public static final SystemOfMeasurement NAUTICAL_MILE = new SystemOfMeasurement(marktr("Nautical Mile"),
            185.2, "kbl", 1852, "NM", "kn", 1.94384);

    /**
     * Known systems of measurement.
     * @since 3406
     */
    public static final Map<String, SystemOfMeasurement> ALL_SYSTEMS = Stream.of(METRIC, CHINESE, IMPERIAL, NAUTICAL_MILE)
            .collect(Collectors.toMap(SystemOfMeasurement::getName, Function.identity()));

    /**
     * Preferences entry for system of measurement.
     * @since 12674 (moved from ProjectionPreference)
     */
    public static final StringProperty PROP_SYSTEM_OF_MEASUREMENT = new StringProperty("system_of_measurement", getDefault().getName());

    private static final CopyOnWriteArrayList<SoMChangeListener> somChangeListeners = new CopyOnWriteArrayList<>();

    /**
     * Removes a global SoM change listener.
     *
     * @param listener the listener. Ignored if null or already absent
     * @since 8554
     */
    public static void removeSoMChangeListener(SoMChangeListener listener) {
        somChangeListeners.remove(listener);
    }

    /**
     * Adds a SoM change listener.
     *
     * @param listener the listener. Ignored if null or already registered.
     * @since 8554
     */
    public static void addSoMChangeListener(SoMChangeListener listener) {
        if (listener != null) {
            somChangeListeners.addIfAbsent(listener);
        }
    }

    protected static void fireSoMChanged(String oldSoM, String newSoM) {
        for (SoMChangeListener l : somChangeListeners) {
            l.systemOfMeasurementChanged(oldSoM, newSoM);
        }
    }

    /**
     * Returns the current global system of measurement.
     * @return The current system of measurement (metric system by default).
     * @since 8554
     */
    public static SystemOfMeasurement getSystemOfMeasurement() {
        return Optional.ofNullable(SystemOfMeasurement.ALL_SYSTEMS.get(PROP_SYSTEM_OF_MEASUREMENT.get()))
                .orElse(SystemOfMeasurement.METRIC);
    }

    /**
     * Sets the current global system of measurement.
     * @param somKey The system of measurement key. Must be defined in {@link SystemOfMeasurement#ALL_SYSTEMS}.
     * @throws IllegalArgumentException if {@code somKey} is not known
     * @since 8554
     */
    public static void setSystemOfMeasurement(String somKey) {
        if (!SystemOfMeasurement.ALL_SYSTEMS.containsKey(somKey)) {
            throw new IllegalArgumentException("Invalid system of measurement: "+somKey);
        }
        String oldKey = PROP_SYSTEM_OF_MEASUREMENT.get();
        if (PROP_SYSTEM_OF_MEASUREMENT.put(somKey)) {
            fireSoMChanged(oldKey, somKey);
        }
    }

    /** Translated name of this system of measurement. */
    private final String name;
    /** First value, in meters, used to translate unit according to above formula. */
    public final double aValue;
    /** Second value, in meters, used to translate unit according to above formula. */
    public final double bValue;
    /** First unit used to format text. */
    public final String aName;
    /** Second unit used to format text. */
    public final String bName;
    /** Speed value for the most common speed symbol, in meters per second
     *  @since 10175 */
    public final double speedValue;
    /** Most common speed symbol (kmh/h, mph, kn, etc.)
     *  @since 10175 */
    public final String speedName;
    /** Specific optional area value, in squared meters, between {@code aValue*aValue} and {@code bValue*bValue}. Set to {@code -1} if not used.
     *  @since 5870 */
    public final double areaCustomValue;
    /** Specific optional area unit. Set to {@code null} if not used.
     *  @since 5870 */
    public final String areaCustomName;

    /**
     * System of measurement. Currently covers only length (and area) units.
     *
     * If a quantity x is given in m (x_m) and in unit a (x_a) then it translates as
     * x_a == x_m / aValue
     *
     * @param name Translated name of this system of measurement
     * @param aValue First value, in meters, used to translate unit according to above formula.
     * @param aName First unit used to format text.
     * @param bValue Second value, in meters, used to translate unit according to above formula.
     * @param bName Second unit used to format text.
     * @param speedName the most common speed symbol (kmh/h, mph, kn, etc.)
     * @param speedValue the speed value for the most common speed symbol, for 1 meter per second
     * @since 15395
     */
    public SystemOfMeasurement(String name, double aValue, String aName, double bValue, String bName, String speedName, double speedValue) {
        this(name, aValue, aName, bValue, bName, speedName, speedValue, -1, null);
    }

    /**
     * System of measurement. Currently covers only length (and area) units.
     *
     * If a quantity x is given in m (x_m) and in unit a (x_a) then it translates as
     * x_a == x_m / aValue
     *
     * @param name Translated name of this system of measurement
     * @param aValue First value, in meters, used to translate unit according to above formula.
     * @param aName First unit used to format text.
     * @param bValue Second value, in meters, used to translate unit according to above formula.
     * @param bName Second unit used to format text.
     * @param speedName the most common speed symbol (kmh/h, mph, kn, etc.)
     * @param speedValue the speed value for the most common speed symbol, for 1 meter per second
     * @param areaCustomValue Specific optional area value, in squared meters, between {@code aValue*aValue} and {@code bValue*bValue}.
     *                        Set to {@code -1} if not used.
     * @param areaCustomName Specific optional area unit. Set to {@code null} if not used.
     *
     * @since 15395
     */
    public SystemOfMeasurement(String name, double aValue, String aName, double bValue, String bName, String speedName, double speedValue,
            double areaCustomValue, String areaCustomName) {
        this.name = name;
        this.aValue = aValue;
        this.aName = aName;
        this.bValue = bValue;
        this.bName = bName;
        this.speedValue = speedValue;
        this.speedName = speedName;
        this.areaCustomValue = areaCustomValue;
        this.areaCustomName = areaCustomName;
    }

    /**
     * Returns the text describing the given distance in this system of measurement.
     * @param dist The distance in metres
     * @return The text describing the given distance in this system of measurement.
     */
    public String getDistText(double dist) {
        return getDistText(dist, null, 0.01);
    }

    /**
     * Returns the text describing the given distance in this system of measurement.
     * @param dist The distance in metres
     * @param format A {@link NumberFormat} to format the area value
     * @param threshold Values lower than this {@code threshold} are displayed as {@code "< [threshold]"}
     * @return The text describing the given distance in this system of measurement.
     * @since 6422
     */
    public String getDistText(final double dist, final NumberFormat format, final double threshold) {
        double a = dist / aValue;
        if (a > bValue / aValue && !Config.getPref().getBoolean("system_of_measurement.use_only_lower_unit", false))
            return formatText(dist / bValue, bName, format);
        else if (a < threshold)
            return "< " + formatText(threshold, aName, format);
        else
            return formatText(a, aName, format);
    }

    /**
     * Returns the text describing the given area in this system of measurement.
     * @param area The area in square metres
     * @return The text describing the given area in this system of measurement.
     * @since 5560
     */
    public String getAreaText(double area) {
        return getAreaText(area, null, 0.01);
    }

    /**
     * Returns the text describing the given area in this system of measurement.
     * @param area The area in square metres
     * @param format A {@link NumberFormat} to format the area value
     * @param threshold Values lower than this {@code threshold} are displayed as {@code "< [threshold]"}
     * @return The text describing the given area in this system of measurement.
     * @since 6422
     */
    public String getAreaText(final double area, final NumberFormat format, final double threshold) {
        double a = area / (aValue*aValue);
        boolean lowerOnly = Config.getPref().getBoolean("system_of_measurement.use_only_lower_unit", false);
        boolean customAreaOnly = Config.getPref().getBoolean("system_of_measurement.use_only_custom_area_unit", false);
        if ((!lowerOnly && areaCustomValue > 0 && a > areaCustomValue / (aValue*aValue)
                && a < (bValue*bValue) / (aValue*aValue)) || customAreaOnly)
            return formatText(area / areaCustomValue, areaCustomName, format);
        else if (!lowerOnly && a >= (bValue*bValue) / (aValue*aValue))
            return formatText(area / (bValue * bValue), bName + '\u00b2', format);
        else if (a < threshold)
            return "< " + formatText(threshold, aName + '\u00b2', format);
        else
            return formatText(a, aName + '\u00b2', format);
    }

    /**
     * Returns the translated name of this system of measurement.
     * @return the translated name of this system of measurement
     * @since 15395
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the default system of measurement for the current country.
     * @return the default system of measurement for the current country
     * @since 15395
     */
    public static SystemOfMeasurement getDefault() {
        switch (Locale.getDefault().getCountry()) {
            case "US":
                // https://en.wikipedia.org/wiki/Metrication_in_the_United_States#Current_use
                // Imperial units still used in transportation and Earth sciences
                return IMPERIAL;
            default:
                return METRIC;
        }
    }

    private static String formatText(double v, String unit, NumberFormat format) {
        if (format != null) {
            return format.format(v) + ' ' + unit;
        }
        return String.format(Locale.US, v < 9.999999 ? "%.2f %s" : "%.1f %s", v, unit);
    }
}
