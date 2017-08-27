// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Export a WMS layer to a serialized binary file that can be imported later via {@link WMSLayerImporter}.
 *
 * @since 5457
 */
public class WMSLayerExporter extends FileExporter {

    /** Which version of the file we export */
    public static final int CURRENT_FILE_VERSION = 6;

    /**
     * Constructs a new {@code WMSLayerExporter}
     */
    public WMSLayerExporter() {
        super(WMSLayerImporter.FILE_FILTER);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(file, "file");
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");

        if (layer instanceof AbstractTileSourceLayer) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeInt(CURRENT_FILE_VERSION); // file version
                oos.writeObject(MainApplication.getMap().mapView.getCenter());
                ImageryPreferenceEntry entry = new ImageryPreferenceEntry(((AbstractTileSourceLayer) layer).getInfo());
                oos.writeObject(Preferences.serializeStruct(entry, ImageryPreferenceEntry.class));
            }
        }
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        setEnabled(e.getSource().getActiveLayer() instanceof AbstractTileSourceLayer);
    }
}
