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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
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
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Color preferences.
 */
public class ColorPreference implements SubPreferenceSetting {

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
        Color color;

        public ColorEntry(String key, String colorHtml) {
            CheckParameterUtil.ensureParameterNotNull(key, "key");
            this.key = key;
            this.color = ColorHelper.html2color(colorHtml);
            if (this.color == null) {
                Logging.warn("Unable to get color from '"+colorHtml+"' for color preference '"+key+'\'');
            }
        }

        public String getDisplay() {
            return Main.pref.getColorName(key);
        }
    }

    private static class ColorTableModel extends AbstractTableModel {

        private final List<ColorEntry> data;
        private final List<ColorEntry> deleted;

        public ColorTableModel() {
            this.data = new ArrayList<>();
            this.deleted = new ArrayList<>();
        }

        public void addEntry(ColorEntry entry) {
            data.add(entry);
        }

        public void removeEntry(int row) {
            deleted.add(data.get(row));
            data.remove(row);
            fireTableDataChanged();
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
            return columnIndex == 0 ? data.get(rowIndex) : data.get(rowIndex).color;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? tr("Name") : tr("Color");
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1 && aValue instanceof Color) {
                data.get(rowIndex).color = (Color) aValue;
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }
    }

    /**
     * Set the colors to be shown in the preference table. This method creates a table model if
     * none exists and overwrites all existing values.
     * @param colorMap the map holding the colors
     * (key = color id (without prefixes, so only <code>background</code>; not <code>color.background</code>),
     * value = html representation of the color.
     */
    public void setColorModel(Map<String, String> colorMap) {
        if (tableModel == null) {
            tableModel = new ColorTableModel();
        }

        tableModel.clear();
        // fill model with colors:
        List<ColorEntry> colorKeyList = new ArrayList<>();
        List<ColorEntry> colorKeyListMappaint = new ArrayList<>();
        List<ColorEntry> colorKeyListLayer = new ArrayList<>();
        for (Map.Entry<String, String> e : colorMap.entrySet()) {
            String key = e.getKey();
            String html = e.getValue();
            if (key.startsWith("layer.")) {
                colorKeyListLayer.add(new ColorEntry(key, html));
            } else if (key.startsWith("mappaint.")) {
                colorKeyListMappaint.add(new ColorEntry(key, html));
            } else {
                colorKeyList.add(new ColorEntry(key, html));
            }
        }
        addColorRows(colorKeyList);
        addColorRows(colorKeyListMappaint);
        addColorRows(colorKeyListLayer);
        if (this.colors != null) {
            this.colors.repaint();
        }
    }

    private void addColorRows(List<ColorEntry> entries) {
        Collections.sort(entries, (e1, e2) -> Collator.getInstance().compare(e1.getDisplay(), e2.getDisplay()));
        entries.forEach(tableModel::addEntry);
    }

    /**
     * Returns a map with the colors in the table (key = color name without prefix, value = html color code).
     * @return a map holding the colors.
     */
    public Map<String, String> getColorModel() {
        Map<String, String> colorMap = new HashMap<>();
        for (ColorEntry e : tableModel.getData()) {
            colorMap.put(e.key, ColorHelper.color2html(e.color));
        }
        return colorMap;
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        fixColorPrefixes();
        setColorModel(Main.pref.getAllColors());

        colorEdit = new JButton(tr("Choose"));
        colorEdit.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            ColorEntry ce = tableModel.getEntry(sel);
            JColorChooser chooser = new JColorChooser(ce.color);
            int answer = JOptionPane.showConfirmDialog(
                    gui, chooser,
                    tr("Choose a color for {0}", ce.getDisplay()),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (answer == JOptionPane.OK_OPTION) {
                colors.setValueAt(chooser.getColor(), sel, 1);
            }
        });
        defaultSet = new JButton(tr("Set to default"));
        defaultSet.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            ColorEntry ce = tableModel.getEntry(sel);
            Color c = Main.pref.getDefaultColor(ce.key);
            if (c != null) {
                colors.setValueAt(c, sel, 1);
            }
        });
        JButton defaultAll = new JButton(tr("Set all to default"));
        defaultAll.addActionListener(e -> {
            List<ColorEntry> data = tableModel.getData();
            for (int i = 0; i < data.size(); ++i) {
                ColorEntry ce = data.get(i);
                Color c = Main.pref.getDefaultColor(ce.key);
                if (c != null) {
                    colors.setValueAt(c, i, 1);
                }
            }
        });
        remove = new JButton(tr("Remove"));
        remove.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            tableModel.removeEntry(sel);
        });
        remove.setEnabled(false);
        colorEdit.setEnabled(false);
        defaultSet.setEnabled(false);

        colors = new JTable(tableModel) {
            @Override public void valueChanged(ListSelectionEvent e) {
                super.valueChanged(e);
                int sel = getSelectedRow();
                remove.setEnabled(sel >= 0 && isRemoveColor(sel));
                colorEdit.setEnabled(sel >= 0);
                defaultSet.setEnabled(sel >= 0);
            }
        };
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
                    if (!Objects.equals(e.color, Main.pref.getDefaultColor(e.key))) {
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

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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

    Boolean isRemoveColor(int row) {
        return tableModel.getEntry(row).key.startsWith("layer.");
    }

    /**
     * Add all missing color entries.
     */
    private static void fixColorPrefixes() {
        PaintColors.values();
        ConflictColors.getColors();
        Severity.getColors();
        MarkerLayer.getGenericColor();
        GpxDrawHelper.getGenericColor();
        OsmDataLayer.getOutsideColor();
        MapScaler.getColor();
        MapStatus.getColors();
        ConflictDialog.getColor();
    }

    @Override
    public boolean ok() {
        boolean ret = false;
        for (ColorEntry d : tableModel.getDeleted()) {
            Main.pref.putColor(d.key, null);
        }
        for (ColorEntry e : tableModel.getData()) {
            if (Main.pref.putColor(e.key, e.color) && e.key.startsWith("mappaint.")) {
                ret = true;
            }
        }
        OsmDataLayer.createHatchTexture();
        return ret;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getDisplayPreference();
    }
}
