// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Arrays;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

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

    private static final Locale DE_DE = Locale.GERMANY;
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final Locale CA_ES_VALENCIA = new Locale("ca", "ES", "valencia");

    /**
     * Unit test of {@link LanguageInfo#getWikiLanguagePrefix}.
     */
    @Test
    public void getWikiLanguagePrefix() {
        Assert.assertEquals("De:", LanguageInfo.getWikiLanguagePrefix(DE_DE, LanguageInfo.LocaleType.DEFAULT));
        Assert.assertEquals("Pt_BR:", LanguageInfo.getWikiLanguagePrefix(PT_BR, LanguageInfo.LocaleType.DEFAULT));
    }

    /**
     * Unit test of {@link LanguageInfo#getJOSMLocaleCode}.
     */
    @Test
    public void getJOSMLocaleCode() {
        Assert.assertEquals("de", LanguageInfo.getJOSMLocaleCode(DE_DE));
        Assert.assertEquals("pt_BR", LanguageInfo.getJOSMLocaleCode(PT_BR));
        Assert.assertEquals("ca@valencia", LanguageInfo.getJOSMLocaleCode(CA_ES_VALENCIA));
    }

    /**
     * Unit test of {@link LanguageInfo#getJavaLocaleCode}.
     */
    @Test
    public void getJavaLocaleCode() {
        Assert.assertEquals("ca__valencia", LanguageInfo.getJavaLocaleCode("ca@valencia"));
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodeXML}.
     */
    @Test
    public void getLanguageCodeXML() {
        Assert.assertEquals("ca-valencia.", LanguageInfo.getLanguageCodeXML());
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodeManifest}.
     */
    @Test
    public void getLanguageCodeManifest() {
        Assert.assertEquals("ca-valencia_", LanguageInfo.getLanguageCodeManifest());
    }

    /**
     * Unit test of {@link LanguageInfo#getLanguageCodes}.
     */
    @Test
    public void getLanguageCodes() {
        Assert.assertEquals(Arrays.asList("ca_ES@valencia", "ca@valencia", "ca_ES", "ca"), LanguageInfo.getLanguageCodes(CA_ES_VALENCIA));
    }
}
