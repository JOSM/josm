// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

public abstract class FileImporter {

    public ExtensionFileFilter filter;

    public FileImporter(ExtensionFileFilter filter) {
        this.filter = filter;
    }

    public boolean acceptFile(File pathname) {
        return filter.acceptName(pathname.getName());
    }

    public void importData(File file) throws IOException, IllegalDataException {
        throw new IOException(tr("Could not read \"{0}\"", file.getName()));
    }

}
