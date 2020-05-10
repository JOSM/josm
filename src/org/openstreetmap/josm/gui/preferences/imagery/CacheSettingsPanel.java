// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.jcs3.access.CacheAccess;
import org.apache.commons.jcs3.engine.stats.behavior.ICacheStats;
import org.apache.commons.jcs3.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs3.engine.stats.behavior.IStats;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.imagery.CachedTileLoaderFactory;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractCachedTileSourceLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.ButtonColumn;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Panel for cache size, location and content management.
 *
 * @author Wiktor Niesiobędzki
 *
 */
public class CacheSettingsPanel extends JPanel {

    private final JosmTextField cacheDir = new JosmTextField(11);
    private final JSpinner maxElementsOnDisk = new JSpinner(new SpinnerNumberModel(
            AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.get().intValue(), 0, Integer.MAX_VALUE, 1));

    /**
     * Creates cache content panel
     */
    public CacheSettingsPanel() {
        super(new GridBagLayout());

        add(new JLabel(tr("Tile cache directory: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(cacheDir, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Maximum size of disk cache (per imagery) in MB: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(maxElementsOnDisk, GBC.eop());

        MainApplication.worker.submit(() -> {
            addToPanel(TMSLayer.getCache(), "TMS");
            addToPanel(WMSLayer.getCache(), "WMS");
            addToPanel(WMTSLayer.getCache(), "WMTS");
        });
    }

    private void addToPanel(final CacheAccess<String, BufferedImageCacheEntry> cache, final String name) {
        final Long cacheSize = getCacheSize(cache);
        final String sizeString = Utils.getSizeString(cacheSize, Locale.getDefault());
        final TableModel tableModel = getTableModel(cache);

        GuiHelper.runInEDT(() -> {
            /* I18n: {0} is cache name (TMS/WMS/WMTS), {1} is size string */
            add(new JLabel(tr("{0} cache, total cache size: {1}", name, sizeString)),
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

    /**
     * Loads the common settings.
     */
    void loadSettings() {
        this.cacheDir.setText(CachedTileLoaderFactory.PROP_TILECACHE_DIR.get());
        this.maxElementsOnDisk.setValue(AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.get());
    }

    /**
     * Saves the common settings.
     * @return true when restart is required
     */
    boolean saveSettings() {
        boolean restartRequired = removeCacheFiles(CachedTileLoaderFactory.PROP_TILECACHE_DIR.get(),
                1024L * 1024L * ((Integer) this.maxElementsOnDisk.getValue()));

        if (!AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.get().equals(this.maxElementsOnDisk.getValue())) {
            AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.put((Integer) this.maxElementsOnDisk.getValue());
            restartRequired = true;
        }


        if (!CachedTileLoaderFactory.PROP_TILECACHE_DIR.get().equals(this.cacheDir.getText())) {
            restartRequired = true;
            removeCacheFiles(CachedTileLoaderFactory.PROP_TILECACHE_DIR.get(), 0); // clear old cache directory
            CachedTileLoaderFactory.PROP_TILECACHE_DIR.put(this.cacheDir.getText());
        }

        return restartRequired;
    }

    private static boolean removeCacheFiles(String path, long maxSize) {
        File directory = new File(path);
        File[] cacheFiles = directory.listFiles((dir, name) -> name.endsWith(".data") || name.endsWith(".key"));
        boolean restartRequired = false;
        if (cacheFiles != null) {
            for (File cacheFile: cacheFiles) {
                if (cacheFile.length() > maxSize) {
                    if (!restartRequired) {
                        JCSCacheManager.shutdown(); // shutdown Cache - so files can by safely deleted
                        restartRequired = true;
                    }
                    Utils.deleteFile(cacheFile);
                    File otherFile = null;
                    if (cacheFile.getName().endsWith(".data")) {
                        otherFile = new File(cacheFile.getPath().replaceAll("\\.data$", ".key"));
                    } else if (cacheFile.getName().endsWith(".key")) {
                        otherFile = new File(cacheFile.getPath().replaceAll("\\.key$", ".data"));
                    }
                    if (otherFile != null) {
                        Utils.deleteFileIfExists(otherFile);
                    }
                }
            }
        }
        return restartRequired;
    }
}
