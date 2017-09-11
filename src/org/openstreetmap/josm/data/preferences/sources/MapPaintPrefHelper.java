// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.openstreetmap.josm.Main;
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

        Collection<String> knownDefaults = new TreeSet<>(Main.pref.getCollection("mappaint.style.known-defaults"));

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
        Main.pref.putCollection("mappaint.style.known-defaults", knownDefaults);

        // XML style is not bundled anymore
        list.remove(Utils.find(list, se -> "resource://styles/standard/elemstyles.xml".equals(se.url)));

        return changed;
    }

    @Override
    public Collection<ExtendedSourceEntry> getDefault() {
        ExtendedSourceEntry defJosmMapcss = new ExtendedSourceEntry(type, "elemstyles.mapcss", "resource://styles/standard/elemstyles.mapcss");
        defJosmMapcss.active = true;
        defJosmMapcss.name = "standard";
        defJosmMapcss.title = tr("JOSM default (MapCSS)");
        defJosmMapcss.description = tr("Internal style to be used as base for runtime switchable overlay styles");
        ExtendedSourceEntry defPL2 = new ExtendedSourceEntry(type, "potlatch2.mapcss", "resource://styles/standard/potlatch2.mapcss");
        defPL2.active = false;
        defPL2.name = "standard";
        defPL2.title = tr("Potlatch 2");
        defPL2.description = tr("the main Potlatch 2 style");

        return Arrays.asList(defJosmMapcss, defPL2);
    }

    @Override
    public Map<String, String> serialize(SourceEntry entry) {
        Map<String, String> res = new HashMap<>();
        res.put("url", entry.url == null ? "" : entry.url);
        res.put("title", entry.title == null ? "" : entry.title);
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
