// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JMenu;
import javax.swing.JSeparator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests of {@link TaggingPresets} class.
 */
public class TaggingPresetsTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Tests that {@code TaggingPresets} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(TaggingPresets.class);
    }

    /**
     * Ensure that sorting the menu does <em>not</em> change the order of the first 3 actions.
     * See {@link TaggingPresetMenu.PresetTextComparator} for the comparator to fix if this test
     * fails <i>after</i> adding a new menu item.
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testNonRegression22783(boolean sort) {
        final MainMenu menu = MainApplication.getMenu();
        TaggingPresets.SORT_MENU.put(sort);
        TaggingPresets.initialize();
        final JMenu presetsMenu = menu.presetsMenu;
        assertAll(() -> assertSame(menu.presetSearchAction, presetsMenu.getItem(0).getAction()),
                () -> assertSame(menu.presetSearchPrimitiveAction, presetsMenu.getItem(1).getAction()),
                () -> assertInstanceOf(PreferencesAction.class, presetsMenu.getItem(2).getAction()),
                () -> assertInstanceOf(JSeparator.class, presetsMenu.getMenuComponent(3)));
    }

    /**
     * Wait for asynchronous icon loading
     * @param presets presets collection
     */
    public static void waitForIconLoading(Collection<TaggingPreset> presets) {
        presets.parallelStream().map(TaggingPreset::getIconLoadingTask).filter(Objects::nonNull).forEach(t -> {
            try {
                t.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Logging.error(e);
            }
        });
    }
}
