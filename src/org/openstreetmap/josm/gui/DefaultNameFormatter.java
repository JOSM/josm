// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * This is the default implementation of a {@see NameFormatter} for names of {@see OsmPrimitive}s.
 *
 */
public class DefaultNameFormatter implements NameFormatter {

    static private DefaultNameFormatter instance;

    /**
     * Replies the unique instance of this formatter
     *
     * @return the unique instance of this formatter
     */
    static public DefaultNameFormatter getInstance() {
        if (instance == null) {
            instance = new DefaultNameFormatter();
        }
        return instance;
    }

    /** the default list of tags which are used as naming tags in relations */
    static public final String[] DEFAULT_NAMING_TAGS_FOR_RELATIONS = {"name", "ref", "restriction", "note"};

    /** the current list of tags used as naming tags in relations */
    static private List<String> namingTagsForRelations =  null;

    /**
     * Replies the list of naming tags used in relations. The list is given (in this order) by:
     * <ul>
     *   <li>by the tag names in the preference <tt>relation.nameOrder</tt></li>
     *   <li>by the default tags in {@see #DEFAULT_NAMING_TAGS_FOR_RELATIONS}
     * </ul>
     *
     * @return the list of naming tags used in relations
     */
    static public List<String> getNamingtagsForRelations() {
        if (namingTagsForRelations == null) {
            namingTagsForRelations = new ArrayList<String>(
                    Main.pref.getCollection("relation.nameOrder", Arrays.asList(DEFAULT_NAMING_TAGS_FOR_RELATIONS))
            );
        }
        return namingTagsForRelations;
    }

    /**
     * Decorates the name of primitive with its id, if the preference
     * <tt>osm-primitives.showid</tt> is set.
     *
     * @param name  the name without the id
     * @param primitive the primitive
     * @return the decorated name
     */
    protected String decorateNameWithId(String name, OsmPrimitive primitive) {
        if (Main.pref.getBoolean("osm-primitives.showid"))
            return name + tr(" [id: {0}]", primitive.getId());
        else
            return name;
    }

    /**
     * Formats a name for a node
     *
     * @param node the node
     * @return the name
     */
    public String format(Node node) {
        String name = "";
        if (node.incomplete) {
            name = tr("incomplete");
        } else {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                name = node.getLocalName();
            } else {
                name = node.getName();
            }
            if (name == null) {
                name = node.isNew() ? tr("node") : ""+ node.getId();
            }
            name += " (" + node.getCoor().latToString(CoordinateFormat.getDefaultFormat()) + ", " + node.getCoor().lonToString(CoordinateFormat.getDefaultFormat()) + ")";
        }
        name = decorateNameWithId(name, node);
        return name;
    }

    /**
     * Formats a name for a way
     *
     * @param way the way
     * @return the name
     */
    public String format(Way way) {
        String name = "";
        if (way.incomplete) {
            name = tr("incomplete");
        } else {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                name = way.getLocalName();
            } else {
                name = way.getName();
            }
            if (name == null) {
                name = way.get("ref");
            }
            if (name == null) {
                name =
                    (way.get("highway") != null) ? tr("highway") :
                        (way.get("railway") != null) ? tr("railway") :
                            (way.get("waterway") != null) ? tr("waterway") :
                                (way.get("landuse") != null) ? tr("landuse") : "";
            }

            int nodesNo = way.getNodesCount();
            if (nodesNo > 1 && way.isClosed()) {
                nodesNo--;
            }
            String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
            name += (name.length() > 0) ? " ("+nodes+")" : nodes;
        }
        name = decorateNameWithId(name, way);
        return name;
    }

    /**
     * Formats a name for a relation
     *
     * @param relation the relation
     * @return the name
     */
    public String format(Relation relation) {
        String name;
        if (relation.incomplete) {
            name = tr("incomplete");
        } else {
            name = relation.get("type");
            if (name == null) {
                name = tr("relation");
            }

            name += " (";
            String nameTag = null;
            Set<String> namingTags = new HashSet<String>(getNamingtagsForRelations());
            for (String n : relation.keySet()) {
                // #3328: "note " and " note" are name tags too
                if (namingTags.contains(n.trim())) {
                    if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                        nameTag = relation.getLocalName();
                    } else {
                        nameTag = relation.getName();
                    }
                    if (nameTag == null) {
                        nameTag = relation.get(n);
                    }
                }
                if (nameTag != null) {
                    break;
                }
            }
            if (nameTag == null) {
                name += Long.toString(relation.getId()) + ", ";
            } else {
                name += "\"" + nameTag + "\", ";
            }

            int mbno = relation.getMembersCount();
            name += trn("{0} member", "{0} members", mbno, mbno) + ")";
        }
        name = decorateNameWithId(name, relation);
        return name;
    }

    /**
     * Formats a name for a changeset
     *
     * @param changeset the changeset
     * @return the name
     */
    public String format(Changeset changeset) {
        return tr("Changeset {0}",changeset.getId());
    }

    /**
     * Builds a default tooltip text for the primitive <code>primitive</code>.
     * 
     * @param primitive the primitmive
     * @return the tooltip text
     */
    public String buildDefaultToolTip(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>id</strong>=")
        .append(primitive.getId())
        .append("<br>");
        ArrayList<String> keyList = new ArrayList<String>(primitive.keySet());
        Collections.sort(keyList);
        for (int i = 0; i < keyList.size(); i++) {
            if (i > 0) {
                sb.append("<br>");
            }
            String key = keyList.get(i);
            sb.append("<strong>")
            .append(key)
            .append("</strong>")
            .append("=");
            String value = primitive.get(key);
            while(value.length() != 0) {
                sb.append(value.substring(0,Math.min(50, value.length())));
                if (value.length() > 50) {
                    sb.append("<br>");
                    value = value.substring(50);
                } else {
                    value = "";
                }
            }
        }
        sb.append("</html>");
        return sb.toString();
    }
}
