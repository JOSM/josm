// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AddImageryLayerAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.Map_Rectifier_WMSmenuAction;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.ImageProvider;

public class ImageryMenu extends JMenu implements MapView.LayerChangeListener {
    Action offsetAction = new JosmAction(
            tr("Imagery offset"), "mapmode/adjustimg", tr("Adjust imagery offset"), null, false, false) {
        {
            putValue("toolbar", "imagery-offset");
            Main.toolbar.register(this);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            List<ImageryLayer> layers = Main.map.mapView.getLayersOfType(ImageryLayer.class);
            if (layers.isEmpty()) {
                setEnabled(false);
                return;
            }
            Component source = null;
            if (e.getSource() instanceof Component) {
                source = (Component)e.getSource();
            }
            JPopupMenu popup = new JPopupMenu();
            if (layers.size() == 1) {
                JComponent c = layers.get(0).getOffsetMenuItem(popup);
                if (c instanceof JMenuItem) {
                    ((JMenuItem) c).getAction().actionPerformed(e);
                } else {
                    if (source == null) return;
                    popup.show(source, source.getWidth()/2, source.getHeight()/2);
                }
                return;
            }
            if (source == null) return;
            for (ImageryLayer layer : layers) {
                JMenuItem layerMenu = layer.getOffsetMenuItem();
                layerMenu.setText(layer.getName());
                layerMenu.setIcon(layer.getIcon());
                popup.add(layerMenu);
            }
            popup.show(source, source.getWidth()/2, source.getHeight()/2);
        }
    };
    JMenuItem singleOffset = new JMenuItem(offsetAction);
    JMenuItem offsetMenuItem = singleOffset;
    Map_Rectifier_WMSmenuAction rectaction = new Map_Rectifier_WMSmenuAction();
    JosmAction blankmenu = new JosmAction(
            tr("Blank Layer"), "blankmenu", tr("Open a blank WMS layer to load data from a file"), null, false) {
        @Override
        public void actionPerformed(ActionEvent ev) {
            if (!isEnabled()) return;
            Main.main.addLayer(new WMSLayer());
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(Main.map != null && Main.map.mapView != null && !Main.map.mapView.getAllLayers().isEmpty());
        }
    };
    int offsPos;

    public ImageryMenu() {
        super(tr("Imagery"));
        MapView.addLayerChangeListener(this);
    }

    public void refreshImageryMenu() {
        removeAll();

        // for each configured WMSInfo, add a menu entry.
        for (final ImageryInfo u : ImageryLayerInfo.instance.getLayers()) {
            add(new AddImageryLayerAction(u));
        }
        addSeparator();
        add(new JMenuItem(rectaction));

        addSeparator();
        offsPos = getMenuComponentCount();
        add(offsetMenuItem);
        addSeparator();
        add(new JMenuItem(blankmenu));
    }

    private JMenuItem getNewOffsetMenu(){
        if (Main.map == null || Main.map.mapView == null) {
            offsetAction.setEnabled(false);
            return singleOffset;
        }
        List<ImageryLayer> layers = Main.map.mapView.getLayersOfType(ImageryLayer.class);
        if (layers.isEmpty()) {
            offsetAction.setEnabled(false);
            return singleOffset;
        }
        offsetAction.setEnabled(true);
        JMenu newMenu = new JMenu(trc("layer","Offset")) {
            // Hack to prevent ToolbarPreference from tracing this menu
            // TODO: Modify ToolbarPreference to not to trace such dynamic submenus?
            @Override
            public MenuElement[] getSubElements() {
                return new MenuElement[0];
            }
        };
        newMenu.setIcon(ImageProvider.get("mapmode", "adjustimg"));
        newMenu.setAction(offsetAction);
        if (layers.size() == 1)
            return (JMenuItem)layers.get(0).getOffsetMenuItem(newMenu);
        for (ImageryLayer layer : layers) {
            JMenuItem layerMenu = layer.getOffsetMenuItem();
            layerMenu.setText(layer.getName());
            layerMenu.setIcon(layer.getIcon());
            newMenu.add(layerMenu);
        }
        return newMenu;
    }

    public void refreshOffsetMenu() {
        JMenuItem newItem = getNewOffsetMenu();
        remove(offsetMenuItem);
        add(newItem, offsPos);
        offsetMenuItem = newItem;
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
    }

    @Override
    public void layerAdded(Layer newLayer) {
        if (newLayer instanceof ImageryLayer) {
            refreshOffsetMenu();
        }
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof ImageryLayer) {
            refreshOffsetMenu();
        }
    }
}
