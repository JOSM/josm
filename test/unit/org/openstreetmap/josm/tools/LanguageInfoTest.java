// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link LanguageInfo}.
 */
public class LanguageInfoTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().i18n("ca@valencia");

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
    public void testWikiLanguagePrefix() {
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
        Assert.assertEquals(Arrays.asList(expected), actual);
    }

    /**
     * Unit test of {@link LanguageInfo#getJOSMLocaleCode}.
     */
    @Test
    public void testGetJOSMLocaleCode() {
        Assert.assertEquals("de", LanguageInfo.getJOSMLocaleCode(DE_DE));
        Assert.assertEquals("pt_BR", LanguageInfo.getJOSMLocaleCode(PT_BR));
        Assert.assertEquals("ca@valencia", LanguageInfo.getJOSMLocaleCode(CA_ES_VALENCIA));
    }

    /**
     * Unit test of {@link LanguageInfo#getJavaLocaleCode}.
     */
    @Test
    public void testGetJavaLocaleCode() {
        Assert.assertEquals("ca__valencia", LanguageInfo.getJavaLocaleCode("ca@valencia"));
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodeXML}.
     */
    @Test
    public void testGetLanguageCodeXML() {
        Assert.assertEquals("ca-valencia.", LanguageInfo.getLanguageCodeXML());
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodeManifest}.
     */
    @Test
    public void testGetLanguageCodeManifest() {
        Assert.assertEquals("ca-valencia_", LanguageInfo.getLanguageCodeManifest());
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodes}.
     */
    @Test
    public void testGetLanguageCodes() {
        Assert.assertEquals(Arrays.asList("ca_ES@valencia", "ca@valencia", "ca_ES", "ca"), LanguageInfo.getLanguageCodes(CA_ES_VALENCIA));
    }
}
