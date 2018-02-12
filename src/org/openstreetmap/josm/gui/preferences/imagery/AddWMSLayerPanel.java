// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.imagery.WMSImagery;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * An imagery panel used to add WMS imagery sources.
 * @since 2599
 */
public class AddWMSLayerPanel extends AddImageryPanel {

    private final transient WMSImagery wms = new WMSImagery();
    private final JCheckBox endpoint = new JCheckBox(tr("Store WMS endpoint only, select layers at usage"));
    private final transient WMSLayerTree tree = new WMSLayerTree();
    private final JComboBox<String> formats = new JComboBox<>();
    private final JLabel wmsInstruction;
    private final JosmTextArea wmsUrl = new JosmTextArea(3, 40).transferFocusOnTab();
    private final JButton showBounds = new JButton(tr("Show bounds"));

    /**
     * Constructs a new {@code AddWMSLayerPanel}.
     */
    public AddWMSLayerPanel() {

        add(new JLabel(tr("1. Enter service URL")), GBC.eol());
        add(rawUrl, GBC.eol().fill());
        rawUrl.setLineWrap(true);
        JButton getLayers = new JButton(tr("Get layers"));
        add(getLayers, GBC.eop().fill());

        add(new JLabel(tr("2. Select layers")), GBC.eol());
        add(endpoint, GBC.eol().fill());
        add(new JScrollPane(tree.getLayerTree()), GBC.eol().fill().weight(1, 100));

        showBounds.setEnabled(false);
        add(showBounds, GBC.eop().fill());

        add(new JLabel(tr("3. Select image format")), GBC.eol());
        add(formats, GBC.eol().fill());

        wmsInstruction = new JLabel(tr("4. Verify generated WMS URL"));
        add(wmsInstruction, GBC.eol());
        wmsInstruction.setLabelFor(wmsUrl);
        add(wmsUrl, GBC.eop().fill());
        wmsUrl.setLineWrap(true);

        add(new JLabel(tr("5. Enter name for this layer")), GBC.eol());
        add(name, GBC.eop().fill());

        getLayers.addActionListener(e -> {
            try {
                wms.attemptGetCapabilities(rawUrl.getText());
                tree.updateTree(wms);
                List<String> wmsFormats = wms.getFormats();
                formats.setModel(new DefaultComboBoxModel<>(wmsFormats.toArray(new String[0])));
                formats.setSelectedItem(wms.getPreferredFormats());
            } catch (MalformedURLException ex1) {
                Logging.log(Logging.LEVEL_ERROR, ex1);
                JOptionPane.showMessageDialog(getParent(), tr("Invalid service URL."),
                        tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex2) {
                Logging.log(Logging.LEVEL_ERROR, ex2);
                JOptionPane.showMessageDialog(getParent(), tr("Could not retrieve WMS layer list."),
                        tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
            } catch (WMSImagery.WMSGetCapabilitiesException ex3) {
                String incomingData = ex3.getIncomingData() != null ? ex3.getIncomingData().trim() : "";
                String title = tr("WMS Error");
                StringBuilder message = new StringBuilder(tr("Could not parse WMS layer list."));
                Logging.log(Logging.LEVEL_ERROR, "Could not parse WMS layer list. Incoming data:\n"+incomingData, ex3);
                if ((incomingData.startsWith("<html>") || incomingData.startsWith("<HTML>"))
                  && (incomingData.endsWith("</html>") || incomingData.endsWith("</HTML>"))) {
                    GuiHelper.notifyUserHtmlError(this, title, message.toString(), incomingData);
                } else {
                    if (ex3.getMessage() != null) {
                        message.append('\n').append(ex3.getMessage());
                    }
                    JOptionPane.showMessageDialog(getParent(), message.toString(), title, JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        endpoint.addItemListener(e -> {
            tree.getLayerTree().setEnabled(!endpoint.isSelected());
            showBounds.setEnabled(!endpoint.isSelected());
            wmsInstruction.setEnabled(!endpoint.isSelected());
            formats.setEnabled(!endpoint.isSelected());
            wmsUrl.setEnabled(!endpoint.isSelected());
            if (endpoint.isSelected()) {
                URL url = wms.getServiceUrl();
                if (url != null) {
                    name.setText(url.getHost());
                }
            } else {
                onLayerSelectionChanged();
            }
        });

        tree.getLayerTree().addPropertyChangeListener("selectedLayers", evt -> onLayerSelectionChanged());

        formats.addActionListener(e -> onLayerSelectionChanged());

        showBounds.addActionListener(e -> {
            if (tree.getSelectedLayers().get(0).bounds != null) {
                SlippyMapBBoxChooser mapPanel = new SlippyMapBBoxChooser();
                mapPanel.setBoundingBox(tree.getSelectedLayers().get(0).bounds);
                JOptionPane.showMessageDialog(null, mapPanel, tr("Show Bounds"), JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, tr("No bounding box was found for this layer."),
                        tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
            }
        });

        registerValidableComponent(endpoint);
        registerValidableComponent(rawUrl);
        registerValidableComponent(wmsUrl);
    }

    protected final void onLayerSelectionChanged() {
        if (wms.getServiceUrl() != null) {
            wmsUrl.setText(wms.buildGetMapUrl(tree.getSelectedLayers(), (String) formats.getSelectedItem()));
            name.setText(wms.getServiceUrl().getHost() + ": " + Utils.join(", ", tree.getSelectedLayers()));
        }
        showBounds.setEnabled(tree.getSelectedLayers().size() == 1);
    }

    @Override
    public ImageryInfo getImageryInfo() {
        final ImageryInfo info;
        if (endpoint.isSelected()) {
            info = new ImageryInfo(getImageryName(), getImageryRawUrl());
            info.setImageryType(ImageryInfo.ImageryType.WMS_ENDPOINT);
        } else {
            info = wms.toImageryInfo(getImageryName(), tree.getSelectedLayers());
            info.setUrl(getWmsUrl());
            info.setImageryType(ImageryType.WMS);
        }
        return info;
    }

    protected final String getWmsUrl() {
        return sanitize(wmsUrl.getText(), ImageryInfo.ImageryType.WMS);
    }

    @Override
    protected boolean isImageryValid() {
        if (getImageryName().isEmpty()) {
            return false;
        }
        if (endpoint.isSelected()) {
            return !getImageryRawUrl().isEmpty();
        } else {
            return !getWmsUrl().isEmpty();
        }
    }
}
