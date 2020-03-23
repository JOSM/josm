// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Test that validates {@code lane:} tags.
 * @since 6592
 */
public class Lanes extends Test.TagTest {

    private static final String[] BLACKLIST = {
        "source:lanes",
        "note:lanes",
        "proposed:lanes",
        "source:proposed:lanes",
        "piste:lanes",
    };

    /**
     * Constructs a new {@code Lanes} test.
     */
    public Lanes() {
        super(tr("Lane tags"), tr("Test that validates ''lane:'' tags."));
    }

    static int getLanesCount(String value) {
        return value.isEmpty() ? 0 : value.replaceAll("[^|]", "").length() + 1;
    }

    protected void checkNumberOfLanesByKey(final OsmPrimitive p, String lanesKey, String message) {
        final Set<Integer> lanesCount =
                p.keySet().stream()
                .filter(x -> x.endsWith(":" + lanesKey))
                .filter(x -> !Arrays.asList(BLACKLIST).contains(x))
                .map(key -> getLanesCount(p.get(key)))
                .collect(Collectors.toSet());

        if (lanesCount.size() > 1) {
            // if not all numbers are the same
            errors.add(TestError.builder(this, Severity.WARNING, 3100)
                    .message(message)
                    .primitives(p)
                    .build());
        } else if (lanesCount.size() == 1 && p.hasKey(lanesKey)) {
            // ensure that lanes <= *:lanes
            try {
                if (Integer.parseInt(p.get(lanesKey)) > lanesCount.iterator().next()) {
                    errors.add(TestError.builder(this, Severity.WARNING, 3100)
                            .message(tr("Number of {0} greater than {1}", lanesKey, "*:" + lanesKey))
                            .primitives(p)
                            .build());
                }
            } catch (NumberFormatException ignore) {
                Logging.debug(ignore.getMessage());
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
            errors.add(TestError.builder(this, Severity.WARNING, 3101)
                    .message(tr("Number of {0} greater than {1}", tr("{0}+{1}", "lanes:forward", "lanes:backward"), "lanes"))
                    .primitives(p)
                    .build());
        }
        } catch (NumberFormatException ignore) {
            Logging.debug(ignore.getMessage());
        }
    }

    @Override
    public void check(OsmPrimitive p) {
        checkNumberOfLanesByKey(p, "lanes", tr("Number of lane dependent values inconsistent"));
        checkNumberOfLanesByKey(p, "lanes:forward", tr("Number of lane dependent values inconsistent in forward direction"));
        checkNumberOfLanesByKey(p, "lanes:backward", tr("Number of lane dependent values inconsistent in backward direction"));
        checkNumberOfLanes(p);
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        return p.isTagged() && super.isPrimitiveUsable(p);
    }
}
