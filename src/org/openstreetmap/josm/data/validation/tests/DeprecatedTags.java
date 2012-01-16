package org.openstreetmap.josm.data.validation.tests;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Utils;

public class DeprecatedTags extends Test {

    private List<DeprecationCheck> checks = new LinkedList<DeprecationCheck>();

    public DeprecatedTags() {
        super(tr("Deprecated Tags"), tr("Checks and corrects deprecated tags."));
        checks.add(new DeprecationCheck(2101).
                testAndRemove("barrier", "wire_fence").
                add("barrier", "fence").
                add("fence_type", "chain"));
        checks.add(new DeprecationCheck(2102).
                testAndRemove("barrier", "wood_fence").
                add("barrier", "fence").
                add("fence_type", "wood"));
        checks.add(new DeprecationCheck(2103).
                testAndRemove("highway", "ford").
                add("highway", "road").
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
    }

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

    private class DeprecationCheck {

        int code;
        List<Tag> test = new LinkedList<Tag>();
        List<Tag> change = new LinkedList<Tag>();
        List<Tag> alternatives = new LinkedList<Tag>();

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

        DeprecationCheck testAndRemove(String key) {
            return test(key).remove(key);
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
                if (value.isEmpty() && !p.hasKey(key)) {
                    return false;
                }
                if (!value.isEmpty() && !value.equals(p.get(key))) {
                    return false;
                }
            }
            return true;
        }

        Command fixPrimitive(OsmPrimitive p) {
            Collection<Command> cmds = new LinkedList<Command>();
            for (Tag tag : change) {
                cmds.add(new ChangePropertyCommand(p, tag.getKey(), tag.getValue()));
            }
            return new SequenceCommand(tr("Deprecation fix of {0}", Utils.join(", ", test)), cmds);
        }

        String getDescription() {
            if (alternatives.isEmpty()) {
                return tr("{0} is deprecated", Utils.join(", ", test));
            } else {
                return tr("{0} is deprecated, use {1} instead", Utils.join(", ", test), Utils.join(tr(" or "), alternatives));
            }
        }
    }

    private class DeprecationError extends TestError {

        OsmPrimitive p;
        DeprecationCheck check;

        DeprecationError(OsmPrimitive p, DeprecationCheck check) {
            super(DeprecatedTags.this, Severity.WARNING, check.getDescription(), check.code, p);
            this.p = p;
            this.check = check;
        }

        @Override
        public boolean isFixable() {
            return !check.change.isEmpty();
        }

        @Override
        public Command getFix() {
            return check.fixPrimitive(p);
        }
    }
}
