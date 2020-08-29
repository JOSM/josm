// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.preferences.ColorInfo;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.gui.MapScaler;
import org.openstreetmap.josm.gui.MapStatus;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.advanced.PreferencesTable;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.FilterField;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

/**
 * Color preferences.
 *
 * GUI preference to let the user customize named colors.
 * @see NamedColorProperty
 */
public class ColorPreference implements SubPreferenceSetting, ListSelectionListener, TableModelListener {

    /**
     * Factory used to create a new {@code ColorPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ColorPreference();
        }
    }

    private ColorTableModel tableModel;
    private JTable colors;

    private JButton colorEdit;
    private JButton defaultSet;
    private JButton remove;

    private static class ColorEntry {
        String key;
        ColorInfo info;

        ColorEntry(String key, ColorInfo info) {
            CheckParameterUtil.ensureParameterNotNull(key, "key");
            CheckParameterUtil.ensureParameterNotNull(info, "info");
            this.key = key;
            this.info = info;
        }

        /**
         * Get a description of the color based on the given info.
         * @return a description of the color
         */
        public String getDisplay() {
            switch (info.getCategory()) {
                case NamedColorProperty.COLOR_CATEGORY_MAPPAINT:
                    if (info.getSource() != null)
                        return tr("Paint style {0}: {1}", tr(I18n.escape(info.getSource())), tr(info.getName()));
                    // fall through
                default:
                    if (info.getSource() != null)
                        return tr(I18n.escape(info.getSource())) + " - " + tr(I18n.escape(info.getName()));
                    else
                        return tr(I18n.escape(info.getName()));
            }
        }

        /**
         * Get the color value to display.
         * Either value (if set) or default value.
         * @return the color value to display
         */
        public Color getDisplayColor() {
            return Optional.ofNullable(info.getValue()).orElse(info.getDefaultValue());
        }

        /**
         * Check if color has been customized by the user or not.
         * @return true if the color is at its default value, false if it is customized by the user.
         */
        public boolean isDefault() {
            return info.getValue() == null || Objects.equals(info.getValue(), info.getDefaultValue());
        }

        /**
         * Convert to a {@link NamedColorProperty}.
         * @return a {@link NamedColorProperty}
         */
        public NamedColorProperty toProperty() {
            return new NamedColorProperty(info.getCategory(), info.getSource(),
                    info.getName(), info.getDefaultValue());
        }

