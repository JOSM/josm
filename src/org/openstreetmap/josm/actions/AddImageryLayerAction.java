// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.actionsupport.AlignImageryPanel;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.preferences.imagery.WMSLayerTree;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.imagery.WMSImagery;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageCallback;

/**
 * Action displayed in imagery menu to add a new imagery layer. 
 * @since 3715
 */
public class AddImageryLayerAction extends JosmAction implements AdaptableAction {

    private static final int MAX_ICON_SIZE = 24;
    private final ImageryInfo info;

    /**
     * Constructs a new {@code AddImageryLayerAction} for the given {@code ImageryInfo}.
     * If an http:// icon is specified, it is fetched asynchronously.
     * @param info The imagery info
     */
    public AddImageryLayerAction(ImageryInfo info) {
        super(info.getMenuName(), /* ICON */"imagery_menu", tr("Add imagery layer {0}",info.getName()), null, false, false);
        putValue("toolbar", "imagery_" + info.getToolbarName());
        this.info = info;
        installAdapters();

        // change toolbar icon from if specified
        try {
            if (info.getIcon() != null) {
                new ImageProvider(info.getIcon()).setOptional(true).
                        setMaxHeight(MAX_ICON_SIZE).setMaxWidth(MAX_ICON_SIZE).getInBackground(new ImageCallback() {
                            @Override
                            public void finished(final ImageIcon result) {
                                if (result != null) {
                                    GuiHelper.runInEDT(new Runnable() {
                                        @Override
                                        public void run() {
                                            putValue(Action.SMALL_ICON, result);
                                        }
                                    });
                                }
                            }
                        });
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final ImageryInfo infoToAdd = ImageryType.WMS_ENDPOINT.equals(info.getImageryType())
                    ? getWMSLayerInfo() : info;
            if (infoToAdd != null) {
                Main.main.addLayer(ImageryLayer.create(infoToAdd));
                AlignImageryPanel.addNagPanelIfNeeded();
            }
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
                throw ex;
            } else {
                JOptionPane.showMessageDialog(Main.parent,
                        ex.getMessage(), tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected ImageryInfo getWMSLayerInfo() {
        try {
            assert (ImageryType.WMS_ENDPOINT.equals(info.getImageryType()));
            final WMSImagery wms = new WMSImagery();
            wms.attemptGetCapabilities(info.getUrl());

            final WMSLayerTree tree = new WMSLayerTree();
            tree.updateTree(wms);
            final JComboBox formats = new JComboBox(wms.getFormats().toArray());
            formats.setToolTipText(tr("Select image format for WMS layer"));

            if (1 != new ExtendedDialog(Main.parent, tr("Select WMS layers"), new String[]{tr("Add layers"), tr("Cancel")}) {{
                final JScrollPane scrollPane = new JScrollPane(tree.getLayerTree());
                scrollPane.setPreferredSize(new Dimension(400, 400));
                final JPanel panel = new JPanel(new GridBagLayout());
                panel.add(scrollPane, GBC.eol().fill());
                panel.add(formats, GBC.eol().fill(GBC.HORIZONTAL));
                setContent(panel);
            }}.showDialog().getValue()) {
                return null;
            }

            final String url = wms.buildGetMapUrl(
                    tree.getSelectedLayers(), (String) formats.getSelectedItem());
            return new ImageryInfo(info.getName(), url, "wms", info.getEulaAcceptanceRequired(), info.getCookies());
        } // exception handling from AddWMSLayerPanel.java
        catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(Main.parent, tr("Invalid service URL."),
                    tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(Main.parent, tr("Could not retrieve WMS layer list."),
                    tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
        } catch (WMSImagery.WMSGetCapabilitiesException ex) {
            JOptionPane.showMessageDialog(Main.parent, tr("Could not parse WMS layer list."),
                    tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
            Main.error("Could not parse WMS layer list. Incoming data:\n"+ex.getIncomingData());
        }
        return null;
    }

    protected boolean isLayerAlreadyPresent() {
        if (Main.isDisplayingMapView()) {
            for (ImageryLayer layer : Main.map.mapView.getLayersOfType(ImageryLayer.class)) {
                if (info.equals(layer.getInfo())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void updateEnabledState() {
        // never enable blacklisted entries. Do not add same imagery layer twice (fix #2519)
        if (info.isBlacklisted() /*|| isLayerAlreadyPresent()*/) { // FIXME check disabled to allow several instances with different settings (see #7981)
            setEnabled(false);
        } else if (info.getImageryType() == ImageryType.TMS || info.getImageryType() == ImageryType.BING || info.getImageryType() == ImageryType.SCANEX) {
            setEnabled(true);
        } else if (Main.isDisplayingMapView() && !Main.map.mapView.getAllLayers().isEmpty()) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
}
