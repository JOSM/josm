// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.xml.sax.SAXException;

/**
 * File importer that reads note dump files (*.osn, .osn.gz and .osn.bz2)
 * @since 7538
 */
public class NoteImporter extends FileImporter {

    private static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "osn", "osn", tr("Note Files"), true);

    /** Create an importer for note dump files */
    public NoteImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(final File file, ProgressMonitor progressMonitor) throws IOException {
        if (Main.isDebugEnabled()) {
            Main.debug("importing notes file " + file.getAbsolutePath());
        }
        try (InputStream is = Compression.getUncompressedFileInputStream(file)) {
            final List<Note> fileNotes = new NoteReader(is).parse();

            List<NoteLayer> noteLayers = null;
            if (Main.map != null) {
                noteLayers = Main.map.mapView.getLayersOfType(NoteLayer.class);
            }
            if (noteLayers != null && !noteLayers.isEmpty()) {
                noteLayers.get(0).getNoteData().addNotes(fileNotes);
            } else {
                GuiHelper.runInEDT(new Runnable() {
                    @Override
                    public void run() {
                        Main.main.addLayer(new NoteLayer(fileNotes, file.getName()));
                    }
                });
            }
        } catch (SAXException e) {
            Main.error("error opening up notes file");
            Main.error(e, true);
            throw new IOException(e.getMessage(), e);
        }
    }
}
