// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;

/**
 * Check for wrong relations
 *
 */
public class RelationChecker extends Test {

    protected static int ROLE_UNKNOWN      = 1701;
    protected static int ROLE_EMPTY        = 1702;
    protected static int WRONG_TYPE        = 1703;
    protected static int HIGH_COUNT        = 1704;
    protected static int LOW_COUNT         = 1705;
    protected static int ROLE_MISSING      = 1706;
    protected static int RELATION_UNKNOWN  = 1707;
    protected static int RELATION_EMPTY    = 1708;

    /**
     * Constructor
     */
    public RelationChecker() {
        super(tr("Relation checker :"),
                tr("This plugin checks for errors in relations."));
    }

    @Override
    public void initialize() throws Exception {
        initializePresets();
    }

    static Collection<TaggingPreset> relationpresets = new LinkedList<TaggingPreset>();

    /**
     * Reads the presets data.
     *
     */
    public void initializePresets() {
        Collection<TaggingPreset> presets = TaggingPresetPreference.taggingPresets;
        if (presets != null) {
            for (TaggingPreset p : presets) {
                for (TaggingPreset.Item i : p.data) {
                    if (i instanceof TaggingPreset.Roles) {
                        relationpresets.add(p);
                        break;
                    }
                }
            }
        }
    }

    public class RoleInfo {
        int total = 0;
        Collection<Node> nodes = new LinkedList<Node>();
        Collection<Way> ways = new LinkedList<Way>();
        Collection<Way> closedways = new LinkedList<Way>();
        Collection<Way> openways = new LinkedList<Way>();
        Collection<Relation> relations = new LinkedList<Relation>();
    }

    @Override
    public void visit(Relation n) {
        LinkedList<TaggingPreset.Role> allroles = new LinkedList<TaggingPreset.Role>();
        for (TaggingPreset p : relationpresets) {
            boolean matches = true;
            TaggingPreset.Roles r = null;
            for (TaggingPreset.Item i : p.data) {
                if (i instanceof TaggingPreset.Key) {
                    TaggingPreset.Key k = (TaggingPreset.Key) i;
                    if (!k.value.equals(n.get(k.key))) {
                        matches = false;
                        break;
                    }
                } else if (i instanceof TaggingPreset.Roles) {
                    r = (TaggingPreset.Roles) i;
                }
            }
            if (matches && r != null) {
                allroles.addAll(r.roles);
            }
        }
        if (allroles.size() == 0) {
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
                    if (m.getWay().isClosed()) {
                        ri.closedways.add(m.getWay());
                    } else {
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
                for (TaggingPreset.Role r : allroles) {
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
                            errors.add(new TestError(this, Severity.WARNING, tr("Role verification problem"),
                                    tr(s, keyname), MessageFormat.format(s, keyname), ROLE_MISSING, n));
                        }
                        else if (vc > count) {
                            String s = marktr("Number of {0} roles too low ({1})");
                            errors.add(new TestError(this, Severity.WARNING, tr("Role verification problem"),
                                    tr(s, keyname, count), MessageFormat.format(s, keyname, count), LOW_COUNT, n));
                        } else {
                            String s = marktr("Number of {0} roles too high ({1})");
                            errors.add(new TestError(this, Severity.WARNING, tr("Role verification problem"),
                                    tr(s, keyname, count), MessageFormat.format(s, keyname, count), HIGH_COUNT, n));
                        }
                    }
                    if (ri != null) {
                        Collection<OsmPrimitive> wrongTypes = new LinkedList<OsmPrimitive>();
                        if (!r.types.contains(PresetType.WAY)) {
                            wrongTypes.addAll(r.types.contains(PresetType.CLOSEDWAY) ? ri.openways : ri.ways);
                        }
                        if (!r.types.contains(PresetType.NODE)) {
                            wrongTypes.addAll(ri.nodes);
                        }
                        if (!r.types.contains(PresetType.RELATION)) {
                            wrongTypes.addAll(ri.relations);
                        }
                        if (!wrongTypes.isEmpty()) {
                            String s = marktr("Member for role {0} of wrong type");
                            LinkedList<OsmPrimitive> highlight = new LinkedList<OsmPrimitive>(wrongTypes);
                            highlight.addFirst(n);
                            errors.add(new TestError(this, Severity.WARNING, tr("Role verification problem"),
                                    tr(s, keyname), MessageFormat.format(s, keyname), WRONG_TYPE,
                                    highlight, wrongTypes));
                        }
                    }
                }
                for (String key : map.keySet()) {
                    if (!done.contains(key)) {
                        if (key.length() > 0) {
                            String s = marktr("Role {0} unknown");
                            errors.add(new TestError(this, Severity.WARNING, tr("Role verification problem"),
                                    tr(s, key), MessageFormat.format(s, key), ROLE_UNKNOWN, n));
                        } else {
                            String s = marktr("Empty role found");
                            errors.add(new TestError(this, Severity.WARNING, tr("Role verification problem"),
                                    tr(s), s, ROLE_EMPTY, n));
                        }
                    }
                }
            }
        }
    }
}
