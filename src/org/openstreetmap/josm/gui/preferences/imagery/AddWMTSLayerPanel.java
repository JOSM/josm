// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.WMTSCapabilities;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.Layer;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.WMTSGetCapabilitiesException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Panel for adding WMTS imagery sources
 * @author Wiktor Niesiobędzki
 *
 */
public class AddWMTSLayerPanel extends AddImageryPanel {
    private final transient JPanel layerPanel = new JPanel(new GridBagLayout());
    private transient JTable layerTable = null;
    private final JCheckBox setDefaultLayer = new JCheckBox(tr("Set default layer?"));
    private List<Entry<String, List<Layer>>> layers;

    /**
     * default constructor
     */
    public AddWMTSLayerPanel() {
        add(new JLabel(tr("{0} Make sure OSM has the permission to use this service", "1.")), GBC.eol());
        add(new JLabel(tr("{0} Enter GetCapabilities URL", "2.")), GBC.eol());
        add(rawUrl, GBC.eop().fill());
        rawUrl.setLineWrap(true);
        rawUrl.setAlignmentY(TOP_ALIGNMENT);
        JButton getLayers = new JButton(tr("Get layers"));
        getLayers.setEnabled(setDefaultLayer.isSelected());
        setDefaultLayer.addActionListener(e -> {
                getLayers.setEnabled(setDefaultLayer.isSelected());
        });
        add(setDefaultLayer, GBC.eop().fill());
        add(getLayers, GBC.eop().fill());
        add(new JLabel(tr("Choose default layer")), GBC.eol().fill());
        layerPanel.setPreferredSize(new Dimension(250, 100));
        add(layerPanel, GBC.eol().fill());

        addCommonSettings();

        add(new JLabel(tr("{0} Enter name for this layer", "3.")), GBC.eol());
        add(name, GBC.eol().fill(GBC.HORIZONTAL));
        registerValidableComponent(rawUrl);

        getLayers.addActionListener(e -> {
            try {
                WMTSCapabilities capabilities = WMTSTileSource.getCapabilities(rawUrl.getText(), getCommonHeaders());
                layers = WMTSTileSource.groupLayersByNameAndTileMatrixSet(capabilities.getLayers());
                layerTable = WMTSTileSource.getLayerSelectionPanel(layers);
                layerPanel.removeAll();
                JScrollPane scrollPane = new JScrollPane(layerTable);
                scrollPane.setPreferredSize(new Dimension(100, 100));
                layerPanel.add(scrollPane, GBC.eol().fill());
                layerPanel.revalidate();
            } catch (Exception ex) {
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
                throw new RuntimeException("TODO");
            }
            int index = layerTable.getSelectedRow();
            if (index < 0) {
                throw new RuntimeException("TODO");
            }
            Layer selectedLayer = layers.get(layerTable.convertRowIndexToModel(index)).getValue().get(0);
            ret.setDefaultLayers(
                    Collections.<DefaultLayer> singletonList(
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
        return !getImageryName().isEmpty() && !getImageryRawUrl().isEmpty();
    }

}
