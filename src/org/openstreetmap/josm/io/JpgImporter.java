// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;

public class JpgImporter extends FileImporter {
    private GpxLayer gpx;

    public JpgImporter() {
        super(new ExtensionFileFilter("jpg", "jpg", tr("Image Files") + " (*.jpg, "+ tr("folder")+")"));
    }

    public JpgImporter(GpxLayer gpx) {
        this();
        this.gpx = gpx;
    }

    @Override
    public boolean acceptFile(File pathname) {
        return super.acceptFile(pathname) || pathname.isDirectory();
    }

    @Override
    public void importData(List<File> sel) throws IOException, IllegalDataException {
        if (sel == null || sel.size() == 0)
            return;
        LinkedList<File> files = new LinkedList<File>();
        addRecursiveFiles(files, sel);
        if(files.isEmpty()) throw new IOException(tr("No image files found."));
        GeoImageLayer.create(files, gpx);
    }

    private void addRecursiveFiles(LinkedList<File> files, List<File> sel) {
        for (File f : sel) {
            if (f.isDirectory()) {
                addRecursiveFiles(files, Arrays.asList(f.listFiles()));
            } else if (f.getName().toLowerCase().endsWith(".jpg")) {
                files.add(f);
            }
        }
    }

    @Override
    public boolean isBatchImporter() {
        return true;
    }

    /**
     * Needs to be the last, to avoid problems.
     */
    @Override
    public double getPriority() {
        return -1000;
    }
}
