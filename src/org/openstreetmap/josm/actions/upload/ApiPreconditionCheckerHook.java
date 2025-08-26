// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks certain basic conditions, that are listed in the OSM API
 * {@link org.openstreetmap.josm.io.Capabilities}.
 */
public class ApiPreconditionCheckerHook implements UploadHook {

    @Override
    public boolean checkUpload(APIDataSet apiData) {
        OsmApi api = OsmApi.getOsmApi();
        try {
            if (NetworkManager.isOffline(OnlineResource.OSM_API)) {
                return false;
            }
            // FIXME: this should run asynchronously and a progress monitor
            // should be displayed.
            api.initialize(NullProgressMonitor.INSTANCE);
            long maxNodes = api.getCapabilities().getMaxWayNodes();
            if (maxNodes > 0) {
                if (!checkMaxNodes(apiData.getPrimitivesToAdd(), maxNodes))
                    return false;
                if (!checkMaxNodes(apiData.getPrimitivesToUpdate(), maxNodes))
                    return false;
                if (!checkMaxNodes(apiData.getPrimitivesToDelete(), maxNodes))
                    return false;
            }
        } catch (OsmTransferCanceledException e) {
            Logging.trace(e);
            return false;
        } catch (OsmApiInitializationException e) {
            ExceptionDialogUtil.explainOsmTransferException(e);
            return false;
        }
        return true;
    }

    private static boolean checkMaxNodes(Collection<OsmPrimitive> primitives, long maxNodes) {
        for (OsmPrimitive osmPrimitive : primitives) {
            for (Map.Entry<String, String> entry: osmPrimitive.getKeys().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!Utils.checkCodePointCount(value, Tagged.MAX_TAG_LENGTH)) {
                    if (osmPrimitive.isDeleted()) {
                        // if OsmPrimitive is going to be deleted we automatically shorten the value
                        Logging.warn(
                                tr("Automatically truncating value of tag ''{0}'' on deleted object {1}",
                                        key,
                                        Long.toString(osmPrimitive.getId())
                                )
                        );
                        osmPrimitive.put(key, Utils.shortenString(value, Tagged.MAX_TAG_LENGTH));
                        continue;
                    }
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            tr("Length of value for tag ''{0}'' on object {1} exceeds the max. allowed length {2}. Values length is {3}.",
                                    key, Long.toString(osmPrimitive.getId()), Tagged.MAX_TAG_LENGTH, Utils.getCodePointCount(value)
                            ),
                            tr("Precondition violation"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    MainApplication.getLayerManager().getEditDataSet().setSelected(Collections.singleton(osmPrimitive));
                    return false;
                }
            }

            if (osmPrimitive instanceof Way &&
                    ((Way) osmPrimitive).getNodesCount() > maxNodes) {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("{0} nodes in way {1} exceed the max. allowed number of nodes {2}",
                                ((Way) osmPrimitive).getNodesCount(),
                                Long.toString(osmPrimitive.getId()),
                                maxNodes
                        ),
                        tr("API Capabilities Violation"),
                        JOptionPane.ERROR_MESSAGE
                );
                MainApplication.getLayerManager().getEditDataSet().setSelected(Collections.singleton(osmPrimitive));
                return false;
            }
        }
        return true;
    }
}
