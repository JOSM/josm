// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.awt.Component;
import java.io.IOException;
import java.util.Collection;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.w3c.dom.Element;

/**
 * Session layer exporter.
 * @since 4685
 */
public interface SessionLayerExporter {

    /**
     * Return the Layers, this Layer depends on.
     * @return the layer dependencies
     */
    Collection<Layer> getDependencies();

    /**
     * The GUI for exporting this layer.
     * @return the export panel
     */
    Component getExportPanel();

    /**
     * Return true, if the layer should be included in the list of exported layers.
     *
     * The user can veto this in the export panel.
     * @return {@code true} if the layer should be included in the list of exported layers, {@code false} otherwise.
     */
    boolean shallExport();

    /**
     * Return true, if some data needs to be included in the zip archive. This decision depends on the user
     * selection in the export panel.
     *
     * If any layer requires zip, the user can only save as .joz. Otherwise both .jos and .joz are possible.
     * @return {@code true} if some data needs to be included in the zip archive, {@code false} otherwise.
     */
    boolean requiresZip();

    /**
     * Save meta data to the .jos file. Return a layer XML element.
     * Use <code>support</code> to save files in the zip archive as needed.
     * @param support support class providing export utilities
     * @return the resulting XML element
     * @throws IOException  if any I/O error occurs
     */
    Element export(ExportSupport support) throws IOException;
}
