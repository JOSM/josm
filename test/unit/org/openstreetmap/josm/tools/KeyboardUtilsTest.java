// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.awt.event.KeyEvent;
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
    public JOSMTestRules rules = new JOSMTestRules();

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
    public void testGetCharactersForKeyE00() {
        char deadCircumflex = (char) KeyEvent.VK_DEAD_CIRCUMFLEX;
        char deadGrave = (char) KeyEvent.VK_DEAD_GRAVE;
        char deadCaron = (char) KeyEvent.VK_DEAD_CARON;
        char deadCircumflex2 = 0x2C6;
        char deadGrave2 = 0x2CB;
        char deadCaron2 = 0x2C7;
        testgetCharactersForKeyE00("ar", 'ذ', '>', '`', deadGrave, deadGrave2);
        if (PlatformManager.isPlatformUnixoid()) {
            testgetCharactersForKeyE00("fr_FR", '²', '$', 'œ', '`', deadGrave, deadGrave2);
        } else {
            testgetCharactersForKeyE00("fr_FR", '²', '$', '`', deadGrave, deadGrave2);
        }
        testgetCharactersForKeyE00("fr_CA", '#', '$', '/', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("sq", '\\', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("it", '\\', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("pt", '\\', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("pt_BR", '\'', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("de", deadCircumflex, deadCircumflex2, '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("cs", ';', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("he", '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("hu", '0', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("pl", '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("bs", '¸', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("hr", '¸', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("sl", '¸', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("sr", '¸', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("ro", ']', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("da", '½', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("fo", '½', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("nl", '@', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("et", deadCaron, deadCaron2, '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("is", '°', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("es", '|', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("es_ES", 'º', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("tr", '"', '*', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("de_LU", deadCircumflex, deadCircumflex2, '²', '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("fr_LU", '$', '²', '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("fr_CH", '²', '$', '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("de_CH", deadCircumflex, deadCircumflex2, '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("de_LI", deadCircumflex, deadCircumflex2, '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("fi_FI", '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("sv_SE", '§', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("no_NO", '|', '`', deadGrave, deadGrave2);
        testgetCharactersForKeyE00("sv_NO", '|', '`', deadGrave, deadGrave2);
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
