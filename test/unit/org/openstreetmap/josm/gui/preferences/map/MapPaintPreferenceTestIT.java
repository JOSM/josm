// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
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
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.ParallelParameterized;
import org.openstreetmap.josm.tools.ImageProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link MapPaintPreference} class.
 */
@RunWith(ParallelParameterized.class)
public class MapPaintPreferenceTestIT {

    /**
     * Setup rule
     */
    @ClassRule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().https().timeout(15000*60).parameters();

    /** Entry to test */
    private final ExtendedSourceEntry source;
    private final List<String> ignoredErrors = new ArrayList<>();
    private static final List<String> errorsToIgnore = new ArrayList<>();

    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeClass
    public static void beforeClass() throws IOException {
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(MapPaintPreferenceTestIT.class));
    }

    /**
     * Returns list of map paint styles to test.
     * @return list of map paint styles to test
     * @throws Exception if an error occurs
     */
    @Parameters(name = "{0} - {1}")
    public static List<Object[]> data() throws Exception {
        ImageProvider.clearCache();
        return new MapPaintPreference.MapPaintSourceEditor().loadAndGetAvailableSources().stream()
                .map(x -> new Object[] {x.getDisplayName(), x.url, x}).collect(Collectors.toList());
    }

    /**
     * Constructs a new {@code MapPaintPreferenceTestIT}
     * @param displayName displayed name
     * @param url URL
     * @param source source entry to test
     */
    public MapPaintPreferenceTestIT(String displayName, String url, ExtendedSourceEntry source) {
        this.source = source;
    }

    /**
     * Test that map paint style is valid.
     * @throws Exception in case of error
     */
    @Test
    public void testStyleValidity() throws Exception {
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

        List<Throwable> errors = new ArrayList<>(style.getErrors());
        errors.stream().map(Throwable::getMessage).filter(MapPaintPreferenceTestIT::isIgnoredSubstring).forEach(ignoredErrors::add);
        errors.removeIf(e -> ignoredErrors.contains(e.getMessage()));

        List<String> warnings = new ArrayList<>(style.getWarnings());
        warnings.stream().filter(MapPaintPreferenceTestIT::isIgnoredSubstring).forEach(ignoredErrors::add);
        warnings.removeAll(ignoredErrors);

        assertTrue(errors.toString() + '\n' + warnings.toString(), errors.isEmpty() && warnings.isEmpty());
        assumeTrue(ignoredErrors.toString(), ignoredErrors.isEmpty());
    }

    private static boolean isIgnoredSubstring(String substring) {
        return errorsToIgnore.parallelStream().anyMatch(x -> substring.contains(x));
    }
}
