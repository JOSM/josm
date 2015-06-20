// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Export a WMS layer to a serialized binary file that can be imported later via {@link WMSLayerImporter}.
 *
 * @since 5457
 */
public class WMSLayerExporter extends FileExporter {

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
        if (layer instanceof WMSLayer) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                ((WMSLayer) layer).writeExternal(oos);
            }
        }
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        setEnabled(newLayer instanceof WMSLayer);
    }
}
