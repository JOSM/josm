// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.LayerDetails;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.Layer;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.WMTSGetCapabilitiesException;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AlignImageryPanel;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.imagery.WMSLayerTree;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.imagery.WMSImagery;
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

/**
 * Action displayed in imagery menu to add a new imagery layer.
 * @since 3715
 */
public class AddImageryLayerAction extends JosmAction implements AdaptableAction {
    private final transient ImageryInfo info;

    static class SelectWmsLayersDialog extends ExtendedDialog {
        SelectWmsLayersDialog(WMSLayerTree tree, JComboBox<String> formats) {
            super(MainApplication.getMainFrame(), tr("Select WMS layers"), tr("Add layers"), tr("Cancel"));
            final JScrollPane scrollPane = new JScrollPane(tree.getLayerTree());
            scrollPane.setPreferredSize(new Dimension(400, 400));
            final JPanel panel = new JPanel(new GridBagLayout());
            panel.add(scrollPane, GBC.eol().fill());
            panel.add(formats, GBC.eol().fill(GBC.HORIZONTAL));
            setContent(panel);
        }
    }

    /**
     * Constructs a new {@code AddImageryLayerAction} for the given {@code ImageryInfo}.
     * If an http:// icon is specified, it is fetched asynchronously.
     * @param info The imagery info
     */
    public AddImageryLayerAction(ImageryInfo info) {
        super(info.getMenuName(), /* ICON */"imagery_menu", tr("Add imagery layer {0}", info.getName()), null,
                true, ToolbarPreferences.IMAGERY_PREFIX + info.getToolbarName(), false);
        putValue("help", ht("/Preferences/Imagery"));
        setTooltip(info.getToolTipText().replaceAll("</?html>", ""));
        this.info = info;
        installAdapters();

        // change toolbar icon from if specified
        String icon = info.getIcon();
        if (icon != null) {
            new ImageProvider(icon).setOptional(true).getResourceAsync(result -> {
                if (result != null) {
                    GuiHelper.runInEDT(() -> result.attachImageIcon(this));
                }
            });
        }
    }

