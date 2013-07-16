// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Import a WMS layer from a serialized binary file previously exported via {@link WMSLayerExporter}.
 * @since 5457
 */
public class WMSLayerImporter extends FileImporter {

    /**
     * The file filter used in "open" and "save" dialogs for WMS layers.
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "wms", "wms", tr("WMS Files (*.wms)"));

    private final WMSLayer wmsLayer;

    /**
     * Constructs a new {@code WMSLayerImporter}.
     */
    public WMSLayerImporter() {
        this(new WMSLayer());
    }

    /**
     * Constructs a new {@code WMSLayerImporter} that will import data to the specified WMS layer.
     * @param wmsLayer The WMS layer.
     */
    public WMSLayerImporter(WMSLayer wmsLayer) {
        super(FILE_FILTER);
        this.wmsLayer = wmsLayer;
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        CheckParameterUtil.ensureParameterNotNull(file, "file");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        try {
            wmsLayer.readExternal(ois);
        } catch (ClassNotFoundException e) {
            throw new IllegalDataException(e);
        } finally {
            Utils.close(ois);
        }

        // FIXME: remove UI stuff from IO subsystem
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                Main.main.addLayer(wmsLayer);
                wmsLayer.onPostLoadFromFile();
            }
        });
    }

    /**
     * Replies the imported WMS layer.
     * @return The imported WMS layer.
     * @see #importData(File, ProgressMonitor)
     */
    public final WMSLayer getWmsLayer() {
        return wmsLayer;
    }
}
