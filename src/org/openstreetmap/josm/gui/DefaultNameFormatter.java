// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trc_lazy;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.history.HistoryNameFormatter;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;

/**
 * This is the default implementation of a {@see NameFormatter} for names of {@see OsmPrimitive}s.
 *
 */
public class DefaultNameFormatter implements NameFormatter, HistoryNameFormatter {

    static private DefaultNameFormatter instance;

    private static final LinkedList<NameFormatterHook> formatHooks = new LinkedList<NameFormatterHook>();

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
    
    /**
     * Registers a format hook. Adds the hook at the first position of the format hooks.
     * (for plugins)
     *
     * @param hook the format hook. Ignored if null.
     */
    public static void registerFormatHook(NameFormatterHook hook) {
        if (hook == null) return;
        if (!formatHooks.contains(hook)) {
            formatHooks.add(0,hook);
        }
    }

    /**
     * Unregisters a format hook. Removes the hook from the list of format hooks.
     *
     * @param hook the format hook. Ignored if null.
     */
    public static void unregisterFormatHook(NameFormatterHook hook) {
        if (hook == null) return;
        if (formatHooks.contains(hook)) {
            formatHooks.remove(hook);
        }
    }

    /** the default list of tags which are used as naming tags in relations */
    static public final String[] DEFAULT_NAMING_TAGS_FOR_RELATIONS = {"name", "ref", "restriction", "landuse", "natural",
        "public_transport", ":LocationCode", "note"};

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
     * <tt>osm-primitives.showid</tt> is set. Shows unique id if osm-primitives.showid.new-primitives is set
     *
     * @param name  the name without the id
     * @param primitive the primitive
     * @return the decorated name
     */
    protected String decorateNameWithId(String name, IPrimitive primitive) {
        if (Main.pref.getBoolean("osm-primitives.showid"))
            if (Main.pref.getBoolean("osm-primitives.showid.new-primitives"))
                return name + tr(" [id: {0}]", primitive.getUniqueId());
            else
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
    public String format(INode node) {
        String name = "";
        if (node.isIncomplete()) {
            name = tr("incomplete");
        } else {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                name = node.getLocalName();
            } else {
                name = node.getName();
            }
            if(name == null)
            {
                String s;
                if((s = node.get("addr:housename")) != null) {
                    /* I18n: name of house as parameter */
                    name = tr("House {0}", s);
                }
                if(name == null && (s = node.get("addr:housenumber")) != null) {
                    String t = node.get("addr:street");
                    if(t != null) {
                        /* I18n: house number, street as parameter, number should remain
                        before street for better visibility */
                        name =  tr("House number {0} at {1}", s, t);
                    }
                    else {
                        /* I18n: house number as parameter */
                        name = tr("House number {0}", s);
                    }
                }
            }

            if (name == null) {
                name = node.isNew() ? tr("node") : ""+ node.getId();
            }
            name += " \u200E(" + node.getCoor().latToString(CoordinateFormat.getDefaultFormat()) + ", " + node.getCoor().lonToString(CoordinateFormat.getDefaultFormat()) + ")";
        }
        name = decorateNameWithId(name, node);

        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkFormat(node, name);
            if (hookResult != null) {
                return hookResult;
            }
        }

