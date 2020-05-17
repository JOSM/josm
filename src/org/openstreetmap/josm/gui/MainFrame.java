// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.LayerStateChangeListener;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is the JOSM main window. It updates it's title.
 * @author Michael Zangl
 * @since 10340
 */
public class MainFrame extends JFrame {
    private final transient LayerStateChangeListener updateTitleOnLayerStateChange = (layer, newValue) -> onLayerChange(layer);

    private final transient PropertyChangeListener updateTitleOnSaveChange = evt -> {
        if (evt.getPropertyName().equals(OsmDataLayer.REQUIRES_SAVE_TO_DISK_PROP)
                || evt.getPropertyName().equals(OsmDataLayer.REQUIRES_UPLOAD_TO_SERVER_PROP)) {
            OsmDataLayer layer = (OsmDataLayer) evt.getSource();
            onLayerChange(layer);
        }
    };

    protected transient WindowGeometry geometry;
    protected int windowState = JFrame.NORMAL;
    private final MainPanel panel;
    private MainMenu menu;

    /**
     * Create a new main window.
     */
    public MainFrame() {
        this(new WindowGeometry(new Rectangle(10, 10, 500, 500)));
    }

    /**
     * Create a new main window. The parameter will be removed in the future.
     * @param geometry The initial geometry to use.
     * @since 12127
     */
    public MainFrame(WindowGeometry geometry) {
        super();
        this.geometry = geometry;
        this.panel = new MainPanel(MainApplication.getLayerManager());
        setContentPane(new JPanel(new BorderLayout()));
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
        List<Image> l = Stream.of(
                /* ICON */ "logo_16x16x32",
                /* ICON */ "logo_16x16x8",
                /* ICON */ "logo_32x32x32",
                /* ICON */ "logo_32x32x8",
                /* ICON */ "logo_48x48x32",
                /* ICON */ "logo_48x48x8",
                /* ICON */ "logo")
                .map(ImageProvider::getIfAvailable)
                .filter(Objects::nonNull)
                .map(ImageIcon::getImage)
                .collect(Collectors.toList());
        setIconImages(l);
        addWindowListener(new ExitWindowAdapter());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // This listener is never removed, since the main frame exists forever.
        MainApplication.getLayerManager().addActiveLayerChangeListener(e -> refreshTitle());
        MainApplication.getLayerManager().addAndFireLayerChangeListener(new ManageLayerListeners());
        UserIdentityManager.getInstance().addListener(this::refreshTitle);
        Config.getPref().addKeyPreferenceChangeListener("draw.show-user", e -> refreshTitle());
        refreshTitle();

        getContentPane().add(panel, BorderLayout.CENTER);
        menu.initialize();
    }

    /**
     * Stores the current state of the main frame.
     */
    public void storeState() {
        if (geometry != null) {
            geometry.remember("gui.geometry");
        }
        Config.getPref().putBoolean("gui.maximized", (windowState & JFrame.MAXIMIZED_BOTH) != 0);
    }

    /**
     * Gets the main menu used for this window.
     * @return The main menu.
     * @throws IllegalStateException if the main frame has not been initialized yet
     * @see #initialize
     */
    public MainMenu getMenu() {
        if (menu == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return menu;
    }

    /**
     * Gets the main panel.
     * @return The main panel.
     * @since 12125
     */
    public MainPanel getPanel() {
        return panel;
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
                Logging.debug("Main window: maximizing not supported");
            }
        } else {
            throw new UnsupportedOperationException("Unimplemented.");
        }
    }

    /**
     * Update the title of the window to reflect the current content.
     */
    public void refreshTitle() {
        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        boolean dirty = editLayer != null && (editLayer.requiresSaveToFile()
                || (editLayer.requiresUploadToServer() && !editLayer.isUploadDiscouraged()));
        String userInfo = UserIdentityManager.getInstance().getUserName();
        if (userInfo != null && Config.getPref().getBoolean("draw.show-user", false))
            userInfo = tr(" ({0})", "@" + userInfo);
        else
            userInfo = "";
        setTitle((dirty ? "* " : "") + tr("Java OpenStreetMap Editor") + userInfo);
        getRootPane().putClientProperty("Window.documentModified", dirty);
    }

    private void onLayerChange(OsmDataLayer layer) {
        if (layer == MainApplication.getLayerManager().getEditLayer()) {
            refreshTitle();
        }
    }

    static final class ExitWindowAdapter extends WindowAdapter {
        @Override
        public void windowClosing(final WindowEvent evt) {
            MainApplication.exitJosm(true, 0, null);
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
