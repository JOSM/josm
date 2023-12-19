// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.I18n;

/**
 * Unit tests of {@link LanguageInfo}.
 */
@I18n("ca@valencia")
class LanguageInfoTest {
    private static final Locale EN_NZ = new Locale("en", "NZ");
    private static final Locale DE_DE = Locale.GERMANY;
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final Locale CA_ES_VALENCIA = new Locale("ca", "ES", "valencia");
    private static final Locale ZN_CN = Locale.SIMPLIFIED_CHINESE;
    private static final Locale ZN_TW = Locale.TRADITIONAL_CHINESE;
    private static final Locale EN_GB = Locale.UK;
    private static final Locale RU = new Locale("ru");
    private static final Locale NB = new Locale("nb");
    private static final Locale AST = new Locale("ast");

    /**
     * Unit test of {@link LanguageInfo#getWikiLanguagePrefix}.
     */
    @Test
    void testWikiLanguagePrefix() {
        testGetWikiLanguagePrefixes(LanguageInfo.LocaleType.DEFAULT,
                "En:", "De:", "Pt_BR:", "Ca-Valencia:", "Zh_CN:", "Zh_TW:", "Ast:", "En_GB:", "Ru:", "Nb:");
        testGetWikiLanguagePrefixes(LanguageInfo.LocaleType.DEFAULTNOTENGLISH,
                null, "De:", "Pt_BR:", "Ca-Valencia:", "Zh_CN:", "Zh_TW:", "Ast:", "En_GB:", "Ru:", "Nb:");
        testGetWikiLanguagePrefixes(LanguageInfo.LocaleType.BASELANGUAGE,
                null, null, "Pt:", null, "Zh:", "Zh:", null, null, null, null);
        testGetWikiLanguagePrefixes(LanguageInfo.LocaleType.ENGLISH,
                "", "", "", "", "", "", "", "", "", "");
        testGetWikiLanguagePrefixes(LanguageInfo.LocaleType.OSM_WIKI,
                "", "DE:", "Pt:", "Ca:", "Zh-hans:", "Zh-hant:", "Ast:", "", "RU:", "No:");
    }

    private static void testGetWikiLanguagePrefixes(LanguageInfo.LocaleType type, String...expected) {
        final List<String> actual = Stream.of(EN_NZ, DE_DE, PT_BR, CA_ES_VALENCIA, ZN_CN, ZN_TW, AST, EN_GB, RU, NB)
                .map(locale -> LanguageInfo.getWikiLanguagePrefix(locale, type))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(expected), actual);
    }

    /**
     * Unit test of {@link LanguageInfo#getLocale}.
     */
    @Test
    void testGetLocale() {
        assertEquals(RU, LanguageInfo.getLocale("ru"));
        assertEquals(EN_GB, LanguageInfo.getLocale("en_GB"));
        assertEquals(CA_ES_VALENCIA, LanguageInfo.getLocale("ca_ES@valencia"));
        assertEquals(DE_DE, LanguageInfo.getLocale("de_DE"));
        assertEquals(DE_DE, LanguageInfo.getLocale("de_DE.UTF-8")); // LANG, LC_MEASUREMENT
        assertEquals(PT_BR, LanguageInfo.getLocale("pt_BR.UTF-8")); // LANG, LC_MEASUREMENT
    }

    /**
     * Unit test of {@link LanguageInfo#getJOSMLocaleCode}.
     */
    @Test
    void testGetJOSMLocaleCode() {
        assertEquals("de", LanguageInfo.getJOSMLocaleCode(DE_DE));
        assertEquals("pt_BR", LanguageInfo.getJOSMLocaleCode(PT_BR));
        assertEquals("ca@valencia", LanguageInfo.getJOSMLocaleCode(CA_ES_VALENCIA));
    }

    /**
     * Unit test of {@link LanguageInfo#getJavaLocaleCode}.
     */
    @Test
    void testGetJavaLocaleCode() {
        assertEquals("ca__valencia", LanguageInfo.getJavaLocaleCode("ca@valencia"));
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodeXML}.
     */
    @Test
    void testGetLanguageCodeXML() {
        assertEquals("ca-valencia.", LanguageInfo.getLanguageCodeXML());
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodeManifest}.
     */
    @Test
    void testGetLanguageCodeManifest() {
        assertEquals("ca-valencia_", LanguageInfo.getLanguageCodeManifest());
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodes}.
     */
    @Test
    void testGetLanguageCodes() {
        assertEquals(Arrays.asList("ca_ES@valencia", "ca@valencia", "ca_ES", "ca"), LanguageInfo.getLanguageCodes(CA_ES_VALENCIA));
    }
}
