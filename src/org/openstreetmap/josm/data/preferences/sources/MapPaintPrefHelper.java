// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * Helper class for map paint styles preferences.
 * @since 12649 (extracted from gui.preferences package)
 */
public class MapPaintPrefHelper extends SourcePrefHelper {

    /**
     * The unique instance.
     */
    public static final MapPaintPrefHelper INSTANCE = new MapPaintPrefHelper();

    /**
     * Constructs a new {@code MapPaintPrefHelper}.
     */
    public MapPaintPrefHelper() {
        super("mappaint.style.entries", SourceType.MAP_PAINT_STYLE);
    }

    @Override
    public List<SourceEntry> get() {
        List<SourceEntry> ls = super.get();
        if (insertNewDefaults(ls)) {
            put(ls);
        }
        return ls;
    }

    /**
     * If the selection of default styles changes in future releases, add
     * the new entries to the user-configured list. Remember the known URLs,
     * so an item that was deleted explicitly is not added again.
     * @param list new defaults
     * @return {@code true} if a change occurred
     */
    private boolean insertNewDefaults(List<SourceEntry> list) {
        boolean changed = false;

        Collection<String> knownDefaults = new TreeSet<>(Config.getPref().getList("mappaint.style.known-defaults"));

        Collection<ExtendedSourceEntry> defaults = getDefault();
        int insertionIdx = 0;
        for (final SourceEntry def : defaults) {
            int i = Utils.indexOf(list, se -> Objects.equals(def.url, se.url));
            if (i == -1 && !knownDefaults.contains(def.url)) {
                def.active = false;
                list.add(insertionIdx, def);
                insertionIdx++;
                changed = true;
            } else {
                if (i >= insertionIdx) {
                    insertionIdx = i + 1;
                }
            }
            knownDefaults.add(def.url);
        }
        Config.getPref().putList("mappaint.style.known-defaults", new ArrayList<>(knownDefaults));

        return changed;
    }

    @Override
    public Collection<ExtendedSourceEntry> getDefault() {
        ExtendedSourceEntry defJosmMapcss = new ExtendedSourceEntry(type, "elemstyles.mapcss", "resource://styles/standard/elemstyles.mapcss");
        defJosmMapcss.active = true;
        defJosmMapcss.name = "standard";
        defJosmMapcss.icon = new ImageProvider("logo").getResource();
        defJosmMapcss.title = tr("JOSM default (MapCSS)");
        defJosmMapcss.description = tr("Internal style to be used as base for runtime switchable overlay styles");
        return Collections.singletonList(defJosmMapcss);
    }

    @Override
    public Map<String, String> serialize(SourceEntry entry) {
        Map<String, String> res = super.serialize(entry);
        res.put("active", Boolean.toString(entry.active));
        if (entry.name != null) {
            res.put("ptoken", entry.name);
        }
        return res;
    }

    @Override
    public SourceEntry deserialize(Map<String, String> s) {
        return new SourceEntry(type, s.get("url"), s.get("ptoken"), s.get("title"), Boolean.parseBoolean(s.get("active")));
    }
}
