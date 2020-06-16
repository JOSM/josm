// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.stream.Stream;

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

    @Test
    public void testGroupingSeparator() {
        assertTrue(I18n.getAvailableTranslations().count() > 10);
        I18n.getAvailableTranslations().forEach(this::checkGroupingSymbol);
        Stream.of("", "AU", "IE", "US", "UK").map(country -> new Locale("en", country, "")).forEach(this::checkGroupingSymbol);
        Stream.of("", "AT", "CH", "DE").map(country -> new Locale("de", country, "")).forEach(this::checkGroupingSymbol);
    }

    private void checkGroupingSymbol(Locale locale) {
        assertEquals(locale.toString(), "123\u202F456", DecimalFormat.getInstance(locale).format(123_456));
    }

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
