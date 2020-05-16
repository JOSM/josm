// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.Changeset;

/**
 * This class allows to transfer a list of {@link Changeset}s
 */
public class ChangesetTransferable implements Transferable {
    private final List<Changeset> changesets;

    /**
     * Constructs a new transferable
     * @param changesets the list of changesets
     */
    public ChangesetTransferable(List<Changeset> changesets) {
        this.changesets = changesets;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Stream.of(getTransferDataFlavors()).anyMatch(f -> f.equals(flavor));
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return changesets.stream().map(Changeset::toString).collect(Collectors.joining("\n"));
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
