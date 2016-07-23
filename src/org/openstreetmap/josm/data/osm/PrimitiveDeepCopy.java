// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.OsmTransferHandler;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;

/**
 * This class allows to create and keep a deep copy of primitives. Provides methods to access directly added
 * primitives and reference primitives
 * <p>
 * To be removed end of 2016
 * @since 2305
 * @deprecated This has been replaced by Swing Copy+Paste support. Use {@link OsmTransferHandler} instead.
 */
@Deprecated
public class PrimitiveDeepCopy {

    /**
     * Constructs a new {@code PrimitiveDeepCopy} without data.
     */
    public PrimitiveDeepCopy() {
        // Do nothing
    }

    /**
     * Gets the list of primitives that were explicitly added to this copy.
     * @return The added primitives
     */
    public List<PrimitiveData> getDirectlyAdded() {
        try {
            PrimitiveTransferData data = (PrimitiveTransferData) ClipboardUtils.getClipboard().getData(PrimitiveTransferData.DATA_FLAVOR);
            return new ArrayList<>(data.getDirectlyAdded());
        } catch (UnsupportedFlavorException | IOException e) {
            Main.debug(e);
            return Collections.emptyList();
        }
    }

    public boolean isEmpty() {
        return !ClipboardUtils.getClipboard().isDataFlavorAvailable(PrimitiveTransferData.DATA_FLAVOR);
    }
}
