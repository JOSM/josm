// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

class JpegFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    private static final JpegFileFilter instance = new JpegFileFilter();
    public static JpegFileFilter getInstance() {
        return instance;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        } else {
            String name = f.getName().toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".jpeg");
        }
    }

    @Override
    public String getDescription() {
        return tr("JPEG images (*.jpg)");
    }
}
