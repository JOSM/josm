// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        return Stream.of(Arguments.of("Hello world"), Arguments.of(new AutoCompletionItem("Hello world2")), Arguments.of(42.0));
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

    @Test
    void testEmptyEditor() {
        final HistoryComboBox historyComboBox = new HistoryComboBox();
        assertDoesNotThrow(historyComboBox::addCurrentItemToHistory);
        historyComboBox.getEditor().setItem(null);
        assertDoesNotThrow(historyComboBox::addCurrentItemToHistory);
    }

    /**
     * Non-regression test for JOSM #21215: StackOverflowError
     */
    @Test
    void testNonRegression21215() {
        final HistoryComboBox historyComboBox = new HistoryComboBox();
        // utils plugin2 added a listener that pretty much did this
        historyComboBox.addItemListener(event -> historyComboBox.addCurrentItemToHistory());
        final AutoCompletionItem testItem = new AutoCompletionItem("testNonRegression21215");
        // Add the original item
        historyComboBox.getEditor().setItem(testItem);
        historyComboBox.addCurrentItemToHistory();

        // Add a new item
        historyComboBox.getEditor().setItem(new AutoCompletionItem("testNonRegression21215_2"));
        historyComboBox.addCurrentItemToHistory();

        // Readd the first item
        historyComboBox.getEditor().setItem(testItem);
        assertDoesNotThrow(historyComboBox::addCurrentItemToHistory);
    }
}
