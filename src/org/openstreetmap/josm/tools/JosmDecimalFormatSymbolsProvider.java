// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.text.DecimalFormatSymbols;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.util.Locale;

/**
 * JOSM implementation of the {@link java.text.DecimalFormatSymbols DecimalFormatSymbols} class,
 * consistent with ISO 80000-1.
 * This class will only be used with Java 9 and later runtimes, as Java 8 implementation relies
 * on Java Extension Mechanism only, while Java 9 supports application classpath.
 * See {@link java.util.spi.LocaleServiceProvider LocaleServiceProvider} javadoc for more details.
 * @since 12931
 */
public class JosmDecimalFormatSymbolsProvider extends DecimalFormatSymbolsProvider {

    @Override
    public DecimalFormatSymbols getInstance(Locale locale) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        // Override digit group separator to be consistent across languages with ISO 80000-1, chapter 7.3.1
        symbols.setGroupingSeparator('\u202F'); // U+202F: NARROW NO-BREAK SPACE
        return symbols;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return I18n.getAvailableTranslations();
    }
}
