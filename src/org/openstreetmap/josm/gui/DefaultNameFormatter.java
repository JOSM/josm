// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trc_lazy;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.history.HistoryNameFormatter;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.TaggingPresetNameTemplateList;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.Utils.Function;

/**
 * This is the default implementation of a {@link NameFormatter} for names of {@link OsmPrimitive}s.
 *
 */
public class DefaultNameFormatter implements NameFormatter, HistoryNameFormatter {

    static private DefaultNameFormatter instance;

    private static final List<NameFormatterHook> formatHooks = new LinkedList<NameFormatterHook>();

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

    /** The default list of tags which are used as naming tags in relations.
     * A ? prefix indicates a boolean value, for which the key (instead of the value) is used.
     */
    static public final String[] DEFAULT_NAMING_TAGS_FOR_RELATIONS = {"name", "ref", "restriction", "landuse", "natural",
        "public_transport", ":LocationCode", "note", "?building"};

    /** the current list of tags used as naming tags in relations */
    static private List<String> namingTagsForRelations =  null;

    /**
     * Replies the list of naming tags used in relations. The list is given (in this order) by:
     * <ul>
     *   <li>by the tag names in the preference <tt>relation.nameOrder</tt></li>
     *   <li>by the default tags in {@link #DEFAULT_NAMING_TAGS_FOR_RELATIONS}
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
     */
    protected void decorateNameWithId(StringBuilder name, IPrimitive primitive) {
        if (Main.pref.getBoolean("osm-primitives.showid")) {
            if (Main.pref.getBoolean("osm-primitives.showid.new-primitives")) {
                name.append(tr(" [id: {0}]", primitive.getUniqueId()));
            } else {
                name.append(tr(" [id: {0}]", primitive.getId()));
            }
        }
    }

    /**
     * Formats a name for a node
     *
     * @param node the node
     * @return the name
     */
    @Override
    public String format(Node node) {
        StringBuilder name = new StringBuilder();
        if (node.isIncomplete()) {
            name.append(tr("incomplete"));
        } else {
            TaggingPreset preset = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(node);
            if (preset == null) {
                String n;
                if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                    n = node.getLocalName();
                } else {
                    n = node.getName();
                }
                if(n == null)
                {
                    String s;
                    if((s = node.get("addr:housename")) != null) {
                        /* I18n: name of house as parameter */
                        n = tr("House {0}", s);
                    }
                    if(n == null && (s = node.get("addr:housenumber")) != null) {
                        String t = node.get("addr:street");
                        if(t != null) {
                            /* I18n: house number, street as parameter, number should remain
                        before street for better visibility */
                            n =  tr("House number {0} at {1}", s, t);
                        }
                        else {
                            /* I18n: house number as parameter */
                            n = tr("House number {0}", s);
                        }
                    }
                }

                if (n == null) {
                    n = node.isNew() ? tr("node") : ""+ node.getId();
                }
                name.append(n);
            } else {
                preset.nameTemplate.appendText(name, node);
            }
            if (node.getCoor() != null) {
                name.append(" \u200E(").append(node.getCoor().latToString(CoordinateFormat.getDefaultFormat())).append(", ").append(node.getCoor().lonToString(CoordinateFormat.getDefaultFormat())).append(")");
            }
        }
        decorateNameWithId(name, node);


