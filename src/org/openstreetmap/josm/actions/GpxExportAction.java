// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.actions.SaveActionBase.createAndOpenSaveFileChooser;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Exports data to gpx.
 * @since 78
 */
public class GpxExportAction extends DiskAccessAction {

    /**
     * Constructs a new {@code GpxExportAction}.
     */
    public GpxExportAction() {
        super(tr("Export to GPX..."), "exportgpx", tr("Export the data to GPX file."),
                Shortcut.registerShortcut("file:exportgpx", tr("Export to GPX..."), KeyEvent.VK_E, Shortcut.CTRL));
        putValue("help", ht("/Action/GpxExport"));
    }

    /**
     * Get the layer to export.
     * @return The layer to export, either a {@link GpxLayer} or {@link OsmDataLayer}.
     */
    protected Layer getLayer() {
        Layer layer = getLayerManager().getActiveLayer();
        return (layer instanceof GpxLayer || layer instanceof OsmDataLayer) ? layer : null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        Layer layer = getLayer();
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
     * {@link OsmDataLayer} or {@link GpxLayer}.
     *
     * @param layer the layer
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if layer is neither an instance of {@link OsmDataLayer}
     *  nor of {@link GpxLayer}
     */
    public void export(Layer layer) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        if (!(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            throw new IllegalArgumentException(MessageFormat.format("Expected instance of OsmDataLayer or GpxLayer. Got ''{0}''.",
                    layer.getClass().getName()));

        File file = createAndOpenSaveFileChooser(tr("Export GPX file"), GpxImporter.getFileFilter());
        if (file == null)
            return;

        for (FileExporter exporter : ExtensionFileFilter.getExporters()) {
            if (exporter.acceptFile(file, layer)) {
                try {
                    exporter.exportData(file, layer);
                } catch (IOException e) {
                    Logging.error(e);
                }
            }
        }
    }

    /**
     * Refreshes the enabled state
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayer() != null);
    }
}
