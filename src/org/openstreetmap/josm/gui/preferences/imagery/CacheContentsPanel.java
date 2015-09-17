// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.engine.stats.behavior.ICacheStats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Panel for cache content management.
 *
 * @author Wiktor Niesiobędzki
 *
 */
public class CacheContentsPanel extends JPanel {

    /**
     *
     * Class based on:  http://www.camick.com/java/source/ButtonColumn.java
     * https://tips4java.wordpress.com/2009/07/12/table-button-column/
     *
     */
    private static final class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
        private final Action action;
        private final JButton renderButton;
        private JButton editButton;
        private Object editorValue;

        private ButtonColumn(Action action) {
            this.action = action;
            renderButton = new JButton();
            editButton = new JButton();
            editButton.setFocusPainted(false);
            editButton.addActionListener(this);
            editButton.setBorder(new LineBorder(Color.BLUE));
        }

        @Override
        public Object getCellEditorValue() {
            return editorValue;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.action.actionPerformed(e);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.editorValue = value;
            if (value == null) {
                editButton.setText("");
                editButton.setIcon(null);
            } else if (value instanceof Icon) {
                editButton.setText("");
                editButton.setIcon((Icon) value);
            } else {
                editButton.setText(value.toString());
                editButton.setIcon(null);
            }
            this.editorValue = value;
            return editButton;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            if (isSelected) {
                renderButton.setForeground(table.getSelectionForeground());
                renderButton.setBackground(table.getSelectionBackground());
            } else {
                renderButton.setForeground(table.getForeground());
                renderButton.setBackground(UIManager.getColor("Button.background"));
            }

            renderButton.setFocusPainted(hasFocus);

            if (value == null) {
                renderButton.setText("");
                renderButton.setIcon(null);
            } else if (value instanceof Icon) {
                renderButton.setText("");
                renderButton.setIcon((Icon) value);
            } else {
                renderButton.setText(value.toString());
                renderButton.setIcon(null);
            }
            return renderButton;
        }

    }

    private final transient ExecutorService executor =
            Executors.newSingleThreadExecutor(Utils.newThreadFactory(getClass() + "-%d", Thread.NORM_PRIORITY));

    /**
     * Creates cache content panel
     */
    public CacheContentsPanel() {
        super(new GridBagLayout());
        executor.submit(new Runnable() {
            @Override
            public void run() {
                addToPanel(TMSLayer.getCache(), "TMS");
                addToPanel(WMSLayer.getCache(), "WMS");
                addToPanel(WMTSLayer.getCache(), "WMTS");
            }
        });
        executor.shutdown();
    }

    private void addToPanel(final CacheAccess<String, BufferedImageCacheEntry> cache, final String name) {
        final Long cacheSize = getCacheSize(cache);
        final TableModel tableModel = getTableModel(cache);

        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                add(
                        new JLabel(tr("{0} cache, total cache size: {1} bytes", name, cacheSize)),
                        GBC.eol().insets(5, 5, 0, 0));

                add(
                        new JScrollPane(getTableForCache(cache, tableModel)),
                        GBC.eol().fill(GBC.BOTH));
            }
        });
    }

    private Long getCacheSize(CacheAccess<String, BufferedImageCacheEntry> cache) {
        ICacheStats stats = cache.getStatistics();
        for (IStats cacheStats: stats.getAuxiliaryCacheStats()) {
            for (IStatElement<?> statElement: cacheStats.getStatElements()) {
                if ("Data File Length".equals(statElement.getName())) {
                    Object val = statElement.getData();
                    if (val instanceof Long) {
                        return (Long) val;
                    }

                }
            }
        }
        return Long.valueOf(-1);
    }

    private static String[][] getCacheStats(CacheAccess<String, BufferedImageCacheEntry> cache) {
        Set<String> keySet = cache.getCacheControl().getKeySet();
        Map<String, int[]> temp = new ConcurrentHashMap<>(); // use int[] as a Object reference to int, gives better performance
        for (String key: keySet) {
            String[] keyParts = key.split(":", 2);
            if (keyParts.length == 2) {
                int[] counter = temp.get(keyParts[0]);
                if (counter == null) {
                    temp.put(keyParts[0], new int[]{1});
                } else {
                    counter[0]++;
                }
            } else {
                Main.warn("Could not parse the key: {0}. No colon found", key);
            }
        }

        List<Pair<String, Integer>> sortedStats = new ArrayList<>();
        for (Entry<String, int[]> e: temp.entrySet()) {
            sortedStats.add(new Pair<>(e.getKey(), e.getValue()[0]));
        }
        Collections.sort(sortedStats, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                return -1 * o1.b.compareTo(o2.b);
            }
        });
        String[][] ret = new String[sortedStats.size()][3];
        int index = 0;
        for (Pair<String, Integer> e: sortedStats) {
            ret[index] = new String[]{e.a, e.b.toString(), tr("Clear")};
            index++;
        }
        return ret;
    }

    private JTable getTableForCache(final CacheAccess<String, BufferedImageCacheEntry> cache, final TableModel tableModel) {
        final JTable ret = new JTable(tableModel);

        ButtonColumn buttonColumn = new ButtonColumn(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = ret.convertRowIndexToModel(ret.getEditingRow());
                        tableModel.setValueAt("0", row, 1);
                        cache.remove(ret.getValueAt(row, 0) + ":");
                    }
                });
        TableColumn tableColumn = ret.getColumnModel().getColumn(2);
        tableColumn.setCellRenderer(buttonColumn);
        tableColumn.setCellEditor(buttonColumn);
        return ret;
    }

    private DefaultTableModel getTableModel(final CacheAccess<String, BufferedImageCacheEntry> cache) {
        final DefaultTableModel tableModel = new DefaultTableModel(
                getCacheStats(cache),
                new String[]{"Cache name", "Object Count", "Clear"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        return tableModel;
    }
}