        return name;
    }

    private final Comparator<Node> nodeComparator = new Comparator<Node>() {
        @Override
        public int compare(Node n1, Node n2) {
            return format(n1).compareTo(format(n2));
        }
    };

    public Comparator<Node> getNodeComparator() {
        return nodeComparator;
    }


    /**
     * Formats a name for a way
     *
     * @param way the way
     * @return the name
     */
    public String format(IWay way) {
        String name = "";
        if (way.isIncomplete()) {
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
                                (way.get("landuse") != null) ? tr("landuse") : null;
            }
            if(name == null)
            {
                String s;
                if((s = way.get("addr:housename")) != null) {
                    /* I18n: name of house as parameter */
                    name = tr("House {0}", s);
                }
                if(name == null && (s = way.get("addr:housenumber")) != null) {
                    String t = way.get("addr:street");
                    if(t != null) {
                        /* I18n: house number, street as parameter, number should remain
                        before street for better visibility */
                        name =  tr("House number {0} at {1}", s, t);
                    }
                    else {
                        /* I18n: house number as parameter */
                        name = tr("House number {0}", s);
                    }
                }
            }

            int nodesNo = way.getNodesCount();
            if (nodesNo > 1 && way.isClosed()) {
                nodesNo--;
            }
            if(name == null || name.length() == 0) {
                name = String.valueOf(way.getId());
            }
            /* note: length == 0 should no longer happen, but leave the bracket code
               nevertheless, who knows what future brings */
            /* I18n: count of nodes as parameter */
            String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
            name += (name.length() > 0) ? " ("+nodes+")" : nodes;
        }
        name = decorateNameWithId(name, way);
        
        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkFormat(way, name);
            if (hookResult != null) {
                return hookResult;
            }
        }

        return name;
    }

    private final Comparator<Way> wayComparator = new Comparator<Way>() {
        @Override
        public int compare(Way w1, Way w2) {
            return format(w1).compareTo(format(w2));
        }
    };

    public Comparator<Way> getWayComparator() {
        return wayComparator;
    }


    /**
     * Formats a name for a relation
     *
     * @param relation the relation
     * @return the name
     */
    public String format(IRelation relation) {
        String name;
        if (relation.isIncomplete()) {
            name = tr("incomplete");
        } else {
            name = getRelationTypeName(relation);
            String relationName = getRelationName(relation);
            if (relationName == null) {
                relationName = Long.toString(relation.getId());
            } else {
                relationName = "\"" + relationName + "\"";
            }
            name += " (" + relationName + ", ";

            int mbno = relation.getMembersCount();
            name += trn("{0} member", "{0} members", mbno, mbno);

            if (relation instanceof Relation) {
                if (((Relation) relation).hasIncompleteMembers()) {
                    name += ", "+tr("incomplete");
                }
            }

            name += ")";
        }
        name = decorateNameWithId(name, relation);

        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkFormat(relation, name);
            if (hookResult != null) {
                return hookResult;
            }
        }

        return name;
    }

    private final Comparator<Relation> relationComparator = new Comparator<Relation>() {
        @Override
        public int compare(Relation r1, Relation r2) {
            String type1 = getRelationTypeName(r1);
            String type2 = getRelationTypeName(r2);

            int comp = type1.compareTo(type2);
            if (comp != 0)
                return comp;

            String name1 = getRelationName(r1);
            String name2 = getRelationName(r2);

            if (name1 == null && name2 == null)
                return (r1.getUniqueId() > r2.getUniqueId())?1:-1;
            else if (name1 == null)
                return -1;
            else if (name2 == null)
                return 1;
            else if (!name1.isEmpty() && !name2.isEmpty() && Character.isDigit(name1.charAt(0)) && Character.isDigit(name2.charAt(0))) {
                //Compare numerically
                String ln1 = getLeadingNumber(name1);
                String ln2 = getLeadingNumber(name2);

                comp = Long.valueOf(ln1).compareTo(Long.valueOf(ln2));
                if (comp != 0)
                    return comp;

                // put 1 before 0001
                comp = ln1.compareTo(ln2);
                if (comp != 0)
                    return comp;

                comp = name1.substring(ln1.length()).compareTo(name2.substring(ln2.length()));
                if (comp != 0)
                    return comp;
            } else {
                comp = name1.compareToIgnoreCase(name2);
                if (comp != 0)
                    return comp;
            }

            if (r1.getMembersCount() != r2.getMembersCount())
                return (r1.getMembersCount() > r2.getMembersCount())?1:-1;

            comp = Boolean.valueOf(r1.hasIncompleteMembers()).compareTo(Boolean.valueOf(r2.hasIncompleteMembers()));
            if (comp != 0)
                return comp;

            return r1.getUniqueId() > r2.getUniqueId()?1:-1;
        }
    };

    public Comparator<Relation> getRelationComparator() {
        return relationComparator;
    }

    private String getLeadingNumber(String s) {
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        return s.substring(0, i);
    }

    private String getRelationTypeName(IRelation relation) {
        String name = trc("Relation type", relation.get("type"));
        if (name == null) {
            name = (relation.get("public_transport") != null) ? tr("public transport") : null;
        }
        if (name == null) {
            String building  = relation.get("building");
            if(OsmUtils.isTrue(building)) {
                name = tr("building");
            } else if(building != null)
            {
                name = tr(building); // translate tag!
            }
        }
        if (name == null) {
            name = trc("Place type", relation.get("place"));
        }
        if (name == null) {
            name = tr("relation");
        }
        String admin_level = relation.get("admin_level");
        if (admin_level != null) {
            name += "["+admin_level+"]";
        }
        
        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkRelationTypeName(relation, name);
            if (hookResult != null) {
                return hookResult;
            }
        }

        return name;
    }

    private String getNameTagValue(IRelation relation, String nameTag) {
        if (nameTag.equals("name")) {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true))
                return relation.getLocalName();
            else
                return relation.getName();
        } else if (nameTag.equals(":LocationCode")) {
            for (String m : relation.keySet()) {
                if (m.endsWith(nameTag))
                    return relation.get(m);
            }
            return null;
        } else
            return trc_lazy(nameTag, relation.get(nameTag));
    }

    private String getRelationName(IRelation relation) {
        String nameTag = null;
        for (String n : getNamingtagsForRelations()) {
            nameTag = getNameTagValue(relation, n);
            if (nameTag != null)
                return nameTag;
        }
        return null;
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
    public String buildDefaultToolTip(IPrimitive primitive) {
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

    /**
     * Decorates the name of primitive with its id, if the preference
     * <tt>osm-primitives.showid</tt> is set.
     *
     * The id is append to the {@see StringBuilder} passed in in <code>name</code>.
     *
     * @param name  the name without the id
     * @param primitive the primitive
     */
    protected void decorateNameWithId(StringBuilder name, HistoryOsmPrimitive primitive) {
        if (Main.pref.getBoolean("osm-primitives.showid")) {
            name.append(tr(" [id: {0}]", primitive.getId()));
        }
    }

    /**
     * Formats a name for a history node
     *
     * @param node the node
     * @return the name
     */
    public String format(HistoryNode node) {
        StringBuilder sb = new StringBuilder();
        String name;
        if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
            name = node.getLocalName();
        } else {
            name = node.getName();
        }
        if (name == null) {
            sb.append(node.getId());
        } else {
            sb.append(name);
        }
        sb.append(" (")
        .append(node.getCoords().latToString(CoordinateFormat.getDefaultFormat()))
        .append(", ")
        .append(node.getCoords().lonToString(CoordinateFormat.getDefaultFormat()))
        .append(")");
        decorateNameWithId(sb, node);
        return sb.toString();
    }

    /**
     * Formats a name for a way
     *
     * @param way the way
     * @return the name
     */
    public String format(HistoryWay way) {
        StringBuilder sb = new StringBuilder();
        String name;
        if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
            name = way.getLocalName();
        } else {
            name = way.getName();
        }
        if (name != null) {
            sb.append(name);
        }
        if (sb.length() == 0 && way.get("ref") != null) {
            sb.append(way.get("ref"));
        }
        if (sb.length() == 0) {
            sb.append(
                    (way.get("highway") != null) ? tr("highway") :
                        (way.get("railway") != null) ? tr("railway") :
                            (way.get("waterway") != null) ? tr("waterway") :
                                (way.get("landuse") != null) ? tr("landuse") : ""
            );
        }

        int nodesNo = way.isClosed() ? way.getNumNodes() -1 : way.getNumNodes();
        String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
        if(sb.length() == 0 ) {
            sb.append(way.getId());
        }
        /* note: length == 0 should no longer happen, but leave the bracket code
           nevertheless, who knows what future brings */
        sb.append((sb.length() > 0) ? " ("+nodes+")" : nodes);
        decorateNameWithId(sb, way);
        return sb.toString();
    }

    /**
     * Formats a name for a {@see HistoryRelation})
     *
     * @param relation the relation
     * @return the name
     */
    public String format(HistoryRelation relation) {
        StringBuilder sb = new StringBuilder();
        if (relation.get("type") != null) {
            sb.append(relation.get("type"));
        } else {
            sb.append(tr("relation"));
        }
        sb.append(" (");
        String nameTag = null;
        Set<String> namingTags = new HashSet<String>(getNamingtagsForRelations());
        for (String n : relation.getTags().keySet()) {
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
            sb.append(Long.toString(relation.getId())).append(", ");
        } else {
            sb.append("\"").append(nameTag).append("\", ");
        }

        int mbno = relation.getNumMembers();
        sb.append(trn("{0} member", "{0} members", mbno, mbno)).append(")");

        decorateNameWithId(sb, relation);
        return sb.toString();
    }

    /**
     * Builds a default tooltip text for an HistoryOsmPrimitive <code>primitive</code>.
     *
     * @param primitive the primitmive
     * @return the tooltip text
     */
    public String buildDefaultToolTip(HistoryOsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<strong>id</strong>=")
        .append(primitive.getId())
        .append("<br>");
        ArrayList<String> keyList = new ArrayList<String>(primitive.getTags().keySet());
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
