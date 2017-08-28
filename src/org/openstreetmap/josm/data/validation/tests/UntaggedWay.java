// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Checks for untagged ways
 *
 * @author frsantos
 */
public class UntaggedWay extends Test {

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    /** Empty way error */
    protected static final int EMPTY_WAY        = 301;
    /** Untagged way error */
    protected static final int UNTAGGED_WAY     = 302;
    /** Unnamed way error */
    protected static final int UNNAMED_WAY      = 303;
    /** One node way error */
    protected static final int ONE_NODE_WAY     = 304;
    /** Unnamed junction error */
    protected static final int UNNAMED_JUNCTION = 305;
    /** Untagged, but commented way error */
    protected static final int COMMENTED_WAY    = 306;
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private Set<Way> waysUsedInRelations;

    /** Ways that must have a name */
    static final Set<String> NAMED_WAYS = new HashSet<>();
    static {
        NAMED_WAYS.add("motorway");
        NAMED_WAYS.add("trunk");
        NAMED_WAYS.add("primary");
        NAMED_WAYS.add("secondary");
        NAMED_WAYS.add("tertiary");
        NAMED_WAYS.add("residential");
        NAMED_WAYS.add("pedestrian");
    }

    /** Whitelist of roles allowed to reference an untagged way */
    static final Set<String> WHITELIST = new HashSet<>();
    static {
        WHITELIST.add("outer");
        WHITELIST.add("inner");
        WHITELIST.add("perimeter");
        WHITELIST.add("edge");
        WHITELIST.add("outline");
    }

    /**
     * Constructor
     */
    public UntaggedWay() {
        super(tr("Untagged, empty and one node ways"),
              tr("This test checks for untagged, empty and one node ways."));
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable())
            return;

        Map<String, String> tags = w.getKeys();
        if (!tags.isEmpty()) {
            String highway = tags.get(HIGHWAY);
            if (highway != null && NAMED_WAYS.contains(highway) && !tags.containsKey("name") && !tags.containsKey("ref")
                    && !"yes".equals(tags.get("noname"))) {
                boolean isJunction = false;
                boolean hasName = false;
                for (String key : tags.keySet()) {
                    hasName = key.startsWith("name:") || key.endsWith("_name") || key.endsWith("_ref");
                    if (hasName) {
                        break;
                    }
                    if ("junction".equals(key)) {
                        isJunction = true;
                        break;
                    }
                }

                if (!hasName && !isJunction) {
                    errors.add(TestError.builder(this, Severity.WARNING, UNNAMED_WAY)
                            .message(tr("Unnamed ways"))
                            .primitives(w)
                            .build());
                } else if (isJunction) {
                    errors.add(TestError.builder(this, Severity.OTHER, UNNAMED_JUNCTION)
                            .message(tr("Unnamed junction"))
                            .primitives(w)
                            .build());
                }
            }
        }

        if (!w.isTagged() && !waysUsedInRelations.contains(w)) {
            if (w.hasKeys()) {
                errors.add(TestError.builder(this, Severity.WARNING, COMMENTED_WAY)
                        .message(tr("Untagged ways (commented)"))
                        .primitives(w)
                        .build());
            } else {
                errors.add(TestError.builder(this, Severity.WARNING, UNTAGGED_WAY)
                        .message(tr("Untagged ways"))
                        .primitives(w)
                        .build());
            }
        }

        if (w.getNodesCount() == 0) {
            errors.add(TestError.builder(this, Severity.ERROR, EMPTY_WAY)
                    .message(tr("Empty ways"))
                    .primitives(w)
                    .build());
        } else if (w.getNodesCount() == 1) {
            errors.add(TestError.builder(this, Severity.ERROR, ONE_NODE_WAY)
                    .message(tr("One node ways"))
                    .primitives(w)
                    .build());
        }
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        DataSet ds = Main.main.getEditDataSet();
        if (ds == null)
            return;
        waysUsedInRelations = new HashSet<>();
        for (Relation r : ds.getRelations()) {
            if (r.isUsable()) {
                for (RelationMember m : r.getMembers()) {
                    if (r.isMultipolygon() || WHITELIST.contains(m.getRole())) {
                        OsmPrimitive member = m.getMember();
                        if (member instanceof Way && member.isUsable() && !member.isTagged()) {
                            waysUsedInRelations.add((Way) member);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void endTest() {
        waysUsedInRelations = null;
        super.endTest();
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (testError.getTester() instanceof UntaggedWay)
            return testError.getCode() == EMPTY_WAY
                || testError.getCode() == ONE_NODE_WAY;

        return false;
    }

    @Override
    public Command fixError(TestError testError) {
        return deletePrimitivesIfNeeded(testError.getPrimitives());
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        return p.isUsable();
    }
}
