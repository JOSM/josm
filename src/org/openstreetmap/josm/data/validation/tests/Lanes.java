package org.openstreetmap.josm.data.validation.tests;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Predicates;
import org.openstreetmap.josm.tools.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import static org.openstreetmap.josm.tools.I18n.tr;

public class Lanes extends Test.TagTest {

    public Lanes() {
        super(tr("Lane tags"));
    }

    static int getLanesCount(String value) {
        return value.isEmpty() ? 0 : value.split("\\|").length;
    }

    protected void checkEqualNumberOfLanes(final OsmPrimitive p, Pattern keyPattern, String message) {
        final Collection<String> keysForPattern = Utils.filter(p.keySet(), Predicates.stringContainsPattern(keyPattern));
        if (keysForPattern.size() < 2) {
            // nothing to check
            return;
        }
        final Collection<Integer> lanesCount = Utils.transform(keysForPattern, new Utils.Function<String, Integer>() {
            @Override
            public Integer apply(String key) {
                return getLanesCount(p.get(key));
            }
        });
        // if not all numbers are the same
        if (new HashSet<Integer>(lanesCount).size() > 1) {
            errors.add(new TestError(this, Severity.WARNING, message, 3100, p));
        }
    }

    @Override
    public void check(OsmPrimitive p) {
        checkEqualNumberOfLanes(p, Pattern.compile(":lanes$"), tr("Number of lane dependent values inconsistent"));
        checkEqualNumberOfLanes(p, Pattern.compile(":lanes:forward"), tr("Number of lane dependent values inconsistent in forward direction"));
        checkEqualNumberOfLanes(p, Pattern.compile(":lanes:backward"), tr("Number of lane dependent values inconsistent in backward direction"));
    }
}
