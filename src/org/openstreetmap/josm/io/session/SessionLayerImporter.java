// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.IOException;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader.ImportSupport;
import org.w3c.dom.Element;

/**
 * Session layer importer.
 * @since 4668
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface SessionLayerImporter {

    /**
     * Load the layer from xml meta-data.
     * @param elem XML element
     * @param support support class providing import utilities
     * @param progressMonitor progress monitor
     * @return the resulting layer
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if invalid data is read
     */
    Layer load(Element elem, ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException;
}

