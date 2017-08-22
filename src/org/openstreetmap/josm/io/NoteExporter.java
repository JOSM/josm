// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * Exporter to write note data to an XML file
 */
public class NoteExporter extends FileExporter {

    /** File extension filter for .osn files */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osn", "osn", tr("Note Files") + " (*.osn)");

    /** Create a new note exporter with the default .osn file filter */
    public NoteExporter() {
        super(FILE_FILTER);
    }

    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        if (!(layer instanceof NoteLayer))
            return false;
        return super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        Logging.info("exporting notes to file: " + file);
        if (layer instanceof NoteLayer) {
            try (OutputStream os = new FileOutputStream(file);
                 NoteWriter writer = new NoteWriter(os)) {
                writer.write(((NoteLayer) layer).getNoteData());
            }
        }
    }
}
