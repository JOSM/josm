// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Removes discardable tags such as created_by from all modified objects before upload
 */
public class DiscardTagsHook implements UploadHook {

    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {
        List<OsmPrimitive> objectsToUpload = apiDataSet.getPrimitives();
        Collection<String> discardableKeys = new HashSet<String>(OsmPrimitive.getDiscardableKeys());

        boolean needsChange = false;
        OUTER: for (OsmPrimitive osm : objectsToUpload) {
            for (String key : osm.keySet()) {
                if (discardableKeys.contains(key)) {
                    needsChange = true;
                    break OUTER;
                }
            }
        }

        if (needsChange) {
            AbstractMap<String, String> map = new HashMap<String, String>();
            for (String key : discardableKeys) {
                map.put(key, null);
            }

            SequenceCommand removeKeys = new SequenceCommand(tr("Removed obsolete tags"),
                    new ChangePropertyCommand(objectsToUpload, map));
            Main.main.undoRedo.add(removeKeys);
        }
        return true;
    }

}
