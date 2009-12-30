// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

//import javax.swing.JFileChooser;
import java.io.File;

class JpegFileFilter extends javax.swing.filechooser.FileFilter
                                    implements java.io.FileFilter {

    static final private JpegFileFilter instance = new JpegFileFilter();
    public static JpegFileFilter getInstance() {
        return instance;
    }

    @Override public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        } else {
            String name = f.getName().toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".jpeg");
        }
    }

    @Override public String getDescription() {
        return tr("JPEG images (*.jpg)");
    }
}
