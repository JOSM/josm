// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A FileFilter that accepts all files.
 * @since 5572
 */
public class FileFilterAllFiles extends FileFilter {

    private static FileFilterAllFiles instance;

    /**
     * Replies the unique instance.
     * @return the unique instance
     */
    public static synchronized FileFilterAllFiles getInstance() {
        if (instance == null) {
            instance = new FileFilterAllFiles();
        }
        return instance;
    }

    @Override
    public boolean accept(File f) {
        return true;
    }

    @Override
    public String getDescription() {
        return tr("All files (*.*)");
    }
}
