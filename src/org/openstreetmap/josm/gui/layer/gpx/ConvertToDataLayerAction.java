// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An abstract action for a conversion from a {@code T} {@link Layer} to a {@link OsmDataLayer}.
 * @param <T> the source layer class
 */
public abstract class ConvertToDataLayerAction<T extends Layer> extends AbstractAction {
    /** source layer */
    protected final transient T layer;

    /**
     * Constructs a new {@code ConvertToDataLayerAction}
     * @param layer source layer
     */
    protected ConvertToDataLayerAction(final T layer) {
        super(tr("Convert to data layer"));
        new ImageProvider("converttoosm").getResource().attachImageIcon(this, true);
        this.layer = layer;
        putValue("help", ht("/Action/ConvertToDataLayer"));
    }

    /**
     * Performs the conversion to a {@link DataSet}.
     * @return the resulting dataset
     */
    public abstract DataSet convert();

    @Override
    public void actionPerformed(ActionEvent e) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(
                tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>"
                        + "If you want to upload traces, look here:</html>")),
                GBC.eol());
        msg.add(new UrlLabel(Config.getUrls().getOSMWebsite() + "/traces", 2), GBC.eop());
        if (!ConditionalOptionPaneUtil.showConfirmationDialog("convert_to_data", Main.parent, msg, tr("Warning"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION)) {
            return;
        }
        final DataSet ds = convert();
        if (ds != null) {
            final OsmDataLayer osmLayer = new OsmDataLayer(ds, tr("Converted from: {0}", layer.getName()), null);
            if (layer.getAssociatedFile() != null) {
                osmLayer.setAssociatedFile(new File(layer.getAssociatedFile().getParentFile(),
                        layer.getAssociatedFile().getName() + ".osm"));
            }
            osmLayer.setUploadDiscouraged(true);
            MainApplication.getLayerManager().addLayer(osmLayer, false);
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
