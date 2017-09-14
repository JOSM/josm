// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.tagging.ac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link AutoCompletionPriority}.
 */
public class AutoCompletionPriorityTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test getters.
     */
    @Test
    public void testGetters() {
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
    public void testOrdering() {
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
    public void testEqualsContract() {
        EqualsVerifier.forClass(AutoCompletionPriority.class).usingGetClass()
            .verify();
    }

    /**
     * Unit test of method {@link AutoCompletionPriority#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("<Priority; userInput: no, inDataSet: true, inStandard: false, selected: false>",
                AutoCompletionPriority.IS_IN_DATASET.toString());
        assertEquals("<Priority; userInput: 5, inDataSet: false, inStandard: false, selected: false>",
                new AutoCompletionPriority(false, false, false, 5).toString());
    }

    /**
     * Unit test of method {@link AutoCompletionPriority#mergeWith(AutoCompletionPriority)}.
     */
    @Test
    public void testMergeWith() {
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