        String result = name.toString();
        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkFormat(node, result);
            if (hookResult != null)
                return hookResult;
        }

        return result;
    }

    private final Comparator<Node> nodeComparator = new Comparator<Node>() {
        @Override
        public int compare(Node n1, Node n2) {
            return format(n1).compareTo(format(n2));
        }
    };

    @Override
    public Comparator<Node> getNodeComparator() {
        return nodeComparator;
    }


    /**
     * Formats a name for a way
     *
     * @param way the way
     * @return the name
     */
    @Override
    public String format(Way way) {
        StringBuilder name = new StringBuilder();
        if (way.isIncomplete()) {
            name.append(tr("incomplete"));
        } else {
            TaggingPreset preset = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(way);
            if (preset == null) {
                String n;
                if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                    n = way.getLocalName();
                } else {
                    n = way.getName();
                }
                if (n == null) {
                    n = way.get("ref");
                }
                if (n == null) {
                    n =
                            (way.get("highway") != null) ? tr("highway") :
                                (way.get("railway") != null) ? tr("railway") :
                                    (way.get("waterway") != null) ? tr("waterway") :
                                            (way.get("landuse") != null) ? tr("landuse") : null;
                }
                if(n == null)
                {
                    String s;
                    if((s = way.get("addr:housename")) != null) {
                        /* I18n: name of house as parameter */
                        n = tr("House {0}", s);
                    }
                    if(n == null && (s = way.get("addr:housenumber")) != null) {
                        String t = way.get("addr:street");
                        if(t != null) {
                            /* I18n: house number, street as parameter, number should remain
                        before street for better visibility */
                            n =  tr("House number {0} at {1}", s, t);
                        }
                        else {
                            /* I18n: house number as parameter */
                            n = tr("House number {0}", s);
                        }
                    }
                }
                if(n == null && way.get("building") != null) n = tr("building");
                if(n == null || n.length() == 0) {
                    n = String.valueOf(way.getId());
                }

                name.append(n);
            } else {
                preset.nameTemplate.appendText(name, way);
            }

            int nodesNo = way.getRealNodesCount();
            /* note: length == 0 should no longer happen, but leave the bracket code
               nevertheless, who knows what future brings */
            /* I18n: count of nodes as parameter */
            String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
            name.append(" (").append(nodes).append(")");
        }
        decorateNameWithId(name, way);

        String result = name.toString();
        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkFormat(way, result);
            if (hookResult != null)
                return hookResult;
        }

        return result;
    }

    private final Comparator<Way> wayComparator = new Comparator<Way>() {
        @Override
        public int compare(Way w1, Way w2) {
            return format(w1).compareTo(format(w2));
        }
    };

    @Override
    public Comparator<Way> getWayComparator() {
        return wayComparator;
    }


    /**
     * Formats a name for a relation
     *
     * @param relation the relation
     * @return the name
     */
    @Override
    public String format(Relation relation) {
        StringBuilder name = new StringBuilder();
        if (relation.isIncomplete()) {
            name.append(tr("incomplete"));
        } else {
            TaggingPreset preset = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(relation);

            formatRelationNameAndType(relation, name, preset);

            int mbno = relation.getMembersCount();
            name.append(trn("{0} member", "{0} members", mbno, mbno));

            if (relation.hasIncompleteMembers()) {
                name.append(", ").append(tr("incomplete"));
            }

            name.append(")");
        }
        decorateNameWithId(name, relation);

        String result = name.toString();
        for (NameFormatterHook hook: formatHooks) {
            String hookResult = hook.checkFormat(relation, result);
            if (hookResult != null)
                return hookResult;
        }

        return result;
    }

    private void formatRelationNameAndType(Relation relation, StringBuilder result, TaggingPreset preset) {
        if (preset == null) {
            result.append(getRelationTypeName(relation));
            String relationName = getRelationName(relation);
            if (relationName == null) {
                relationName = Long.toString(relation.getId());
            } else {
                relationName = "\"" + relationName + "\"";
            }
            result.append(" (").append(relationName).append(", ");
        } else {
            preset.nameTemplate.appendText(result, relation);
            result.append("(");
        }
    }

    private final Comparator<Relation> relationComparator = new Comparator<Relation>() {
        @Override
        public int compare(Relation r1, Relation r2) {
            //TODO This doesn't work correctly with formatHooks

            TaggingPreset preset1 = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(r1);
            TaggingPreset preset2 = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(r2);

            if (preset1 != null || preset2 != null) {
                StringBuilder name1 = new StringBuilder();
                formatRelationNameAndType(r1, name1, preset1);
                StringBuilder name2 = new StringBuilder();
                formatRelationNameAndType(r2, name2, preset2);

                int comp = name1.toString().compareTo(name2.toString());
                if (comp != 0)
                    return comp;
            } else {

                String type1 = getRelationTypeName(r1);
                String type2 = getRelationTypeName(r2);

                int comp = AlphanumComparator.getInstance().compare(type1, type2);
                if (comp != 0)
                    return comp;

                String name1 = getRelationName(r1);
                String name2 = getRelationName(r2);

                comp = AlphanumComparator.getInstance().compare(name1, name2);
                if (comp != 0)
                    return comp;
            }

            if (r1.getMembersCount() != r2.getMembersCount())
                return (r1.getMembersCount() > r2.getMembersCount())?1:-1;

            int comp = Boolean.valueOf(r1.hasIncompleteMembers()).compareTo(Boolean.valueOf(r2.hasIncompleteMembers()));
            if (comp != 0)
                return comp;

            if (r1.getUniqueId() > r2.getUniqueId())
                return 1;
            else if (r1.getUniqueId() < r2.getUniqueId())
                return -1;
            else
                return 0;
        }
    };

    @Override
    public Comparator<Relation> getRelationComparator() {
        return relationComparator;
    }

    private String getRelationTypeName(IRelation relation) {
        String name = trc("Relation type", relation.get("type"));
        if (name == null) {
            name = (relation.get("public_transport") != null) ? tr("public transport") : null;
        }
        if (name == null) {
            String building  = relation.get("building");
            if (OsmUtils.isTrue(building)) {
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
            if (hookResult != null)
                return hookResult;
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
        } else if (nameTag.startsWith("?") && OsmUtils.isTrue(relation.get(nameTag.substring(1)))) {
            return tr(nameTag.substring(1));
        } else if (nameTag.startsWith("?") && OsmUtils.isFalse(relation.get(nameTag.substring(1)))) {
            return null;
        } else {
            return trc_lazy(nameTag, I18n.escape(relation.get(nameTag)));
        }
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
    @Override
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
        List<String> keyList = new ArrayList<String>(primitive.keySet());
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
     * The id is append to the {@link StringBuilder} passed in in <code>name</code>.
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
    @Override
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
        LatLon coord = node.getCoords();
        if (coord != null) {
            sb.append(" (")
            .append(coord.latToString(CoordinateFormat.getDefaultFormat()))
            .append(", ")
            .append(coord.lonToString(CoordinateFormat.getDefaultFormat()))
            .append(")");
        }
        decorateNameWithId(sb, node);
        return sb.toString();
    }

    /**
     * Formats a name for a way
     *
     * @param way the way
     * @return the name
     */
    @Override
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
     * Formats a name for a {@link HistoryRelation})
     *
     * @param relation the relation
     * @return the name
     */
    @Override
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
        List<String> keyList = new ArrayList<String>(primitive.getTags().keySet());
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

    public String formatAsHtmlUnorderedList(Collection<? extends OsmPrimitive> primitives) {
        return Utils.joinAsHtmlUnorderedList(Utils.transform(primitives, new Function<OsmPrimitive, String>() {

            @Override
            public String apply(OsmPrimitive x) {
                return x.getDisplayName(DefaultNameFormatter.this);
            }
        }));
    }

    public String formatAsHtmlUnorderedList(OsmPrimitive... primitives) {
        return formatAsHtmlUnorderedList(Arrays.asList(primitives));
    }
}
