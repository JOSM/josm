// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.io.imagery.HTMLGrabber;
import org.openstreetmap.josm.io.imagery.OffsetServer;
import org.openstreetmap.josm.io.imagery.OsmosnimkiOffsetServer;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class ImageryPreference implements PreferenceSetting {
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ImageryPreference();
        }
    }
    ImageryProvidersPanel imageryProviders;
    ImageryLayerInfo layerInfo;

    // Common settings
    private Color colFadeColor;
    private JButton btnFadeColor;
    private JSlider fadeAmount = new JSlider(0, 100);
    private JComboBox sharpen;
    private JCheckBox useOffsetServer;
    private JTextField offsetServerUrl;

    // WMS Settings
    private JComboBox browser;
    JCheckBox overlapCheckBox;
    JSpinner spinEast;
    JSpinner spinNorth;
    JSpinner spinSimConn;

    //TMS settings controls
    private JCheckBox autozoomActive = new JCheckBox();
    private JCheckBox autoloadTiles = new JCheckBox();
    private JSpinner minZoomLvl;
    private JSpinner maxZoomLvl;
    private JCheckBox addToSlippyMapChosser = new JCheckBox();
    private JTextField tilecacheDir = new JTextField();

    private JPanel buildCommonSettingsPanel(final PreferenceTabbedPane gui) {
        final JPanel p = new JPanel(new GridBagLayout());

        this.colFadeColor = ImageryLayer.getFadeColor();
        this.btnFadeColor = new JButton();

        this.btnFadeColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JColorChooser chooser = new JColorChooser(colFadeColor);
                int answer = JOptionPane.showConfirmDialog(
                        gui, chooser,
                        tr("Choose a color for {0}", tr("imagery fade")),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    colFadeColor = chooser.getColor();
                    btnFadeColor.setBackground(colFadeColor);
                    btnFadeColor.setText(ColorHelper.color2html(colFadeColor));
                }
            }
        });

        p.add(new JLabel(tr("Fade Color: ")), GBC.std());
        p.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(this.btnFadeColor, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Fade amount: ")), GBC.std());
        p.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(this.fadeAmount, GBC.eol().fill(GBC.HORIZONTAL));

        this.sharpen = new JComboBox(new String[] {
                tr("None"),
                tr("Soft"),
                tr("Strong")});
        p.add(new JLabel(tr("Sharpen (requires layer re-add): ")));
        p.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(this.sharpen, GBC.eol().fill(GBC.HORIZONTAL));

        this.useOffsetServer = new JCheckBox(tr("Use offset server: "));
        this.offsetServerUrl = new JTextField();
        this.useOffsetServer.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                offsetServerUrl.setEnabled(useOffsetServer.isSelected());
            }
        });
        offsetServerUrl.setEnabled(useOffsetServer.isSelected());
        p.add(this.useOffsetServer, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(this.offsetServerUrl, GBC.eol().fill(GBC.HORIZONTAL));
        return p;
    }

    private JPanel buildWMSSettingsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        browser = new JComboBox(new String[] {
                "webkit-image {0}",
                "gnome-web-photo --mode=photo --format=png {0} /dev/stdout",
                "gnome-web-photo-fixed {0}",
        "webkit-image-gtk {0}"});
        browser.setEditable(true);
        p.add(new JLabel(tr("Downloader:")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(browser);

        // Overlap
        p.add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));

        overlapCheckBox = new JCheckBox(tr("Overlap tiles"));
        JLabel labelEast = new JLabel(tr("% of east:"));
        JLabel labelNorth = new JLabel(tr("% of north:"));
        spinEast = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_OVERLAP_EAST.get(), 1, 50, 1));
        spinNorth = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_OVERLAP_NORTH.get(), 1, 50, 1));

        JPanel overlapPanel = new JPanel(new FlowLayout());
        overlapPanel.add(overlapCheckBox);
        overlapPanel.add(labelEast);
        overlapPanel.add(spinEast);
        overlapPanel.add(labelNorth);
        overlapPanel.add(spinNorth);

        p.add(overlapPanel);

        // Simultaneous connections
        p.add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));
        JLabel labelSimConn = new JLabel(tr("Simultaneous connections"));
        spinSimConn = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.get(), 1, 30, 1));
        JPanel overlapPanelSimConn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        overlapPanelSimConn.add(labelSimConn);
        overlapPanelSimConn.add(spinSimConn);
        p.add(overlapPanelSimConn, GBC.eol().fill(GBC.HORIZONTAL));

        return p;
    }

    private JPanel buildTMSSettingsPanel() {
        JPanel tmsTab = new JPanel(new GridBagLayout());

        minZoomLvl = new JSpinner(new SpinnerNumberModel(TMSLayer.DEFAULT_MIN_ZOOM, TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM, 1));
        maxZoomLvl = new JSpinner(new SpinnerNumberModel(TMSLayer.DEFAULT_MAX_ZOOM, TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM, 1));

        tmsTab.add(new JLabel(tr("Auto zoom by default: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std());
        tmsTab.add(autozoomActive, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Autoload tiles by default: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std());
        tmsTab.add(autoloadTiles, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Min. zoom level: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std());
        tmsTab.add(this.minZoomLvl, GBC.eol());

        tmsTab.add(new JLabel(tr("Max. zoom level: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std());
        tmsTab.add(this.maxZoomLvl, GBC.eol());

        tmsTab.add(new JLabel(tr("Add to slippymap chooser: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std());
        tmsTab.add(addToSlippyMapChosser, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Tile cache directory: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std());
        tmsTab.add(tilecacheDir, GBC.eol().fill(GBC.HORIZONTAL));

        return tmsTab;
    }

    private void addSettingsSection(final JPanel p, String name, JPanel section) {
        addSettingsSection(p, name, section, GBC.eol());
    }
    private void addSettingsSection(final JPanel p, String name, JPanel section, GBC gbc) {
        final JLabel lbl = new JLabel(name);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        p.add(lbl,GBC.std());
        p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 0));
        p.add(section, gbc.insets(20,5,0,10));
    }

    private Component buildSettingsPanel(final PreferenceTabbedPane gui) {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        addSettingsSection(p, tr("Common Settings"), buildCommonSettingsPanel(gui));
        addSettingsSection(p, tr("WMS Settings"), buildWMSSettingsPanel());
        addSettingsSection(p, tr("TMS Settings"), buildTMSSettingsPanel(),
                GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JPanel(),GBC.eol().fill(GBC.BOTH));
        return new JScrollPane(p);
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab("imagery", tr("Imagery Preferences"), tr("Modify list of imagery layers displayed in the Imagery menu"));
        JTabbedPane pane = new JTabbedPane();
        layerInfo = new ImageryLayerInfo(ImageryLayerInfo.instance);
        imageryProviders = new ImageryProvidersPanel(gui, layerInfo);
        pane.add(imageryProviders);
        pane.add(buildSettingsPanel(gui));
        pane.add(new OffsetBookmarksPanel(gui));
        loadSettings();
        pane.setTitleAt(0, tr("Imagery providers"));
        pane.setTitleAt(1, tr("Settings"));
        pane.setTitleAt(2, tr("Offset bookmarks"));
        p.add(pane,GBC.std().fill(GBC.BOTH));
    }

    private void loadSettings() {
        // Common settings
        this.btnFadeColor.setBackground(colFadeColor);
        this.btnFadeColor.setText(ColorHelper.color2html(colFadeColor));
        this.fadeAmount.setValue(ImageryLayer.PROP_FADE_AMOUNT.get());
        this.sharpen.setSelectedIndex(Math.max(0, Math.min(2, ImageryLayer.PROP_SHARPEN_LEVEL.get())));
        this.useOffsetServer.setSelected(OffsetServer.PROP_SERVER_ENABLED.get());
        this.offsetServerUrl.setText(OsmosnimkiOffsetServer.PROP_SERVER_URL.get());

        // WMS Settings
        this.browser.setSelectedItem(HTMLGrabber.PROP_BROWSER.get());
        this.overlapCheckBox.setSelected(WMSLayer.PROP_OVERLAP.get());
        this.spinEast.setValue(WMSLayer.PROP_OVERLAP_EAST.get());
        this.spinNorth.setValue(WMSLayer.PROP_OVERLAP_NORTH.get());
        this.spinSimConn.setValue(WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.get());

        // TMS Settings
        this.autozoomActive.setSelected(TMSLayer.PROP_DEFAULT_AUTOZOOM.get());
        this.autoloadTiles.setSelected(TMSLayer.PROP_DEFAULT_AUTOLOAD.get());
        this.addToSlippyMapChosser.setSelected(TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get());
        this.maxZoomLvl.setValue(TMSLayer.getMaxZoomLvl(null));
        this.minZoomLvl.setValue(TMSLayer.getMinZoomLvl(null));
        this.tilecacheDir.setText(TMSLayer.PROP_TILECACHE_DIR.get());
    }

    @Override
    public boolean ok() {
        boolean restartRequired = false;
        layerInfo.save();
        ImageryLayerInfo.instance.clear();
        ImageryLayerInfo.instance.load();
        Main.main.menu.imageryMenu.refreshImageryMenu();
        Main.main.menu.imageryMenu.refreshOffsetMenu();
        OffsetBookmark.saveBookmarks();

        WMSLayer.PROP_OVERLAP.put(overlapCheckBox.getModel().isSelected());
        WMSLayer.PROP_OVERLAP_EAST.put((Integer) spinEast.getModel().getValue());
        WMSLayer.PROP_OVERLAP_NORTH.put((Integer) spinNorth.getModel().getValue());
        WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.put((Integer) spinSimConn.getModel().getValue());

        HTMLGrabber.PROP_BROWSER.put(browser.getEditor().getItem().toString());
        OffsetServer.PROP_SERVER_ENABLED.put(useOffsetServer.isSelected());
        OsmosnimkiOffsetServer.PROP_SERVER_URL.put(offsetServerUrl.getText());

        if (TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get() != this.addToSlippyMapChosser.isSelected()) {
            restartRequired = true;
        }
        TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.put(this.addToSlippyMapChosser.isSelected());
        TMSLayer.PROP_DEFAULT_AUTOZOOM.put(this.autozoomActive.isSelected());
        TMSLayer.PROP_DEFAULT_AUTOLOAD.put(this.autoloadTiles.isSelected());
        TMSLayer.setMaxZoomLvl((Integer)this.maxZoomLvl.getValue());
        TMSLayer.setMinZoomLvl((Integer)this.minZoomLvl.getValue());
        TMSLayer.PROP_TILECACHE_DIR.put(this.tilecacheDir.getText());

        ImageryLayer.PROP_FADE_AMOUNT.put(this.fadeAmount.getValue());
        ImageryLayer.setFadeColor(this.colFadeColor);
        ImageryLayer.PROP_SHARPEN_LEVEL.put(sharpen.getSelectedIndex());

        return restartRequired;
    }


    /**
     * Updates a server URL in the preferences dialog. Used by plugins.
     *
     * @param server
     *            The server name
     * @param url
     *            The server URL
     */
    public void setServerUrl(String server, String url) {
        for (int i = 0; i < imageryProviders.model.getRowCount(); i++) {
            if (server.equals(imageryProviders.model.getValueAt(i, 0).toString())) {
                imageryProviders.model.setValueAt(url, i, 1);
                return;
            }
        }
        imageryProviders.model.addRow(new String[] { server, url });
    }

    /**
     * Gets a server URL in the preferences dialog. Used by plugins.
     *
     * @param server
     *            The server name
     * @return The server URL
     */
    public String getServerUrl(String server) {
        for (int i = 0; i < imageryProviders.model.getRowCount(); i++) {
            if (server.equals(imageryProviders.model.getValueAt(i, 0).toString()))
                return imageryProviders.model.getValueAt(i, 1).toString();
        }
        return null;
    }

    static class ImageryProvidersPanel extends JPanel {
        final ImageryLayerTableModel model;
        final ImageryDefaultLayerTableModel modeldef;
        private final ImageryLayerInfo layerInfo;
        private JTable listActive;
        final JTable listdef;
        final JMapViewer map;
        final PreferenceTabbedPane gui;

        public ImageryProvidersPanel(final PreferenceTabbedPane gui, ImageryLayerInfo layerInfo) {
            super(new GridBagLayout());
            this.gui = gui;
            this.layerInfo = layerInfo;
            this.model = new ImageryLayerTableModel();

            listActive = new JTable(model) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return model.getValueAt(rowAtPoint(p), columnAtPoint(p)).toString();
                }
            };

            modeldef = new ImageryDefaultLayerTableModel();
            listdef = new JTable(modeldef) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return (String) modeldef.getValueAt(rowAtPoint(p), columnAtPoint(p));
                }
            };

            TableColumnModel mod = listdef.getColumnModel();
            mod.getColumn(2).setPreferredWidth(800);
            mod.getColumn(1).setPreferredWidth(400);
            mod.getColumn(0).setPreferredWidth(50);
            mod = listActive.getColumnModel();
            mod.getColumn(2).setPreferredWidth(50);
            mod.getColumn(1).setPreferredWidth(800);
            mod.getColumn(0).setPreferredWidth(200);

            RemoveEntryAction remove = new RemoveEntryAction();
            listActive.getSelectionModel().addListSelectionListener(remove);

            add(new JLabel(tr("Available default entries:")), GBC.eol().insets(5, 5, 0, 0));
            // Add default item list
            JScrollPane scrolldef = new JScrollPane(listdef);
            scrolldef.setPreferredSize(new Dimension(200, 200));
            add(scrolldef, GBC.std().insets(0, 5, 0, 0).fill(GridBagConstraints.BOTH).weight(1.0, 0.6).insets(5, 0, 0, 0));

            // Add default item map
            map = new JMapViewer();
            map.setZoomContolsVisible(false);
            map.setMinimumSize(new Dimension(100, 200));
            add(map, GBC.std().insets(5, 5, 0, 0).fill(GridBagConstraints.BOTH).weight(0.33, 0.6).insets(5, 0, 0, 0));

            listdef.getSelectionModel().addListSelectionListener(new DefListSelectionListener());

            JToolBar tb = new JToolBar(JToolBar.VERTICAL);
            tb.setFloatable(false);
            tb.setBorderPainted(false);
            tb.setOpaque(false);
            tb.add(new ReloadAction());
            add(tb, GBC.eol().anchor(GBC.SOUTH).insets(0, 0, 5, 0));

            ActivateAction activate = new ActivateAction();
            listdef.getSelectionModel().addListSelectionListener(activate);
            JButton btnActivate = new JButton(activate);

            JToolBar tb2 = new JToolBar(JToolBar.VERTICAL);
            tb2.setFloatable(false);
            tb2.setBorderPainted(false);
            tb2.setOpaque(false);
            tb2.add(btnActivate);
            add(tb2, GBC.eol().anchor(GBC.CENTER).insets(5, 15, 5, 0));

            add(Box.createHorizontalGlue(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));

            add(new JLabel(tr("Selected entries:")), GBC.eol().insets(5, 0, 0, 0));
            JScrollPane scroll = new JScrollPane(listActive);
            add(scroll, GBC.std().fill(GridBagConstraints.BOTH).span(GridBagConstraints.RELATIVE).weight(1.0, 0.4).insets(5, 0, 0, 5));
            scroll.setPreferredSize(new Dimension(200, 200));

            JToolBar sideButtonTB = new JToolBar(JToolBar.VERTICAL);
            sideButtonTB.setFloatable(false);
            sideButtonTB.setBorderPainted(false);
            sideButtonTB.setOpaque(false);
            sideButtonTB.add(new NewEntryAction());
            //sideButtonTB.add(edit); TODO
            sideButtonTB.add(remove);
            add(sideButtonTB, GBC.eol().anchor(GBC.NORTH).insets(0, 0, 5, 5));

        }

        // Listener of default providers list selection
        private final class DefListSelectionListener implements ListSelectionListener {
            // The current drawn rectangles and polygons
            private final Map<Integer, MapRectangle> mapRectangles;
            private final Map<Integer, List<MapPolygon>> mapPolygons;

            private DefListSelectionListener() {
                this.mapRectangles = new HashMap<Integer, MapRectangle>();
                this.mapPolygons = new HashMap<Integer, List<MapPolygon>>();
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                // First index is set to -1 when the list is refreshed, so discard all map rectangles and polygons
                if (e.getFirstIndex() == -1) {
                    map.removeAllMapRectangles();
                    map.removeAllMapPolygons();
                    mapRectangles.clear();
                    mapPolygons.clear();
                    // Only process complete (final) selection events
                } else if (!e.getValueIsAdjusting()) {
                    for (int i = e.getFirstIndex(); i<=e.getLastIndex(); i++) {
                        updateBoundsAndShapes(i);
                    }
                    // If needed, adjust map to show all map rectangles and polygons
                    if (!mapRectangles.isEmpty() || !mapPolygons.isEmpty()) {
                        map.setDisplayToFitMapElements(false, true, true);
                        map.zoomOut();
                    }
                }
            }
            
            private void updateBoundsAndShapes(int i) {
                ImageryBounds bounds = modeldef.getRow(i).getBounds();
                if (bounds != null) {
                    List<Shape> shapes = bounds.getShapes();
                    if (shapes != null && !shapes.isEmpty()) {
                        if (listdef.getSelectionModel().isSelectedIndex(i)) {
                            if (!mapPolygons.containsKey(i)) {
                                List<MapPolygon> list = new ArrayList<MapPolygon>();
                                mapPolygons.put(i, list);
                                // Add new map polygons
                                for (Shape shape : shapes) {
                                    MapPolygon polygon = new MapPolygonImpl(shape.getPoints());
                                    list.add(polygon);
                                    map.addMapPolygon(polygon);
                                }
                            }
                        } else if (mapPolygons.containsKey(i)) {
                            // Remove previously drawn map polygons
                            for (MapPolygon polygon : mapPolygons.get(i)) {
                                map.removeMapPolygon(polygon);
                            }
                            mapPolygons.remove(i);
                        }
                     // Only display bounds when no polygons (shapes) are defined for this provider
                    } else {
                        if (listdef.getSelectionModel().isSelectedIndex(i)) {
                            if (!mapRectangles.containsKey(i)) {
                                // Add new map rectangle
                                MapRectangle rectangle = new MapRectangleImpl(bounds);
                                mapRectangles.put(i, rectangle);
                                map.addMapRectangle(rectangle);
                            }
                        } else if (mapRectangles.containsKey(i)) {
                            // Remove previously drawn map rectangle
                            map.removeMapRectangle(mapRectangles.get(i));
                            mapRectangles.remove(i);
                        }
                    }
                }
            }
        }

        class NewEntryAction extends AbstractAction {
            public NewEntryAction() {
                putValue(NAME, tr("New"));
                putValue(SHORT_DESCRIPTION, tr("Add a new WMS/TMS entry by entering the URL"));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
            }

            public void actionPerformed(ActionEvent evt) {
                AddWMSLayerPanel p = new AddWMSLayerPanel();
                int answer = JOptionPane.showConfirmDialog(
                        gui, p,
                        tr("Add Imagery URL"),
                        JOptionPane.OK_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION) {
                    model.addRow(new ImageryInfo(p.getUrlName(), p.getUrl()));
                }
            }
        }

        class RemoveEntryAction extends AbstractAction implements ListSelectionListener {

            public RemoveEntryAction() {
                putValue(NAME, tr("Remove"));
                putValue(SHORT_DESCRIPTION, tr("Remove entry"));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
                updateEnabledState();
            }

            protected void updateEnabledState() {
                setEnabled(listActive.getSelectedRowCount() > 0);
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnabledState();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Integer i;
                while ((i = listActive.getSelectedRow()) != -1) {
                    model.removeRow(i);
                }
            }
        }

        class ActivateAction extends AbstractAction implements ListSelectionListener {
            public ActivateAction() {
                putValue(NAME, tr("Activate"));
                putValue(SHORT_DESCRIPTION, tr("copy selected defaults"));
                putValue(SMALL_ICON, ImageProvider.get("preferences", "activate-down"));
            }

            protected void updateEnabledState() {
                setEnabled(listdef.getSelectedRowCount() > 0);
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnabledState();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] lines = listdef.getSelectedRows();
                if (lines.length == 0) {
                    JOptionPane.showMessageDialog(
                            gui,
                            tr("Please select at least one row to copy."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                outer: for (int i = 0; i < lines.length; i++) {
                    ImageryInfo info = modeldef.getRow(lines[i]);

                    // Check if an entry with exactly the same values already
                    // exists
                    for (int j = 0; j < model.getRowCount(); j++) {
                        if (info.equalsBaseValues(model.getRow(j))) {
                            // Select the already existing row so the user has
                            // some feedback in case an entry exists
                            listActive.getSelectionModel().setSelectionInterval(j, j);
                            listActive.scrollRectToVisible(listActive.getCellRect(j, 0, true));
                            continue outer;
                        }
                    }

                    if (info.getEulaAcceptanceRequired() != null) {
                        if (!confirmEulaAcceptance(gui, info.getEulaAcceptanceRequired())) {
                            continue outer;
                        }
                    }

                    model.addRow(new ImageryInfo(info));
                    int lastLine = model.getRowCount() - 1;
                    listActive.getSelectionModel().setSelectionInterval(lastLine, lastLine);
                    listActive.scrollRectToVisible(listActive.getCellRect(lastLine, 0, true));
                }
            }
        }

        class ReloadAction extends AbstractAction {
            public ReloadAction() {
                putValue(SHORT_DESCRIPTION, tr("reload defaults"));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
            }

            public void actionPerformed(ActionEvent evt) {
                layerInfo.loadDefaults(true);
                modeldef.fireTableDataChanged();
            }
        }

        /**
         * The table model for imagery layer list
         */
        class ImageryLayerTableModel extends DefaultTableModel {
            public ImageryLayerTableModel() {
                setColumnIdentifiers(new String[] { tr("Menu Name"), tr("Imagery URL"), trc("layer", "Zoom") });
            }

            public ImageryInfo getRow(int row) {
                return layerInfo.getLayers().get(row);
            }

            public void addRow(ImageryInfo i) {
                layerInfo.add(i);
                int p = getRowCount() - 1;
                fireTableRowsInserted(p, p);
            }

            @Override
            public void removeRow(int i) {
                layerInfo.remove(getRow(i));
                fireTableRowsDeleted(i, i);
            }

            @Override
            public int getRowCount() {
                return layerInfo.getLayers().size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                ImageryInfo info = layerInfo.getLayers().get(row);
                switch (column) {
                case 0:
                    return info.getName();
                case 1:
                    return info.getExtendedUrl();
                case 2:
                    return (info.getImageryType() == ImageryType.WMS || info.getImageryType() == ImageryType.HTML) ?
                            (info.getPixelPerDegree() == 0.0 ? "" : info.getPixelPerDegree()) :
                                (info.getMaxZoom() == 0 ? "" : info.getMaxZoom());
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                ImageryInfo info = layerInfo.getLayers().get(row);
                switch (column) {
                case 0:
                    info.setName((String) o);
                    break;
                case 1:
                    info.setExtendedUrl((String)o);
                    break;
                case 2:
                    info.setPixelPerDegree(0);
                    info.setMaxZoom(0);
                    try {
                        if(info.getImageryType() == ImageryType.WMS || info.getImageryType() == ImageryType.HTML) {
                            info.setPixelPerDegree(Double.parseDouble((String) o));
                        } else {
                            info.setMaxZoom(Integer.parseInt((String) o));
                        }
                    } catch (NumberFormatException e) {
                    }
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        }

        /**
         * The table model for the default imagery layer list
         */
        class ImageryDefaultLayerTableModel extends DefaultTableModel {
            public ImageryDefaultLayerTableModel() {
                setColumnIdentifiers(new String[]{"", tr("Menu Name (Default)"), tr("Imagery URL (Default)")});
            }

            public ImageryInfo getRow(int row) {
                return layerInfo.getDefaultLayers().get(row);
            }

            @Override
            public int getRowCount() {
                return layerInfo.getDefaultLayers().size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                ImageryInfo info = layerInfo.getDefaultLayers().get(row);
                switch (column) {
                case 0:
                    return info.getCountryCode();
                case 1:
                    return info.getName();
                case 2:
                    return info.getExtendedUrl();
                }
                return null;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        }

        private boolean confirmEulaAcceptance(PreferenceTabbedPane gui, String eulaUrl) {
            URL url = null;
            try {
                url = new URL(eulaUrl.replaceAll("\\{lang\\}", Locale.getDefault().toString()));
                JEditorPane htmlPane = null;
                try {
                    htmlPane = new JEditorPane(url);
                } catch (IOException e1) {
                    // give a second chance with a default Locale 'en'
                    try {
                        url = new URL(eulaUrl.replaceAll("\\{lang\\}", "en"));
                        htmlPane = new JEditorPane(url);
                    } catch (IOException e2) {
                        JOptionPane.showMessageDialog(gui ,tr("EULA license URL not available: {0}", eulaUrl));
                        return false;
                    }
                }
                Box box = Box.createVerticalBox();
                htmlPane.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(htmlPane);
                scrollPane.setPreferredSize(new Dimension(400, 400));
                box.add(scrollPane);
                int option = JOptionPane.showConfirmDialog(Main.parent, box, tr("Please abort if you are not sure"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.YES_OPTION)
                    return true;
            } catch (MalformedURLException e2) {
                JOptionPane.showMessageDialog(gui ,tr("Malformed URL for the EULA licence: {0}", eulaUrl));
            }
            return false;
        }
    }

    static class OffsetBookmarksPanel extends JPanel {
        List<OffsetBookmark> bookmarks = OffsetBookmark.allBookmarks;
        OffsetsBookmarksModel model = new OffsetsBookmarksModel();

        public OffsetBookmarksPanel(final PreferenceTabbedPane gui) {
            super(new GridBagLayout());
            final JTable list = new JTable(model) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return model.getValueAt(rowAtPoint(p), columnAtPoint(p)).toString();
                }
            };
            JScrollPane scroll = new JScrollPane(list);
            add(scroll, GBC.eol().fill(GridBagConstraints.BOTH));
            scroll.setPreferredSize(new Dimension(200, 200));

            TableColumnModel mod = list.getColumnModel();
            mod.getColumn(0).setPreferredWidth(150);
            mod.getColumn(1).setPreferredWidth(200);
            mod.getColumn(2).setPreferredWidth(300);
            mod.getColumn(3).setPreferredWidth(150);
            mod.getColumn(4).setPreferredWidth(150);

            JPanel buttonPanel = new JPanel(new FlowLayout());

            JButton add = new JButton(tr("Add"));
            buttonPanel.add(add, GBC.std().insets(0, 5, 0, 0));
            add.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    OffsetBookmark b = new OffsetBookmark(Main.getProjection(),"","",0,0);
                    model.addRow(b);
                }
            });

            JButton delete = new JButton(tr("Delete"));
            buttonPanel.add(delete, GBC.std().insets(0, 5, 0, 0));
            delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (list.getSelectedRow() == -1) {
                        JOptionPane.showMessageDialog(gui, tr("Please select the row to delete."));
                    } else {
                        Integer i;
                        while ((i = list.getSelectedRow()) != -1) {
                            model.removeRow(i);
                        }
                    }
                }
            });

            add(buttonPanel,GBC.eol());
        }

        /**
         * The table model for imagery offsets list
         */
        class OffsetsBookmarksModel extends DefaultTableModel {
            public OffsetsBookmarksModel() {
                setColumnIdentifiers(new String[] { tr("Projection"),  tr("Layer"), tr("Name"), tr("Easting"), tr("Northing"),});
            }

            public OffsetBookmark getRow(int row) {
                return bookmarks.get(row);
            }

            public void addRow(OffsetBookmark i) {
                bookmarks.add(i);
                int p = getRowCount() - 1;
                fireTableRowsInserted(p, p);
            }

            @Override
            public void removeRow(int i) {
                bookmarks.remove(getRow(i));
                fireTableRowsDeleted(i, i);
            }

            @Override
            public int getRowCount() {
                return bookmarks.size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                OffsetBookmark info = bookmarks.get(row);
                switch (column) {
                case 0:
                    if (info.proj == null) return "";
                    return info.proj.toString();
                case 1:
                    return info.layerName;
                case 2:
                    return info.name;
                case 3:
                    return info.dx;
                case 4:
                    return info.dy;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                OffsetBookmark info = bookmarks.get(row);
                switch (column) {
                case 1:
                    info.layerName = o.toString();
                    break;
                case 2:
                    info.name = o.toString();
                    break;
                case 3:
                    info.dx = Double.parseDouble((String) o);
                    break;
                case 4:
                    info.dy = Double.parseDouble((String) o);
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 1;
            }
        }
    }

    public static void initialize() {
        ImageryLayerInfo.instance.clear();
        ImageryLayerInfo.instance.loadDefaults(false);
        ImageryLayerInfo.instance.load();
        OffsetBookmark.loadBookmarks();
        Main.main.menu.imageryMenu.refreshImageryMenu();
        Main.main.menu.imageryMenu.refreshOffsetMenu();
    }
}
