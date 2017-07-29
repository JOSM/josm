// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trcLazy;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.ComponentOrientation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetNameTemplateList;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is the default implementation of a {@link NameFormatter} for names of {@link OsmPrimitive}s
 * and {@link HistoryOsmPrimitive}s.
 * @since 1990
 */
public class DefaultNameFormatter implements NameFormatter, HistoryNameFormatter {

    private static DefaultNameFormatter instance;

    private static final List<NameFormatterHook> FORMAT_HOOKS = new LinkedList<>();

    /**
     * Replies the unique instance of this formatter
     *
     * @return the unique instance of this formatter
     */
    public static synchronized DefaultNameFormatter getInstance() {
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
        if (!FORMAT_HOOKS.contains(hook)) {
            FORMAT_HOOKS.add(0, hook);
        }
    }

    /**
     * Unregisters a format hook. Removes the hook from the list of format hooks.
     *
     * @param hook the format hook. Ignored if null.
     */
    public static void unregisterFormatHook(NameFormatterHook hook) {
        if (hook == null) return;
        if (FORMAT_HOOKS.contains(hook)) {
            FORMAT_HOOKS.remove(hook);
        }
    }

    /** The default list of tags which are used as naming tags in relations.
     * A ? prefix indicates a boolean value, for which the key (instead of the value) is used.
     */
    private static final String[] DEFAULT_NAMING_TAGS_FOR_RELATIONS = {"name", "ref", "restriction", "landuse", "natural",
        "leisure", "amenity", "public_transport", ":LocationCode", "note", "?building"};

    /** the current list of tags used as naming tags in relations */
    private static List<String> namingTagsForRelations;

    /**
     * Replies the list of naming tags used in relations. The list is given (in this order) by:
     * <ul>
     *   <li>by the tag names in the preference <tt>relation.nameOrder</tt></li>
     *   <li>by the default tags in {@link #DEFAULT_NAMING_TAGS_FOR_RELATIONS}
     * </ul>
     *
     * @return the list of naming tags used in relations
     */
    public static synchronized List<String> getNamingtagsForRelations() {
        if (namingTagsForRelations == null) {
            namingTagsForRelations = new ArrayList<>(
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
     * Formats a name for an {@link OsmPrimitive}.
     *
     * @param osm the primitive
     * @return the name
     * @since 10991
     */
    public String format(OsmPrimitive osm) {
        if (osm instanceof Node) {
            return format((Node) osm);
        } else if (osm instanceof Way) {
            return format((Way) osm);
        } else if (osm instanceof Relation) {
            return format((Relation) osm);
        }
        return null;
    }

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
                if (n == null) {
                    String s = node.get("addr:housename");
                    if (s != null) {
                        /* I18n: name of house as parameter */
                        n = tr("House {0}", s);
                    }
                    if (n == null && (s = node.get("addr:housenumber")) != null) {
                        String t = node.get("addr:street");
                        if (t != null) {
                            /* I18n: house number, street as parameter, number should remain
                        before street for better visibility */
                            n = tr("House number {0} at {1}", s, t);
                        } else {
                            /* I18n: house number as parameter */
                            n = tr("House number {0}", s);
                        }
                    }
                }

                if (n == null) {
                    n = node.isNew() ? tr("node") : Long.toString(node.getId());
                }
                name.append(n);
            } else {
                preset.nameTemplate.appendText(name, node);
            }
            if (node.getCoor() != null) {
                name.append(" \u200E(").append(node.getCoor().latToString(CoordinateFormat.getDefaultFormat())).append(", ")
                    .append(node.getCoor().lonToString(CoordinateFormat.getDefaultFormat())).append(')');
            }
        }
        decorateNameWithId(name, node);


        String result = name.toString();
        for (NameFormatterHook hook: FORMAT_HOOKS) {
            String hookResult = hook.checkFormat(node, result);
            if (hookResult != null)
                return hookResult;
        }

        return result;
    }

    private final Comparator<Node> nodeComparator = (n1, n2) -> format(n1).compareTo(format(n2));

    @Override
    public Comparator<Node> getNodeComparator() {
        return nodeComparator;
    }

