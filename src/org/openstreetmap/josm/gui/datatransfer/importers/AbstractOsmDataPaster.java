// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;

import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This is an abstract class that helps implementing the transfer support required by swing.
 * <p>
 * It implements a mechanism to import a given data flavor into the current OSM data layer.
 * @author Michael Zangl
 * @since 10604
 */
public abstract class AbstractOsmDataPaster {
    protected final DataFlavor df;

    /**
     * Create a new {@link AbstractOsmDataPaster}
     * @param df The data flavor that this support supports.
     */
    protected AbstractOsmDataPaster(DataFlavor df) {
        this.df = df;
    }

    /**
     * Checks if this supports importing the given transfer support.
     * @param support The support that should be supported.
     * @return True if we support that transfer.
     */
    public boolean supports(TransferSupport support) {
        return support.isDataFlavorSupported(df) && isCopy(support);
    }

    /**
     * Checks if this supports any of the available flavors.
     * @param available The flavors that should be supported
     * @return True if any of them is supported.
     */
    public boolean supports(Collection<DataFlavor> available) {
        return available.contains(df);
    }

    private static boolean isCopy(TransferSupport support) {
        return !support.isDrop() || (TransferHandler.COPY & support.getSourceDropActions()) == TransferHandler.COPY;
    }

    /**
     * Attempts to import the given transfer data.
     * @param support The transfer support to import from.
     * @param layer The layer to paste at. May be null.
     * @param pasteAt The position to paste at.
     * @return <code>true</code> if the import was successful.
     * @throws UnsupportedFlavorException if the requested data flavor is not supported
     * @throws IOException if an I/O error occurs
     */
    public abstract boolean importData(TransferSupport support, OsmDataLayer layer, EastNorth pasteAt)
            throws UnsupportedFlavorException, IOException;

    /**
     * Imports only if this import changes the tags only. Does nothing if more than tags would be changed.
     * @param support The support
     * @param selection The primitives to apply on.
     * @return <code>true</code> if an import was done.
     * @throws UnsupportedFlavorException if the requested data flavor is not supported
     * @throws IOException if an I/O error occurs
     */
    public boolean importTagsOn(TransferSupport support, Collection<? extends OsmPrimitive> selection)
            throws UnsupportedFlavorException, IOException {
        return false;
    }
}
