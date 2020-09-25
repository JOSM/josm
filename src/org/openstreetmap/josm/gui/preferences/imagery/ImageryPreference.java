// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
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
    private final CacheSettingsPanel cacheSettingsPanel = new CacheSettingsPanel();

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
        super(/* ICON(preferences/) */ "imagery", tr("Imagery preferences"),
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
        pane.addTab(tr("Cache"), cacheSettingsPanel);
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
        cacheSettingsPanel.loadSettings();
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
        boolean cacheRestartRequired = cacheSettingsPanel.saveSettings();

        return commonRestartRequired || wmsRestartRequired || tmsRestartRequired || cacheRestartRequired;
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
            TableHelper.setFont(list, getClass());
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

            JButton add = new JButton(tr("Add"), ImageProvider.get("dialogs/add", ImageProvider.ImageSizes.SMALLICON));
            buttonPanel.add(add, GBC.std().insets(0, 5, 0, 0));
            add.addActionListener(e -> model.addRow(new OffsetBookmark(ProjectionRegistry.getProjection().toCode(), "", "", "", 0, 0)));

            JButton delete = new JButton(tr("Delete"), ImageProvider.get("dialogs/delete", ImageProvider.ImageSizes.SMALLICON));
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
