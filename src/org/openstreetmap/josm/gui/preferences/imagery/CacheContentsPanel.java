// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.engine.stats.behavior.ICacheStats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.ButtonColumn;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * Panel for cache content management.
 *
 * @author Wiktor Niesiobędzki
 *
 */
public class CacheContentsPanel extends JPanel {

    /**
     * Creates cache content panel
     */
    public CacheContentsPanel() {
        super(new GridBagLayout());
        MainApplication.worker.submit(() -> {
            addToPanel(TMSLayer.getCache(), "TMS");
            addToPanel(WMSLayer.getCache(), "WMS");
            addToPanel(WMTSLayer.getCache(), "WMTS");
        });
    }

    private void addToPanel(final CacheAccess<String, BufferedImageCacheEntry> cache, final String name) {
        final Long cacheSize = getCacheSize(cache);
        final TableModel tableModel = getTableModel(cache);

        GuiHelper.runInEDT(() -> {
            add(new JLabel(tr("{0} cache, total cache size: {1} bytes", name, cacheSize)),
                GBC.eol().insets(5, 5, 0, 0));
            add(new JScrollPane(getTableForCache(cache, tableModel)),
                GBC.eol().fill(GBC.BOTH));
        });
    }

    private static Long getCacheSize(CacheAccess<String, BufferedImageCacheEntry> cache) {
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

    /**
     * Returns the cache stats.
     * @param cache imagery cache
     * @return the cache stats
     */
    public static String[][] getCacheStats(CacheAccess<String, BufferedImageCacheEntry> cache) {
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
                Logging.warn("Could not parse the key: {0}. No colon found", key);
            }
        }

        List<Pair<String, Integer>> sortedStats = new ArrayList<>();
        for (Entry<String, int[]> e: temp.entrySet()) {
            sortedStats.add(new Pair<>(e.getKey(), e.getValue()[0]));
        }
        sortedStats.sort(Comparator.comparing(o -> o.b, Comparator.reverseOrder()));
        String[][] ret = new String[sortedStats.size()][3];
        int index = 0;
        for (Pair<String, Integer> e: sortedStats) {
            ret[index] = new String[]{e.a, e.b.toString(), tr("Clear")};
            index++;
        }
        return ret;
    }

    private static JTable getTableForCache(final CacheAccess<String, BufferedImageCacheEntry> cache, final TableModel tableModel) {
        final JTable ret = new JTable(tableModel);

        ButtonColumn buttonColumn = new ButtonColumn(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = ret.convertRowIndexToModel(ret.getEditingRow());
                        tableModel.setValueAt("0", row, 1);
                        cache.remove(ret.getValueAt(row, 0).toString() + ':');
                    }
                });
        TableColumn tableColumn = ret.getColumnModel().getColumn(2);
        tableColumn.setCellRenderer(buttonColumn);
        tableColumn.setCellEditor(buttonColumn);
        return ret;
    }

    private static DefaultTableModel getTableModel(final CacheAccess<String, BufferedImageCacheEntry> cache) {
        return new DefaultTableModel(
                getCacheStats(cache),
                new String[]{tr("Cache name"), tr("Object Count"), tr("Clear")}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
    }
}
