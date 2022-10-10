// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Test class for {@link FilterDialog}
 */
@BasicPreferences
class FilterDialogTest {
    private static final List<Filter> FILTERS = Stream.of("type:node", "type:way", "type:relation")
            .map(Filter::readFromString).map(Filter::new).collect(Collectors.toList());

    @RegisterExtension
    static JOSMTestRules josmTestRules = new JOSMTestRules().main();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testNonRegression22439(int indexToRemove) throws ReflectiveOperationException {
        FilterDialog filterDialog = new FilterDialog();
        FilterTableModel filterModel = filterDialog.getFilterModel();
        filterModel.addFilters(FILTERS.toArray(new Filter[0]));
        Filter toRemove = filterModel.getValue(indexToRemove);

        assertEquals(FILTERS.get(indexToRemove), toRemove, "The indexes don't match between lists");

        filterModel.getSelectionModel().setSelectionInterval(indexToRemove, indexToRemove);
        Field deleteField = FilterDialog.class.getDeclaredField("deleteAction");
        ReflectionUtils.setObjectsAccessible(deleteField);
        AbstractAction deleteAction = (AbstractAction) deleteField.get(filterDialog);
        deleteAction.actionPerformed(null);

        assertEquals(2, filterModel.getFilters().size());
        assertFalse(filterModel.getFilters().contains(toRemove));
    }
}
