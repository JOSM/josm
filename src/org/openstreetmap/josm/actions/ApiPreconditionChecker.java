package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction.UploadHook;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;

public class ApiPreconditionChecker implements UploadHook {

    public boolean checkUpload(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update,
            Collection<OsmPrimitive> delete) {
        OsmApi api = OsmApi.getOsmApi();
        try {
            api.initialize();
            long maxNodes = 0;
            if (api.getCapabilities().isDefined("waynodes", "maximum")) {
                maxNodes = api.getCapabilities().getLong("waynodes","maximum");
            }
            long maxElements = 0;
            if (api.getCapabilities().isDefined("changesets", "maximum_elements")) {
                maxElements = api.getCapabilities().getLong("changesets", "maximum_elements");
            }

            if (maxNodes > 0) {
                if( !checkMaxNodes(add, maxNodes))
                    return false;
                if( !checkMaxNodes(update, maxNodes))
                    return false;
                if( !checkMaxNodes(delete, maxNodes))
                    return false;
            }

            if (maxElements  > 0) {
                int total = 0;
                total = add.size() + update.size() + delete.size();
                if(total > maxElements) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Current number of changes exceeds the max. number of changes, current is {0}, max is {1}",
                                    total,
                                    maxElements
                            ),
                            tr("API Capabilities Violation"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
            }
        } catch (OsmApiInitializationException e) {
            ExceptionDialogUtil.explainOsmTransferException(e);
            return false;
        }
        return true;
    }

    private boolean checkMaxNodes(Collection<OsmPrimitive> add, long maxNodes) {
        for (OsmPrimitive osmPrimitive : add) {
            for (Entry<String,String> e : osmPrimitive.entrySet()) {
                if(e.getValue().length() > 255) {
                    if (osmPrimitive.isDeleted()) {
                        // if OsmPrimitive is going to be deleted we automatically shorten the
                        // value
                        System.out.println(
                                tr("Warning: automatically truncating value of tag ''{0}'' on deleted primitive {1}",
                                        e.getKey(),
                                        Long.toString(osmPrimitive.getId())
                                )
                        );
                        osmPrimitive.put(e.getKey(), e.getValue().substring(0, 255));
                        continue;
                    }
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("Length of value for tag ''{0}'' on primitive {1} exceeds the max. allowed length {2}. Values length is {3}.",
                                    e.getKey(), Long.toString(osmPrimitive.getId()), 255, e.getValue().length()
                            ),
                            tr("Precondition Violation"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    List<OsmPrimitive> newNodes = new LinkedList<OsmPrimitive>();
                    newNodes.add(osmPrimitive);
                    Main.main.getCurrentDataSet().setSelected(newNodes);
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
                List<OsmPrimitive> newNodes = new LinkedList<OsmPrimitive>();
                newNodes.add(osmPrimitive);

                Main.main.getCurrentDataSet().setSelected(newNodes);
                return false;
            }
        }
        return true;
    }
}
