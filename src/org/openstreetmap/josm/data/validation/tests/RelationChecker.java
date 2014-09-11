// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Roles;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.TaggingPresets;

/**
 * Check for wrong relations.
 * @since 3669
 */
public class RelationChecker extends Test {

    protected static final int ROLE_UNKNOWN      = 1701;
    protected static final int ROLE_EMPTY        = 1702;
    protected static final int WRONG_TYPE        = 1703;
    protected static final int HIGH_COUNT        = 1704;
    protected static final int LOW_COUNT         = 1705;
    protected static final int ROLE_MISSING      = 1706;
    protected static final int RELATION_UNKNOWN  = 1707;
    protected static final int RELATION_EMPTY    = 1708;

    /**
     * Error message used to group errors related to role problems.
     * @since 6731
     */
    public static final String ROLE_VERIF_PROBLEM_MSG = tr("Role verification problem");

    /**
     * Constructor
     */
    public RelationChecker() {
        super(tr("Relation checker"),
                tr("Checks for errors in relations."));
    }

    @Override
    public void initialize() {
        initializePresets();
    }

    private static Collection<TaggingPreset> relationpresets = new LinkedList<>();

    /**
     * Reads the presets data.
     */
    public static synchronized void initializePresets() {
        if (!relationpresets.isEmpty()) {
            // the presets have already been initialized
            return;
        }
        for (TaggingPreset p : TaggingPresets.getTaggingPresets()) {
            for (TaggingPresetItem i : p.data) {
                if (i instanceof Roles) {
                    relationpresets.add(p);
                    break;
                }
            }
        }
    }

    private static class RoleInfo {
        private int total = 0;
        private Collection<Node> nodes = new LinkedList<>();
        private Collection<Way> ways = new LinkedList<>();
        private Collection<Way> openways = new LinkedList<>();
        private Collection<Relation> relations = new LinkedList<>();
    }

    @Override
    public void visit(Relation n) {
        LinkedList<Role> allroles = buildAllRoles(n);
        if (allroles.isEmpty() && n.hasTag("type", "route")
                && n.hasTag("route", "train", "subway", "monorail", "tram", "bus", "trolleybus", "aerialway", "ferry")) {
            errors.add(new TestError(this, Severity.WARNING,
                    tr("Route scheme is unspecified. Add {0} ({1}=public_transport; {2}=legacy)", "public_transport:version", "2", "1"),
                    RELATION_UNKNOWN, n));
        } else if (allroles.isEmpty()) {
            errors.add(new TestError(this, Severity.WARNING, tr("Relation type is unknown"), RELATION_UNKNOWN, n));
        }

        HashMap<String, RoleInfo> map = buildRoleInfoMap(n);
        if (map.isEmpty()) {
            errors.add(new TestError(this, Severity.ERROR, tr("Relation is empty"), RELATION_EMPTY, n));
        } else if (!allroles.isEmpty()) {
            checkRoles(n, allroles, map);
        }
    }

    private HashMap<String, RoleInfo> buildRoleInfoMap(Relation n) {
        HashMap<String,RoleInfo> map = new HashMap<>();
        for (RelationMember m : n.getMembers()) {
            String role = m.getRole();
            RoleInfo ri = map.get(role);
            if (ri == null) {
                ri = new RoleInfo();
            }
            ri.total++;
            if (m.isRelation()) {
                ri.relations.add(m.getRelation());
            } else if(m.isWay()) {
                ri.ways.add(m.getWay());
                if (!m.getWay().isClosed()) {
                    ri.openways.add(m.getWay());
                }
            }
            else if (m.isNode()) {
                ri.nodes.add(m.getNode());
            }
            map.put(role, ri);
        }
        return map;
    }

    private LinkedList<Role> buildAllRoles(Relation n) {
        LinkedList<Role> allroles = new LinkedList<>();
        for (TaggingPreset p : relationpresets) {
            boolean matches = true;
            Roles r = null;
            for (TaggingPresetItem i : p.data) {
                if (i instanceof Key) {
                    Key k = (Key) i;
                    if (!k.value.equals(n.get(k.key))) {
                        matches = false;
                        break;
                    }
                } else if (i instanceof Roles) {
                    r = (Roles) i;
                }
            }
            if (matches && r != null) {
                allroles.addAll(r.roles);
            }
        }
        return allroles;
    }

