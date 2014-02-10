// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Predicates;
import org.openstreetmap.josm.tools.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Test that validates {@code lane:} tags.
 * @since 6592
 */
public class Lanes extends Test.TagTest {

    /**
     * Constructs a new {@code Lanes} test.
     */
    public Lanes() {
        super(tr("Lane tags"));
    }

    static int getLanesCount(String value) {
        return value.isEmpty() ? 0 : value.replaceAll("[^|]", "").length() + 1;
    }

    protected void checkNumberOfLanesByKey(final OsmPrimitive p, String lanesKey, String message) {
        final Collection<String> keysForPattern = Utils.filter(p.keySet(),
                Predicates.stringContainsPattern(Pattern.compile(":" + lanesKey + "$")));
        if (keysForPattern.size() < 1) {
            // nothing to check
            return;
        }
        final Set<Integer> lanesCount = new HashSet<Integer>(Utils.transform(keysForPattern, new Utils.Function<String, Integer>() {
            @Override
            public Integer apply(String key) {
                return getLanesCount(p.get(key));
            }
        }));
        if (lanesCount.size() > 1) {
            // if not all numbers are the same
            errors.add(new TestError(this, Severity.WARNING, message, 3100, p));
        } else if (lanesCount.size() == 1 && p.hasKey(lanesKey)) {
            // ensure that lanes <= *:lanes
            try {
                if (Integer.parseInt(p.get(lanesKey)) > lanesCount.iterator().next()) {
                    errors.add(new TestError(this, Severity.WARNING, tr("Number of {0} greater than {1}", lanesKey, "*:" + lanesKey), 3100, p));
                }
            } catch (NumberFormatException ignore) {
                Main.debug(ignore.getMessage());
            }
        }
    }

    protected void checkNumberOfLanes(final OsmPrimitive p) {
        final String lanes = p.get("lanes");
        if (lanes == null) return;
        final String forward = Utils.firstNonNull(p.get("lanes:forward"), "0");
        final String backward = Utils.firstNonNull(p.get("lanes:backward"), "0");
        try {
        if (Integer.parseInt(lanes) < Integer.parseInt(forward) + Integer.parseInt(backward)) {
            errors.add(new TestError(this, Severity.WARNING,
                    tr("Number of {0} greater than {1}", tr("{0}+{1}", "lanes:forward", "lanes:backward"), "lanes"), 3101, p));
        }
        } catch (NumberFormatException ignore) {
            Main.debug(ignore.getMessage());
        }
    }

    @Override
    public void check(OsmPrimitive p) {
        checkNumberOfLanesByKey(p, "lanes", tr("Number of lane dependent values inconsistent"));
        checkNumberOfLanesByKey(p, "lanes:forward", tr("Number of lane dependent values inconsistent in forward direction"));
        checkNumberOfLanesByKey(p, "lanes:backward", tr("Number of lane dependent values inconsistent in backward direction"));
        checkNumberOfLanes(p);
    }
}
