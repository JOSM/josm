// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.SessionLoadAction.Loader;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Utils;

/**
 * File importer allowing to import session files (*.jos/joz files).
 * @since 6245
 */
public class SessionImporter extends FileImporter {

    /**
     * The file filter used to load JOSM session files
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "jos,joz", "jos", tr("Session file (*.jos, *.joz)"));

    /**
     * Constructs a new {@code SessionImporter}.
     */
    public SessionImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        boolean zip = Utils.hasExtension(file, "joz");
        MainApplication.worker.submit(new Loader(file, zip));
    }
}
