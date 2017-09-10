// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Background thread that monitors certain files and perform relevant actions when they change.
 * @since 7185
 */
public class FileWatcher {

    private WatchService watcher;
    private Thread thread;

    private final Map<Path, StyleSource> styleMap = new HashMap<>();
    private final Map<Path, SourceEntry> ruleMap = new HashMap<>();

    /**
     * Constructs a new {@code FileWatcher}.
     */
    public FileWatcher() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            thread = new Thread((Runnable) this::processEvents, "File Watcher");
        } catch (IOException e) {
            Logging.error(e);
        }
    }

    /**
     * Starts the File Watcher thread.
     */
    public final void start() {
        if (thread != null && !thread.isAlive()) {
            thread.start();
        }
    }

    /**
     * Registers a map paint style for local file changes, allowing dynamic reloading.
     * @param style The style to watch
     * @throws IllegalArgumentException if {@code style} is null or if it does not provide a local file
     * @throws IllegalStateException if the watcher service failed to start
     * @throws IOException if an I/O error occurs
     */
    public void registerStyleSource(StyleSource style) throws IOException {
        register(style, styleMap);
    }

    /**
     * Registers a validator rule for local file changes, allowing dynamic reloading.
     * @param rule The rule to watch
     * @throws IllegalArgumentException if {@code rule} is null or if it does not provide a local file
     * @throws IllegalStateException if the watcher service failed to start
     * @throws IOException if an I/O error occurs
     * @since 7276
     */
    public void registerValidatorRule(SourceEntry rule) throws IOException {
        register(rule, ruleMap);
    }

    private <T extends SourceEntry> void register(T obj, Map<Path, T> map) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(obj, "obj");
        if (watcher == null) {
            throw new IllegalStateException("File watcher is not available");
        }
        // Get local file, as this method is only called for local style sources
        File file = new File(obj.url);
        // Get parent directory as WatchService allows only to monitor directories, not single files
        File dir = file.getParentFile();
        if (dir == null) {
            throw new IllegalArgumentException("Resource "+obj+" does not have a parent directory");
        }
        synchronized (this) {
            // Register directory. Can be called several times for a same directory without problem
            // (it returns the same key so it should not send events several times)
            dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            map.put(file.toPath(), obj);
        }
    }

    /**
     * Process all events for the key queued to the watcher.
     */
    private void processEvents() {
        Logging.debug("File watcher thread started");
        while (true) {

            // wait for key to be signaled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();

                if (StandardWatchEventKinds.OVERFLOW.equals(kind)) {
                    continue;
                }

                // The filename is the context of the event.
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                if (filename == null) {
                    continue;
                }

                // Only way to get full path (http://stackoverflow.com/a/7802029/2257172)
                Path fullPath = ((Path) key.watchable()).resolve(filename);

                try {
                    // Some filesystems fire two events when a file is modified. Skip first event (file is empty)
                    if (Files.size(fullPath) == 0) {
                        continue;
                    }
                } catch (IOException ex) {
                    Logging.error(ex);
                    continue;
                }

                synchronized (this) {
                    StyleSource style = styleMap.get(fullPath);
                    SourceEntry rule = ruleMap.get(fullPath);
                    if (style != null) {
                        Logging.info("Map style "+style.getDisplayString()+" has been modified. Reloading style...");
                        Executors.newSingleThreadExecutor(Utils.newThreadFactory("mapstyle-reload-%d", Thread.NORM_PRIORITY)).submit(
                                new MapPaintStyleLoader(Collections.singleton(style)));
                    } else if (rule != null) {
                        Logging.info("Validator rule "+rule.getDisplayString()+" has been modified. Reloading rule...");
                        MapCSSTagChecker tagChecker = OsmValidator.getTest(MapCSSTagChecker.class);
                        if (tagChecker != null) {
                            try {
                                tagChecker.addMapCSS(rule.url);
                            } catch (IOException | ParseException e) {
                                Logging.warn(e);
                            }
                        }
                    } else if (Logging.isDebugEnabled()) {
                        Logging.debug("Received {0} event for unregistered file: {1}", kind.name(), fullPath);
                    }
                }
            }

            // Reset the key -- this step is critical to receive
            // further watch events. If the key is no longer valid, the directory
            // is inaccessible so exit the loop.
            if (!key.reset()) {
                break;
            }
        }
    }
}
