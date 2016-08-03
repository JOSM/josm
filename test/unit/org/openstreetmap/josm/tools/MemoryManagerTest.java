// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
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
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().memoryManagerLeaks();

    /**
     * Test {@link MemoryManager#allocateMemory(String, long, java.util.function.Supplier)}
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    public void testUseMemory() throws NotEnoughMemoryException {
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
    @Test(expected = IllegalStateException.class)
    public void testUseAfterFree() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, Object::new);
        testMemory.free();
        testMemory.get();
    }

    /**
     * Test that {@link MemoryHandle#get()} checks for free after free.
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test(expected = IllegalStateException.class)
    public void testFreeAfterFree() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, Object::new);
        testMemory.free();
        testMemory.free();
    }

    /**
     * Test that too big allocations fail
     * @throws NotEnoughMemoryException always
     */
    @Test(expected = NotEnoughMemoryException.class)
    public void testAllocationFails() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        long available = manager.getAvailableMemory();

        manager.allocateMemory("test", available + 1, () -> {
            fail("Should not reach");
            return null;
        });
    }

    /**
     * Test that allocations with null object fail
     * @throws NotEnoughMemoryException never
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSupplierFails() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();

        manager.allocateMemory("test", 1, () -> null);
    }

    /**
     * Test {@link MemoryManager#isAvailable(long)}
     */
    @Test
    public void testIsAvailable() {
        MemoryManager manager = MemoryManager.getInstance();
        assertTrue(manager.isAvailable(10));
        assertTrue(manager.isAvailable(100));
        assertTrue(manager.isAvailable(10));
    }

    /**
     * Test {@link MemoryManager#isAvailable(long)} for negative number
     * @throws NotEnoughMemoryException never
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIsAvailableFails() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();

        manager.isAvailable(-10);
    }

    /**
     * Test {@link MemoryManager#resetState()}
     * @throws NotEnoughMemoryException if there is not enough memory
     */
    @Test
    public void testResetState() throws NotEnoughMemoryException {
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
    @Test(expected = IllegalStateException.class)
    public void testResetStateUseAfterFree() throws NotEnoughMemoryException {
        MemoryManager manager = MemoryManager.getInstance();
        MemoryHandle<Object> testMemory = manager.allocateMemory("test", 10, Object::new);

        assertFalse(manager.resetState().isEmpty());
        testMemory.get();
    }

    /**
     * Reset the state of the memory manager
     * @param allowMemoryManagerLeaks If this is set, no exception is thrown if there were leaking entries.
     */
    public static void resetState(boolean allowMemoryManagerLeaks) {
        List<MemoryHandle<?>> hadLeaks = MemoryManager.getInstance().resetState();
        if (!allowMemoryManagerLeaks) {
            assertTrue("Memory manager had leaking memory: " + hadLeaks, hadLeaks.isEmpty());
        }
    }
}
