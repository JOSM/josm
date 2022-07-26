// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.AssignmentInstruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.AbstractExtendedSourceEntryTestCase;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;
import org.openstreetmap.josm.testutils.annotations.JosmHome;
import org.openstreetmap.josm.testutils.annotations.MapStyles;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Integration tests of {@link MapPaintPreference} class.
 */
@HTTP
@HTTPS
@JosmHome("test/config/unit/mapPaintPreferencesTestIT-josm.home")
@MapStyles
@Timeout(value = 15, unit = TimeUnit.MINUTES)
@IntegrationTest
class MapPaintPreferenceTestIT extends AbstractExtendedSourceEntryTestCase {
    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeAll
    public static void beforeClass() throws IOException {
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(MapPaintPreferenceTestIT.class));
    }

    /**
     * Returns list of map paint styles to test.
     * @return list of map paint styles to test
     * @throws Exception if an error occurs
     */
    public static List<Object[]> data() throws Exception {
        ImageProvider.clearCache();
        return getTestParameters(new MapPaintPreference.MapPaintSourceEditor().loadAndGetAvailableSources());
    }

    /**
     * Test that map paint style is valid.
     * @param displayName displayed name
     * @param url URL
     * @param source source entry to test
     * @throws Exception in case of error
     */
    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("data")
    void testStyleValidity(String displayName, String url, ExtendedSourceEntry source) throws Exception {
        assumeFalse(isIgnoredSubstring(source, source.url));
        StyleSource style = MapPaintStyles.addStyle(source);
        if (style instanceof MapCSSStyleSource) {
            // Force loading of all icons to detect missing ones
            for (MapCSSRule rule : ((MapCSSStyleSource) style).rules) {
                for (Instruction instruction : rule.declaration.instructions) {
                    if (instruction instanceof AssignmentInstruction) {
                        AssignmentInstruction ai = (AssignmentInstruction) instruction;
                        if (StyleKeys.ICON_IMAGE.equals(ai.key)
                         || StyleKeys.FILL_IMAGE.equals(ai.key)
                         || StyleKeys.REPEAT_IMAGE.equals(ai.key)) {
                            if (ai.val instanceof String) {
                                MapPaintStyles.getIconProvider(new IconReference((String) ai.val, style), true);
                            }
                        }
                    }
                }
            }
        }

        List<String> ignoredErrors = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>(style.getErrors());
        errors.stream().map(Throwable::getMessage).filter(s -> isIgnoredSubstring(source, s)).forEach(ignoredErrors::add);
        errors.removeIf(e -> ignoredErrors.contains(e.getMessage()));

        List<String> warnings = new ArrayList<>(style.getWarnings());
        warnings.stream().filter(s -> isIgnoredSubstring(source, s)).forEach(ignoredErrors::add);
        warnings.removeAll(ignoredErrors);

        // #16567 - Shouldn't be necessary to print displayName if Ant worked properly
        // See https://josm.openstreetmap.de/ticket/16567#comment:53
        // See https://bz.apache.org/bugzilla/show_bug.cgi?id=64564
        // See https://github.com/apache/ant/pull/121
        assertTrue(errors.isEmpty() && warnings.isEmpty(), displayName + " => " + errors + '\n' + warnings);
        assumeTrue(ignoredErrors.isEmpty(), ignoredErrors.toString());
    }
}
