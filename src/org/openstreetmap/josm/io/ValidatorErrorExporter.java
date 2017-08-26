// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.ValidatorLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * Exporter to write validator errors to an XML file.
 * @since 12667
 */
public class ValidatorErrorExporter extends FileExporter {

    /** File extension filter for .xml files */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "xml", "xml", tr("Validation Error Files") + " (*.xml)");

    /** Create a new validator error exporter with the default .xml file filter */
    public ValidatorErrorExporter() {
        super(FILE_FILTER);
    }

    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        if (!(layer instanceof ValidatorLayer))
            return false;
        return super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        if (layer instanceof ValidatorLayer && editLayer != null) {
            Logging.info("exporting validation errors to file: " + file);
            try (OutputStream os = new FileOutputStream(file);
                 ValidatorErrorWriter writer = new ValidatorErrorWriter(os)) {
                writer.write(editLayer.validationErrors);
            }
        }
    }
}
