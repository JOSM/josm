// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.LayerDetails;
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

    private transient WMSImagery wms;
    private final JCheckBox endpoint = new JCheckBox(tr("Store WMS endpoint only, select layers at usage"));
    private final JCheckBox setDefaultLayers = new JCheckBox(tr("Use selected layers as default"));
    private final transient WMSLayerTree tree = new WMSLayerTree();
    private final JComboBox<String> formats = new JComboBox<>();
    private final JosmTextArea wmsUrl = new JosmTextArea(3, 40).transferFocusOnTab();
    private final JButton showBounds = new JButton(tr("Show bounds"));

    /**
     * Constructs a new {@code AddWMSLayerPanel}.
     */
    public AddWMSLayerPanel() {

        add(new JLabel(tr("{0} Make sure OSM has the permission to use this service", "1.")), GBC.eol());
        add(new JLabel(tr("{0} Enter GetCapabilities URL", "2.")), GBC.eol());
        add(rawUrl, GBC.eol().fill(GBC.HORIZONTAL));
        rawUrl.setLineWrap(true);
        JButton getLayers = new JButton(tr("{0} Get layers", "3."));
        add(getLayers, GBC.eop().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("{0} Select layers", "4.")), GBC.eol());

        add(endpoint, GBC.eol());
        setDefaultLayers.setEnabled(false);
        add(setDefaultLayers, GBC.eol());
        add(new JScrollPane(tree.getLayerTree()), GBC.eol().fill());

        showBounds.setEnabled(false);
        add(showBounds, GBC.eop().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("{0} Select image format", "5.")), GBC.eol());
        add(formats, GBC.eol().fill(GBC.HORIZONTAL));

        addCommonSettings();

        JLabel wmsInstruction = new JLabel(tr("{0} Edit generated {1} URL (optional)", "6.", "WMS"));
        add(wmsInstruction, GBC.eol());
        wmsInstruction.setLabelFor(wmsUrl);
        add(wmsUrl, GBC.eop().fill(GBC.HORIZONTAL));
        wmsUrl.setLineWrap(true);

        add(new JLabel(tr("{0} Enter name for this layer", "7.")), GBC.eol());
        add(name, GBC.eop().fill(GBC.HORIZONTAL));

        getLayers.addActionListener(e -> {
            try {
                wms = new WMSImagery(rawUrl.getText(), getCommonHeaders());
                tree.updateTree(wms);
                Collection<String> wmsFormats = wms.getFormats();
                formats.setModel(new DefaultComboBoxModel<>(wmsFormats.toArray(new String[0])));
                formats.setSelectedItem(wms.getPreferredFormat());
            } catch (MalformedURLException | InvalidPathException ex1) {
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

        ActionListener availabilityManagerAction = a -> {
            setDefaultLayers.setEnabled(endpoint.isSelected());
            boolean enabled = !endpoint.isSelected() || setDefaultLayers.isSelected();
            tree.getLayerTree().setEnabled(enabled);
            showBounds.setEnabled(enabled);
            wmsInstruction.setEnabled(enabled);
            formats.setEnabled(enabled);
            wmsUrl.setEnabled(enabled);
            if (endpoint.isSelected() && !setDefaultLayers.isSelected() && wms != null) {
                name.setText(wms.buildRootUrl());
            }
            onLayerSelectionChanged();
        };

        endpoint.addActionListener(availabilityManagerAction);
        setDefaultLayers.addActionListener(availabilityManagerAction);

        tree.getLayerTree().addPropertyChangeListener("selectedLayers", evt -> onLayerSelectionChanged());

        formats.addActionListener(e -> onLayerSelectionChanged());

        showBounds.addActionListener(e -> {
            if (tree.getSelectedLayers().get(0).getBounds() != null) {
                SlippyMapBBoxChooser mapPanel = new SlippyMapBBoxChooser();
                mapPanel.setBoundingBox(tree.getSelectedLayers().get(0).getBounds());
                JOptionPane.showMessageDialog(null, mapPanel, tr("Show Bounds"), JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, tr("No bounding box was found for this layer."),
                        tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
            }
        });

        registerValidableComponent(endpoint);
        registerValidableComponent(rawUrl);
        registerValidableComponent(wmsUrl);
        registerValidableComponent(setDefaultLayers);
    }

    protected final void onLayerSelectionChanged() {
        if (wms != null && wms.buildRootUrl() != null) {
            wmsUrl.setText(wms.buildGetMapUrl(
                    tree.getSelectedLayers().stream().map(LayerDetails::getName).collect(Collectors.toList()),
                    (List<String>) null,
                    (String) formats.getSelectedItem(),
                    true // TODO: ask user about transparency
                )
            );
            name.setText(wms.buildRootUrl() + ": " + Utils.join(", ", tree.getSelectedLayers()));
        }
        showBounds.setEnabled(tree.getSelectedLayers().size() == 1);
    }

    @Override
    public ImageryInfo getImageryInfo() {
        ImageryInfo info = null;
        if (endpoint.isSelected()) {
            info = new ImageryInfo(getImageryName(), getImageryRawUrl());
            info.setImageryType(ImageryInfo.ImageryType.WMS_ENDPOINT);
            if (setDefaultLayers.isSelected()) {
                info.setDefaultLayers(tree.getSelectedLayers().stream()
                        .map(x -> new DefaultLayer(
                                ImageryInfo.ImageryType.WMS_ENDPOINT,
                                x.getName(),
                                "", // TODO: allow selection of styles
                                ""))
                        .collect(Collectors.toList()));
                info.setServerProjections(wms.getServerProjections(tree.getSelectedLayers()));
            }
        } else {
            if (wms != null && wms.buildRootUrl() != null) {
                // TODO: ask user about transparency
                info = wms.toImageryInfo(getImageryName(), tree.getSelectedLayers(), (List<String>) null, true);
            } else {
                info = new ImageryInfo(getImageryName(), getWmsUrl());
            }
            info.setImageryType(ImageryType.WMS);
        }
        info.setGeoreferenceValid(getCommonIsValidGeoreference());
        info.setCustomHttpHeaders(getCommonHeaders());
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
        if (setDefaultLayers.isSelected() && (tree == null || tree.getSelectedLayers().isEmpty())) {
            return false;
        }
        if (endpoint.isSelected()) {
            return !getImageryRawUrl().isEmpty();
        } else {
            return !getWmsUrl().isEmpty();
        }
    }
}
