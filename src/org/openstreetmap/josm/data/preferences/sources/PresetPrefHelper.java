// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.tools.ImageProvider;

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
        super("taggingpreset.entries", SourceType.TAGGING_PRESET);
    }

    @Override
    public Collection<ExtendedSourceEntry> getDefault() {
        ExtendedSourceEntry i = new ExtendedSourceEntry(type, "defaultpresets.xml", "resource://data/defaultpresets.xml");
        i.title = tr("Internal Preset");
        i.icon = new ImageProvider("logo").getResource();
        i.description = tr("The default preset for JOSM");
        return Collections.singletonList(i);
    }
}
