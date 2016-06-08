// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.LayerStateChangeListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This is the JOSM main window. It updates it's title.
 * @author Michael Zangl
 * @since 10340
 */
public class MainFrame extends JFrame {
    protected transient WindowGeometry geometry;
    protected int windowState = JFrame.NORMAL;
    private MainMenu menu;

    private final transient LayerStateChangeListener updateTitleOnLayerStateChange = new LayerStateChangeListener() {
        @Override
        public void uploadDiscouragedChanged(OsmDataLayer layer, boolean newValue) {
            onLayerChange(layer);
        }
    };

    private final transient PropertyChangeListener updateTitleOnSaveChange = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(OsmDataLayer.REQUIRES_SAVE_TO_DISK_PROP)
                    || evt.getPropertyName().equals(OsmDataLayer.REQUIRES_UPLOAD_TO_SERVER_PROP)) {
                OsmDataLayer layer = (OsmDataLayer) evt.getSource();
                onLayerChange(layer);
            }
        }
    };

    /**
     * Create a new main window.
     */
    public MainFrame() {
        this(new JPanel(), new WindowGeometry(new Rectangle(10, 10, 500, 500)));
    }

    /**
     * Create a new main window.
     * @param contentPanePrivate The content
     * @param geometry The inital geometry to use.
     */
    public MainFrame(Container contentPanePrivate, WindowGeometry geometry) {
        super();
        this.geometry = geometry;
        setContentPane(contentPanePrivate);
    }

    /**
     * Initializes the content of the window and get the current status panel.
     */
    public void initialize() {
        menu = new MainMenu();
        addComponentListener(new WindowPositionSizeListener());
        addWindowStateListener(new WindowPositionSizeListener());

        setJMenuBar(menu);
        geometry.applySafe(this);
        List<Image> l = new LinkedList<>();
        l.add(ImageProvider.get("logo_16x16x32").getImage());
        l.add(ImageProvider.get("logo_16x16x8").getImage());
        l.add(ImageProvider.get("logo_32x32x32").getImage());
        l.add(ImageProvider.get("logo_32x32x8").getImage());
        l.add(ImageProvider.get("logo_48x48x32").getImage());
        l.add(ImageProvider.get("logo_48x48x8").getImage());
        l.add(ImageProvider.get("logo").getImage());
        setIconImages(l);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent arg0) {
                Main.exitJosm(true, 0);
            }
        });
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // This listener is never removed, since the main frame exists forever.
        Main.getLayerManager().addActiveLayerChangeListener(new ActiveLayerChangeListener() {
            @Override
            public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
                refreshTitle();
            }
        });
        Main.getLayerManager().addLayerChangeListener(new ManageLayerListeners(), true);

        refreshTitle();

        getContentPane().add(Main.panel, BorderLayout.CENTER);
        Main.panel.add(Main.main.gettingStarted, BorderLayout.CENTER);
        menu.initialize();
    }

    /**
     * Stores the current state of the main frame.
     */
    public void storeState() {
        if (geometry != null) {
            geometry.remember("gui.geometry");
        }
        Main.pref.put("gui.maximized", (windowState & JFrame.MAXIMIZED_BOTH) != 0);
    }

    /**
     * Gets the main menu used for this window.
     * @return The main menu.
     */
    public MainMenu getMenu() {
        if (menu == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return menu;
    }

    /**
     * Sets this frame to be maximized.
     * @param maximized <code>true</code> if the window should be maximized.
     */
    public void setMaximized(boolean maximized) {
        if (maximized) {
            if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH)) {
                windowState = JFrame.MAXIMIZED_BOTH;
                setExtendedState(windowState);
            } else {
                Main.debug("Main window: maximizing not supported");
            }
        } else {
            throw new UnsupportedOperationException("Unimplemented.");
        }
    }

    /**
     * Update the title of the window to reflect the current content.
     */
    public void refreshTitle() {
        OsmDataLayer editLayer = Main.getLayerManager().getEditLayer();
        boolean dirty = editLayer != null && (editLayer.requiresSaveToFile()
                || (editLayer.requiresUploadToServer() && !editLayer.isUploadDiscouraged()));
        setTitle((dirty ? "* " : "") + tr("Java OpenStreetMap Editor"));
        getRootPane().putClientProperty("Window.documentModified", dirty);
    }

    private void onLayerChange(OsmDataLayer layer) {
        if (layer == Main.getLayerManager().getEditLayer()) {
            refreshTitle();
        }
    }

    /**
     * Manages the layer listeners, adds them to every layer.
     */
    private final class ManageLayerListeners implements LayerChangeListener {
        @Override
        public void layerAdded(LayerAddEvent e) {
            if (e.getAddedLayer() instanceof OsmDataLayer) {
                OsmDataLayer osmDataLayer = (OsmDataLayer) e.getAddedLayer();
                osmDataLayer.addLayerStateChangeListener(updateTitleOnLayerStateChange);
            }
            e.getAddedLayer().addPropertyChangeListener(updateTitleOnSaveChange);
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            if (e.getRemovedLayer() instanceof OsmDataLayer) {
                OsmDataLayer osmDataLayer = (OsmDataLayer) e.getRemovedLayer();
                osmDataLayer.removeLayerStateChangeListener(updateTitleOnLayerStateChange);
            }
            e.getRemovedLayer().removePropertyChangeListener(updateTitleOnSaveChange);
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            // not used
        }
    }

    private class WindowPositionSizeListener extends WindowAdapter implements ComponentListener {
        @Override
        public void windowStateChanged(WindowEvent e) {
            windowState = e.getNewState();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            // Do nothing
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            handleComponentEvent(e);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            handleComponentEvent(e);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            // Do nothing
        }

        private void handleComponentEvent(ComponentEvent e) {
            Component c = e.getComponent();
            if (c instanceof JFrame && c.isVisible()) {
                if (windowState == JFrame.NORMAL) {
                    geometry = new WindowGeometry((JFrame) c);
                } else {
                    geometry.fixScreen((JFrame) c);
                }
            }
        }
    }

}