    private void checkRoles(Relation n, LinkedList<Role> allroles, HashMap<String, RoleInfo> map) {
        List<String> done = new LinkedList<>();
        // Remove empty roles if several exist (like in route=hiking, see #9844)
        List<Role> emptyRoles = new LinkedList<>();
        for (Role r : allroles) {
            if ("".equals(r.key)) {
                emptyRoles.add(r);
            }
        }
        if (emptyRoles.size() > 1) {
            allroles.removeAll(emptyRoles);
        }
        for (Role r : allroles) {
            done.add(r.key);
            String keyname = r.key;
            if ("".equals(keyname)) {
                keyname = tr("<empty>");
            }
            RoleInfo ri = map.get(r.key);
            checkRoleCounts(n, r, keyname, ri);
            if (ri != null) {
                if (r.types != null) {
                    checkRoleTypes(n, r, keyname, ri);
                }
                if (r.memberExpression != null) {
                    checkRoleMemberExpressions(n, r, keyname, ri);
                }
            }
        }
        for (String key : map.keySet()) {
            if (!done.contains(key)) {
                if (key.length() > 0) {
                    String s = marktr("Role {0} unknown");
                    errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                            tr(s, key), MessageFormat.format(s, key), ROLE_UNKNOWN, n));
                } else {
                    String s = marktr("Empty role found");
                    errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                            tr(s), s, ROLE_EMPTY, n));
                }
            }
        }
    }

    private void checkRoleMemberExpressions(Relation n, Role r, String keyname, RoleInfo ri) {
        Set<OsmPrimitive> notMatching = new HashSet<>();
        Collection<OsmPrimitive> allPrimitives = new ArrayList<>();
        allPrimitives.addAll(ri.nodes);
        allPrimitives.addAll(ri.ways);
        allPrimitives.addAll(ri.relations);
        for (OsmPrimitive p : allPrimitives) {
            if (p.isUsable() && !r.memberExpression.match(p)) {
                notMatching.add(p);
            }
        }
        if (!notMatching.isEmpty()) {
            String s = marktr("Member for role ''{0}'' does not match ''{1}''");
            LinkedList<OsmPrimitive> highlight = new LinkedList<>(notMatching);
            highlight.addFirst(n);
            errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                    tr(s, keyname, r.memberExpression), MessageFormat.format(s, keyname, r.memberExpression), WRONG_TYPE,
                    highlight, notMatching));
        }
    }

    private void checkRoleTypes(Relation n, Role r, String keyname, RoleInfo ri) {
        Set<OsmPrimitive> wrongTypes = new HashSet<>();
        if (!r.types.contains(TaggingPresetType.WAY)) {
            wrongTypes.addAll(r.types.contains(TaggingPresetType.CLOSEDWAY) ? ri.openways : ri.ways);
        }
        if (!r.types.contains(TaggingPresetType.NODE)) {
            wrongTypes.addAll(ri.nodes);
        }
        if (!r.types.contains(TaggingPresetType.RELATION)) {
            wrongTypes.addAll(ri.relations);
        }
        if (!wrongTypes.isEmpty()) {
            String s = marktr("Member for role {0} of wrong type");
            LinkedList<OsmPrimitive> highlight = new LinkedList<>(wrongTypes);
            highlight.addFirst(n);
            errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                    tr(s, keyname), MessageFormat.format(s, keyname), WRONG_TYPE,
                    highlight, wrongTypes));
        }
    }

    private void checkRoleCounts(Relation n, Role r, String keyname, RoleInfo ri) {
        long count = (ri == null) ? 0 : ri.total;
        long vc = r.getValidCount(count);
        if (count != vc) {
            if (count == 0) {
                String s = marktr("Role {0} missing");
                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                        tr(s, keyname), MessageFormat.format(s, keyname), ROLE_MISSING, n));
            }
            else if (vc > count) {
                String s = marktr("Number of {0} roles too low ({1})");
                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                        tr(s, keyname, count), MessageFormat.format(s, keyname, count), LOW_COUNT, n));
            } else {
                String s = marktr("Number of {0} roles too high ({1})");
                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                        tr(s, keyname, count), MessageFormat.format(s, keyname, count), HIGH_COUNT, n));
            }
        }
    }

    @Override
    public Command fixError(TestError testError) {
        if (isFixable(testError)) {
            return new DeleteCommand(testError.getPrimitives());
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        Collection<? extends OsmPrimitive> primitives = testError.getPrimitives();
        return testError.getCode() == RELATION_EMPTY && !primitives.isEmpty() && primitives.iterator().next().isNew();
    }
}
