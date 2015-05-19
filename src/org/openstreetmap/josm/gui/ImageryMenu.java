// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.MenuComponent;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AddImageryLayerAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.MapRectifierWMSmenuAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Imagery menu, holding entries for imagery preferences, offset actions and dynamic imagery entries
 * depending on current maview coordinates.
 * @since 3737
 */
public class ImageryMenu extends JMenu implements LayerChangeListener {

    /**
     * Compare ImageryInfo objects alphabetically by name.
     *
     * ImageryInfo objects are normally sorted by country code first
     * (for the preferences). We don't want this in the imagery menu.
     */
    public static final Comparator<ImageryInfo> alphabeticImageryComparator = new Comparator<ImageryInfo>() {
        @Override
        public int compare(ImageryInfo ii1, ImageryInfo ii2) {
            return ii1.getName().toLowerCase().compareTo(ii2.getName().toLowerCase());
        }
    };

    private transient Action offsetAction = new JosmAction(
            tr("Imagery offset"), "mapmode/adjustimg", tr("Adjust imagery offset"), null, false, false) {
        {
            putValue("toolbar", "imagery-offset");
            Main.toolbar.register(this);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<ImageryLayer> layers = Main.map.mapView.getLayersOfType(ImageryLayer.class);
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
                JComponent c = layers.iterator().next().getOffsetMenuItem(popup);
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

    private final JMenuItem singleOffset = new JMenuItem(offsetAction);
    private JMenuItem offsetMenuItem = singleOffset;
    private final MapRectifierWMSmenuAction rectaction = new MapRectifierWMSmenuAction();

    /**
     * Constructs a new {@code ImageryMenu}.
     * @param subMenu submenu in that contains plugin-managed additional imagery layers
     */
    public ImageryMenu(JMenu subMenu) {
        super(tr("Imagery"));
        setupMenuScroller();
        MapView.addLayerChangeListener(this);
        // build dynamically
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                refreshImageryMenu();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
        MainMenu.add(subMenu, rectaction);
    }

    private void setupMenuScroller() {
        if (!GraphicsEnvironment.isHeadless()) {
            MenuScroller.setScrollerFor(this, 150, 2);
        }
    }

    /**
     * Refresh imagery menu.
     *
     * Outside this class only called in {@link ImageryPreference#initialize()}.
     * (In order to have actions ready for the toolbar, see #8446.)
     */
    public void refreshImageryMenu() {
        removeDynamicItems();

        addDynamic(offsetMenuItem);
        addDynamicSeparator();

        // for each configured ImageryInfo, add a menu entry.
        final List<ImageryInfo> savedLayers = new ArrayList<>(ImageryLayerInfo.instance.getLayers());
        Collections.sort(savedLayers, alphabeticImageryComparator);
        for (final ImageryInfo u : savedLayers) {
            addDynamic(new AddImageryLayerAction(u));
        }

        // list all imagery entries where the current map location
        // is within the imagery bounds
        if (Main.isDisplayingMapView()) {
            MapView mv = Main.map.mapView;
            LatLon pos = mv.getProjection().eastNorth2latlon(mv.getCenter());
            final List<ImageryInfo> inViewLayers = new ArrayList<>();

            for (ImageryInfo i : ImageryLayerInfo.instance.getDefaultLayers()) {
                if (i.getBounds() != null && i.getBounds().contains(pos)) {
                    inViewLayers.add(i);
                }
            }
            // Do not suggest layers already in use
            inViewLayers.removeAll(ImageryLayerInfo.instance.getLayers());
            // For layers containing complex shapes, check that center is in one
            // of its shapes (fix #7910)
            for (Iterator<ImageryInfo> iti = inViewLayers.iterator(); iti.hasNext(); ) {
                List<Shape> shapes = iti.next().getBounds().getShapes();
                if (shapes != null && !shapes.isEmpty()) {
                    boolean found = false;
                    for (Iterator<Shape> its = shapes.iterator(); its.hasNext() && !found; ) {
                        found = its.next().contains(pos);
                    }
                    if (!found) {
                        iti.remove();
                    }
                }
            }
            if (!inViewLayers.isEmpty()) {
                Collections.sort(inViewLayers, alphabeticImageryComparator);
                addDynamicSeparator();
                for (ImageryInfo i : inViewLayers) {
                    addDynamic(new AddImageryLayerAction(i));
                }
            }
        }

        addDynamicSeparator();
        JMenu subMenu = Main.main.menu.imagerySubMenu;
        int heightUnrolled = 30*(getItemCount()+subMenu.getItemCount());
        if (heightUnrolled < Main.panel.getHeight()) {
            // add all items of submenu if they will fit on screen
            int n = subMenu.getItemCount();
            for (int i=0; i<n; i++) {
                addDynamic(subMenu.getItem(i).getAction());
            }
        } else {
            // or add the submenu itself
            addDynamic(subMenu);
        }
    }

    private JMenuItem getNewOffsetMenu(){
        if (!Main.isDisplayingMapView()) {
            offsetAction.setEnabled(false);
            return singleOffset;
        }
        Collection<ImageryLayer> layers = Main.map.mapView.getLayersOfType(ImageryLayer.class);
        if (layers.isEmpty()) {
            offsetAction.setEnabled(false);
            return singleOffset;
        }
        offsetAction.setEnabled(true);
        JMenu newMenu = new JMenu(trc("layer","Offset"));
        newMenu.setIcon(ImageProvider.get("mapmode", "adjustimg"));
        newMenu.setAction(offsetAction);
        if (layers.size() == 1)
            return (JMenuItem)layers.iterator().next().getOffsetMenuItem(newMenu);
        for (ImageryLayer layer : layers) {
            JMenuItem layerMenu = layer.getOffsetMenuItem();
            layerMenu.setText(layer.getName());
            layerMenu.setIcon(layer.getIcon());
            newMenu.add(layerMenu);
        }
        return newMenu;
    }

    public void refreshOffsetMenu() {
        offsetMenuItem = getNewOffsetMenu();
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

    /**
     * Collection to store temporary menu items. They will be deleted
     * (and possibly recreated) when refreshImageryMenu() is called.
     * @since 5803
     */
    private List<Object> dynamicItems = new ArrayList<>(20);

    /**
     * Remove all the items in @field dynamicItems collection
     * @since 5803
     */
    private void removeDynamicItems() {
        for (Object item : dynamicItems) {
            if (item instanceof JMenuItem) {
                remove((JMenuItem)item);
            }
            if (item instanceof MenuComponent) {
                remove((MenuComponent)item);
            }
            if (item instanceof Component) {
                remove((Component)item);
            }
        }
        dynamicItems.clear();
    }

    private void addDynamicSeparator() {
        JPopupMenu.Separator s =  new JPopupMenu.Separator();
        dynamicItems.add(s);
        add(s);
    }

    private void addDynamic(Action a) {
        dynamicItems.add( this.add(a) );
    }

    private void addDynamic(JMenuItem it) {
        dynamicItems.add( this.add(it) );
    }
}
