// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.openstreetmap.josm.Main;

/**
 * This class allows all components of JOSM to register reclaimable amounts to memory.
 * <p>
 * It can be used to hold imagery caches or other data that can be reconstructed form disk/web if required.
 * <p>
 * Reclaimable storage implementations may be added in the future.
 *
 * @author Michael Zangl
 * @since 10588
 */
public class MemoryManager {
    /**
     * assumed minimum JOSM memory footprint
     */
    private static final long JOSM_CORE_FOOTPRINT = 50L * 1024L * 1024L;

    private static final MemoryManager INSTANCE = new MemoryManager();

    private final ArrayList<MemoryHandle<?>> activeHandles = new ArrayList<>();

    protected MemoryManager() {
    }

    /**
     * Allocates a basic, fixed memory size.
     * <p>
     * If there is enough free memory, the factory is used to procude one element which is then returned as memory handle.
     * <p>
     * You should invoke {@link MemoryHandle#free()} if you do not need that handle any more.
     * @param <T> The content type of the memory-
     * @param name A name for the memory area. Only used for debugging.
     * @param maxBytes The maximum amount of bytes the content may have
     * @param factory The factory to use to procude the content if there is sufficient memory.
     * @return A memory handle to the content.
     * @throws NotEnoughMemoryException If there is not enough memory to allocate.
     */
    public synchronized <T> MemoryHandle<T> allocateMemory(String name, long maxBytes, Supplier<T> factory) throws NotEnoughMemoryException {
        if (isAvailable(maxBytes)) {
            T content = factory.get();
            if (content == null) {
                throw new IllegalArgumentException("Factory did not return a content element.");
            }
            Logging.info(MessageFormat.format("Allocate for {0}: {1} MB of memory. Available: {2} MB.",
                    name, maxBytes / 1024 / 1024, getAvailableMemory() / 1024 / 1024));
            MemoryHandle<T> handle = new ManualFreeMemoryHandle<>(name, content, maxBytes);
            activeHandles.add(handle);
            return handle;
        } else {
            throw new NotEnoughMemoryException(maxBytes);
        }
    }

    /**
     * Check if that memory is available
     * @param maxBytes The memory to check for
     * @return <code>true</code> if that memory is available.
     */
    public synchronized boolean isAvailable(long maxBytes) {
        if (maxBytes < 0) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot allocate negative number of bytes: {0}", maxBytes));
        }
        return getAvailableMemory() >= maxBytes;
    }

    /**
     * Gets the maximum amount of memory available for use in this manager.
     * @return The maximum amount of memory.
     */
    public synchronized long getMaxMemory() {
        return Runtime.getRuntime().maxMemory() - JOSM_CORE_FOOTPRINT;
    }

    /**
     * Gets the memory that is considered free.
     * @return The memory that can be used for new allocations.
     */
    public synchronized long getAvailableMemory() {
        return getMaxMemory() - activeHandles.stream().mapToLong(MemoryHandle::getSize).sum();
    }

    /**
     * Get the global memory manager instance.
     * @return The memory manager.
     */
    public static MemoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Reset the state of this manager to the default state.
     * @return true if there were entries that have been reset.
     */
    protected synchronized List<MemoryHandle<?>> resetState() {
        ArrayList<MemoryHandle<?>> toFree = new ArrayList<>(activeHandles);
        toFree.forEach(MemoryHandle::free);
        return toFree;
    }

    /**
     * A memory area managed by the {@link MemoryManager}.
     * @author Michael Zangl
     * @param <T> The content type.
     */
    public interface MemoryHandle<T> {

        /**
         * Gets the content of this memory area.
         * <p>
         * This method should be the prefered access to the memory since it will do error checking when {@link #free()} was called.
         * @return The memory area content.
         */
        T get();

        /**
         * Get the size that was requested for this memory area.
         * @return the size
         */
        long getSize();

        /**
         * Manually release this memory area. There should be no memory consumed by this afterwards.
         */
        void free();
    }

    private class ManualFreeMemoryHandle<T> implements MemoryHandle<T> {
        private final String name;
        private T content;
        private final long size;

        ManualFreeMemoryHandle(String name, T content, long size) {
            this.name = name;
            this.content = content;
            this.size = size;
        }

        @Override
        public T get() {
            if (content == null) {
                throw new IllegalStateException(MessageFormat.format("Memory area was accessed after free(): {0}", name));
            }
            return content;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public void free() {
            if (content == null) {
                throw new IllegalStateException(MessageFormat.format("Memory area was already marked as freed: {0}", name));
            }
            content = null;
            synchronized (MemoryManager.this) {
                activeHandles.remove(this);
            }
        }

        @Override
        public String toString() {
            return "MemoryHandle [name=" + name + ", size=" + size + ']';
        }
    }

    /**
     * This exception is thrown if there is not enough memory for allocating the given object.
     * @author Michael Zangl
     */
    public static class NotEnoughMemoryException extends Exception {
        NotEnoughMemoryException(long memoryBytesRequired) {
            super(tr("To add another layer you need to allocate at least {0,number,#}MB memory to JOSM using -Xmx{0,number,#}M "
                            + "option (see http://forum.openstreetmap.org/viewtopic.php?id=25677).\n"
                            + "Currently you have {1,number,#}MB memory allocated for JOSM",
                            memoryBytesRequired / 1024 / 1024, Runtime.getRuntime().maxMemory() / 1024 / 1024));
        }
    }
}
