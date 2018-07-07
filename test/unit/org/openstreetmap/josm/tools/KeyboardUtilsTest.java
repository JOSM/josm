// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link KeyboardUtils} class.
 */
public class KeyboardUtilsTest {
    /**
     * Initializes test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().platform();

    /**
     * Checks that definition of extended characters is correct
     */
    @Test
    public void testExtendedCharacters() {
        Map<Integer, Character> map = new LinkedHashMap<>();
        KeyboardUtils.addLatinCharacters(map);
        KeyboardUtils.addSymbolCharacters(map);
        KeyboardUtils.addArabicCharacters(map);
        KeyboardUtils.addArmenianCharacters(map);
        KeyboardUtils.addCyrillicCharacters(map);
        KeyboardUtils.addGeorgianCharacters(map);
        KeyboardUtils.addGreekCharacters(map);
        KeyboardUtils.addHebrewCharacters(map);
        KeyboardUtils.addJapaneseCharacters(map);
        KeyboardUtils.addThaiCharacters(map);
        for (Entry<Integer, Character> e : map.entrySet()) {
            assertEquals(e.getKey().intValue(), KeyboardUtils.EXTENDED_KEYCODE_FLAG | (int) e.getValue());
        }
    }

    /**
     * Unit test of {@link KeyboardUtils#getCharactersForKey} - E00 character
     */
    @Test
    public void testgetCharactersForKeyE00() {
        testgetCharactersForKeyE00("ar", 'ذ', '>', '`', '~');
        testgetCharactersForKeyE00("fr_FR", '²', '$', '`', '~');
        testgetCharactersForKeyE00("fr_CA", '#', '$', '/', '`', '~');
        testgetCharactersForKeyE00("sq", '\\', '`', '~');
        testgetCharactersForKeyE00("it", '\\', '`', '~');
        testgetCharactersForKeyE00("pt", '\\', '`', '~');
        testgetCharactersForKeyE00("pt_BR", '\'', '`', '~');
        testgetCharactersForKeyE00("de", '^', '`', '~');
        testgetCharactersForKeyE00("cs", ';', '`', '~');
        testgetCharactersForKeyE00("he", '`', '~');
        testgetCharactersForKeyE00("hu", '0', '`', '~');
        testgetCharactersForKeyE00("pl", 'µ', '^', '˛', '`', '~');
        testgetCharactersForKeyE00("bs", '¸', '`', '~');
        testgetCharactersForKeyE00("hr", '¸', '`', '~');
        testgetCharactersForKeyE00("sl", '¸', '`', '~');
        testgetCharactersForKeyE00("sr", '¸', '`', '~');
        testgetCharactersForKeyE00("ro", ']', '`', '~');
        testgetCharactersForKeyE00("da", '½', '`', '~');
        testgetCharactersForKeyE00("fo", '½', '`', '~');
        testgetCharactersForKeyE00("nl", '@', '`', '~');
        testgetCharactersForKeyE00("et", 'ˇ', '`', '~');
        testgetCharactersForKeyE00("is", '°', '`', '~');
        testgetCharactersForKeyE00("es", '|', '`', '~');
        testgetCharactersForKeyE00("es_ES", 'º', '`', '~');
        testgetCharactersForKeyE00("tr", '"', '*', '`', '~');
        testgetCharactersForKeyE00("de_LU", '^', '²', '§', '`', '~');
        testgetCharactersForKeyE00("fr_LU", '$', '²', '§', '`', '~');
        testgetCharactersForKeyE00("de_CH", '^', '§', '`', '~');
        testgetCharactersForKeyE00("fr_CH", '²', '$', '§', '`', '~');
        testgetCharactersForKeyE00("de_LI", '^', '§', '`', '~');
        testgetCharactersForKeyE00("fi_FI", '§', '`', '~');
        testgetCharactersForKeyE00("sv_SE", '§', '`', '~');
        testgetCharactersForKeyE00("no_NO", '|', '`', '~');
        testgetCharactersForKeyE00("sv_NO", '|', '`', '~');
    }

    private static void testgetCharactersForKeyE00(String locale, Character... expected) {
        if (locale.contains("_")) {
            String[] l = locale.split("_");
            testgetCharactersForKeyE00(new Locale(l[0], l[1]), expected);
        } else {
            testgetCharactersForKeyE00(new Locale(locale), expected);
        }
    }

    private static void testgetCharactersForKeyE00(Locale locale, Character... expected) {
        assertEquals(Arrays.asList(expected), KeyboardUtils.getCharactersForKey('E', 0, locale));
    }
}
