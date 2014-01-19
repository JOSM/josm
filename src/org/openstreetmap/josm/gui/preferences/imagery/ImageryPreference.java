// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * Imagery preferences, including imagery providers, settings and offsets.
 */
public final class ImageryPreference extends DefaultTabPreferenceSetting {

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
        super("imagery", tr("Imagery Preferences"), tr("Modify list of imagery layers displayed in the Imagery menu"), false, new JTabbedPane());
    }

    private ImageryProvidersPanel imageryProviders;
    private ImageryLayerInfo layerInfo;

    private CommonSettingsPanel commonSettings;
    private WMSSettingsPanel wmsSettings;
    private TMSSettingsPanel tmsSettings;

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

    private Component buildSettingsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        addSettingsSection(p, tr("Common Settings"), commonSettings = new CommonSettingsPanel());
        addSettingsSection(p, tr("WMS Settings"), wmsSettings = new WMSSettingsPanel(),
                GBC.eol().fill(GBC.HORIZONTAL));
        addSettingsSection(p, tr("TMS Settings"), tmsSettings = new TMSSettingsPanel(),
                GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JPanel(),GBC.eol().fill(GBC.BOTH));
        return new JScrollPane(p);
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
        loadSettings();
        p.add(pane,GBC.std().fill(GBC.BOTH));
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
        ImageryLayerInfo.instance.load();
        Main.main.menu.imageryMenu.refreshOffsetMenu();
        OffsetBookmark.saveBookmarks();
        
        DownloadDialog.getInstance().refreshTileSources();

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
        imageryProviders.activeModel.addRow(new String[] { server, url });
    }

    /**
     * Gets a server URL in the preferences dialog. Used by plugins.
     *
     * @param server
     *            The server name
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
        private final ImageryLayerInfo layerInfo;

        private static class ImageryTableCellRenderer extends DefaultTableCellRenderer {

            private List<ImageryInfo> layers;

            public ImageryTableCellRenderer(List<ImageryInfo> layers) {
                this.layers = layers;
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean
                    isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setBackground(Main.pref.getUIColor("Table.background"));
                if (isSelected) {
                    label.setForeground(Main.pref.getUIColor("Table.foreground"));
                }
                if (value != null) { // Fix #8159
                    String t = value.toString();
                    for (ImageryInfo l : layers) {
                        if (l.getExtendedUrl().equals(t)) {
                            label.setBackground(Main.pref.getColor(
                                    marktr("Imagery Background: Default"),
                                    new Color(200,255,200)));
                            break;
                        }
                    }
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
                    return activeModel.getValueAt(rowAtPoint(p), columnAtPoint(p)).toString();
                }
            };

            defaultModel = new ImageryDefaultLayerTableModel();
            defaultTable = new JTable(defaultModel) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return (String) defaultModel.getValueAt(rowAtPoint(p), columnAtPoint(p));
                }
            };

            defaultModel.addTableModelListener(
                    new TableModelListener() {
                        @Override
                        public void tableChanged(TableModelEvent e) {
                            activeTable.repaint();
                        }
                    }
                    );

            activeModel.addTableModelListener(
                    new TableModelListener() {
                        @Override
                        public void tableChanged(TableModelEvent e) {
                            defaultTable.repaint();
                        }
                    }
                    );

            TableColumnModel mod = defaultTable.getColumnModel();
            mod.getColumn(2).setPreferredWidth(800);
            mod.getColumn(2).setCellRenderer(new ImageryTableCellRenderer(layerInfo.getLayers()));
            mod.getColumn(1).setPreferredWidth(400);
            mod.getColumn(0).setPreferredWidth(50);

            mod = activeTable.getColumnModel();
            mod.getColumn(1).setPreferredWidth(800);
            mod.getColumn(1).setCellRenderer(new ImageryTableCellRenderer(layerInfo.getDefaultLayers()));
            mod.getColumn(0).setPreferredWidth(200);

            RemoveEntryAction remove = new RemoveEntryAction();
            activeTable.getSelectionModel().addListSelectionListener(remove);

            add(new JLabel(tr("Available default entries:")), GBC.eol().insets(5, 5, 0, 0));
            // Add default item list
            JScrollPane scrolldef = new JScrollPane(defaultTable);
            scrolldef.setPreferredSize(new Dimension(200, 200));
            add(scrolldef, GBC.std().insets(0, 5, 0, 0).fill(GridBagConstraints.BOTH).weight(1.0, 0.6).insets(5, 0, 0, 0));

            // Add default item map
            defaultMap = new JMapViewer();
            defaultMap.setZoomContolsVisible(false);
            defaultMap.setMinimumSize(new Dimension(100, 200));
            add(defaultMap, GBC.std().insets(5, 5, 0, 0).fill(GridBagConstraints.BOTH).weight(0.33, 0.6).insets(5, 0, 0, 0));

            defaultTable.getSelectionModel().addListSelectionListener(new DefListSelectionListener());

            defaultToolbar = new JToolBar(JToolBar.VERTICAL);
            defaultToolbar.setFloatable(false);
            defaultToolbar.setBorderPainted(false);
            defaultToolbar.setOpaque(false);
            defaultToolbar.add(new ReloadAction());
            add(defaultToolbar, GBC.eol().anchor(GBC.SOUTH).insets(0, 0, 5, 0));

            ActivateAction activate = new ActivateAction();
            defaultTable.getSelectionModel().addListSelectionListener(activate);
            JButton btnActivate = new JButton(activate);

            middleToolbar = new JToolBar(JToolBar.HORIZONTAL);
            middleToolbar.setFloatable(false);
            middleToolbar.setBorderPainted(false);
            middleToolbar.setOpaque(false);
            middleToolbar.add(btnActivate);
            add(middleToolbar, GBC.eol().anchor(GBC.CENTER).insets(5, 15, 5, 0));

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
                this.mapRectangles = new HashMap<Integer, MapRectangle>();
                this.mapPolygons = new HashMap<Integer, List<MapPolygon>>();
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                // First index is set to -1 when the list is refreshed, so discard all map rectangles and polygons
                if (e.getFirstIndex() == -1) {
                    defaultMap.removeAllMapRectangles();
                    defaultMap.removeAllMapPolygons();
                    mapRectangles.clear();
                    mapPolygons.clear();
                    // Only process complete (final) selection events
                } else if (!e.getValueIsAdjusting()) {
                    for (int i = e.getFirstIndex(); i<=e.getLastIndex(); i++) {
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
                                List<MapPolygon> list = new ArrayList<MapPolygon>();
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

            public NewEntryAction(ImageryInfo.ImageryType type) {
                putValue(NAME, type.toString());
                putValue(SHORT_DESCRIPTION, tr("Add a new {0} entry by entering the URL", type.toString()));
                putValue(SMALL_ICON, ImageProvider.get("dialogs",
                            "add" + (ImageryInfo.ImageryType.WMS.equals(type) ? "_wms" : ImageryInfo.ImageryType.TMS.equals(type) ? "_tms" : "")));
                this.type = type;
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                final AddImageryPanel p;
                if (ImageryInfo.ImageryType.WMS.equals(type)) {
                    p = new AddWMSLayerPanel();
                } else if (ImageryInfo.ImageryType.TMS.equals(type)) {
                    p = new AddTMSLayerPanel();
                } else {
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
                            JOptionPane.showMessageDialog(Main.parent,
                                    ex.getMessage(), tr("Error"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        }

        private class RemoveEntryAction extends AbstractAction implements ListSelectionListener {

            public RemoveEntryAction() {
                putValue(NAME, tr("Remove"));
                putValue(SHORT_DESCRIPTION, tr("Remove entry"));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
                updateEnabledState();
            }

            protected void updateEnabledState() {
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
            public ActivateAction() {
                putValue(NAME, tr("Activate"));
                putValue(SHORT_DESCRIPTION, tr("copy selected defaults"));
                putValue(SMALL_ICON, ImageProvider.get("preferences", "activate-down"));
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

                Set<String> acceptedEulas = new HashSet<String>();

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
            public ReloadAction() {
                putValue(SHORT_DESCRIPTION, tr("reload defaults"));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                layerInfo.loadDefaults(true);
                defaultModel.fireTableDataChanged();
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
                setColumnIdentifiers(new String[] { tr("Menu Name"), tr("Imagery URL")});
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
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                if (layerInfo.getLayers().size() <= row) return;
                ImageryInfo info = layerInfo.getLayers().get(row);
                switch (column) {
                case 0:
                    info.setName((String) o);
                    break;
                case 1:
                    info.setExtendedUrl((String)o);
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
                url = new URL(eulaUrl.replaceAll("\\{lang\\}", LanguageInfo.getWikiLanguagePrefix()));
                JosmEditorPane htmlPane = null;
                try {
                    htmlPane = new JosmEditorPane(url);
                } catch (IOException e1) {
                    // give a second chance with a default Locale 'en'
                    try {
                        url = new URL(eulaUrl.replaceAll("\\{lang\\}", ""));
                        htmlPane = new JosmEditorPane(url);
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
                    OffsetBookmark b = new OffsetBookmark(Main.getProjection().toCode(),"","",0,0);
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
                    if (info.projectionCode == null) return "";
                    return info.projectionCode.toString();
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

    /**
     * Initializes imagery preferences.
     */
    public static void initialize() {
        ImageryLayerInfo.instance.clear();
        ImageryLayerInfo.instance.loadDefaults(false);
        ImageryLayerInfo.instance.load();
        OffsetBookmark.loadBookmarks();
        Main.main.menu.imageryMenu.refreshImageryMenu();
        Main.main.menu.imageryMenu.refreshOffsetMenu();
    }
}
