// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

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
import org.openstreetmap.josm.spi.preferences.Config;
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

    private DefaultTableModel tableModel;
    private JTable colors;
    private final List<String> del = new ArrayList<>();

    private JButton colorEdit;
    private JButton defaultSet;
    private JButton remove;

    /**
     * Set the colors to be shown in the preference table. This method creates a table model if
     * none exists and overwrites all existing values.
     * @param colorMap the map holding the colors
     * (key = color id (without prefixes, so only <code>background</code>; not <code>color.background</code>),
     * value = html representation of the color.
     */
    public void setColorModel(Map<String, String> colorMap) {
        if (tableModel == null) {
            tableModel = new DefaultTableModel();
            tableModel.addColumn(tr("Name"));
            tableModel.addColumn(tr("Color"));
        }

        // clear old model:
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
        // fill model with colors:
        Map<String, String> colorKeyList = new TreeMap<>();
        Map<String, String> colorKeyListMappaint = new TreeMap<>();
        Map<String, String> colorKeyListLayer = new TreeMap<>();
        for (String key : colorMap.keySet()) {
            if (key.startsWith("layer ")) {
                colorKeyListLayer.put(getName(key), key);
            } else if (key.startsWith("mappaint.")) {
                // use getName(key)+key, as getName() may be ambiguous
                colorKeyListMappaint.put(getName(key)+key, key);
            } else {
                colorKeyList.put(getName(key), key);
            }
        }
        addColorRows(colorMap, colorKeyList);
        addColorRows(colorMap, colorKeyListMappaint);
        addColorRows(colorMap, colorKeyListLayer);
        if (this.colors != null) {
            this.colors.repaint();
        }
    }

    private void addColorRows(Map<String, String> colorMap, Map<String, String> keyMap) {
        for (String value : keyMap.values()) {
            Vector<Object> row = new Vector<>(2);
            String html = colorMap.get(value);
            Color color = ColorHelper.html2color(html);
            if (color == null) {
                Logging.warn("Unable to get color from '"+html+"' for color preference '"+value+'\'');
            }
            row.add(value);
            row.add(color);
            tableModel.addRow(row);
        }
    }

    /**
     * Returns a map with the colors in the table (key = color name without prefix, value = html color code).
     * @return a map holding the colors.
     */
    public Map<String, String> getColorModel() {
        String key;
        String value;
        Map<String, String> colorMap = new HashMap<>();
        for (int row = 0; row < tableModel.getRowCount(); ++row) {
            key = (String) tableModel.getValueAt(row, 0);
            value = ColorHelper.color2html((Color) tableModel.getValueAt(row, 1));
            colorMap.put(key, value);
        }
        return colorMap;
    }

    private static String getName(String o) {
        return Main.pref.getColorName("color." + o);
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        fixColorPrefixes();
        setColorModel(Main.pref.getAllColors());

        colorEdit = new JButton(tr("Choose"));
        colorEdit.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            JColorChooser chooser = new JColorChooser((Color) colors.getValueAt(sel, 1));
            int answer = JOptionPane.showConfirmDialog(
                    gui, chooser,
                    tr("Choose a color for {0}", getName((String) colors.getValueAt(sel, 0))),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (answer == JOptionPane.OK_OPTION) {
                colors.setValueAt(chooser.getColor(), sel, 1);
            }
        });
        defaultSet = new JButton(tr("Set to default"));
        defaultSet.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            String name = (String) colors.getValueAt(sel, 0);
            Color c = Main.pref.getDefaultColor(name);
            if (c != null) {
                colors.setValueAt(c, sel, 1);
            }
        });
        JButton defaultAll = new JButton(tr("Set all to default"));
        defaultAll.addActionListener(e -> {
            for (int i = 0; i < colors.getRowCount(); ++i) {
                String name = (String) colors.getValueAt(i, 0);
                Color c = Main.pref.getDefaultColor(name);
                if (c != null) {
                    colors.setValueAt(c, i, 1);
                }
            }
        });
        remove = new JButton(tr("Remove"));
        remove.addActionListener(e -> {
            int sel = colors.getSelectedRow();
            del.add((String) colors.getValueAt(sel, 0));
            tableModel.removeRow(sel);
        });
        remove.setEnabled(false);
        colorEdit.setEnabled(false);
        defaultSet.setEnabled(false);

        colors = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

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
        final TableCellRenderer oldColorsRenderer = colors.getDefaultRenderer(Object.class);
        colors.setDefaultRenderer(Object.class, (t, o, selected, focus, row, column) -> {
            if (o == null)
                return new JLabel();
            if (column == 1) {
                Color c = (Color) o;
                JLabel l = new JLabel(ColorHelper.color2html(c));
                GuiHelper.setBackgroundReadable(l, c);
                l.setOpaque(true);
                return l;
            }
            return oldColorsRenderer.getTableCellRendererComponent(t, getName(o.toString()), selected, focus, row, column);
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
        return ((String) colors.getValueAt(row, 0)).startsWith("layer ");
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
        for (String d : del) {
            Config.getPref().put("color."+d, null);
        }
        for (int i = 0; i < colors.getRowCount(); ++i) {
            String key = (String) colors.getValueAt(i, 0);
            if (Main.pref.putColor(key, (Color) colors.getValueAt(i, 1)) && key.startsWith("mappaint.")) {
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
