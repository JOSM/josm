// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Roles;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;

/**
 * Check for wrong relations
 *
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
                tr("This plugin checks for errors in relations."));
    }

    @Override
    public void initialize() {
        initializePresets();
    }

    private static Collection<TaggingPreset> relationpresets = new LinkedList<TaggingPreset>();

    /**
     * Reads the presets data.
     *
     */
    public void initializePresets() {
        Collection<TaggingPreset> presets = TaggingPresetPreference.taggingPresets;
        if (presets != null) {
            for (TaggingPreset p : presets) {
                for (TaggingPresetItem i : p.data) {
                    if (i instanceof Roles) {
                        relationpresets.add(p);
                        break;
                    }
                }
            }
        }
    }

    private static class RoleInfo {
        private int total = 0;
        private Collection<Node> nodes = new LinkedList<Node>();
        private Collection<Way> ways = new LinkedList<Way>();
        private Collection<Way> openways = new LinkedList<Way>();
        private Collection<Relation> relations = new LinkedList<Relation>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(Relation n) {
        LinkedList<Role> allroles = new LinkedList<Role>();
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
        if (allroles.isEmpty()) {
            errors.add( new TestError(this, Severity.WARNING, tr("Relation type is unknown"),
                    RELATION_UNKNOWN, n) );
        } else {
            HashMap<String,RoleInfo> map = new HashMap<String, RoleInfo>();
            for (RelationMember m : n.getMembers()) {
                String s = "";
                if (m.hasRole()) {
                    s = m.getRole();
                }
                RoleInfo ri = map.get(s);
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
                map.put(s, ri);
            }
            if(map.isEmpty()) {
                errors.add( new TestError(this, Severity.ERROR, tr("Relation is empty"),
                        RELATION_EMPTY, n) );
            } else {
                LinkedList<String> done = new LinkedList<String>();
                for (Role r : allroles) {
                    done.add(r.key);
                    String keyname = r.key;
                    if ("".equals(keyname)) {
                        keyname = tr("<empty>");
                    }
                    RoleInfo ri = map.get(r.key);
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
                    if (ri != null) {
                        if (r.types != null) {
                            Set<OsmPrimitive> wrongTypes = new HashSet<OsmPrimitive>();
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
                                LinkedList<OsmPrimitive> highlight = new LinkedList<OsmPrimitive>(wrongTypes);
                                highlight.addFirst(n);
                                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                                        tr(s, keyname), MessageFormat.format(s, keyname), WRONG_TYPE,
                                        highlight, wrongTypes));
                            }
                        }
                        if (r.memberExpression != null) {
                            Set<OsmPrimitive> notMatching = new HashSet<OsmPrimitive>();
                            for (Collection<OsmPrimitive> c : Arrays.asList(new Collection[]{ri.nodes, ri.ways, ri.relations})) {
                                for (OsmPrimitive p : c) {
                                    if (p.isUsable() && !r.memberExpression.match(p)) {
                                        notMatching.add(p);
                                    }
                                }
                            }
                            if (!notMatching.isEmpty()) {
                                String s = marktr("Member for role ''{0}'' does not match ''{1}''");
                                LinkedList<OsmPrimitive> highlight = new LinkedList<OsmPrimitive>(notMatching);
                                highlight.addFirst(n);
                                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                                        tr(s, keyname, r.memberExpression), MessageFormat.format(s, keyname, r.memberExpression), WRONG_TYPE,
                                        highlight, notMatching));
                            }
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
