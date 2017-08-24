// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.importers.AbstractOsmDataPaster;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * A transfer handler class that allows you to manage a prioritized stack of transfer handlers.
 * @author Michael Zangl
 * @since 10881
 */
public abstract class AbstractStackTransferHandler extends TransferHandler {

    protected abstract Collection<AbstractOsmDataPaster> getSupportedPasters();

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        // import everything for now, only support copy.
        for (AbstractOsmDataPaster df : getSupportedPasters()) {
            if (df.supports(support)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean importData(TransferSupport support) {
        return importData(support, MainApplication.getLayerManager().getEditLayer(), null);
    }

    protected boolean importData(TransferSupport support, OsmDataLayer layer, EastNorth center) {
        for (AbstractOsmDataPaster df : getSupportedPasters()) {
            if (df.supports(support)) {
                try {
                    if (Logging.isDebugEnabled()) {
                        Logging.debug("{0} pasting {1} at {2}",
                                df.getClass().getSimpleName(), Arrays.toString(support.getDataFlavors()), center);
                    }
                    if (df.importData(support, layer, center)) {
                        return true;
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    Logging.warn(e);
                } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                    BugReport.intercept(e).put("paster", df).put("flavors", support::getDataFlavors).warn();
                }
            }
        }
        return super.importData(support);
    }
}
