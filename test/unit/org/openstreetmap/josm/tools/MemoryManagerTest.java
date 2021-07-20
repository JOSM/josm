// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.MemoryManager.MemoryHandle;
import org.openstreetmap.josm.tools.MemoryManager.NotEnoughMemoryException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link MemoryManager} class.
 * @author Michael Zangl
 */
public class MemoryManagerTest {
    /**
     * Base test environment
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().memoryManagerLeaks();

    /**
     * Test {@link MemoryManager#allocateMemory(String, long, java.util.function.Supplier)}
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    void testUseMemory() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        long available = manager.getAvailableMemory();
        assertTrue(available < Runtime.getRuntime().maxMemory());
        assertEquals(available, manager.getMaxMemory());

        Object o1 = new Object();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, () -> o1);
        assertEquals(available - 10, manager.getAvailableMemory());
        assertSame(o1, testMemory.get());
        assertEquals(10, testMemory.getSize());
        assertTrue(testMemory.toString().startsWith("MemoryHandle"));

        manager.allocateMemory("test2", 10, Object::new);
        assertEquals(available - 20, manager.getAvailableMemory());

        testMemory.free();
        assertEquals(available - 10, manager.getAvailableMemory());
    }

    /**
     * Test that {@link MemoryHandle#get()} checks for use after free.
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    void testUseAfterFree() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, Object::new);
        testMemory.free();
        assertThrows(IllegalStateException.class, () -> testMemory.get());
    }

    /**
     * Test that {@link MemoryHandle#get()} checks for free after free.
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    void testFreeAfterFree() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, Object::new);
        testMemory.free();
        assertThrows(IllegalStateException.class, () -> testMemory.free());
    }

    /**
     * Test that too big allocations fail
     */
    @Test
    void testAllocationFails() {
        MemoryManager manager = MemoryManager.getInstance();
        long available = manager.getAvailableMemory();

        assertThrows(NotEnoughMemoryException.class, () -> manager.allocateMemory("test", available + 1, () -> {
            fail("Should not reach");
            return null;
        }));
    }

    /**
     * Test that allocations with null object fail
     * @throws NotEnoughMemoryException never
     */
    @Test
    void testSupplierFails() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();

        assertThrows(IllegalArgumentException.class, () -> manager.allocateMemory("test", 1, () -> null));
    }

    /**
     * Test {@link MemoryManager#isAvailable(long)}
     */
    @Test
    void testIsAvailable() {
        MemoryManager manager = MemoryManager.getInstance();
        assertTrue(manager.isAvailable(10));
        assertTrue(manager.isAvailable(100));
        assertTrue(manager.isAvailable(10));
    }

    /**
     * Test {@link MemoryManager#isAvailable(long)} for negative number
     * @throws NotEnoughMemoryException never
     */
    @Test
    void testIsAvailableFails() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();

        assertThrows(IllegalArgumentException.class, () -> manager.isAvailable(-10));
    }

    /**
     * Test {@link MemoryManager#resetState()}
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    void testResetState() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        assertTrue(manager.resetState().isEmpty());

        manager.allocateMemory("test", 10, Object::new);
        manager.allocateMemory("test2", 10, Object::new);
        assertEquals(2, manager.resetState().size());

        assertTrue(manager.resetState().isEmpty());
    }

    /**
     * Test {@link MemoryManager#resetState()}
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    void testResetStateUseAfterFree() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, Object::new);

        assertFalse(manager.resetState().isEmpty());
        assertThrows(IllegalStateException.class, () -> testMemory.get());
    }

    /**
     * Reset the state of the memory manager
     * @param allowMemoryManagerLeaks If this is set, no exception is thrown if there were leaking entries.
     */
    public static void resetState(boolean allowMemoryManagerLeaks) {
        List<MemoryHandle<?>> hadLeaks = MemoryManager.getInstance().resetState();
        if (!allowMemoryManagerLeaks) {
            assertTrue(hadLeaks.isEmpty(), "Memory manager had leaking memory: " + hadLeaks);
        }
    }
}
