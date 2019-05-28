// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;

/**
 * Check area type ways for errors
 *
 * @author stoecker
 * @since 3669
 */
public class UnclosedWays extends Test {

    /**
     * Constructs a new {@code UnclosedWays} test.
     */
    public UnclosedWays() {
        super(tr("Unclosed Ways"), tr("This tests if ways which should be circular are closed."));
    }

    /**
     * A check performed by UnclosedWays test.
     * @since 6390
     */
    private static class UnclosedWaysCheck {
        /** The unique numeric code for this check */
        public final int code;
        /** The OSM key checked */
        public final String key;
        /** The English message */
        private final String engMessage;
        /** The special values, to be ignored if ignore is set to true; to be considered only if ignore is set to false */
        private final Set<String> specialValues;
        /** The boolean indicating if special values must be ignored or considered only */
        private final boolean ignore;

        /**
         * Constructs a new {@code UnclosedWaysCheck}.
         * @param code The unique numeric code for this check
         * @param key The OSM key checked
         * @param engMessage The English message
         */
        UnclosedWaysCheck(int code, String key, String engMessage) {
            this(code, key, engMessage, Collections.<String>emptySet());
        }

        /**
         * Constructs a new {@code UnclosedWaysCheck}.
         * @param code The unique numeric code for this check
         * @param key The OSM key checked
         * @param engMessage The English message
         * @param ignoredValues The ignored values.
         */
        UnclosedWaysCheck(int code, String key, String engMessage, Set<String> ignoredValues) {
            this(code, key, engMessage, ignoredValues, true);
        }

        /**
         * Constructs a new {@code UnclosedWaysCheck}.
         * @param code The unique numeric code for this check
         * @param key The OSM key checked
         * @param engMessage The English message
         * @param specialValues The special values, to be ignored if ignore is set to true; to be considered only if ignore is set to false
         * @param ignore indicates if special values must be ignored or considered only
         */
        UnclosedWaysCheck(int code, String key, String engMessage, Set<String> specialValues, boolean ignore) {
            this.code = code;
            this.key = key;
            this.engMessage = engMessage;
            this.specialValues = specialValues;
            this.ignore = ignore;
        }

        /**
         * Returns the test error of the given way, if any.
         * @param w The way to check
         * @param test parent test
         * @return The test error if the way is erroneous, {@code null} otherwise
         */
        public final TestError getTestError(Way w, UnclosedWays test) {
            String value = w.get(key);
            if (isValueErroneous(value)) {
                return TestError.builder(test, Severity.WARNING, code)
                        .message(tr("Unclosed way"), engMessage, engMessage.contains("{0}") ? new Object[]{value} : new Object[]{})
                        .primitives(w)
                        .highlight(Arrays.asList(w.firstNode(), w.lastNode()))
                        .build();
            }
            return null;
        }

        protected boolean isValueErroneous(String value) {
            return value != null && ignore != specialValues.contains(value);
        }
    }

    /**
     * A check performed by UnclosedWays test where the key is treated as boolean.
     * @since 6390
     */
    private static final class UnclosedWaysBooleanCheck extends UnclosedWaysCheck {

        /**
         * Constructs a new {@code UnclosedWaysBooleanCheck}.
         * @param code The unique numeric code for this check
         * @param key The OSM key checked
         * @param engMessage The English message
         */
        UnclosedWaysBooleanCheck(int code, String key, String engMessage) {
            super(code, key, engMessage);
        }

        @Override
        protected boolean isValueErroneous(String value) {
            Boolean btest = OsmUtils.getOsmBoolean(value);
            // Not a strict boolean comparison to handle building=house like a building=yes
            return (btest != null && btest) || (btest == null && value != null);
        }
    }

    private static final UnclosedWaysCheck[] checks = {
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        // list contains natural tag allowed on unclosed ways as well as those only allowed on nodes to avoid
        // duplicate warnings
        new UnclosedWaysCheck(1101, "natural", marktr("natural type {0}"),
            new HashSet<>(Arrays.asList("arete", "bay", "cave", "cliff", "coastline", "gorge", "gully", "peak",
                            "ridge", "saddle", "strait", "tree", "tree_row", "valley", "volcano"))),

        new UnclosedWaysCheck(1102, "landuse", marktr("landuse type {0}")),
        new UnclosedWaysCheck(1103, "amenities", marktr("amenities type {0}")),
        new UnclosedWaysCheck(1104, "sport",     marktr("sport type {0}"),
                new HashSet<>(Arrays.asList("water_slide", "climbing", "skiing", "toboggan", "bobsleigh"))),
        new UnclosedWaysCheck(1105, "tourism",   marktr("tourism type {0}"),
                new HashSet<>(Arrays.asList("attraction", "artwork"))),
        new UnclosedWaysCheck(1106, "shop",      marktr("shop type {0}")),
        new UnclosedWaysCheck(1107, "leisure",   marktr("leisure type {0}"),
                new HashSet<>(Arrays.asList("track", "slipway"))),
        new UnclosedWaysCheck(1108, "waterway",  marktr("waterway type {0}"),
                new HashSet<>(Arrays.asList("riverbank")), false),
        new UnclosedWaysCheck(1109, "boundary", marktr("boundary type {0}")),
        new UnclosedWaysCheck(1110, "area:highway", marktr("area:highway type {0}")),
        new UnclosedWaysBooleanCheck(1120, "building", marktr("building")),
        new UnclosedWaysBooleanCheck(1130, "area",     marktr("area")),
        // 1131: Area style way is not closed
        // CHECKSTYLE.ON: SingleSpaceSeparator
    };

    /**
     * Returns the set of checked OSM keys.
     * @return The set of checked OSM keys.
     * @since 6390
     */
    public Set<String> getCheckedKeys() {
        Set<String> keys = new HashSet<>();
        for (UnclosedWaysCheck c : checks) {
            keys.add(c.key);
        }
        return keys;
    }

    @Override
    public void visit(Way w) {

        if (!w.isUsable() || w.isArea())
            return;

        for (OsmPrimitive parent: w.getReferrers()) {
            if (parent instanceof Relation && ((Relation) parent).isMultipolygon())
                return;
        }

        for (UnclosedWaysCheck c : checks) {
            TestError error = c.getTestError(w, this);
            if (error != null) {
                errors.add(error);
                return;
            }
        }
        // code 1131: other area style ways
        if (ElemStyles.hasOnlyAreaElements(w) && !w.getNodes().isEmpty()) {
            errors.add(TestError.builder(this, Severity.WARNING, 1131)
                    .message(tr("Unclosed way"), marktr("Area style way is not closed"), new Object())
                    .primitives(w)
                    .highlight(Arrays.asList(w.firstNode(), w.lastNode()))
                    .build());
        }
    }
}
