// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks and corrects deprecated tags.
 * @since 4442
 */
public class DeprecatedTags extends Test {

    private List<DeprecationCheck> checks = new LinkedList<DeprecationCheck>();

    /**
     * Constructs a new {@code DeprecatedTags} test.
     */
    public DeprecatedTags() {
        super(tr("Deprecated Tags"), tr("Checks and corrects deprecated tags."));
        checks.add(new DeprecationCheck(2101).
                testAndRemove("barrier", "wire_fence").
                add("barrier", "fence").
                add("fence_type", "chain_link"));
        checks.add(new DeprecationCheck(2102).
                testAndRemove("barrier", "wood_fence").
                add("barrier", "fence").
                add("fence_type", "wood"));
        checks.add(new DeprecationCheck(2103).
                testAndRemove("highway", "ford").
                add("ford", "yes"));
        // from http://wiki.openstreetmap.org/wiki/Deprecated_features
        checks.add(new DeprecationCheck(2104).
                test("class").
                alternative("highway"));
        checks.add(new DeprecationCheck(2105).
                testAndRemove("highway", "stile").
                add("barrier", "stile"));
        checks.add(new DeprecationCheck(2106).
                testAndRemove("highway", "incline").
                add("highway", "road").
                add("incline", "up"));
        checks.add(new DeprecationCheck(2107).
                testAndRemove("highway", "incline_steep").
                add("highway", "road").
                add("incline", "up"));
        checks.add(new DeprecationCheck(2108).
                testAndRemove("highway", "unsurfaced").
                add("highway", "road").
                add("incline", "unpaved"));
        checks.add(new DeprecationCheck(2109).
                test("landuse", "wood").
                alternative("landuse", "forest").
                alternative("natural", "wood"));
        checks.add(new DeprecationCheck(2110).
                testAndRemove("natural", "marsh").
                add("natural", "wetland").
                add("wetland", "marsh"));
        checks.add(new DeprecationCheck(2111).
                test("highway", "byway"));
        checks.add(new DeprecationCheck(2112).
                test("power_source").
                alternative("generator:source"));
        checks.add(new DeprecationCheck(2113).
                test("power_rating").
                alternative("generator:output"));
        // from http://wiki.openstreetmap.org/wiki/Tag:shop=organic
        checks.add(new DeprecationCheck(2114).
                testAndRemove("shop", "organic").
                add("shop", "supermarket").
                add("organic", "only"));
        // from http://wiki.openstreetmap.org/wiki/Key:bicycle_parking
        checks.add(new DeprecationCheck(2115).
                testAndRemove("bicycle_parking", "sheffield").
                add("bicycle_parking", "stands"));
        // http://wiki.openstreetmap.org/wiki/Tag:emergency=phone
        checks.add(new DeprecationCheck(2116).
                testAndRemove("amenity", "emergency_phone").
                add("emergency", "phone"));
        // fix #8132 - http://wiki.openstreetmap.org/wiki/Tag:sport=gaelic_football
        checks.add(new DeprecationCheck(2117).
                testAndRemove("sport", "gaelic_football").
                add("sport", "gaelic_games"));
        // see #8847 / #8961 - http://wiki.openstreetmap.org/wiki/Tag:power=station
        checks.add(new DeprecationCheck(2118).
                test("power", "station").
                alternative("power", "plant").
                alternative("power", "sub_station"));
        checks.add(new DeprecationCheck(2119).
                testAndRemove("generator:method", "dam").
                add("generator:method", "water-storage"));
        checks.add(new DeprecationCheck(2120).
                testAndRemove("generator:method", "pumped-storage").
                add("generator:method", "water-pumped-storage"));
        checks.add(new DeprecationCheck(2121).
                testAndRemove("generator:method", "pumping").
                add("generator:method", "water-pumped-storage"));
        // see #8962 - http://wiki.openstreetmap.org/wiki/Key:fence_type
        checks.add(new DeprecationCheck(2122).
                test("fence_type", "chain").
                alternative("barrier", "chain").
                alternative("fence_type", "chain_link"));
        // see #9000 - http://wiki.openstreetmap.org/wiki/Key:entrance
        checks.add(new DeprecationCheck(2123).
                test("building", "entrance").
                alternative("entrance"));
        // see #9213 - Useless tag proposed in internal preset for years
        checks.add(new DeprecationCheck(2124).
                testAndRemove("board_type", "board"));
        // see #8434 - http://wiki.openstreetmap.org/wiki/Proposed_features/monitoring_station
        checks.add(new DeprecationCheck(2125).
                testAndRemove("man_made", "measurement_station").
                add("man_made", "monitoring_station"));
        checks.add(new DeprecationCheck(2126).
                testAndRemove("measurement", "water_level").
                add("monitoring:water_level", "yes"));
        checks.add(new DeprecationCheck(2127).
                testAndRemove("measurement", "weather").
                add("monitoring:weather", "yes"));
        checks.add(new DeprecationCheck(2128).
                testAndRemove("measurement", "seismic").
                add("monitoring:seismic_activity", "yes"));
        checks.add(new DeprecationCheck(2129).
                test("monitoring:river_level").
                alternative("monitoring:water_level"));
    }

