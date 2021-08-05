// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;

/**
 * Test class for {@link HistoryComboBox}
 * @author Taylor Smock
 */
@FullPreferences
class HistoryComboBoxTest {
    static Stream<Arguments> testNonRegression21203() {
        return Stream.of(Arguments.of("Hello world"), Arguments.of(new AutoCompletionItem("Hello world2")));
    }

    /**
     * Non-regression test for #21203
     * @param object object to set as editor item
     */
    @ParameterizedTest
    @MethodSource
    void testNonRegression21203(final Object object) {
        final HistoryComboBox historyComboBox = new HistoryComboBox();
        // Sanity check
        assertEquals(0, historyComboBox.getModel().getSize());
        historyComboBox.getEditor().setItem(object);
        assertDoesNotThrow(historyComboBox::addCurrentItemToHistory);
    }

    /**
     * This ensures that we do throw on unknown objects for #21203
     */
    @Test
    void testNonRegression21203Throws() {
        final HistoryComboBox historyComboBox = new HistoryComboBox();
        // Sanity check
        assertEquals(0, historyComboBox.getModel().getSize());
        historyComboBox.getEditor().setItem(new Object());
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                historyComboBox::addCurrentItemToHistory);
        assertEquals("Object is not supported in addCurrentItemToHistory", illegalArgumentException.getMessage());
    }
}
