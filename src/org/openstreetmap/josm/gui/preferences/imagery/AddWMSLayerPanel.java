// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.io.imagery.WMSImagery;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

public class AddWMSLayerPanel extends AddImageryPanel {

    private final WMSImagery wms = new WMSImagery();
    private final JTextArea rawUrl = new JTextArea(3, 40);
    private final JCheckBox endpoint = new JCheckBox(tr("Store WMS endpoint only, select layers at usage"));
    private final WMSLayerTree tree = new WMSLayerTree();
    private final JTextArea wmsUrl = new JTextArea(3, 40);
    private final JTextField name = new JTextField();

    public AddWMSLayerPanel() {
        super(new GridBagLayout());

        add(new JLabel(tr("1. Enter service URL")), GBC.eol());
        add(rawUrl, GBC.eol().fill());
        rawUrl.setLineWrap(true);
        JButton getLayers = new JButton(tr("Get layers"));
        add(getLayers, GBC.eop().fill());

        add(new JLabel(tr("2. Select layers")), GBC.eol());
        add(endpoint, GBC.eol().fill());
        add(new JScrollPane(tree.getLayerTree()), GBC.eol().fill().weight(1, 100));
        final JButton showBounds = new JButton(tr("Show bounds"));
        showBounds.setEnabled(false);
        add(new JScrollPane(showBounds), GBC.eop().fill());

        add(new JLabel(tr("3. Verify generated WMS URL")), GBC.eol());
        add(wmsUrl, GBC.eop().fill());
        wmsUrl.setLineWrap(true);

        add(new JLabel(tr("4. Enter name for this layer")), GBC.eol());
        add(name, GBC.eop().fill());

        getLayers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    wms.attemptGetCapabilities(rawUrl.getText());
                    tree.updateTree(wms);
                } catch (MalformedURLException ex) {
                    JOptionPane.showMessageDialog(getParent(), tr("Invalid service URL."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(getParent(), tr("Could not retrieve WMS layer list."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                } catch (WMSImagery.WMSGetCapabilitiesException ex) {
                    JOptionPane.showMessageDialog(getParent(), tr("Could not parse WMS layer list."),
                            tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                    System.err.println("Could not parse WMS layer list. Incoming data:");
                    System.err.println(ex.getIncomingData());
                }
            }
        });

        endpoint.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                tree.getLayerTree().setEnabled(!endpoint.isSelected());
                showBounds.setEnabled(!endpoint.isSelected());
                wmsUrl.setEnabled(!endpoint.isSelected());
            }
        });

        tree.getLayerTree().addPropertyChangeListener("selectedLayers", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (wms.getServiceUrl() != null) {
                    wmsUrl.setText(wms.buildGetMapUrl(tree.getSelectedLayers()));
                    name.setText(wms.getServiceUrl().getHost() + ": " + Utils.join(", ", tree.getSelectedLayers()));
                }
                showBounds.setEnabled(tree.getSelectedLayers().size() == 1);
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

    }

    @Override
    public ImageryInfo getImageryInfo() {
        final ImageryInfo info;
        if (endpoint.isSelected()) {
            info = new ImageryInfo(name.getText(), rawUrl.getText());
            info.setImageryType(ImageryInfo.ImageryType.WMS_ENDPOINT);
        } else {
            info = wms.toImageryInfo(name.getText(), tree.getSelectedLayers());
            info.setUrl(wmsUrl.getText());
        }
        return info;
    }
}
