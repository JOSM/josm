// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;

/**
 * This is a transferable that only transfers the tags.
 * @author Michael Zangl
 * @since 10637
 */
public class TagTransferable implements Transferable {
    private final TagTransferData data;

    /**
     * Transfer the tag transfer data.
     * @param data The data.
     */
    public TagTransferable(TagTransferData data) {
        this.data = data;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {TagTransferData.FLAVOR, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Stream.of(getTransferDataFlavors()).anyMatch(f -> f.equals(flavor));
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return getStringData();
        } else if (TagTransferData.FLAVOR.equals(flavor)) {
            return data;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    private String getStringData() {
        StringBuilder string = new StringBuilder();
        for (Entry<String, String> e : data.getTags().entrySet()) {
            if (string.length() > 0) {
                string.append('\n');
            }
            string.append(e.getKey())
                .append('=')
                .append(e.getValue());
        }
        return string.toString();
    }
}
