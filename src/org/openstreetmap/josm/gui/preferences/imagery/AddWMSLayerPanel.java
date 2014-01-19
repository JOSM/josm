// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.imagery.WMSImagery;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * An imagery panel used to add WMS imagery sources
 */
public class AddWMSLayerPanel extends AddImageryPanel {

    private final WMSImagery wms = new WMSImagery();
    private final JCheckBox endpoint = new JCheckBox(tr("Store WMS endpoint only, select layers at usage"));
    private final WMSLayerTree tree = new WMSLayerTree();
    private final JComboBox formats = new JComboBox();
    private final JLabel wmsInstruction;
    private final JosmTextArea wmsUrl = new JosmTextArea(3, 40);
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
        add(new JScrollPane(showBounds), GBC.eop().fill());

        add(new JLabel(tr("3. Select image format")), GBC.eol());
        add(formats, GBC.eol().fill());

        add(wmsInstruction = new JLabel(tr("4. Verify generated WMS URL")), GBC.eol());
        add(wmsUrl, GBC.eop().fill());
        wmsUrl.setLineWrap(true);

        add(new JLabel(tr("5. Enter name for this layer")), GBC.eol());
        add(name, GBC.eop().fill());

        getLayers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    wms.attemptGetCapabilities(rawUrl.getText());
                    tree.updateTree(wms);
                    formats.setModel(new DefaultComboBoxModel(wms.getFormats().toArray()));
                } catch (MalformedURLException ex) {
                    JOptionPane.showMessageDialog(getParent(), tr("Invalid service URL."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(getParent(), tr("Could not retrieve WMS layer list."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                } catch (WMSImagery.WMSGetCapabilitiesException ex) {
                    JOptionPane.showMessageDialog(getParent(), tr("Could not parse WMS layer list."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                    Main.error("Could not parse WMS layer list. Incoming data:\n"+ex.getIncomingData());
                }
            }
        });

        endpoint.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
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
            }
        });

        tree.getLayerTree().addPropertyChangeListener("selectedLayers", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                onLayerSelectionChanged();
            }
        });

        formats.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLayerSelectionChanged();
            }
        });

        showBounds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tree.getSelectedLayers().get(0).bounds != null) {
                    SlippyMapBBoxChooser mapPanel = new SlippyMapBBoxChooser();
                    mapPanel.setBoundingBox(tree.getSelectedLayers().get(0).bounds);
                    JOptionPane.showMessageDialog(null, mapPanel, tr("Show Bounds"), JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, tr("No bounding box was found for this layer."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                }
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
        }
        return info;
    }

    protected final String getWmsUrl() {
        return sanitize(wmsUrl.getText());
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
