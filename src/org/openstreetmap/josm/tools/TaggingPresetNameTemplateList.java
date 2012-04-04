// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;

/**
 * List of tagging presets with name templates, allows to find appropriate template based on existing primitive
 */
public class TaggingPresetNameTemplateList {

    private static TaggingPresetNameTemplateList instance;

    public static TaggingPresetNameTemplateList getInstance() {
        if (instance == null) {
            instance = new TaggingPresetNameTemplateList();
        }
        return instance;
    }
    private final List<TaggingPreset> presetsWithPattern = new LinkedList<TaggingPreset>();

    private TaggingPresetNameTemplateList() {
        if (TaggingPresetPreference.taggingPresets != null) {
            for (TaggingPreset tp : TaggingPresetPreference.taggingPresets) {
                if (tp.nameTemplate != null) {
                    presetsWithPattern.add(tp);
                }
            }
        }
    }

    public TaggingPreset findPresetTemplate(OsmPrimitive primitive) {

        for (TaggingPreset t : presetsWithPattern) {
            if (t.matches(EnumSet.of(PresetType.forPrimitive(primitive)), primitive.getKeys(), false)) {
                return t;
            }
        }
        return null;
    }
}
