// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.WMTSCapabilities;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.Layer;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.WMTSGetCapabilitiesException;
import org.openstreetmap.josm.gui.layer.imagery.WMTSLayerSelection;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Panel for adding WMTS imagery sources
 * @author Wiktor Niesiobędzki
 * @since 8568
 */
public class AddWMTSLayerPanel extends AddImageryPanel {
    private final transient JPanel layerPanel = new JPanel(new GridBagLayout());
    private transient WMTSLayerSelection layerTable;
    private final JCheckBox setDefaultLayer = new JCheckBox(tr("Set default layer?"));

    /**
     * default constructor
     */
    public AddWMTSLayerPanel() {
        add(new JLabel(tr("{0} Make sure OSM has the permission to use this service", "1.")), GBC.eol());
        add(new JLabel(tr("{0} Enter GetCapabilities URL", "2.")), GBC.eol());
        add(rawUrl, GBC.eop().fill(GBC.HORIZONTAL));
        rawUrl.setLineWrap(true);
        rawUrl.setAlignmentY(TOP_ALIGNMENT);
        JButton getLayers = new JButton(tr("{0} Get layers", "3."));
        getLayers.setEnabled(setDefaultLayer.isSelected());
        setDefaultLayer.addActionListener(e -> getLayers.setEnabled(setDefaultLayer.isSelected()));
        add(setDefaultLayer, GBC.eop());
        add(getLayers, GBC.eop().fill(GBC.HORIZONTAL));
        add(new JLabel(tr("Choose default layer")), GBC.eol());
        add(layerPanel, GBC.eol().fill());

        addCommonSettings();

        add(new JLabel(tr("{0} Enter name for this layer", "4.")), GBC.eol());
        add(name, GBC.eol().fill(GBC.HORIZONTAL));
        registerValidableComponent(rawUrl);
        registerValidableComponent(setDefaultLayer);

        getLayers.addActionListener(e -> {
            try {
                WMTSCapabilities capabilities = WMTSTileSource.getCapabilities(rawUrl.getText(), getCommonHeaders());
                layerTable = new WMTSLayerSelection(WMTSTileSource.groupLayersByNameAndTileMatrixSet(capabilities.getLayers()));
                layerTable.getTable().getSelectionModel().addListSelectionListener(lsl -> {
                    if (layerTable.getSelectedLayer() != null) {
                        name.setText(capabilities.getBaseUrl() + ": " + layerTable.getSelectedLayer().getUserTitle());
                    } else {
                        name.setText(capabilities.getBaseUrl() + ": ");
                    }
                });
                layerPanel.removeAll();
                layerPanel.add(layerTable, GBC.eol().fill());
                layerPanel.revalidate();
            } catch (IOException | WMTSGetCapabilitiesException ex) {
                Logging.trace(ex);
                JOptionPane.showMessageDialog(
                        getParent(),
                        tr("Error getting layers: {0}", ex.getMessage()),
                        tr("WMTS Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    protected ImageryInfo getImageryInfo() {
        ImageryInfo ret = new ImageryInfo(getImageryName(), "wmts:" + sanitize(getImageryRawUrl(), ImageryType.WMTS));
        if (setDefaultLayer.isSelected()) {
            if (layerTable == null) {
                // did not call get capabilities
                throw new IllegalArgumentException(tr("You need to get fetch layers"));
            }
            Layer selectedLayer = layerTable.getSelectedLayer();
            ret.setDefaultLayers(
                    Collections.<DefaultLayer>singletonList(
                            new DefaultLayer(
                                    ImageryType.WMTS,
                                    selectedLayer.getIdentifier(),
                                    selectedLayer.getStyle(),
                                    selectedLayer.getTileMatrixSet().getIdentifier()
                                    )
                            )
                    );
        }
        ret.setCustomHttpHeaders(getCommonHeaders());
        ret.setGeoreferenceValid(getCommonIsValidGeoreference());
        ret.setImageryType(ImageryType.WMTS);
        try {
            new WMTSTileSource(ret); // check if constructor throws an error
        } catch (IOException | WMTSGetCapabilitiesException e) {
            Logging.warn(e);
            throw new IllegalArgumentException(e); // if so, wrap exception, so proper message will be shown to the user
        }
        return ret;
    }

    @Override
    protected boolean isImageryValid() {
        return ((setDefaultLayer.isSelected() && layerTable != null && layerTable.getSelectedLayer() != null)
                || !setDefaultLayer.isSelected()
                ) && !getImageryName().isEmpty() && !getImageryRawUrl().isEmpty();
    }
}