    @Override
    public String format(Way way) {
        StringBuilder name = new StringBuilder();

        char mark;
        // If current language is left-to-right (almost all languages)
        if (ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight()) {
            // will insert Left-To-Right Mark to ensure proper display of text in the case when object name is right-to-left
            mark = '\u200E';
        } else {
            // otherwise will insert Right-To-Left Mark to ensure proper display in the opposite case
            mark = '\u200F';
        }
        // Initialize base direction of the string
        name.append(mark);

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
                    n = way.hasKey("highway") ? tr("highway") :
                        way.hasKey("railway") ? tr("railway") :
                        way.hasKey("waterway") ? tr("waterway") :
                        way.hasKey("landuse") ? tr("landuse") : null;
                }
                if (n == null) {
                    String s = way.get("addr:housename");
                    if (s != null) {
                        /* I18n: name of house as parameter */
                        n = tr("House {0}", s);
                    }
                    if (n == null && (s = way.get("addr:housenumber")) != null) {
                        String t = way.get("addr:street");
                        if (t != null) {
                            /* I18n: house number, street as parameter, number should remain
                        before street for better visibility */
                            n = tr("House number {0} at {1}", s, t);
                        } else {
                            /* I18n: house number as parameter */
                            n = tr("House number {0}", s);
                        }
                    }
                }
                if (n == null && way.hasKey("building")) {
                    n = tr("building");
                }
                if (n == null || n.isEmpty()) {
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
            name.append(mark).append(" (").append(nodes).append(')');
        }
        decorateNameWithId(name, way);

        String result = name.toString();
        for (NameFormatterHook hook: FORMAT_HOOKS) {
            String hookResult = hook.checkFormat(way, result);
            if (hookResult != null)
                return hookResult;
        }

