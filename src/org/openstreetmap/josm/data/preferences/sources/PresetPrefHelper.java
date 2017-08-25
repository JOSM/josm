// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for tagging presets preferences.
 * @since 12649 (extracted from gui.preferences package)
 */
public class PresetPrefHelper extends SourcePrefHelper {

    /**
     * The unique instance.
     */
    public static final PresetPrefHelper INSTANCE = new PresetPrefHelper();

    /**
     * Constructs a new {@code PresetPrefHelper}.
     */
    public PresetPrefHelper() {
        super("taggingpreset.entries");
    }

    @Override
    public Collection<ExtendedSourceEntry> getDefault() {
        ExtendedSourceEntry i = new ExtendedSourceEntry("defaultpresets.xml", "resource://data/defaultpresets.xml");
        i.title = tr("Internal Preset");
        i.description = tr("The default preset for JOSM");
        return Collections.singletonList(i);
    }

    @Override
    public Map<String, String> serialize(SourceEntry entry) {
        Map<String, String> res = new HashMap<>();
        res.put("url", entry.url);
        res.put("title", entry.title == null ? "" : entry.title);
        return res;
    }

    @Override
    public SourceEntry deserialize(Map<String, String> s) {
        return new SourceEntry(s.get("url"), null, s.get("title"), true);
    }
}
