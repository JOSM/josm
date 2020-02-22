// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.tools.Logging;

/**
 * An auto filter rule determines how auto filter can be built from visible map data.
 * Several rules can be registered, but only one rule is active at the same time.
 * Rules are identified by the OSM key on which they apply.
 * The dynamic values discovering operates only below a certain zoom level, for performance reasons.
 * @since 12400
 */
public class AutoFilterRule {

    /**
     * Property to determine if the auto filter should assume sensible defaults for values (such as layer=1 for bridge=yes).
     */
    private static final BooleanProperty PROP_AUTO_FILTER_DEFAULTS = new BooleanProperty("auto.filter.defaults", true);

    private final String key;

    private final int minZoomLevel;

    private Function<OsmPrimitive, IntStream> defaultValueSupplier = p -> IntStream.empty();

    private ToIntFunction<String> valueExtractor = Integer::parseInt;

    private IntFunction<String> valueFormatter = Integer::toString;

    /**
     * Constructs a new {@code AutoFilterRule}.
     * @param key the OSM key on which the rule applies
     * @param minZoomLevel the minimum zoom level at which the rule applies
     */
    public AutoFilterRule(String key, int minZoomLevel) {
        this.key = key;
        this.minZoomLevel = minZoomLevel;
    }

    /**
     * Returns the OSM key on which the rule applies.
     * @return the OSM key on which the rule applies
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the minimum zoom level at which the rule applies.
     * @return the minimum zoom level at which the rule applies
     */
    public int getMinZoomLevel() {
        return minZoomLevel;
    }

    /**
     * Formats the numeric value
     * @param value the numeric value to format
     * @return the formatted value
     */
    public String formatValue(int value) {
        return valueFormatter.apply(value);
    }

    /**
     * Sets a OSM value formatter that defines the associated button label.
     * @param valueFormatter OSM value formatter. Cannot be null
     * @return {@code this}
     * @throws NullPointerException if {@code valueFormatter} is null
     */
    public AutoFilterRule setValueFormatter(IntFunction<String> valueFormatter) {
        this.valueFormatter = Objects.requireNonNull(valueFormatter);
        return this;
    }

    /**
     * Sets the function which yields default values for the given OSM primitive.
     * This function is invoked if the primitive does not have this {@linkplain #getKey() key}.
     * @param defaultValueSupplier the function which yields default values for the given OSM primitive
     * @return {@code this}
     * @throws NullPointerException if {@code defaultValueSupplier} is null
     */
    public AutoFilterRule setDefaultValueSupplier(Function<OsmPrimitive, IntStream> defaultValueSupplier) {
        this.defaultValueSupplier = Objects.requireNonNull(defaultValueSupplier);
        return this;
    }

    /**
     * Sets the function which extracts a numeric value from an OSM value
     * @param valueExtractor the function which extracts a numeric value from an OSM value
     * @return {@code this}
     * @throws NullPointerException if {@code valueExtractor} is null
     */
    public AutoFilterRule setValueExtractor(ToIntFunction<String> valueExtractor) {
        this.valueExtractor = Objects.requireNonNull(valueExtractor);
        return this;
    }

    /**
     * Returns the numeric values for the given OSM primitive
     * @param osm the primitive
     * @return a stream of numeric values
     */
    public IntStream getTagValuesForPrimitive(OsmPrimitive osm) {
        String value = osm.get(key);
        if (value != null) {
            Pattern p = Pattern.compile("(-?[0-9]+)-(-?[0-9]+)");
            return OsmUtils.splitMultipleValues(value).flatMapToInt(v -> {
                Matcher m = p.matcher(v);
                if (m.matches()) {
                    int a = valueExtractor.applyAsInt(m.group(1));
                    int b = valueExtractor.applyAsInt(m.group(2));
                    return IntStream.rangeClosed(Math.min(a, b), Math.max(a, b));
                } else {
                    try {
                        return IntStream.of(valueExtractor.applyAsInt(v));
                    } catch (NumberFormatException e) {
                        Logging.trace(e);
                        return IntStream.empty();
                    }
                }
            });
        }
        return PROP_AUTO_FILTER_DEFAULTS.get() ? defaultValueSupplier.apply(osm) : IntStream.empty();
    }

    /**
     * Returns the default list of auto filter rules. Plugins can extend the list by registering additional rules.
     * @return the default list of auto filter rules
     */
    public static AutoFilterRule[] defaultRules() {
        return new AutoFilterRule[]{
            new AutoFilterRule("level", 17)
                // #17109, support values like 0.5 or 1.5 - level values are multiplied by 2 when parsing, values are divided by 2 for formatting
                .setValueExtractor(s -> (int) (Double.parseDouble(s) * 2.))
                .setValueFormatter(v -> DecimalFormat.getInstance(Locale.ROOT).format(v / 2.)),
            new AutoFilterRule("layer", 16)
                    .setDefaultValueSupplier(AutoFilterRule::defaultLayer),
            new AutoFilterRule("maxspeed", 16)
                    .setValueExtractor(s -> Integer.parseInt(s.replace(" mph", ""))),
            new AutoFilterRule("voltage", 5)
                    .setValueFormatter(s -> s % 1000 == 0 ? (s / 1000) + "kV" : s + "V"),
            new AutoFilterRule("building:levels", 17),
            new AutoFilterRule("gauge", 5),
            new AutoFilterRule("frequency", 5),
            new AutoFilterRule("incline", 13)
                    .setValueExtractor(s -> Integer.parseInt(s.replaceAll("%$", "")))
                    .setValueFormatter(v -> v + "\u2009%"),
            new AutoFilterRule("lanes", 13),
            new AutoFilterRule("admin_level", 11)
        };
    }

    /**
     * Returns the default auto filter rule for the given key
     * @param key the OSM key
     * @return default auto filter rule for the given key
     */
    static Optional<AutoFilterRule> getDefaultRule(String key) {
        return Arrays.stream(AutoFilterRule.defaultRules())
                .filter(r -> key.equals(r.getKey()))
                .findFirst();
    }

    private static IntStream defaultLayer(OsmPrimitive osm) {
        // assume sensible defaults, see #17496
        if (osm.hasTag("bridge") || osm.hasTag("power", "line") || osm.hasTag("location", "overhead")) {
            return IntStream.of(1);
        } else if (osm.isKeyTrue("tunnel") || osm.hasTag("tunnel", "culvert") || osm.hasTag("location", "underground")) {
            return IntStream.of(-1);
        } else if (osm.hasTag("tunnel", "building_passage") || osm.hasKey("highway", "railway", "waterway")) {
            return IntStream.of(0);
        } else {
            return IntStream.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoFilterRule that = (AutoFilterRule) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return key + '[' + minZoomLevel + ']';
    }
}
