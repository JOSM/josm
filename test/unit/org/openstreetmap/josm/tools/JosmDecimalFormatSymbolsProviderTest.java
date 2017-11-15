// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link JosmDecimalFormatSymbolsProvider}.
 */
public class JosmDecimalFormatSymbolsProviderTest {

    /**
     * Setup rule.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link JosmDecimalFormatSymbolsProvider#parseDouble}.
     */
    @Test
    public void testParseDouble() {
        final Locale defLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals(0.3, JosmDecimalFormatSymbolsProvider.parseDouble("0.3"), 1e-7);
            assertEquals(0.3, JosmDecimalFormatSymbolsProvider.parseDouble("0,3"), 1e-7);
            Locale.setDefault(Locale.FRENCH);
            assertEquals(0.3, JosmDecimalFormatSymbolsProvider.parseDouble("0.3"), 1e-7);
            assertEquals(0.3, JosmDecimalFormatSymbolsProvider.parseDouble("0,3"), 1e-7);
        } finally {
            Locale.setDefault(defLocale);
        }
    }
}
