// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.imagery.CachedTileLoaderFactory;
import org.openstreetmap.josm.gui.layer.AbstractCachedTileSourceLayer;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * {@code JPanel} giving access to common imagery settings.
 * @since 5465
 */
public class CommonSettingsPanel extends JPanel {

    // Common Settings
    private final JosmComboBox<String> sharpen;
    private final JosmTextField tilecacheDir = new JosmTextField(11);
    private final JSpinner maxElementsOnDisk;
    private final JSlider tilesZoom = new JSlider(-2, 2, 0);


    /**
     * Constructs a new {@code CommonSettingsPanel}.
     */
    public CommonSettingsPanel() {
        super(new GridBagLayout());

        this.maxElementsOnDisk = new JSpinner(new SpinnerNumberModel(
                AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.get().intValue(), 0, Integer.MAX_VALUE, 1));

        this.sharpen = new JosmComboBox<>(new String[] {
                tr("None"),
                tr("Soft"),
                tr("Strong")});
        add(new JLabel(tr("Sharpen (requires layer re-add): ")));
        add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        add(this.sharpen, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Tile cache directory: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(tilecacheDir, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Maximum size of disk cache (per imagery) in MB: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(this.maxElementsOnDisk, GBC.eol());

        this.tilesZoom.setPaintLabels(true);
        this.tilesZoom.setMajorTickSpacing(2);
        this.tilesZoom.setMinorTickSpacing(1);
        this.tilesZoom.setPaintTicks(true);
        add(new JLabel(tr("Tiles zoom offset:")));
        add(GBC.glue(5, 0), GBC.std());
        add(this.tilesZoom, GBC.eol());
    }

    /**
     * Loads the common settings.
     */
    public void loadSettings() {
        this.sharpen.setSelectedIndex(Utils.clamp(ImageryLayer.PROP_SHARPEN_LEVEL.get(), 0, 2));
        this.tilecacheDir.setText(CachedTileLoaderFactory.PROP_TILECACHE_DIR.get());
        this.maxElementsOnDisk.setValue(AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.get());
        this.tilesZoom.setValue(AbstractTileSourceLayer.ZOOM_OFFSET.get());
    }

    /**
     * Saves the common settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        ImageryLayer.PROP_SHARPEN_LEVEL.put(sharpen.getSelectedIndex());

        boolean restartRequired = false;
        restartRequired |= removeCacheFiles(CachedTileLoaderFactory.PROP_TILECACHE_DIR.get(), 1024L * 1024L * ((Integer) this.maxElementsOnDisk.getValue()));

        if (!AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.get().equals(this.maxElementsOnDisk.getValue())) {
            AbstractCachedTileSourceLayer.MAX_DISK_CACHE_SIZE.put((Integer) this.maxElementsOnDisk.getValue());
            restartRequired = true;
        }


        if (!CachedTileLoaderFactory.PROP_TILECACHE_DIR.get().equals(this.tilecacheDir.getText())) {
            restartRequired = true;
            restartRequired |= removeCacheFiles(CachedTileLoaderFactory.PROP_TILECACHE_DIR.get(), 0); // clear old cache directory
            CachedTileLoaderFactory.PROP_TILECACHE_DIR.put(this.tilecacheDir.getText());
        }

        if (!AbstractTileSourceLayer.ZOOM_OFFSET.get().equals(this.tilesZoom.getValue())) {
            // TODO: make warning about too small MEMORY_CACHE_SIZE?
            AbstractTileSourceLayer.ZOOM_OFFSET.put(this.tilesZoom.getValue());
            restartRequired = true;
        }
        return restartRequired;
    }

    private static boolean removeCacheFiles(String path, long maxSize) {

        File directory = new File(path);
        File[] cacheFiles = directory.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".data") || name.endsWith(".key"));
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
