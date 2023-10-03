// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.tagging.ac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link AutoCompletionPriority}.
 */
class AutoCompletionPriorityTest {
    /**
     * Test getters.
     */
    @Test
    void testGetters() {
        assertTrue(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET.isInStandard());
        assertTrue(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET.isInDataSet());
        assertFalse(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET.isSelected());
        assertNull(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET.getUserInput());

        assertFalse(AutoCompletionPriority.IS_IN_DATASET.isInStandard());
        assertTrue(AutoCompletionPriority.IS_IN_DATASET.isInDataSet());
        assertFalse(AutoCompletionPriority.IS_IN_DATASET.isSelected());
        assertNull(AutoCompletionPriority.IS_IN_DATASET.getUserInput());

        assertTrue(AutoCompletionPriority.IS_IN_STANDARD.isInStandard());
        assertFalse(AutoCompletionPriority.IS_IN_STANDARD.isInDataSet());
        assertFalse(AutoCompletionPriority.IS_IN_STANDARD.isSelected());
        assertNull(AutoCompletionPriority.IS_IN_STANDARD.getUserInput());

        assertFalse(AutoCompletionPriority.IS_IN_SELECTION.isInStandard());
        assertFalse(AutoCompletionPriority.IS_IN_SELECTION.isInDataSet());
        assertTrue(AutoCompletionPriority.IS_IN_SELECTION.isSelected());
        assertNull(AutoCompletionPriority.IS_IN_SELECTION.getUserInput());

        assertFalse(AutoCompletionPriority.UNKNOWN.isInStandard());
        assertFalse(AutoCompletionPriority.UNKNOWN.isInDataSet());
        assertFalse(AutoCompletionPriority.UNKNOWN.isSelected());
        assertNull(AutoCompletionPriority.UNKNOWN.getUserInput());

        assertEquals(Integer.valueOf(5), new AutoCompletionPriority(false, false, false, 5).getUserInput());
    }

    /**
     * Test ordering of priorities.
     */
    @Test
    void testOrdering() {
        SortedSet<AutoCompletionPriority> set = new TreeSet<>();
        set.add(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET);
        set.add(AutoCompletionPriority.IS_IN_DATASET);
        set.add(AutoCompletionPriority.IS_IN_STANDARD);
        set.add(AutoCompletionPriority.IS_IN_SELECTION);
        set.add(AutoCompletionPriority.UNKNOWN);
        set.add(new AutoCompletionPriority(false, false, false, 5));
        set.add(new AutoCompletionPriority(false, false, false, 0));
        set.add(new AutoCompletionPriority(false, false, false, 1));

        assertEquals(Arrays.asList(
                AutoCompletionPriority.UNKNOWN,
                AutoCompletionPriority.IS_IN_STANDARD,
                AutoCompletionPriority.IS_IN_DATASET,
                AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET,
                AutoCompletionPriority.IS_IN_SELECTION,
                new AutoCompletionPriority(false, false, false, 5),
                new AutoCompletionPriority(false, false, false, 1),
                new AutoCompletionPriority(false, false, false, 0)
                ), new ArrayList<>(set));
    }

    /**
     * Unit test of methods {@link AutoCompletionPriority#equals} and {@link AutoCompletionPriority#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(AutoCompletionPriority.class).usingGetClass()
            .verify();
    }

    /**
     * Unit test of method {@link AutoCompletionPriority#toString()}.
     */
    @Test
    void testToString() {
        assertEquals("<Priority; userInput: no, inDataSet: true, inStandard: false, selected: false>",
                AutoCompletionPriority.IS_IN_DATASET.toString());
        assertEquals("<Priority; userInput: 5, inDataSet: false, inStandard: false, selected: false>",
                new AutoCompletionPriority(false, false, false, 5).toString());
    }

    /**
     * Unit test of method {@link AutoCompletionPriority#mergeWith(AutoCompletionPriority)}.
     */
    @Test
    void testMergeWith() {
        assertEquals(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET,
                AutoCompletionPriority.IS_IN_DATASET.mergeWith(AutoCompletionPriority.IS_IN_STANDARD));
        assertEquals(AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET,
                AutoCompletionPriority.IS_IN_STANDARD.mergeWith(AutoCompletionPriority.IS_IN_DATASET));
        assertEquals(AutoCompletionPriority.IS_IN_SELECTION,
                AutoCompletionPriority.UNKNOWN.mergeWith(AutoCompletionPriority.IS_IN_SELECTION));
        assertEquals(new AutoCompletionPriority(false, false, false, 0),
                new AutoCompletionPriority(false, false, false, 5).mergeWith(new AutoCompletionPriority(false, false, false, 0)));
    }
}
