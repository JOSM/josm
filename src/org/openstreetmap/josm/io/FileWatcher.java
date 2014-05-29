// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Background thread that monitors certain files and perform relevant actions when they change.
 * @since 7185
 */
public class FileWatcher {

    private WatchService watcher;

    private final Map<Path, StyleSource> styleMap = new HashMap<>();

    /**
     * Constructs a new {@code FileWatcher}.
     */
    public FileWatcher() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    processEvents();
                }
            }, "File Watcher").start();
        } catch (IOException e) {
            Main.error(e);
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
        CheckParameterUtil.ensureParameterNotNull(style, "style");
        if (watcher == null) {
            throw new IllegalStateException("File watcher is not available");
        }
        try (MirroredInputStream mis = style.getMirroredInputStream()) {
            // Get underlying file
            File file = mis.getFile();
            if (file == null) {
                throw new IllegalArgumentException("Style "+style+" does not have a local file");
            }
            // Get parent directory as WatchService allows only to monitor directories, not single files
            File dir = file.getParentFile();
            if (dir == null) {
                throw new IllegalArgumentException("Style "+style+" does not have a parent directory");
            }
            synchronized(this) {
                // Register directory. Can be called several times for a same directory without problem
                // (it returns the same key so it should not send events several times)
                dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                styleMap.put(file.toPath(), style);
            }
        }
    }

    /**
     * Process all events for the key queued to the watcher.
     */
    private void processEvents() {
        if (Main.isDebugEnabled()) {
            Main.debug("File watcher thread started");
        }
        while (true) {

            // wait for key to be signaled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();

                if (StandardWatchEventKinds.OVERFLOW.equals(kind)) {
                    continue;
                }

                // The filename is the context of the event.
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path filename = ev.context();
                if (filename == null) {
                    continue;
                }

                // Only way to get full path (http://stackoverflow.com/a/7802029/2257172)
                Path fullPath = ((Path)key.watchable()).resolve(filename);

                synchronized(this) {
                    StyleSource style = styleMap.get(fullPath);
                    if (style != null) {
                        Main.info("Map style "+style.getDisplayString()+" has been modified. Reloading style...");
                        //style.loadStyleSource();
                        Main.worker.submit(new MapPaintStyleLoader(Collections.singleton(style)));
                    } else if (Main.isDebugEnabled()) {
                        Main.debug("Received ENTRY_MODIFY event for unregistered file: "+fullPath);
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
