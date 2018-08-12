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
        testgetCharactersForKeyE00("ar", 'ذ', '>', '`', deadGrave);
        testgetCharactersForKeyE00("fr_FR", '²', '$', '`', deadGrave);
        testgetCharactersForKeyE00("fr_CA", '#', '$', '/', '`', deadGrave);
        testgetCharactersForKeyE00("sq", '\\', '`', deadGrave);
        testgetCharactersForKeyE00("it", '\\', '`', deadGrave);
        testgetCharactersForKeyE00("pt", '\\', '`', deadGrave);
        testgetCharactersForKeyE00("pt_BR", '\'', '`', deadGrave);
        testgetCharactersForKeyE00("de", deadCircumflex, '`', deadGrave);
        testgetCharactersForKeyE00("cs", ';', '`', deadGrave);
        testgetCharactersForKeyE00("he", '`', deadGrave);
        testgetCharactersForKeyE00("hu", '0', '`', deadGrave);
        testgetCharactersForKeyE00("pl", '`', deadGrave);
        testgetCharactersForKeyE00("bs", '¸', '`', deadGrave);
        testgetCharactersForKeyE00("hr", '¸', '`', deadGrave);
        testgetCharactersForKeyE00("sl", '¸', '`', deadGrave);
        testgetCharactersForKeyE00("sr", '¸', '`', deadGrave);
        testgetCharactersForKeyE00("ro", ']', '`', deadGrave);
        testgetCharactersForKeyE00("da", '½', '`', deadGrave);
        testgetCharactersForKeyE00("fo", '½', '`', deadGrave);
        testgetCharactersForKeyE00("nl", '@', '`', deadGrave);
        testgetCharactersForKeyE00("et", deadCaron, '`', deadGrave);
        testgetCharactersForKeyE00("is", '°', '`', deadGrave);
        testgetCharactersForKeyE00("es", '|', '`', deadGrave);
        testgetCharactersForKeyE00("es_ES", 'º', '`', deadGrave);
        testgetCharactersForKeyE00("tr", '"', '*', '`', deadGrave);
        testgetCharactersForKeyE00("de_LU", deadCircumflex, '²', '§', '`', deadGrave);
        testgetCharactersForKeyE00("fr_LU", '$', '²', '§', '`', deadGrave);
        testgetCharactersForKeyE00("fr_CH", '²', '$', '§', '`', deadGrave);
        testgetCharactersForKeyE00("de_CH", deadCircumflex, '§', '`', deadGrave);
        testgetCharactersForKeyE00("de_LI", deadCircumflex, '§', '`', deadGrave);
        testgetCharactersForKeyE00("fi_FI", '§', '`', deadGrave);
        testgetCharactersForKeyE00("sv_SE", '§', '`', deadGrave);
        testgetCharactersForKeyE00("no_NO", '|', '`', deadGrave);
        testgetCharactersForKeyE00("sv_NO", '|', '`', deadGrave);
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
