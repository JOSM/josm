// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;

/**
 * Imagery preferences, including imagery providers, settings and offsets.
 * @since 3715
 */
public final class ImageryPreference extends DefaultTabPreferenceSetting {

    private ImageryProvidersPanel imageryProviders;
    private ImageryLayerInfo layerInfo;

    private final CommonSettingsPanel commonSettings = new CommonSettingsPanel();
    private final WMSSettingsPanel wmsSettings = new WMSSettingsPanel();
    private final TMSSettingsPanel tmsSettings = new TMSSettingsPanel();

    /**
     * Factory used to create a new {@code ImageryPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ImageryPreference();
        }
    }

    private ImageryPreference() {
        super(/* ICON(preferences/) */ "imagery", tr("Imagery preferences..."),
                tr("Modify list of imagery layers displayed in the Imagery menu"),
                false, new JTabbedPane());
    }

    private static void addSettingsSection(final JPanel p, String name, JPanel section) {
        addSettingsSection(p, name, section, GBC.eol());
    }

    private static void addSettingsSection(final JPanel p, String name, JPanel section, GBC gbc) {
        final JLabel lbl = new JLabel(name);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setLabelFor(section);
        p.add(lbl, GBC.std());
        p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 0));
        p.add(section, gbc.insets(20, 5, 0, 10));
    }

    private Component buildSettingsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        addSettingsSection(p, tr("Common Settings"), commonSettings);
        addSettingsSection(p, tr("WMS Settings"), wmsSettings,
                GBC.eol().fill(GBC.HORIZONTAL));
        addSettingsSection(p, tr("TMS Settings"), tmsSettings,
                GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JPanel(), GBC.eol().fill(GBC.BOTH));
        return GuiHelper.setDefaultIncrement(new JScrollPane(p));
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab(this);
        JTabbedPane pane = getTabPane();
        layerInfo = new ImageryLayerInfo(ImageryLayerInfo.instance);
        imageryProviders = new ImageryProvidersPanel(gui, layerInfo);
        pane.addTab(tr("Imagery providers"), imageryProviders);
        pane.addTab(tr("Settings"), buildSettingsPanel());
        pane.addTab(tr("Offset bookmarks"), new OffsetBookmarksPanel(gui));
        pane.addTab(tr("Cache contents"), new CacheContentsPanel());
        loadSettings();
        p.add(pane, GBC.std().fill(GBC.BOTH));
    }

    /**
     * Returns the imagery providers panel.
     * @return The imagery providers panel.
     */
    public ImageryProvidersPanel getProvidersPanel() {
        return imageryProviders;
    }

    private void loadSettings() {
        commonSettings.loadSettings();
        wmsSettings.loadSettings();
        tmsSettings.loadSettings();
    }

    @Override
    public boolean ok() {
        layerInfo.save();
        ImageryLayerInfo.instance.clear();
        ImageryLayerInfo.instance.load(false);
        MainApplication.getMenu().imageryMenu.refreshOffsetMenu();
        OffsetBookmark.saveBookmarks();

        if (!GraphicsEnvironment.isHeadless()) {
            DownloadDialog.getInstance().refreshTileSources();
        }

        boolean commonRestartRequired = commonSettings.saveSettings();
        boolean wmsRestartRequired = wmsSettings.saveSettings();
        boolean tmsRestartRequired = tmsSettings.saveSettings();

        return commonRestartRequired || wmsRestartRequired || tmsRestartRequired;
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
        for (int i = 0; i < imageryProviders.activeModel.getRowCount(); i++) {
            if (server.equals(imageryProviders.activeModel.getValueAt(i, 0).toString())) {
                imageryProviders.activeModel.setValueAt(url, i, 1);
                return;
            }
        }
        imageryProviders.activeModel.addRow(new String[] {server, url});
    }

    /**
     * Gets a server URL in the preferences dialog. Used by plugins.
     *
     * @param server The server name
     * @return The server URL
     */
    public String getServerUrl(String server) {
        for (int i = 0; i < imageryProviders.activeModel.getRowCount(); i++) {
            if (server.equals(imageryProviders.activeModel.getValueAt(i, 0).toString()))
                return imageryProviders.activeModel.getValueAt(i, 1).toString();
        }
        return null;
    }

    /**
     * A panel displaying imagery providers.
     */
    public static class ImageryProvidersPanel extends JPanel {
        // Public JTables and JMapViewer
        /** The table of active providers **/
        public final JTable activeTable;
        /** The table of default providers **/
        public final JTable defaultTable;
        /** The selection listener synchronizing map display with table of default providers **/
        private final transient DefListSelectionListener defaultTableListener;
        /** The map displaying imagery bounds of selected default providers **/
        public final JMapViewer defaultMap;

        // Public models
        /** The model of active providers **/
        public final ImageryLayerTableModel activeModel;
        /** The model of default providers **/
        public final ImageryDefaultLayerTableModel defaultModel;

        // Public JToolbars
        /** The toolbar on the right of active providers **/
        public final JToolBar activeToolbar;
        /** The toolbar on the middle of the panel **/
        public final JToolBar middleToolbar;
        /** The toolbar on the right of default providers **/
        public final JToolBar defaultToolbar;

        // Private members
        private final PreferenceTabbedPane gui;
        private final transient ImageryLayerInfo layerInfo;

        /**
         * class to render the URL information of Imagery source
         * @since 8065
         */
        private static class ImageryURLTableCellRenderer extends DefaultTableCellRenderer {

            private static final NamedColorProperty IMAGERY_BACKGROUND_COLOR = new NamedColorProperty(
                    marktr("Imagery Background: Default"),
                    new Color(200, 255, 200));

            private final transient List<ImageryInfo> layers;

            ImageryURLTableCellRenderer(List<ImageryInfo> layers) {
                this.layers = layers;
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean
                    isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                GuiHelper.setBackgroundReadable(label, UIManager.getColor("Table.background"));
                if (value != null) { // Fix #8159
                    String t = value.toString();
                    for (ImageryInfo l : layers) {
                        if (l.getExtendedUrl().equals(t)) {
                            GuiHelper.setBackgroundReadable(label, IMAGERY_BACKGROUND_COLOR.get());
                            break;
                        }
                    }
                    label.setToolTipText((String) value);
                }
                return label;
            }
        }

        /**
         * class to render the name information of Imagery source
         * @since 8064
         */
        private static class ImageryNameTableCellRenderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean
                    isSelected, boolean hasFocus, int row, int column) {
                ImageryInfo info = (ImageryInfo) value;
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, info == null ? null : info.getName(), isSelected, hasFocus, row, column);
                GuiHelper.setBackgroundReadable(label, UIManager.getColor("Table.background"));
                if (info != null) {
                    label.setToolTipText(info.getToolTipText());
                }
                return label;
            }
        }

        /**
         * Constructs a new {@code ImageryProvidersPanel}.
         * @param gui The parent preference tab pane
         * @param layerInfoArg The list of imagery entries to display
         */
        public ImageryProvidersPanel(final PreferenceTabbedPane gui, ImageryLayerInfo layerInfoArg) {
            super(new GridBagLayout());
            this.gui = gui;
            this.layerInfo = layerInfoArg;
            this.activeModel = new ImageryLayerTableModel();

            activeTable = new JTable(activeModel) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    try {
                        return activeModel.getValueAt(rowAtPoint(p), columnAtPoint(p)).toString();
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Logging.debug(ex);
                        return null;
                    }
                }
            };
            activeTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            defaultModel = new ImageryDefaultLayerTableModel();
            defaultTable = new JTable(defaultModel);

            defaultModel.addTableModelListener(e -> activeTable.repaint());
            activeModel.addTableModelListener(e -> defaultTable.repaint());

            TableColumnModel mod = defaultTable.getColumnModel();
            mod.getColumn(2).setPreferredWidth(800);
            mod.getColumn(2).setCellRenderer(new ImageryURLTableCellRenderer(layerInfo.getLayers()));
            mod.getColumn(1).setPreferredWidth(400);
            mod.getColumn(1).setCellRenderer(new ImageryNameTableCellRenderer());
            mod.getColumn(0).setPreferredWidth(50);

            mod = activeTable.getColumnModel();
            mod.getColumn(1).setPreferredWidth(800);
            mod.getColumn(1).setCellRenderer(new ImageryURLTableCellRenderer(layerInfo.getAllDefaultLayers()));
            mod.getColumn(0).setPreferredWidth(200);

            RemoveEntryAction remove = new RemoveEntryAction();
            activeTable.getSelectionModel().addListSelectionListener(remove);

            add(new JLabel(tr("Available default entries:")), GBC.std().insets(5, 5, 0, 0));
            add(new JLabel(tr("Boundaries of selected imagery entries:")), GBC.eol().insets(5, 5, 0, 0));

            // Add default item list
            JScrollPane scrolldef = new JScrollPane(defaultTable);
            scrolldef.setPreferredSize(new Dimension(200, 200));
            add(scrolldef, GBC.std().insets(0, 5, 0, 0).fill(GridBagConstraints.BOTH).weight(1.0, 0.6).insets(5, 0, 0, 0));

            // Add default item map
            defaultMap = new JMapViewer();
            defaultMap.setTileSource(SlippyMapBBoxChooser.DefaultOsmTileSourceProvider.get()); // for attribution
            defaultMap.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        defaultMap.getAttribution().handleAttribution(e.getPoint(), true);
                    }
                }
            });
            defaultMap.setZoomControlsVisible(false);
            defaultMap.setMinimumSize(new Dimension(100, 200));
            add(defaultMap, GBC.std().insets(5, 5, 0, 0).fill(GridBagConstraints.BOTH).weight(0.33, 0.6).insets(5, 0, 0, 0));

            defaultTableListener = new DefListSelectionListener();
            defaultTable.getSelectionModel().addListSelectionListener(defaultTableListener);

            defaultToolbar = new JToolBar(JToolBar.VERTICAL);
            defaultToolbar.setFloatable(false);
            defaultToolbar.setBorderPainted(false);
            defaultToolbar.setOpaque(false);
            defaultToolbar.add(new ReloadAction());
            add(defaultToolbar, GBC.eol().anchor(GBC.SOUTH).insets(0, 0, 5, 0));

            HtmlPanel help = new HtmlPanel(tr("New default entries can be added in the <a href=\"{0}\">Wiki</a>.",
                Config.getUrls().getJOSMWebsite()+"/wiki/Maps"));
            help.enableClickableHyperlinks();
            add(help, GBC.eol().insets(10, 0, 0, 0).fill(GBC.HORIZONTAL));

            ActivateAction activate = new ActivateAction();
            defaultTable.getSelectionModel().addListSelectionListener(activate);
            JButton btnActivate = new JButton(activate);

            middleToolbar = new JToolBar(JToolBar.HORIZONTAL);
            middleToolbar.setFloatable(false);
            middleToolbar.setBorderPainted(false);
            middleToolbar.setOpaque(false);
            middleToolbar.add(btnActivate);
            add(middleToolbar, GBC.eol().anchor(GBC.CENTER).insets(5, 5, 5, 0));

            add(Box.createHorizontalGlue(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));

            add(new JLabel(tr("Selected entries:")), GBC.eol().insets(5, 0, 0, 0));
            JScrollPane scroll = new JScrollPane(activeTable);
            add(scroll, GBC.std().fill(GridBagConstraints.BOTH).span(GridBagConstraints.RELATIVE).weight(1.0, 0.4).insets(5, 0, 0, 5));
            scroll.setPreferredSize(new Dimension(200, 200));

            activeToolbar = new JToolBar(JToolBar.VERTICAL);
            activeToolbar.setFloatable(false);
            activeToolbar.setBorderPainted(false);
            activeToolbar.setOpaque(false);
            activeToolbar.add(new NewEntryAction(ImageryInfo.ImageryType.WMS));
            activeToolbar.add(new NewEntryAction(ImageryInfo.ImageryType.TMS));
            activeToolbar.add(new NewEntryAction(ImageryInfo.ImageryType.WMTS));
            //activeToolbar.add(edit); TODO
            activeToolbar.add(remove);
            add(activeToolbar, GBC.eol().anchor(GBC.NORTH).insets(0, 0, 5, 5));
        }

        // Listener of default providers list selection
        private final class DefListSelectionListener implements ListSelectionListener {
            // The current drawn rectangles and polygons
            private final Map<Integer, MapRectangle> mapRectangles;
            private final Map<Integer, List<MapPolygon>> mapPolygons;

            private DefListSelectionListener() {
                this.mapRectangles = new HashMap<>();
                this.mapPolygons = new HashMap<>();
            }

            private void clearMap() {
                defaultMap.removeAllMapRectangles();
                defaultMap.removeAllMapPolygons();
                mapRectangles.clear();
                mapPolygons.clear();
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                // First index can be set to -1 when the list is refreshed, so discard all map rectangles and polygons
                if (e.getFirstIndex() == -1) {
                    clearMap();
                } else if (!e.getValueIsAdjusting()) {
                    // Only process complete (final) selection events
                    for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
                        updateBoundsAndShapes(i);
                    }
                    // If needed, adjust map to show all map rectangles and polygons
                    if (!mapRectangles.isEmpty() || !mapPolygons.isEmpty()) {
                        defaultMap.setDisplayToFitMapElements(false, true, true);
                        defaultMap.zoomOut();
                    }
                }
            }

            private void updateBoundsAndShapes(int i) {
                ImageryBounds bounds = defaultModel.getRow(i).getBounds();
                if (bounds != null) {
                    List<Shape> shapes = bounds.getShapes();
                    if (shapes != null && !shapes.isEmpty()) {
                        if (defaultTable.getSelectionModel().isSelectedIndex(i)) {
                            if (!mapPolygons.containsKey(i)) {
                                List<MapPolygon> list = new ArrayList<>();
                                mapPolygons.put(i, list);
                                // Add new map polygons
                                for (Shape shape : shapes) {
                                    MapPolygon polygon = new MapPolygonImpl(shape.getPoints());
                                    list.add(polygon);
                                    defaultMap.addMapPolygon(polygon);
                                }
                            }
                        } else if (mapPolygons.containsKey(i)) {
                            // Remove previously drawn map polygons
                            for (MapPolygon polygon : mapPolygons.get(i)) {
                                defaultMap.removeMapPolygon(polygon);
                            }
                            mapPolygons.remove(i);
                        }
                        // Only display bounds when no polygons (shapes) are defined for this provider
                    } else {
                        if (defaultTable.getSelectionModel().isSelectedIndex(i)) {
                            if (!mapRectangles.containsKey(i)) {
                                // Add new map rectangle
                                Coordinate topLeft = new Coordinate(bounds.getMaxLat(), bounds.getMinLon());
                                Coordinate bottomRight = new Coordinate(bounds.getMinLat(), bounds.getMaxLon());
                                MapRectangle rectangle = new MapRectangleImpl(topLeft, bottomRight);
                                mapRectangles.put(i, rectangle);
                                defaultMap.addMapRectangle(rectangle);
                            }
                        } else if (mapRectangles.containsKey(i)) {
                            // Remove previously drawn map rectangle
                            defaultMap.removeMapRectangle(mapRectangles.get(i));
                            mapRectangles.remove(i);
                        }
                    }
                }
            }
        }

        private class NewEntryAction extends AbstractAction {

            private final ImageryInfo.ImageryType type;

            NewEntryAction(ImageryInfo.ImageryType type) {
                putValue(NAME, type.toString());
                putValue(SHORT_DESCRIPTION, tr("Add a new {0} entry by entering the URL", type.toString()));
                String icon = /* ICON(dialogs/) */ "add";
                switch (type) {
                case WMS:
                    icon = /* ICON(dialogs/) */ "add_wms";
                    break;
                case TMS:
                    icon = /* ICON(dialogs/) */ "add_tms";
                    break;
                case WMTS:
                    icon = /* ICON(dialogs/) */ "add_wmts";
                    break;
                default:
                    break;
                }
                new ImageProvider("dialogs", icon).getResource().attachImageIcon(this, true);
                this.type = type;
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                final AddImageryPanel p;
                switch (type) {
                case WMS:
                    p = new AddWMSLayerPanel();
                    break;
                case TMS:
                    p = new AddTMSLayerPanel();
                    break;
                case WMTS:
                    p = new AddWMTSLayerPanel();
                    break;
                default:
                    throw new IllegalStateException("Type " + type + " not supported");
                }

                final AddImageryDialog addDialog = new AddImageryDialog(gui, p);
                addDialog.showDialog();

                if (addDialog.getValue() == 1) {
                    try {
                        activeModel.addRow(p.getImageryInfo());
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage() == null || ex.getMessage().isEmpty())
                            throw ex;
                        else {
                            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                                    ex.getMessage(), tr("Error"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        }

        private class RemoveEntryAction extends AbstractAction implements ListSelectionListener {

            /**
             * Constructs a new {@code RemoveEntryAction}.
             */
            RemoveEntryAction() {
                putValue(NAME, tr("Remove"));
                putValue(SHORT_DESCRIPTION, tr("Remove entry"));
                new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
                updateEnabledState();
            }

            protected final void updateEnabledState() {
                setEnabled(activeTable.getSelectedRowCount() > 0);
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnabledState();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Integer i;
                while ((i = activeTable.getSelectedRow()) != -1) {
                    activeModel.removeRow(i);
                }
            }
        }

        private class ActivateAction extends AbstractAction implements ListSelectionListener {

            /**
             * Constructs a new {@code ActivateAction}.
             */
            ActivateAction() {
                putValue(NAME, tr("Activate"));
                putValue(SHORT_DESCRIPTION, tr("Copy selected default entries from the list above into the list below."));
                new ImageProvider("preferences", "activate-down").getResource().attachImageIcon(this, true);
            }

            protected void updateEnabledState() {
                setEnabled(defaultTable.getSelectedRowCount() > 0);
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnabledState();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] lines = defaultTable.getSelectedRows();
                if (lines.length == 0) {
                    JOptionPane.showMessageDialog(
                            gui,
                            tr("Please select at least one row to copy."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                Set<String> acceptedEulas = new HashSet<>();

                outer:
                for (int line : lines) {
                    ImageryInfo info = defaultModel.getRow(line);

                    // Check if an entry with exactly the same values already exists
                    for (int j = 0; j < activeModel.getRowCount(); j++) {
                        if (info.equalsBaseValues(activeModel.getRow(j))) {
                            // Select the already existing row so the user has
                            // some feedback in case an entry exists
                            activeTable.getSelectionModel().setSelectionInterval(j, j);
                            activeTable.scrollRectToVisible(activeTable.getCellRect(j, 0, true));
                            continue outer;
                        }
                    }

                    String eulaURL = info.getEulaAcceptanceRequired();
                    // If set and not already accepted, ask for EULA acceptance
                    if (eulaURL != null && !acceptedEulas.contains(eulaURL)) {
                        if (confirmEulaAcceptance(gui, eulaURL)) {
                            acceptedEulas.add(eulaURL);
                        } else {
                            continue outer;
                        }
                    }

                    activeModel.addRow(new ImageryInfo(info));
                    int lastLine = activeModel.getRowCount() - 1;
                    activeTable.getSelectionModel().setSelectionInterval(lastLine, lastLine);
                    activeTable.scrollRectToVisible(activeTable.getCellRect(lastLine, 0, true));
                }
            }
        }

        private class ReloadAction extends AbstractAction {

            /**
             * Constructs a new {@code ReloadAction}.
             */
            ReloadAction() {
                putValue(SHORT_DESCRIPTION, tr("Update default entries"));
                new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this, true);
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                layerInfo.loadDefaults(true, MainApplication.worker, false);
                defaultModel.fireTableDataChanged();
                defaultTable.getSelectionModel().clearSelection();
                defaultTableListener.clearMap();
                /* loading new file may change active layers */
                activeModel.fireTableDataChanged();
            }
        }

        /**
         * The table model for imagery layer list
         */
        public class ImageryLayerTableModel extends DefaultTableModel {
            /**
             * Constructs a new {@code ImageryLayerTableModel}.
             */
            public ImageryLayerTableModel() {
                setColumnIdentifiers(new String[] {tr("Menu Name"), tr("Imagery URL")});
            }

            /**
             * Returns the imagery info at the given row number.
             * @param row The row number
             * @return The imagery info at the given row number
             */
            public ImageryInfo getRow(int row) {
                return layerInfo.getLayers().get(row);
            }

            /**
             * Adds a new imagery info as the last row.
             * @param i The imagery info to add
             */
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
                default:
                    throw new ArrayIndexOutOfBoundsException(Integer.toString(column));
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                if (layerInfo.getLayers().size() <= row) return;
                ImageryInfo info = layerInfo.getLayers().get(row);
                switch (column) {
                case 0:
                    info.setName((String) o);
                    info.clearId();
                    break;
                case 1:
                    info.setExtendedUrl((String) o);
                    info.clearId();
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException(Integer.toString(column));
                }
            }
        }

        /**
         * The table model for the default imagery layer list
         */
        public class ImageryDefaultLayerTableModel extends DefaultTableModel {
            /**
             * Constructs a new {@code ImageryDefaultLayerTableModel}.
             */
            public ImageryDefaultLayerTableModel() {
                setColumnIdentifiers(new String[]{"", tr("Menu Name (Default)"), tr("Imagery URL (Default)")});
            }

            /**
             * Returns the imagery info at the given row number.
             * @param row The row number
             * @return The imagery info at the given row number
             */
            public ImageryInfo getRow(int row) {
                return layerInfo.getAllDefaultLayers().get(row);
            }

            @Override
            public int getRowCount() {
                return layerInfo.getAllDefaultLayers().size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                ImageryInfo info = layerInfo.getAllDefaultLayers().get(row);
                switch (column) {
                case 0:
                    return info.getCountryCode();
                case 1:
                    return info;
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

        private static boolean confirmEulaAcceptance(PreferenceTabbedPane gui, String eulaUrl) {
            URL url;
            try {
                url = new URL(eulaUrl.replaceAll("\\{lang\\}", LanguageInfo.getWikiLanguagePrefix()));
                JosmEditorPane htmlPane;
                try {
                    htmlPane = new JosmEditorPane(url);
                } catch (IOException e1) {
                    Logging.trace(e1);
                    // give a second chance with a default Locale 'en'
                    try {
                        url = new URL(eulaUrl.replaceAll("\\{lang\\}", ""));
                        htmlPane = new JosmEditorPane(url);
                    } catch (IOException e2) {
                        Logging.debug(e2);
                        JOptionPane.showMessageDialog(gui, tr("EULA license URL not available: {0}", eulaUrl));
                        return false;
                    }
                }
                Box box = Box.createVerticalBox();
                htmlPane.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(htmlPane);
                scrollPane.setPreferredSize(new Dimension(400, 400));
                box.add(scrollPane);
                int option = JOptionPane.showConfirmDialog(MainApplication.getMainFrame(), box, tr("Please abort if you are not sure"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.YES_OPTION)
                    return true;
            } catch (MalformedURLException e2) {
                JOptionPane.showMessageDialog(gui, tr("Malformed URL for the EULA licence: {0}", eulaUrl));
            }
            return false;
        }
    }

    static class OffsetBookmarksPanel extends JPanel {
        private final OffsetsBookmarksModel model = new OffsetsBookmarksModel();

        /**
         * Constructs a new {@code OffsetBookmarksPanel}.
         * @param gui the preferences tab pane
         */
        OffsetBookmarksPanel(final PreferenceTabbedPane gui) {
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
            add.addActionListener(e -> model.addRow(new OffsetBookmark(ProjectionRegistry.getProjection().toCode(), "", "", "", 0, 0)));

            JButton delete = new JButton(tr("Delete"));
            buttonPanel.add(delete, GBC.std().insets(0, 5, 0, 0));
            delete.addActionListener(e -> {
                if (list.getSelectedRow() == -1) {
                    JOptionPane.showMessageDialog(gui, tr("Please select the row to delete."));
                } else {
                    Integer i;
                    while ((i = list.getSelectedRow()) != -1) {
                        model.removeRow(i);
                    }
                }
            });

            add(buttonPanel, GBC.eol());
        }

        /**
         * The table model for imagery offsets list
         */
        private static class OffsetsBookmarksModel extends DefaultTableModel {

            /**
             * Constructs a new {@code OffsetsBookmarksModel}.
             */
            OffsetsBookmarksModel() {
                setColumnIdentifiers(new String[] {tr("Projection"), tr("Layer"), tr("Name"), tr("Easting"), tr("Northing")});
            }

            private static OffsetBookmark getRow(int row) {
                return OffsetBookmark.getBookmarkByIndex(row);
            }

            private void addRow(OffsetBookmark i) {
                OffsetBookmark.addBookmark(i);
                int p = getRowCount() - 1;
                fireTableRowsInserted(p, p);
            }

            @Override
            public void removeRow(int i) {
                OffsetBookmark.removeBookmark(getRow(i));
                fireTableRowsDeleted(i, i);
            }

            @Override
            public int getRowCount() {
                return OffsetBookmark.getBookmarksSize();
            }

            @Override
            public Object getValueAt(int row, int column) {
                OffsetBookmark info = OffsetBookmark.getBookmarkByIndex(row);
                switch (column) {
                case 0:
                    if (info.getProjectionCode() == null) return "";
                    return info.getProjectionCode();
                case 1:
                    return info.getImageryName();
                case 2:
                    return info.getName();
                case 3:
                    return info.getDisplacement().east();
                case 4:
                    return info.getDisplacement().north();
                default:
                    throw new ArrayIndexOutOfBoundsException(column);
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                OffsetBookmark info = OffsetBookmark.getBookmarkByIndex(row);
                switch (column) {
                case 1:
                    String name = o.toString();
                    info.setImageryName(name);
                    List<ImageryInfo> layers = ImageryLayerInfo.instance.getLayers().stream()
                            .filter(l -> Objects.equals(name, l.getName())).collect(Collectors.toList());
                    if (layers.size() == 1) {
                        info.setImageryId(layers.get(0).getId());
                    } else {
                        Logging.warn("Not a single layer for the name '" + info.getImageryName() + "': " + layers);
                    }
                    break;
                case 2:
                    info.setName(o.toString());
                    break;
                case 3:
                    double dx = Double.parseDouble((String) o);
                    info.setDisplacement(new EastNorth(dx, info.getDisplacement().north()));
                    break;
                case 4:
                    double dy = Double.parseDouble((String) o);
                    info.setDisplacement(new EastNorth(info.getDisplacement().east(), dy));
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException(column);
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 1;
            }
        }
    }

    /**
     * Initializes imagery preferences.
     */
    public static void initialize() {
        ImageryLayerInfo.instance.load(false);
        OffsetBookmark.loadBookmarks();
        MainApplication.getMenu().imageryMenu.refreshImageryMenu();
        MainApplication.getMenu().imageryMenu.refreshOffsetMenu();
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Imagery");
    }
}
