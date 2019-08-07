// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * An auto filter rule determines how auto filter can be built from visible map data.
 * Several rules can be registered, but only one rule is active at the same time.
 * Rules are identified by the OSM key on which they apply.
 * The dynamic values discovering operates only below a certain zoom level, for performance reasons.
 * @since 12400
 */
public class AutoFilterRule {

    private final String key;

    private final int minZoomLevel;

    private UnaryOperator<String> valueFormatter = s -> s;

    private Comparator<String> valueComparator = Comparator.comparingInt(s -> Integer.parseInt(valueFormatter.apply(s)));

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
     * Returns the OSM value formatter that defines the associated button label.
     * @return the OSM value formatter that defines the associated button label (identity by default)
     */
    public Function<String, String> getValueFormatter() {
        return valueFormatter;
    }

    /**
     * Sets a OSM value formatter that defines the associated button label.
     * @param valueFormatter OSM value formatter. Cannot be null
     * @return {@code this}
     * @throws NullPointerException if {@code valueFormatter} is null
     */
    public AutoFilterRule setValueFormatter(UnaryOperator<String> valueFormatter) {
        this.valueFormatter = Objects.requireNonNull(valueFormatter);
        return this;
    }

    /**
     * Returns the OSM value comparator used to order the buttons.
     * @return the OSM value comparator
     */
    public Comparator<String> getValueComparator() {
        return valueComparator;
    }

    /**
     * Sets the OSM value comparator used to order the buttons.
     * @param valueComparator the OSM value comparator
     * @return {@code this}
     * @throws NullPointerException if {@code valueComparator} is null
     */
    public AutoFilterRule setValueComparator(Comparator<String> valueComparator) {
        this.valueComparator = valueComparator;
        return this;
    }

    /**
     * Returns the default list of auto filter rules. Plugins can extend the list by registering additional rules.
     * @return the default list of auto filter rules
     */
    public static AutoFilterRule[] defaultRules() {
        return new AutoFilterRule[] {
            new AutoFilterRule("level", 17),
            new AutoFilterRule("layer", 16),
            new AutoFilterRule("maxspeed", 16)
                .setValueFormatter(s -> s.replace(" mph", "")),
            new AutoFilterRule("voltage", 5)
                .setValueFormatter(s -> s.replaceAll("000$", "k") + 'V')
                .setValueComparator(Comparator.comparingInt(Integer::parseInt))
        };
    }

    @Override
    public String toString() {
        return key + '[' + minZoomLevel + ']';
    }
}
