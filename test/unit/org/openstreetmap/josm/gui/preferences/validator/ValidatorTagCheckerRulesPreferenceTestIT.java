// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.ParseResult;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link ValidatorTagCheckerRulesPreference} class.
 */
class ValidatorTagCheckerRulesPreferenceTestIT {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().timeout(20_000);

    /**
     * Test that available tag checker rules are valid.
     * @throws Exception in case of error
     */
    @Test
    void testValidityOfAvailableRules() throws Exception {
        Collection<ExtendedSourceEntry> sources = new ValidatorTagCheckerRulesPreference.TagCheckerRulesSourceEditor()
                .loadAndGetAvailableSources();
        assertFalse(sources.isEmpty(), sources::toString);
        Collection<Throwable> allErrors = new ArrayList<>();
        MapCSSTagChecker tagChecker = new MapCSSTagChecker();
        for (ExtendedSourceEntry source : sources) {
            System.out.print(source.url);
            try {
                ParseResult result = tagChecker.addMapCSS(source.url);
                assertFalse(result.parseChecks.isEmpty(), result::toString);
                System.out.println(result.parseErrors.isEmpty() ? " => OK" : " => KO");
                allErrors.addAll(result.parseErrors);
            } catch (IOException e) {
                System.out.println(" => KO");
                allErrors.add(e);
                e.printStackTrace();
            }
        }
        assertTrue(allErrors.isEmpty(), allErrors::toString);
    }
}
