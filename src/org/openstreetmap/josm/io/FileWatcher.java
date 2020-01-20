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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Background thread that monitors certain files and perform relevant actions when they change.
 * @since 7185
 */
public class FileWatcher {

    private WatchService watcher;
    private Thread thread;

    private static final Map<SourceType, Consumer<SourceEntry>> loaderMap = new EnumMap<>(SourceType.class);
    private final Map<Path, SourceEntry> sourceMap = new HashMap<>();

    private static class InstanceHolder {
        static final FileWatcher INSTANCE = new FileWatcher();
    }

    /**
     * Returns the default instance.
     * @return the default instance
     * @since 14128
     */
    public static FileWatcher getDefaultInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Constructs a new {@code FileWatcher}.
     */
    public FileWatcher() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            thread = new Thread((Runnable) this::processEvents, "File Watcher");
        } catch (IOException | UnsupportedOperationException | UnsatisfiedLinkError e) {
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
     * Registers a source for local file changes, allowing dynamic reloading.
     * @param src The source to watch
     * @throws IllegalArgumentException if {@code rule} is null or if it does not provide a local file
     * @throws IllegalStateException if the watcher service failed to start
     * @throws IOException if an I/O error occurs
     * @since 12825
     */
    public void registerSource(SourceEntry src) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(src, "src");
        if (watcher == null) {
            throw new IllegalStateException("File watcher is not available");
        }
        // Get local file, as this method is only called for local style sources
        File file = new File(src.url);
        // Get parent directory as WatchService allows only to monitor directories, not single files
        File dir = file.getParentFile();
        if (dir == null) {
            throw new IllegalArgumentException("Resource "+src+" does not have a parent directory");
        }
        synchronized (this) {
            // Register directory. Can be called several times for a same directory without problem
            // (it returns the same key so it should not send events several times)
            dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            sourceMap.put(file.toPath(), src);
        }
    }

    /**
     * Registers a source loader, allowing dynamic reloading when an entry changes.
     * @param type the source type for which the loader operates
     * @param loader the loader in charge of reloading any source of given type when it changes
     * @return the previous loader registered for this source type, if any
     * @since 12825
     */
    public static Consumer<SourceEntry> registerLoader(SourceType type, Consumer<SourceEntry> loader) {
        return loaderMap.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(loader, "loader"));
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
                    Logging.trace(ex);
                    continue;
                }

                synchronized (this) {
                    SourceEntry source = sourceMap.get(fullPath);
                    if (source != null) {
                        Consumer<SourceEntry> loader = loaderMap.get(source.type);
                        if (loader != null) {
                            Logging.info("Source "+source.getDisplayString()+" has been modified. Reloading it...");
                            loader.accept(source);
                        } else {
                            Logging.warn("Received {0} event for unregistered source type: {1}", kind.name(), source.type);
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
