// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests of {@link JosmDecimalFormatSymbolsProvider}.
 */
@org.openstreetmap.josm.testutils.annotations.I18n
class JosmDecimalFormatSymbolsProviderTest {
    @BeforeAll
    static void beforeAll() throws IOException {
        assertEquals("SPI,CLDR", System.getProperty("java.locale.providers"),
                "This test must be launched with -Djava.locale.providers=SPI,CLDR");
        try (InputStream in = I18n.class.getResourceAsStream("/META-INF/services/java.text.spi.DecimalFormatSymbolsProvider")) {
            assertNotNull(in);
            assertEquals("org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider",
                    new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
        }
    }

    static Stream<Locale> testGroupingSeparator() {
        System.out.println(Locale.getDefault());

        assertTrue(I18n.getAvailableTranslations().count() > 10);
        return Stream.concat(
                I18n.getAvailableTranslations(),
                Stream.concat(Stream.of("", "AU", "IE", "US", "UK").map(country -> new Locale("en", country, "")),
                        Stream.of("", "AT", "CH", "DE").map(country -> new Locale("de", country, ""))
                ));
    }

    @ParameterizedTest
    @MethodSource
    void testGroupingSeparator(Locale locale) {
        final String formattedNumber = DecimalFormat.getInstance(locale).format(123_456);
        // Note: If you have to add another numeral system, please indicate the name and the locale(s) it is for.
        if (formattedNumber.startsWith("1")) {
            // Western Arabic (for most locales)
            assertEquals("123\u202F456", formattedNumber, locale.toString() + ": " + locale.getDisplayName());
        } else if (formattedNumber.startsWith("١")) {
            // Eastern Arabic (for Arabic locale)
            assertEquals("١٢٣\u202F٤٥٦", formattedNumber, locale.toString() + ": " + locale.getDisplayName());
        } else if (formattedNumber.startsWith("۱")) {
            // Urdu (for Persian locale)
            assertEquals("۱۲۳\u202F۴۵۶", formattedNumber, locale.toString() + ": " + locale.getDisplayName());
        } else if (formattedNumber.startsWith("१")) {
            // Devanagari (for Marathi locale)
            assertEquals("१२३\u202F४५६", formattedNumber, locale.toString() + ": " + locale.getDisplayName());
        } else {
            fail(locale.toString() + " (" + locale.getDisplayName() + "): " + formattedNumber);
        }
    }

    /**
     * Test {@link JosmDecimalFormatSymbolsProvider#parseDouble}.
     */
    @Test
    void testParseDouble() {
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
