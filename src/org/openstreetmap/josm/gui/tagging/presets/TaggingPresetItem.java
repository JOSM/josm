// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.util.LruCache;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Class that represents single part of a preset - one field or text label that is shown to user
 * @since 6068
 */
public abstract class TaggingPresetItem {

    // cache the parsing of types using a LRU cache
    private static final Map<String, Set<TaggingPresetType>> TYPE_CACHE = new LruCache<>(16);;

    protected void initAutoCompletionField(AutoCompletingTextField field, String... key) {
        initAutoCompletionField(field, Arrays.asList(key));
    }

    protected void initAutoCompletionField(AutoCompletingTextField field, List<String> keys) {
        DataSet data = OsmDataManager.getInstance().getEditDataSet();
        if (data == null) {
            return;
        }
        AutoCompletionList list = new AutoCompletionList();
        AutoCompletionManager.of(data).populateWithTagValues(list, keys);
        field.setAutoCompletionList(list);
    }

    /**
     * Called by {@link TaggingPreset#createPanel} during tagging preset panel creation.
     * All components defining this tagging preset item must be added to given panel.
     *
     * @param p The panel where components must be added
     * @param sel The related selected OSM primitives
     * @param presetInitiallyMatches Whether this {@link TaggingPreset} already matched before applying,
     *                               i.e. whether the map feature already existed on the primitive.
     * @return {@code true} if this item adds semantic tagging elements, {@code false} otherwise.
     */
    protected abstract boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches);

    /**
     * Adds the new tags to apply to selected OSM primitives when the preset holding this item is applied.
     * @param changedTags The list of changed tags to modify if needed
     */
    protected abstract void addCommands(List<Tag> changedTags);

    /**
     * Tests whether the tags match this item.
     * Note that for a match, at least one positive and no negative is required.
     * @param tags the tags of an {@link OsmPrimitive}
     * @return {@code true} if matches (positive), {@code null} if neutral, {@code false} if mismatches (negative).
     */
    public Boolean matches(Map<String, String> tags) {
        return null;
    }

    protected static Set<TaggingPresetType> getType(String types) throws SAXException {
        if (types == null || types.isEmpty()) {
            throw new SAXException(tr("Unknown type: {0}", types));
        }
        if (TYPE_CACHE.containsKey(types))
            return TYPE_CACHE.get(types);
        Set<TaggingPresetType> result = EnumSet.noneOf(TaggingPresetType.class);
        for (String type : Arrays.asList(types.split(","))) {
            try {
                TaggingPresetType presetType = TaggingPresetType.fromString(type);
                if (presetType != null) {
                    result.add(presetType);
                }
            } catch (IllegalArgumentException e) {
                throw new SAXException(tr("Unknown type: {0}", type), e);
            }
        }
        TYPE_CACHE.put(types, result);
        return result;
    }

    protected static String fixPresetString(String s) {
        return s == null ? s : s.replace("'", "''");
    }

    protected static String getLocaleText(String text, String textContext, String defaultText) {
        if (text == null) {
            return defaultText;
        } else if (textContext != null) {
            return trc(textContext, fixPresetString(text));
        } else {
            return tr(fixPresetString(text));
        }
    }

    protected static Integer parseInteger(String str) {
        if (str == null || str.isEmpty())
            return null;
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            Logging.trace(e);
        }
        return null;
    }

    protected static ImageIcon loadImageIcon(String iconName, File zipIcons, Integer maxSize) {
        final Collection<String> s = Config.getPref().getList("taggingpreset.icon.sources", null);
        ImageProvider imgProv = new ImageProvider(iconName).setDirs(s).setId("presets").setArchive(zipIcons).setOptional(true);
        if (maxSize != null && maxSize > 0) {
            imgProv.setMaxSize(maxSize);
        }
        return imgProv.get();
    }

    /**
     * Determine whether the given preset items match the tags
     * @param data the preset items
     * @param tags the tags to match
     * @return whether the given preset items match the tags
     * @since 9932
     */
    public static boolean matches(Iterable<? extends TaggingPresetItem> data, Map<String, String> tags) {
        boolean atLeastOnePositiveMatch = false;
        for (TaggingPresetItem item : data) {
            Boolean m = item.matches(tags);
            if (m != null && !m)
                return false;
            else if (m != null) {
                atLeastOnePositiveMatch = true;
            }
        }
        return atLeastOnePositiveMatch;
    }
}
