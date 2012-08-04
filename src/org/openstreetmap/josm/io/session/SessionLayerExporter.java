// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.awt.Component;
import java.io.IOException;
import java.util.Collection;

import org.w3c.dom.Element;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;

public interface SessionLayerExporter {

    /**
     * Return the Layers, this Layer depends on.
     */
    Collection<Layer> getDependencies();

    /**
     * The GUI for exporting this layer.
     */
    Component getExportPanel();

    /**
     * Return true, if the layer should be included in the
     * list of exported layers.
     *
     * The user can veto this in the export panel.
     */
    boolean shallExport();

    /**
     * Return true, if some data needs to be included in
     * the zip archive. This decision depends on the user
     * selection in the export panel.
     *
     * If any layer requires zip, the user can only save as
     * .joz. Otherwise both .jos and .joz are possible.
     */
    boolean requiresZip();

    /**
     * Save meta data to the .jos file. Return a layer XML element.
     * Use <code>support</code> to save files in the zip archive as needed.
     */
    Element export(ExportSupport support) throws IOException;

}

