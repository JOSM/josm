// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ListenableWeakReference}
 * @author Michael Zangl
 * @since 12181
 */
public class ListenableWeakReferenceTest {
    /**
     * Default test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();
    private Object object;
    private boolean called;

    /**
     * Tests that {@link ListenableWeakReference#onDereference()} is called.
     * @throws InterruptedException never
     */
    @Test
    public void testOnDereference() throws InterruptedException {
        object = new Object();
        called = false;
        ListenableWeakReference<Object> weak = new ListenableWeakReference<>(object, () -> called = true);
        assertFalse(called);
        assertSame(object, weak.get());

        // now delete it
        object = null;
        System.gc();
        System.runFinalization();
        // now we wait for the listener thread
        Thread.sleep(200);
        assertTrue(called);

        assertNotNull(weak);
        assertNull(weak.get());
    }

}
