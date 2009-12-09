package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;

public class ApiPreconditionCheckerHook implements UploadHook {

    public boolean checkUpload(APIDataSet apiData) {
        OsmApi api = OsmApi.getOsmApi();
        try {
            // FIXME: this should run asynchronously and a progress monitor
            // should be displayed.
            api.initialize(NullProgressMonitor.INSTANCE);
            long maxNodes = 0;
            if (api.getCapabilities().isDefined("waynodes", "maximum")) {
                maxNodes = api.getCapabilities().getLong("waynodes","maximum");
            }
            if (maxNodes > 0) {
                if( !checkMaxNodes(apiData.getPrimitivesToAdd(), maxNodes))
                    return false;
                if( !checkMaxNodes(apiData.getPrimitivesToUpdate(), maxNodes))
                    return false;
                if( !checkMaxNodes(apiData.getPrimitivesToDelete(), maxNodes))
                    return false;
            }
        } catch (OsmApiInitializationException e) {
            ExceptionDialogUtil.explainOsmTransferException(e);
            return false;
        }
        return true;
    }

    private boolean checkMaxNodes(Collection<OsmPrimitive> primitives, long maxNodes) {
        for (OsmPrimitive osmPrimitive : primitives) {
            for (Entry<String,String> e : osmPrimitive.entrySet()) {
                if(e.getValue().length() > 255) {
                    if (osmPrimitive.isDeleted()) {
                        // if OsmPrimitive is going to be deleted we automatically shorten the
                        // value
                        System.out.println(
                                tr("Warning: automatically truncating value of tag ''{0}'' on deleted object {1}",
                                        e.getKey(),
                                        Long.toString(osmPrimitive.getId())
                                )
                        );
                        osmPrimitive.put(e.getKey(), e.getValue().substring(0, 255));
                        continue;
                    }
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("Length of value for tag ''{0}'' on object {1} exceeds the max. allowed length {2}. Values length is {3}.",
                                    e.getKey(), Long.toString(osmPrimitive.getId()), 255, e.getValue().length()
                            ),
                            tr("Precondition Violation"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    Main.main.getCurrentDataSet().setSelected(Collections.singleton(osmPrimitive));
                    return false;
                }
            }

            if (osmPrimitive instanceof Way &&
                    ((Way)osmPrimitive).getNodesCount() > maxNodes) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("{0} nodes in way {1} exceed the max. allowed number of nodes {2}",
                                ((Way)osmPrimitive).getNodesCount(),
                                Long.toString(osmPrimitive.getId()),
                                maxNodes
                        ),
                        tr("API Capabilities Violation"),
                        JOptionPane.ERROR_MESSAGE
                );
                Main.main.getCurrentDataSet().setSelected(Collections.singleton(osmPrimitive));
                return false;
            }
        }
        return true;
    }
}