        return result;
    }

    private final Comparator<Way> wayComparator = (w1, w2) -> format(w1).compareTo(format(w2));

    @Override
    public Comparator<Way> getWayComparator() {
        return wayComparator;
    }

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

            name.append(')');
        }
        decorateNameWithId(name, relation);

        String result = name.toString();
        for (NameFormatterHook hook: FORMAT_HOOKS) {
            String hookResult = hook.checkFormat(relation, result);
            if (hookResult != null)
                return hookResult;
        }

        return result;
    }

    private static StringBuilder formatRelationNameAndType(Relation relation, StringBuilder result, TaggingPreset preset) {
        if (preset == null) {
            result.append(getRelationTypeName(relation));
            String relationName = getRelationName(relation);
            if (relationName == null) {
                relationName = Long.toString(relation.getId());
            } else {
                relationName = '\"' + relationName + '\"';
            }
            result.append(" (").append(relationName).append(", ");
        } else {
            preset.nameTemplate.appendText(result, relation);
            result.append('(');
        }
        return result;
    }

    private final Comparator<Relation> relationComparator = (r1, r2) -> {
        //TODO This doesn't work correctly with formatHooks

        TaggingPreset preset1 = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(r1);
        TaggingPreset preset2 = TaggingPresetNameTemplateList.getInstance().findPresetTemplate(r2);

        if (preset1 != null || preset2 != null) {
            String name11 = formatRelationNameAndType(r1, new StringBuilder(), preset1).toString();
            String name21 = formatRelationNameAndType(r2, new StringBuilder(), preset2).toString();

            int comp1 = AlphanumComparator.getInstance().compare(name11, name21);
            if (comp1 != 0)
                return comp1;
        } else {

            String type1 = getRelationTypeName(r1);
            String type2 = getRelationTypeName(r2);

            int comp2 = AlphanumComparator.getInstance().compare(type1, type2);
            if (comp2 != 0)
                return comp2;

            String name12 = getRelationName(r1);
            String name22 = getRelationName(r2);

            comp2 = AlphanumComparator.getInstance().compare(name12, name22);
            if (comp2 != 0)
                return comp2;
        }

        int comp3 = Integer.compare(r1.getMembersCount(), r2.getMembersCount());
        if (comp3 != 0)
            return comp3;


        comp3 = Boolean.compare(r1.hasIncompleteMembers(), r2.hasIncompleteMembers());
        if (comp3 != 0)
            return comp3;

        return Long.compare(r1.getUniqueId(), r2.getUniqueId());
    };

    @Override
    public Comparator<Relation> getRelationComparator() {
        return relationComparator;
    }

    private static String getRelationTypeName(IRelation relation) {
        String name = trc("Relation type", relation.get("type"));
        if (name == null) {
            name = relation.hasKey("public_transport") ? tr("public transport") : null;
        }
        if (name == null) {
            String building = relation.get("building");
            if (OsmUtils.isTrue(building)) {
                name = tr("building");
            } else if (building != null) {
                name = tr(building); // translate tag!
            }
        }
        if (name == null) {
            name = trc("Place type", relation.get("place"));
        }
        if (name == null) {
            name = tr("relation");
        }
        String adminLevel = relation.get("admin_level");
        if (adminLevel != null) {
            name += '['+adminLevel+']';
        }

        for (NameFormatterHook hook: FORMAT_HOOKS) {
            String hookResult = hook.checkRelationTypeName(relation, name);
            if (hookResult != null)
                return hookResult;
        }

        return name;
    }

    private static String getNameTagValue(IRelation relation, String nameTag) {
        if ("name".equals(nameTag)) {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true))
                return relation.getLocalName();
            else
                return relation.getName();
        } else if (":LocationCode".equals(nameTag)) {
            for (String m : relation.keySet()) {
                if (m.endsWith(nameTag))
                    return relation.get(m);
            }
            return null;
        } else if (nameTag.startsWith("?") && OsmUtils.isTrue(relation.get(nameTag.substring(1)))) {
            return tr(nameTag.substring(1));
        } else if (nameTag.startsWith("?") && OsmUtils.isFalse(relation.get(nameTag.substring(1)))) {
            return null;
        } else if (nameTag.startsWith("?")) {
            return trcLazy(nameTag, I18n.escape(relation.get(nameTag.substring(1))));
        } else {
            return trcLazy(nameTag, I18n.escape(relation.get(nameTag)));
        }
    }

    private static String getRelationName(IRelation relation) {
        String nameTag;
        for (String n : getNamingtagsForRelations()) {
            nameTag = getNameTagValue(relation, n);
            if (nameTag != null)
                return nameTag;
        }
        return null;
    }

    @Override
    public String format(Changeset changeset) {
        return tr("Changeset {0}", changeset.getId());
    }

    /**
     * Builds a default tooltip text for the primitive <code>primitive</code>.
     *
     * @param primitive the primitmive
     * @return the tooltip text
     */
    public String buildDefaultToolTip(IPrimitive primitive) {
        return buildDefaultToolTip(primitive.getId(), primitive.getKeys());
    }

    private static String buildDefaultToolTip(long id, Map<String, String> tags) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("<html><strong>id</strong>=")
          .append(id)
          .append("<br>");
        List<String> keyList = new ArrayList<>(tags.keySet());
        Collections.sort(keyList);
        for (int i = 0; i < keyList.size(); i++) {
            if (i > 0) {
                sb.append("<br>");
            }
            String key = keyList.get(i);
            sb.append("<strong>")
              .append(key)
              .append("</strong>=");
            String value = tags.get(key);
            while (!value.isEmpty()) {
                sb.append(value.substring(0, Math.min(50, value.length())));
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
     * The id is append to the {@link StringBuilder} passed in <code>name</code>.
     *
     * @param name  the name without the id
     * @param primitive the primitive
     */
    protected void decorateNameWithId(StringBuilder name, HistoryOsmPrimitive primitive) {
        if (Main.pref.getBoolean("osm-primitives.showid")) {
            name.append(tr(" [id: {0}]", primitive.getId()));
        }
    }

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
            .append(')');
        }
        decorateNameWithId(sb, node);
        return sb.toString();
    }

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
                    way.hasKey("highway") ? tr("highway") :
                    way.hasKey("railway") ? tr("railway") :
                    way.hasKey("waterway") ? tr("waterway") :
                    way.hasKey("landuse") ? tr("landuse") : ""
                    );
        }

        int nodesNo = way.isClosed() ? (way.getNumNodes() -1) : way.getNumNodes();
        String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
        if (sb.length() == 0) {
            sb.append(way.getId());
        }
        /* note: length == 0 should no longer happen, but leave the bracket code
           nevertheless, who knows what future brings */
        sb.append((sb.length() > 0) ? (" ("+nodes+')') : nodes);
        decorateNameWithId(sb, way);
        return sb.toString();
    }

    @Override
    public String format(HistoryRelation relation) {
        StringBuilder sb = new StringBuilder();
        String type = relation.get("type");
        if (type != null) {
            sb.append(type);
        } else {
            sb.append(tr("relation"));
        }
        sb.append(" (");
        String nameTag = null;
        Set<String> namingTags = new HashSet<>(getNamingtagsForRelations());
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
            sb.append('\"').append(nameTag).append("\", ");
        }

        int mbno = relation.getNumMembers();
        sb.append(trn("{0} member", "{0} members", mbno, mbno)).append(')');

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
        return buildDefaultToolTip(primitive.getId(), primitive.getTags());
    }

    /**
     * Formats the given collection of primitives as an HTML unordered list.
     * @param primitives collection of primitives to format
     * @param maxElements the maximum number of elements to display
     * @return HTML unordered list
     */
    public String formatAsHtmlUnorderedList(Collection<? extends OsmPrimitive> primitives, int maxElements) {
        Collection<String> displayNames = primitives.stream().map(x -> x.getDisplayName(this)).collect(Collectors.toList());
        return Utils.joinAsHtmlUnorderedList(Utils.limit(displayNames, maxElements, "..."));
    }

    /**
     * Formats the given primitive as an HTML unordered list.
     * @param primitive primitive to format
     * @return HTML unordered list
     */
    public String formatAsHtmlUnorderedList(OsmPrimitive primitive) {
        return formatAsHtmlUnorderedList(Collections.singletonList(primitive), 1);
    }
}
