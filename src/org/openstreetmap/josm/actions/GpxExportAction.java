// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.actions.SaveActionBase.createAndOpenSaveFileChooser;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Exports data to gpx.
 */
public class GpxExportAction extends DiskAccessAction {

    public GpxExportAction() {
        super(tr("Export to GPX..."), "exportgpx", tr("Export the data to GPX file."),
                Shortcut.registerShortcut("file:exportgpx", tr("Export to GPX..."), KeyEvent.VK_E, Shortcut.GROUP_MENU));
        putValue("help", ht("/Action/GpxExport"));
    }

    protected GpxLayer getLayer() {
        if (!Main.isDisplayingMapView()) return null;
        if (Main.map.mapView.getActiveLayer() == null) return null;
        Layer layer = Main.map.mapView.getActiveLayer();
        if (! (layer instanceof GpxLayer)) return null;
        return (GpxLayer)layer;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        GpxLayer layer = getLayer();
        if (layer == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Nothing to export. Get some data first."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        export(layer);
    }

    /**
     * Exports a layer to a file. Launches a file chooser to request the user to enter a file name.
     *
     * <code>layer</code> must not be null. <code>layer</code> must be an instance of
     * {@see OsmDataLayer} or {@see GpxLayer}.
     *
     * @param layer the layer
     * @exception IllegalArgumentException thrown if layer is null
     * @exception IllegalArgumentException thrown if layer is neither an instance of {@see OsmDataLayer}
     *  nor of {@see GpxLayer}
     */
    public void export(Layer layer) {
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "layer"));
        if (! (layer instanceof OsmDataLayer) && ! (layer instanceof GpxLayer))
            throw new IllegalArgumentException(tr("Expected instance of OsmDataLayer or GpxLayer. Got ''{0}''.", layer.getClass().getName()));

        File file = createAndOpenSaveFileChooser(tr("Export GPX file"), ".gpx");
        if (file == null)
            return;

        for (FileExporter exporter : ExtensionFileFilter.exporters) {
            if (exporter.acceptFile(file, layer)) {
                try {
                    exporter.exportData(file, layer);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        boolean check =
        Main.isDisplayingMapView()
        && Main.map.mapView.getActiveLayer() != null;
        if(!check) {
            setEnabled(false);
            return;
        }
        Layer layer = Main.map.mapView.getActiveLayer();
        setEnabled(layer instanceof GpxLayer);
    }
}
