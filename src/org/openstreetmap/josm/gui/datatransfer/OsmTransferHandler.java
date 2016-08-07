// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.TransferHandler;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.datatransfer.importers.AbstractOsmDataPaster;
import org.openstreetmap.josm.gui.datatransfer.importers.FilePaster;
import org.openstreetmap.josm.gui.datatransfer.importers.PrimitiveDataPaster;
import org.openstreetmap.josm.gui.datatransfer.importers.PrimitiveTagTransferPaster;
import org.openstreetmap.josm.gui.datatransfer.importers.TagTransferPaster;
import org.openstreetmap.josm.gui.datatransfer.importers.TextTagPaster;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This transfer hanlder provides the ability to transfer OSM data. It allows you to receive files, primitives or tags.
 * @author Michael Zangl
 * @since 10604
 */
public class OsmTransferHandler extends TransferHandler {

    private static final Collection<AbstractOsmDataPaster> SUPPORTED = Arrays.asList(
            new FilePaster(), new PrimitiveDataPaster(),
            new PrimitiveTagTransferPaster(),
            new TagTransferPaster(), new TextTagPaster());

    @Override
    public boolean canImport(TransferSupport support) {
        // import everything for now, only support copy.
        for (AbstractOsmDataPaster df : SUPPORTED) {
            if (df.supports(support)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean importData(TransferSupport support) {
        return importData(support, Main.getLayerManager().getEditLayer(), null);
    }

    private boolean importData(TransferSupport support, OsmDataLayer layer, EastNorth center) {
        for (AbstractOsmDataPaster df : SUPPORTED) {
            if (df.supports(support)) {
                try {
                    if (df.importData(support, layer, center)) {
                        return true;
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    Main.warn(e);
                }
            }
        }
        return super.importData(support);
    }

    private boolean importTags(TransferSupport support, Collection<? extends OsmPrimitive> primitives) {
        for (AbstractOsmDataPaster df : SUPPORTED) {
            if (df.supports(support)) {
                try {
                    if (df.importTagsOn(support, primitives)) {
                        return true;
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    Main.warn(e);
                }
            }
        }
        return super.importData(support);
    }

    /**
     * Paste the current clipboard current at the given position
     * @param editLayer The layer to paste on.
     * @param mPosition The position to paste at. If it is <code>null</code>, the original position will be used.
     */
    public void pasteOn(OsmDataLayer editLayer, EastNorth mPosition) {
        Transferable transferable = ClipboardUtils.getClipboard().getContents(null);
        pasteOn(editLayer, mPosition, transferable);
    }

    /**
     * Paste the given clipboard current at the given position
     * @param editLayer The layer to paste on.
     * @param mPosition The position to paste at. If it is <code>null</code>, the original position will be used.
     * @param transferable The transferable to use.
     */
    public void pasteOn(OsmDataLayer editLayer, EastNorth mPosition, Transferable transferable) {
        importData(new TransferSupport(Main.panel, transferable), editLayer, mPosition);
    }

    /**
     * Paste the given tags on the primitives.
     * @param primitives The primitives to paste on.
     */
    public void pasteTags(Collection<? extends OsmPrimitive> primitives) {
        Transferable transferable = ClipboardUtils.getClipboard().getContents(null);
        importTags(new TransferSupport(Main.panel, transferable), primitives);
    }

    /**
     * Check if any primitive data or any other supported data is available in the clipboard.
     * @return <code>true</code> if any flavor is supported.
     */
    public boolean isDataAvailable() {
        try {
            Collection<DataFlavor> available = Arrays.asList(ClipboardUtils.getClipboard().getAvailableDataFlavors());
            for (AbstractOsmDataPaster s : SUPPORTED) {
                if (s.supports(available)) {
                    return true;
                }
            }
        } catch (IllegalStateException e) {
            Main.debug(e);
        }
        return false;
    }
}