    /**
     * Visiting call for primitives.
     * @param p The primitive to inspect.
     */
    public void visit(OsmPrimitive p) {
        for (DeprecationCheck check : checks) {
            if (check.matchesPrimitive(p)) {
                errors.add(new DeprecationError(p, check));
            }
        }
    }

    @Override
    public void visit(Node n) {
        visit((OsmPrimitive) n);
    }

    @Override
    public void visit(Way w) {
        visit((OsmPrimitive) w);
    }

    @Override
    public void visit(Relation r) {
        visit((OsmPrimitive) r);
    }

    private static class DeprecationCheck {

        private int code;
        private final List<Tag> test = new LinkedList<Tag>();
        private final List<Tag> change = new LinkedList<Tag>();
        private final List<Tag> alternatives = new LinkedList<Tag>();

        public DeprecationCheck(int code) {
            this.code = code;
        }

        DeprecationCheck test(String key, String value) {
            test.add(new Tag(key, value));
            return this;
        }

        DeprecationCheck test(String key) {
            return test(key, null);
        }

        DeprecationCheck add(String key, String value) {
            change.add(new Tag(key, value));
            return this;
        }

        DeprecationCheck remove(String key) {
            change.add(new Tag(key));
            return this;
        }

        DeprecationCheck testAndRemove(String key, String value) {
            return test(key, value).remove(key);
        }

        DeprecationCheck alternative(String key, String value) {
            alternatives.add(new Tag(key, value));
            return this;
        }

        DeprecationCheck alternative(String key) {
            return alternative(key, null);
        }

        boolean matchesPrimitive(OsmPrimitive p) {
            for (Tag tag : test) {
                String key = tag.getKey();
                String value = tag.getValue();
                if (value.isEmpty() && !p.hasKey(key))
                    return false;
                if (!value.isEmpty() && !value.equals(p.get(key)))
                    return false;
            }
            return true;
        }

        Command fixPrimitive(OsmPrimitive p) {
            Collection<Command> cmds = new LinkedList<Command>();
            for (Tag tag : change) {
                cmds.add(new ChangePropertyCommand(p, tag.getKey(), tag.getValue()));
            }
            if (test.size() == 1 && alternatives.size() == 1) {
                cmds.add(new ChangePropertyKeyCommand(p, test.get(0).getKey(), alternatives.get(0).getKey())); 
            }
            return new SequenceCommand(tr("Deprecation fix of {0}", Utils.join(", ", test)), cmds);
        }

        String getDescription() {
            if (alternatives.isEmpty())
                return tr("{0} is deprecated", Utils.join(", ", test));
            else
                return tr("{0} is deprecated, use {1} instead", Utils.join(", ", test), Utils.join(tr(" or "), alternatives));
        }
    }

    private class DeprecationError extends TestError {

        private OsmPrimitive p;
        private DeprecationCheck check;

        public DeprecationError(OsmPrimitive p, DeprecationCheck check) {
            super(DeprecatedTags.this, Severity.WARNING, check.getDescription(), check.code, p);
            this.p = p;
            this.check = check;
        }

        @Override
        public boolean isFixable() {
            return !check.change.isEmpty() || (check.test.size() == 1 && check.alternatives.size() == 1);
        }

        @Override
        public Command getFix() {
            return check.fixPrimitive(p);
        }
    }
}
