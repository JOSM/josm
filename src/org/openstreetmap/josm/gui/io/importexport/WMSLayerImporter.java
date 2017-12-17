// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.Map;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.CheckParameterUtil;

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

    /**
     * Constructs a new {@code WMSLayerImporter}.
     */
    public WMSLayerImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        CheckParameterUtil.ensureParameterNotNull(file, "file");
        final EastNorth zoomTo;
        ImageryInfo info = null;
        final ImageryLayer layer;

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
            int sfv = ois.readInt();
            if (sfv < 5) {
                throw new InvalidClassException(tr("Unsupported WMS file version; found {0}, expected {1}", sfv, 5));
            } else if (sfv == 5) {
                ois.readInt(); // dax - not needed
                ois.readInt(); // day - not needed
                zoomTo = null;

                int imageSize = ois.readInt();
                double pixelPerDegree = ois.readDouble();

                String name = (String) ois.readObject();
                String extendedUrl = (String) ois.readObject();

                info = new ImageryInfo(name);
                info.setExtendedUrl(extendedUrl);
                info.setPixelPerDegree(pixelPerDegree);
                info.setTileSize(imageSize);
            } else if (sfv == WMSLayerExporter.CURRENT_FILE_VERSION) {
                zoomTo = (EastNorth) ois.readObject();

                @SuppressWarnings("unchecked")
                ImageryPreferenceEntry entry = StructUtils.deserializeStruct(
                        (Map<String, String>) ois.readObject(),
                        ImageryPreferenceEntry.class);
                info = new ImageryInfo(entry);
            } else {
                throw new InvalidClassException(tr("Unsupported WMS file version; found {0}, expected {1}", sfv, 6));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalDataException(e);
        }
        layer = ImageryLayer.create(info);


        // FIXME: remove UI stuff from IO subsystem
        GuiHelper.runInEDT(() -> {
            MainApplication.getLayerManager().addLayer(layer);
            if (zoomTo != null) {
                MainApplication.getMap().mapView.zoomTo(zoomTo);
            }
        });
    }
}