    /**
     * Converts general ImageryInfo to specific one, that does not need any user action to initialize
     * see: https://josm.openstreetmap.de/ticket/13868
     * @param info ImageryInfo that will be converted (or returned when no conversion needed)
     * @return ImageryInfo object that's ready to be used to create TileSource
     */
    private ImageryInfo convertImagery(ImageryInfo info) {
        try {
            switch(info.getImageryType()) {
            case WMS_ENDPOINT:
                // convert to WMS type
                if (info.getDefaultLayers() == null || info.getDefaultLayers().isEmpty()) {
                    return getWMSLayerInfo(info);
                } else {
                    return info;
                }
            case WMTS:
                // specify which layer to use
                if (info.getDefaultLayers() == null || info.getDefaultLayers().isEmpty()) {
                    WMTSTileSource tileSource = new WMTSTileSource(info);
                    DefaultLayer layerId = tileSource.userSelectLayer();
                    if (layerId != null) {
                        ImageryInfo copy = new ImageryInfo(info);
                        copy.setDefaultLayers(Collections.singletonList(layerId));
                        String layerName = tileSource.getLayers().stream()
                                .filter(x -> x.getIdentifier().equals(layerId.getLayerName()))
                                .map(Layer::getUserTitle)
                                .findFirst()
                                .orElse("");
                        copy.setName(copy.getName() + ": " + layerName);
                        return copy;
                    }
                    return null;
                } else {
                    return info;
                }
            default:
                return info;
            }
        } catch (MalformedURLException ex) {
            handleException(ex, tr("Invalid service URL."), tr("WMS Error"), null);
        } catch (IOException ex) {
            handleException(ex, tr("Could not retrieve WMS layer list."), tr("WMS Error"), null);
        } catch (WMSGetCapabilitiesException ex) {
            handleException(ex, tr("Could not parse WMS layer list."), tr("WMS Error"),
                    "Could not parse WMS layer list. Incoming data:\n" + ex.getIncomingData());
        } catch (WMTSGetCapabilitiesException ex) {
            handleException(ex, tr("Could not parse WMTS layer list."), tr("WMTS Error"),
                    "Could not parse WMTS layer list.");
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        ImageryLayer layer = null;
        try {
            final ImageryInfo infoToAdd = convertImagery(info);
            if (infoToAdd != null) {
                layer = ImageryLayer.create(infoToAdd);
                getLayerManager().addLayer(layer);
                AlignImageryPanel.addNagPanelIfNeeded(infoToAdd);
            }
        } catch (IllegalArgumentException | ReportedException ex) {
            if (ex.getMessage() == null || ex.getMessage().isEmpty() || GraphicsEnvironment.isHeadless()) {
                throw ex;
            } else {
                Logging.error(ex);
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), ex.getMessage(), tr("Error"), JOptionPane.ERROR_MESSAGE);
                if (layer != null) {
                    getLayerManager().removeLayer(layer);
                }
            }
        }
    }

    /**
     * Asks user to choose a WMS layer from a WMS endpoint.
     * @param info the WMS endpoint.
     * @return chosen WMS layer, or null
     * @throws IOException if any I/O error occurs while contacting the WMS endpoint
     * @throws WMSGetCapabilitiesException if the WMS getCapabilities request fails
     */
    protected static ImageryInfo getWMSLayerInfo(ImageryInfo info) throws IOException, WMSGetCapabilitiesException {
        try {
            CheckParameterUtil.ensureThat(ImageryType.WMS_ENDPOINT.equals(info.getImageryType()), "wms_endpoint imagery type expected");
            final WMSImagery wms = new WMSImagery(info.getUrl(), info.getCustomHttpHeaders());

            final WMSLayerTree tree = new WMSLayerTree();
            tree.updateTree(wms);

            Collection<String> wmsFormats = wms.getFormats();
            final JComboBox<String> formats = new JComboBox<>(wmsFormats.toArray(new String[0]));
            formats.setSelectedItem(wms.getPreferredFormat());
            formats.setToolTipText(tr("Select image format for WMS layer"));

            if (!GraphicsEnvironment.isHeadless()) {
                ExtendedDialog dialog = new ExtendedDialog(MainApplication.getMainFrame(),
                        tr("Select WMS layers"), tr("Add layers"), tr("Cancel"));
                final JScrollPane scrollPane = new JScrollPane(tree.getLayerTree());
                scrollPane.setPreferredSize(new Dimension(400, 400));
                final JPanel panel = new JPanel(new GridBagLayout());
                panel.add(scrollPane, GBC.eol().fill());
                panel.add(formats, GBC.eol().fill(GBC.HORIZONTAL));
                dialog.setContent(panel);

                if (dialog.showDialog().getValue() != 1) {
                    return null;
                }
            }

            final String url = wms.buildGetMapUrl(
                    tree.getSelectedLayers().stream().map(LayerDetails::getName).collect(Collectors.toList()),
                    (List<String>) null,
                    (String) formats.getSelectedItem(),
                    true // TODO: ask the user if transparent layer is wanted
                    );

            String selectedLayers = tree.getSelectedLayers().stream()
                    .map(LayerDetails::getName)
                    .collect(Collectors.joining(", "));
            // Use full copy of original Imagery info to copy all attributes. Only overwrite what's different
            ImageryInfo ret = new ImageryInfo(info);
            ret.setUrl(url);
            ret.setImageryType(ImageryType.WMS);
            ret.setName(info.getName() + selectedLayers);
            ret.setServerProjections(wms.getServerProjections(tree.getSelectedLayers()));
            return ret;
        } catch (MalformedURLException ex) {
            handleException(ex, tr("Invalid service URL."), tr("WMS Error"), null);
        } catch (IOException ex) {
            handleException(ex, tr("Could not retrieve WMS layer list."), tr("WMS Error"), null);
        } catch (WMSGetCapabilitiesException ex) {
            handleException(ex, tr("Could not parse WMS layer list."), tr("WMS Error"),
                    "Could not parse WMS layer list. Incoming data:\n" + ex.getIncomingData());
        }
        return null;
    }

    private static void handleException(Exception ex, String uiMessage, String uiTitle, String logMessage) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), uiMessage, uiTitle, JOptionPane.ERROR_MESSAGE);
        }
        Logging.log(Logging.LEVEL_ERROR, logMessage, ex);
    }

    @Override
    protected void updateEnabledState() {
        if (info.isBlacklisted()) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    @Override
    public String toString() {
        return "AddImageryLayerAction [info=" + info + ']';
    }
}
