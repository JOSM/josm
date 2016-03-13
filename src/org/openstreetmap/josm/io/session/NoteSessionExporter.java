// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.IOException;
import java.io.OutputStream;

import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.io.NoteWriter;

/**
 * Session exporter for {@link NoteLayer}.
 * @since 9746
 */
public class NoteSessionExporter extends GenericSessionExporter<NoteLayer> {

    /**
     * Constructs a new {@code NoteSessionExporter}.
     * @param layer Note layer to export
     */
    public NoteSessionExporter(NoteLayer layer) { // NO_UCD (unused code)
        super(layer, "osm-notes", "0.1", "osn");
    }

    @Override
    protected void addDataFile(OutputStream out) throws IOException {
        @SuppressWarnings("resource")
        NoteWriter writer = new NoteWriter(out);
        writer.write(layer.getNoteData());
        writer.flush();
    }
}