        @Override
        public String toString() {
            return "ColorEntry{" + getDisplay() + ' ' + ColorHelper.color2html(getDisplayColor()) + '}';
        }
    }

    private static class ColorTableModel extends AbstractTableModel {

        private final List<ColorEntry> data;
        private final List<ColorEntry> deleted;

        ColorTableModel() {
            this.data = new ArrayList<>();
            this.deleted = new ArrayList<>();
        }

        public void addEntry(ColorEntry entry) {
            data.add(entry);
        }

        public void removeEntry(int row) {
            deleted.add(data.get(row));
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public ColorEntry getEntry(int row) {
            return data.get(row);
        }

        public List<ColorEntry> getData() {
            return data;
        }

        public List<ColorEntry> getDeleted() {
            return deleted;
        }

        public void clear() {
            data.clear();
            deleted.clear();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columnIndex == 0 ? data.get(rowIndex) : data.get(rowIndex).getDisplayColor();
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? tr("Name") : tr("Color");
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1 && aValue instanceof Color) {
                data.get(rowIndex).info.setValue((Color) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    /**
     * Set the colors to be shown in the preference table. This method creates a table model if
     * none exists and overwrites all existing values.
     * @param colorMap the map holding the colors
     * (key = preference key, value = {@link ColorInfo} instance)
     */
    public void setColors(Map<String, ColorInfo> colorMap) {
        if (tableModel == null) {
            tableModel = new ColorTableModel();
        }
        tableModel.clear();

        // fill model with colors:
        colorMap.entrySet().stream()
                .map(e -> new ColorEntry(e.getKey(), e.getValue()))
                .sorted((e1, e2) -> {
                    int cat = Integer.compare(
                            getCategoryPriority(e1.info.getCategory()),
                            getCategoryPriority(e2.info.getCategory()));
                    if (cat != 0) return cat;
                    return Collator.getInstance().compare(e1.getDisplay(), e2.getDisplay());
                })
                .forEach(tableModel::addEntry);

        if (this.colors != null) {
            this.colors.repaint();
        }
    }

    private static int getCategoryPriority(String category) {
        switch (category) {
            case NamedColorProperty.COLOR_CATEGORY_GENERAL: return 1;
            case NamedColorProperty.COLOR_CATEGORY_MAPPAINT: return 2;
            default: return 3;
        }
    }

    /**
     * Returns a map with the colors in the table (key = preference key, value = color info).
     * @return a map holding the colors.
     */
    public Map<String, ColorInfo> getColors() {
        return tableModel.getData().stream().collect(Collectors.toMap(e -> e.key, e -> e.info));
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        fixColorPrefixes();
        setColors(Preferences.main().getAllNamedColors());

        colorEdit = new JButton(tr("Choose"));
        colorEdit.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            ColorEntry ce = (ColorEntry) colors.getValueAt(sel, 0);
            JColorChooser chooser = new JColorChooser(ce.getDisplayColor());
            int answer = JOptionPane.showConfirmDialog(
                    gui, chooser,
                    tr("Choose a color for {0}", ce.getDisplay()),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (answer == JOptionPane.OK_OPTION) {
                colors.setValueAt(chooser.getColor(), sel, 1);
            }
        });
        defaultSet = new JButton(tr("Reset to default"));
        defaultSet.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            ColorEntry ce = (ColorEntry) colors.getValueAt(sel, 0);
            Color c = ce.info.getDefaultValue();
            if (c != null) {
                colors.setValueAt(c, sel, 1);
            }
        });
        JButton defaultAll = new JButton(tr("Set all to default"));
        defaultAll.addActionListener(e -> {
            List<ColorEntry> data = tableModel.getData();
            for (ColorEntry ce : data) {
                Color c = ce.info.getDefaultValue();
                if (c != null) {
                    ce.info.setValue(c);
                }
            }
            tableModel.fireTableDataChanged();
        });
        remove = new JButton(tr("Remove"));
        remove.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            sel = colors.convertRowIndexToModel(sel);
            tableModel.removeEntry(sel);
        });
        remove.setEnabled(false);
        colorEdit.setEnabled(false);
        defaultSet.setEnabled(false);

        colors = new JTable(tableModel);
        TableHelper.setFont(colors, PreferencesTable.class);
        colors.setAutoCreateRowSorter(true);
        FilterField colorFilter = new FilterField().filter(colors, tableModel);
        colors.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    colorEdit.doClick();
                }
            }
        });
        colors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        colors.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null && comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    ColorEntry e = (ColorEntry) value;
                    label.setText(e.getDisplay());
                    if (!e.isDefault()) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else {
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                    }
                    return label;
                }
                return comp;
            }
        });
        colors.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null && comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    Color c = (Color) value;
                    label.setText(ColorHelper.color2html(c));
                    GuiHelper.setBackgroundReadable(label, c);
                    label.setOpaque(true);
                    return label;
                }
                return comp;
            }
        });
        colors.getColumnModel().getColumn(1).setWidth(100);
        colors.setToolTipText(tr("Colors used by different objects in JOSM."));
        colors.setPreferredScrollableViewportSize(new Dimension(100, 112));

        colors.getSelectionModel().addListSelectionListener(this);
        colors.getModel().addTableModelListener(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(colorFilter, GBC.eol().fill(GBC.HORIZONTAL));
        JScrollPane scrollpane = new JScrollPane(colors);
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.add(scrollpane, GBC.eol().fill(GBC.BOTH));
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        panel.add(buttonPanel, GBC.eol().insets(5, 0, 5, 5).fill(GBC.HORIZONTAL));
        buttonPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
        buttonPanel.add(colorEdit, GBC.std().insets(0, 5, 0, 0));
        buttonPanel.add(defaultSet, GBC.std().insets(5, 5, 5, 0));
        buttonPanel.add(defaultAll, GBC.std().insets(0, 5, 0, 0));
        buttonPanel.add(remove, GBC.std().insets(0, 5, 0, 0));
        gui.getDisplayPreference().addSubTab(this, tr("Colors"), panel);
    }

    @SuppressWarnings({"PMD.UnusedFormalParameter", "UnusedVariable"})
    private static boolean isRemoveColor(ColorEntry ce) {
        return false;
        //COLOR_CATEGORY_LAYER is no longer supported and was the only one that could be removed.
        //Maybe this is useful for other categories in the future.
        //return NamedColorProperty.COLOR_CATEGORY_LAYER.equals(ce.info.getCategory());
    }

    /**
     * Add all missing color entries.
     */
    private static void fixColorPrefixes() {
        PaintColors.values();
        ConflictColors.getColors();
        Severity.getColors();
        MarkerLayer.DEFAULT_COLOR_PROPERTY.get();
        GpxDrawHelper.DEFAULT_COLOR_PROPERTY.get();
        OsmDataLayer.getOutsideColor();
        MapScaler.getColor();
        MapStatus.getColors();
        ConflictDialog.getColor();
    }

    @Override
    public boolean ok() {
        for (ColorEntry d : tableModel.getDeleted()) {
            d.toProperty().remove();
        }
        for (ColorEntry e : tableModel.getData()) {
            if (e.info.getValue() != null) {
                e.toProperty().put(e.info.getValue());
            }
        }
        OsmDataLayer.createHatchTexture();
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getDisplayPreference();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateEnabledState();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateEnabledState();
    }

    private void updateEnabledState() {
        int sel = colors.getSelectedRow();
        if (sel < 0 || sel >= colors.getRowCount()) {
            return;
        }
        ColorEntry ce = (ColorEntry) colors.getValueAt(sel, 0);
        remove.setEnabled(ce != null && isRemoveColor(ce));
        colorEdit.setEnabled(ce != null);
        defaultSet.setEnabled(ce != null && !ce.isDefault());
    }
}
