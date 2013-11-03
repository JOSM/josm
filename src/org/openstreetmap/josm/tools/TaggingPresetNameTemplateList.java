// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;

/**
 * List of tagging presets with name templates, allows to find appropriate template based on existing primitive
 */
public final class TaggingPresetNameTemplateList {

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
            Collection<TaggingPresetType> type = Collections.singleton(TaggingPresetType.forPrimitive(primitive));
            if (t.typeMatches(type)) {
                if (t.nameTemplateFilter != null) {
                    if (t.nameTemplateFilter.match(primitive))
                        return t;
                    else {
                        continue;
                    }
                } else if (t.matches(type, primitive.getKeys(), false)) {
                    return t;
                }
            }
        }
        return null;
    }
}
