// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Check;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Combo;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Text;

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

    private final List<TaggingPreset> presetsWithPattern = new ArrayList<TaggingPreset>();

    private TaggingPresetNameTemplateList() {
        for (TaggingPreset tp: TaggingPresetPreference.taggingPresets) {
            if (tp.nameTemplate != null) {
                presetsWithPattern.add(tp);
            }
        }
    }

    public TaggingPreset findPresetTemplate(OsmPrimitive primitive) {

        PresetType presetType;
        switch (primitive.getType()) {
        case NODE:
            presetType = PresetType.NODE;
            break;
        case WAY:
            if (((Way) primitive).isClosed()) {
                presetType = PresetType.CLOSEDWAY;
            } else {
                presetType = PresetType.WAY;
            }
            break;
        case RELATION:
            presetType = PresetType.RELATION;
            break;
        default:
            throw new AssertionError();
        }

        for(TaggingPreset t : presetsWithPattern) {


            if (       t.types == null
                    || t.types.contains(presetType)
                    || (presetType == PresetType.CLOSEDWAY && t.types.contains(PresetType.WAY))) {
                int found = 0;

                if (t.nameTemplateFilter != null) {
                    if (t.nameTemplateFilter.match(primitive))
                        return t;
                    else {
                        continue;
                    }
                }

                for(TaggingPreset.Item i : t.data) {
                    if(i instanceof TaggingPreset.Key) {
                        String val = ((TaggingPreset.Key)i).value;
                        String key = ((TaggingPreset.Key)i).key;
                        // we subtract 100 if not found and add 1 if found
                        if (val != null && val.equals(primitive.get(key))) {
                            found+=1;
                        } else {
                            found-=100;
                        }
                    } else {
                        String key = null;
                        if ((i instanceof Text) && ((Text)i).required) {
                            key = ((Text)i).key;
                        } else if ((i instanceof Combo) && ((Combo)i).required) {
                            key = ((Combo)i).key;
                        } else if ((i instanceof Check) && ((Check)i).required) {
                            key = ((Check)i).key;
                        }
                        if (key != null) {
                            if (primitive.get(key) != null) {
                                found += 1;
                            } else {
                                found -= 100;
                            }
                        }
                    }
                }

                if(found > 0)
                    return t; // First matching preset wins
            }
        }

        return null;

    }


}
