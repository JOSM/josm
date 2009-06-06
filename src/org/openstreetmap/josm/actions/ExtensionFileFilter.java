// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.io.FileImporter;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.io.NMEAImporter;
import org.openstreetmap.josm.io.OsmImporter;

/**
 * A file filter that filters after the extension. Also includes a list of file
 * filters used in JOSM.
 *
 * @author imi
 */
public class ExtensionFileFilter extends FileFilter {
    private final String extension;
    private final String description;
    public final String defaultExtension;

    public static ArrayList<FileImporter> importers = new ArrayList<FileImporter>(Arrays.asList(new OsmImporter(),
            new GpxImporter(), new NMEAImporter()));

    /**
     * Construct an extension file filter by giving the extension to check after.
     *
     */
    public ExtensionFileFilter(String extension, String defExt, String description) {
        this.extension = extension;
        defaultExtension = defExt;
        this.description = description;
    }

    public boolean acceptName(String filename) {
        String name = filename.toLowerCase();
        for (String ext : extension.split(","))
            if (name.endsWith("."+ext))
                return true;
        return false;
    }

    @Override public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        return acceptName(pathname.getName());
    }

    @Override public String getDescription() {
        return description;
    }
}
