// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.ParseResult;
import org.openstreetmap.josm.gui.preferences.AbstractExtendedSourceEntryTestCase;
import org.openstreetmap.josm.testutils.annotations.HTTPS;

/**
 * Integration tests of {@link ValidatorTagCheckerRulesPreference} class.
 */
@HTTPS
@Timeout(20)
class ValidatorTagCheckerRulesPreferenceTestIT extends AbstractExtendedSourceEntryTestCase {
    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeAll
    public static void beforeClass() throws IOException {
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(ValidatorTagCheckerRulesPreferenceTestIT.class));
    }

    /**
     * Returns list of entries to test.
     * @return list of entries to test
     * @throws Exception in case of error
     */
    public static List<Object[]> data() throws Exception {
        return getTestParameters(new ValidatorTagCheckerRulesPreference.TagCheckerRulesSourceEditor()
                .loadAndGetAvailableSources());
    }

    /**
     * Test that available tag checker rule is valid.
     * @param displayName displayed name
     * @param url URL
     * @param source source entry to test
     * @throws Exception in case of error
     */
    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("data")
    void testValidityOfAvailableRule(String displayName, String url, ExtendedSourceEntry source) throws Exception {
        assumeFalse(isIgnoredSubstring(source, source.url));
        List<String> ignoredErrors = new ArrayList<>();
        Set<String> errors = new HashSet<>();
        System.out.print(source.url);
        try {
            ParseResult result = new MapCSSTagChecker().addMapCSS(source.url);
            assertFalse(result.parseChecks.isEmpty(), result::toString);
            System.out.println(result.parseErrors.isEmpty() ? " => OK" : " => KO");
            result.parseErrors.forEach(e -> handleException(source, e, errors, ignoredErrors));
        } catch (IOException e) {
            System.out.println(" => KO");
            e.printStackTrace();
            handleException(source, e, errors, ignoredErrors);
        }
        // #16567 - Shouldn't be necessary to print displayName if Ant worked properly
        // See https://josm.openstreetmap.de/ticket/16567#comment:53
        // See https://bz.apache.org/bugzilla/show_bug.cgi?id=64564
        // See https://github.com/apache/ant/pull/121
        assertTrue(errors.isEmpty(), displayName + " => " + errors);
        assumeTrue(ignoredErrors.isEmpty(), ignoredErrors.toString());
    }
}
