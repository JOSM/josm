// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Removes discardable tags such as created_by from all modified objects before upload
 */
public class DiscardTagsHook implements UploadHook {

    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {
        List<OsmPrimitive> objectsToUpload = apiDataSet.getPrimitives();
        Collection<String> discardableKeys = new HashSet<>(AbstractPrimitive.getDiscardableKeys());

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
            Map<String, String> map = new HashMap<>();
            for (String key : discardableKeys) {
                map.put(key, null);
            }

            SequenceCommand removeKeys = new SequenceCommand(tr("Removed obsolete tags"),
                    new ChangePropertyCommand(objectsToUpload, map));
            MainApplication.undoRedo.add(removeKeys);
        }
        return true;
    }

}
