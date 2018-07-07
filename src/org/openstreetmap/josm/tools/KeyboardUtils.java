// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.event.KeyEvent;
import java.awt.im.InputContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openstreetmap.josm.Main;

/**
 * Keyboard utils.
 * @since 14012
 */
public final class KeyboardUtils {

    /**
     * The flag for extended key codes.
     */
    public static final int EXTENDED_KEYCODE_FLAG = 0x01000000;

    private static final Map<Integer, Integer> regularKeyCodesMap = new LinkedHashMap<>();
    static {
        // http://hg.openjdk.java.net/jdk/jdk/file/fa2f93f99dbc/src/java.desktop/share/classes/sun/awt/ExtendedKeyCodes.java#l67
        regularKeyCodesMap.put(0x08, KeyEvent.VK_BACK_SPACE);
        regularKeyCodesMap.put(0x09, KeyEvent.VK_TAB);
        regularKeyCodesMap.put(0x0a, KeyEvent.VK_ENTER);
        regularKeyCodesMap.put(0x1B, KeyEvent.VK_ESCAPE);
        regularKeyCodesMap.put(0x20AC, KeyEvent.VK_EURO_SIGN);
        regularKeyCodesMap.put(0x20, KeyEvent.VK_SPACE);
        regularKeyCodesMap.put(0x21, KeyEvent.VK_EXCLAMATION_MARK);
        regularKeyCodesMap.put(0x22, KeyEvent.VK_QUOTEDBL);
        regularKeyCodesMap.put(0x23, KeyEvent.VK_NUMBER_SIGN);
        regularKeyCodesMap.put(0x24, KeyEvent.VK_DOLLAR);
        regularKeyCodesMap.put(0x26, KeyEvent.VK_AMPERSAND);
        regularKeyCodesMap.put(0x27, KeyEvent.VK_QUOTE);
        regularKeyCodesMap.put(0x28, KeyEvent.VK_LEFT_PARENTHESIS);
        regularKeyCodesMap.put(0x29, KeyEvent.VK_RIGHT_PARENTHESIS);
        regularKeyCodesMap.put(0x2A, KeyEvent.VK_ASTERISK);
        regularKeyCodesMap.put(0x2B, KeyEvent.VK_PLUS);
        regularKeyCodesMap.put(0x2C, KeyEvent.VK_COMMA);
        regularKeyCodesMap.put(0x2D, KeyEvent.VK_MINUS);
        regularKeyCodesMap.put(0x2E, KeyEvent.VK_PERIOD);
        regularKeyCodesMap.put(0x2F, KeyEvent.VK_SLASH);
        regularKeyCodesMap.put(0x30, KeyEvent.VK_0);
        regularKeyCodesMap.put(0x31, KeyEvent.VK_1);
        regularKeyCodesMap.put(0x32, KeyEvent.VK_2);
        regularKeyCodesMap.put(0x33, KeyEvent.VK_3);
        regularKeyCodesMap.put(0x34, KeyEvent.VK_4);
        regularKeyCodesMap.put(0x35, KeyEvent.VK_5);
        regularKeyCodesMap.put(0x36, KeyEvent.VK_6);
        regularKeyCodesMap.put(0x37, KeyEvent.VK_7);
        regularKeyCodesMap.put(0x38, KeyEvent.VK_8);
        regularKeyCodesMap.put(0x39, KeyEvent.VK_9);
        regularKeyCodesMap.put(0x3A, KeyEvent.VK_COLON);
        regularKeyCodesMap.put(0x3B, KeyEvent.VK_SEMICOLON);
        regularKeyCodesMap.put(0x3C, KeyEvent.VK_LESS);
        regularKeyCodesMap.put(0x3D, KeyEvent.VK_EQUALS);
        regularKeyCodesMap.put(0x3E, KeyEvent.VK_GREATER);
        regularKeyCodesMap.put(0x40, KeyEvent.VK_AT);
        regularKeyCodesMap.put(0x41, KeyEvent.VK_A);
        regularKeyCodesMap.put(0x42, KeyEvent.VK_B);
        regularKeyCodesMap.put(0x43, KeyEvent.VK_C);
        regularKeyCodesMap.put(0x44, KeyEvent.VK_D);
        regularKeyCodesMap.put(0x45, KeyEvent.VK_E);
        regularKeyCodesMap.put(0x46, KeyEvent.VK_F);
        regularKeyCodesMap.put(0x47, KeyEvent.VK_G);
        regularKeyCodesMap.put(0x48, KeyEvent.VK_H);
        regularKeyCodesMap.put(0x49, KeyEvent.VK_I);
        regularKeyCodesMap.put(0x4A, KeyEvent.VK_J);
        regularKeyCodesMap.put(0x4B, KeyEvent.VK_K);
        regularKeyCodesMap.put(0x4C, KeyEvent.VK_L);
        regularKeyCodesMap.put(0x4D, KeyEvent.VK_M);
        regularKeyCodesMap.put(0x4E, KeyEvent.VK_N);
        regularKeyCodesMap.put(0x4F, KeyEvent.VK_O);
        regularKeyCodesMap.put(0x50, KeyEvent.VK_P);
        regularKeyCodesMap.put(0x51, KeyEvent.VK_Q);
        regularKeyCodesMap.put(0x52, KeyEvent.VK_R);
        regularKeyCodesMap.put(0x53, KeyEvent.VK_S);
        regularKeyCodesMap.put(0x54, KeyEvent.VK_T);
        regularKeyCodesMap.put(0x55, KeyEvent.VK_U);
        regularKeyCodesMap.put(0x56, KeyEvent.VK_V);
        regularKeyCodesMap.put(0x57, KeyEvent.VK_W);
        regularKeyCodesMap.put(0x58, KeyEvent.VK_X);
        regularKeyCodesMap.put(0x59, KeyEvent.VK_Y);
        regularKeyCodesMap.put(0x5A, KeyEvent.VK_Z);
        regularKeyCodesMap.put(0x5B, KeyEvent.VK_OPEN_BRACKET);
        regularKeyCodesMap.put(0x5C, KeyEvent.VK_BACK_SLASH);
        regularKeyCodesMap.put(0x5D, KeyEvent.VK_CLOSE_BRACKET);
        regularKeyCodesMap.put(0x5E, KeyEvent.VK_CIRCUMFLEX);
        regularKeyCodesMap.put(0x5F, KeyEvent.VK_UNDERSCORE);
        regularKeyCodesMap.put(0x60, KeyEvent.VK_BACK_QUOTE);
        regularKeyCodesMap.put(0x61, KeyEvent.VK_A);
        regularKeyCodesMap.put(0x62, KeyEvent.VK_B);
        regularKeyCodesMap.put(0x63, KeyEvent.VK_C);
        regularKeyCodesMap.put(0x64, KeyEvent.VK_D);
        regularKeyCodesMap.put(0x65, KeyEvent.VK_E);
        regularKeyCodesMap.put(0x66, KeyEvent.VK_F);
        regularKeyCodesMap.put(0x67, KeyEvent.VK_G);
        regularKeyCodesMap.put(0x68, KeyEvent.VK_H);
        regularKeyCodesMap.put(0x69, KeyEvent.VK_I);
        regularKeyCodesMap.put(0x6A, KeyEvent.VK_J);
        regularKeyCodesMap.put(0x6B, KeyEvent.VK_K);
        regularKeyCodesMap.put(0x6C, KeyEvent.VK_L);
        regularKeyCodesMap.put(0x6D, KeyEvent.VK_M);
        regularKeyCodesMap.put(0x6E, KeyEvent.VK_N);
        regularKeyCodesMap.put(0x6F, KeyEvent.VK_O);
        regularKeyCodesMap.put(0x70, KeyEvent.VK_P);
        regularKeyCodesMap.put(0x71, KeyEvent.VK_Q);
        regularKeyCodesMap.put(0x72, KeyEvent.VK_R);
        regularKeyCodesMap.put(0x73, KeyEvent.VK_S);
        regularKeyCodesMap.put(0x74, KeyEvent.VK_T);
        regularKeyCodesMap.put(0x75, KeyEvent.VK_U);
        regularKeyCodesMap.put(0x76, KeyEvent.VK_V);
        regularKeyCodesMap.put(0x77, KeyEvent.VK_W);
        regularKeyCodesMap.put(0x78, KeyEvent.VK_X);
        regularKeyCodesMap.put(0x79, KeyEvent.VK_Y);
        regularKeyCodesMap.put(0x7A, KeyEvent.VK_Z);
        regularKeyCodesMap.put(0x7B, KeyEvent.VK_BRACELEFT);
        regularKeyCodesMap.put(0x7D, KeyEvent.VK_BRACERIGHT);
        regularKeyCodesMap.put(0x7F, KeyEvent.VK_DELETE);
        regularKeyCodesMap.put(0xA1, KeyEvent.VK_INVERTED_EXCLAMATION_MARK);
    }

    private KeyboardUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Returns Keycodes declared in {@link KeyEvent} with corresponding Unicode values.
     * @return Map of KeyEvent VK_ characters constants indexed by their unicode value
     */
    public static Map<Integer, Integer> getRegularKeyCodesMap() {
        return regularKeyCodesMap;
    }

    /**
     * Returns the plausible characters expected to be displayed for the given physical key and current input locale.
     * Physical keys are defined as per <a href="https://en.wikipedia.org/wiki/ISO/IEC_9995#ISO/IEC_9995-2">ISO/IEC 9995-2</a>
     * keyboard layout. Only E00 is currently supported.
     * @param row row letter as per ISO/IEC 9995-2 (A to E)
     * @param column column number as per ISO/IEC 9995-2 (0 to 14, plus 99)
     * @return the plausible characters expected to be displayed for the given physical key and current input locale
     */
    public static List<Character> getCharactersForKey(char row, int column) {
        return getCharactersForKey(row, column, InputContext.getInstance().getLocale());
    }

    /**
     * Returns the plausible characters expected to be displayed for the given physical key and locale.
     * Physical keys are defined as per <a href="https://en.wikipedia.org/wiki/ISO/IEC_9995#ISO/IEC_9995-2">ISO/IEC 9995-2</a>
     * keyboard layout. Only E00 is currently supported.
     * @param row row letter as per ISO/IEC 9995-2 (A to E)
     * @param column column number as per ISO/IEC 9995-2 (0 to 14, plus 99)
     * @param l locale (defining language and country)
     * @return the plausible characters expected to be displayed for the given physical key and locale
     */
    public static List<Character> getCharactersForKey(char row, int column, Locale l) {
        if ('E' == row && 0 == column) {
            List<Character> result = new ArrayList<>();

            // One good resource:
            // https://docs.microsoft.com/en-us/globalization/windows-keyboard-layouts

            // By language. Codes noted below are extended ones obtained with Windows OSK
            switch (l.getLanguage()) {
            case "ar": // Arabic
                result.add('ذ'); // https://docs.microsoft.com/fr-fr/globalization/keyboards/kbda1.html
                result.add('>'); // https://docs.microsoft.com/fr-fr/globalization/keyboards/kbda2.html
                break;
            case "fr": // French
                if ("CA".equals(l.getCountry())) {
                    // Canada, see https://en.wikipedia.org/wiki/QWERTY#Canadian_French
                    result.add('#');
                } else if (!"LU".equals(l.getCountry())) {
                    // France and Belgium, https://en.wikipedia.org/wiki/AZERTY
                    result.add('²'); // 10000B2
                }
                // BÉPO, https://en.wikipedia.org/wiki/Keyboard_layout#B%C3%89PO
                result.add('$');
                break;
            case "sq": // Albanian
            case "it": // Italian
            case "pt": // Portuguese
                if ("BR".equals(l.getCountry())) {
                    // Brazil, https://en.wikipedia.org/wiki/QWERTY#Brazil
                    result.add('\'');
                } else {
                    // Albanian, https://en.wikipedia.org/wiki/Albanian_keyboard_layout
                    //           https://docs.microsoft.com/fr-fr/globalization/keyboards/kbdal.html
                    // Italian, https://en.wikipedia.org/wiki/QWERTY#Italian
                    // Portugal, https://en.wikipedia.org/wiki/QWERTY#Portugal
                    result.add('\\');
                }
                break;
            case "de": // German
                // https://en.wikipedia.org/wiki/German_keyboard_layout
                result.add('^');
                break;
            case "cs": // Czech
            case "he": // Hebrew
                // https://en.wikipedia.org/wiki/QWERTZ#Czech_(QWERTZ)
                // https://en.wikipedia.org/wiki/Hebrew_keyboard
                result.add(';');
                break;
            case "hu":
                // Hungary, https://en.wikipedia.org/wiki/QWERTZ#Hungary
                result.add('0');
                break;
            case "pl": // Polish
                // Poland, https://en.wikipedia.org/wiki/QWERTZ#Poland
                result.add('µ');
                result.add('^');
                result.add('˛'); // https://en.wikipedia.org/wiki/Ogonek
                break;
            case "bs": // Bosnian
            case "hr": // Croatian
            case "sl": // Slovenian
            case "sr": // Serbian
                // https://en.wikipedia.org/wiki/QWERTZ#South_Slavic_Latin
                result.add('¸'); // Copied from https://upload.wikimedia.org/wikipedia/commons/2/2e/KB_Slovene.svg
                break;
            case "ro": // Romanian
                // https://en.wikipedia.org/wiki/QWERTZ#Romanian
                result.add(']');
                break;
            case "da": // Danish
            case "fo": // Faroese
                // https://en.wikipedia.org/wiki/QWERTY#Danish
                // https://en.wikipedia.org/wiki/QWERTY#Faroese
                result.add('½'); // Copied from https://upload.wikimedia.org/wikipedia/commons/4/46/KB_Danish_text.svg
                break;
            case "nl": // Dutch
                // https://en.wikipedia.org/wiki/QWERTY#Dutch_(Netherlands)
                result.add('@');
                break;
            case "et": // Estonian
                // https://en.wikipedia.org/wiki/QWERTY#Estonian
                result.add('ˇ'); // https://en.wikipedia.org/wiki/Caron
                break;
            case "is": // Icelandic
                // https://en.wikipedia.org/wiki/Icelandic_keyboard_layout
                result.add('°'); // https://en.wikipedia.org/wiki/Ring_(diacritic)
                break;
            case "es": // Spanish
                // Latin America only, https://en.wikipedia.org/wiki/QWERTY#Latin_America
                if (!"ES".equals(l.getCountry())) {
                    result.add('|');
                }
                break;
            case "tr": // Turkish
                // https://en.wikipedia.org/wiki/QWERTY#Turkish_(Q-keyboard)
                // https://en.wikipedia.org/wiki/Keyboard_layout#Turkish_(F-keyboard)
                result.add('"');
                result.add('*');
                break;
            }

            // By country regardless of language
            switch (l.getCountry()) {
            case "LU": // Luxembourg
                result.add('²');
                // Fall-through
            case "CH": // Swiss
            case "LI": // Liechenstein
            case "FI": // Finland
            case "SE": // Sweden
                // https://en.wikipedia.org/wiki/QWERTZ#Switzerland_(German,_French,_Italian,_Romansh),_Liechtenstein,_Luxembourg
                // https://en.wikipedia.org/wiki/QWERTY#Finnish_multilingual
                // https://en.wikipedia.org/wiki/QWERTY#Swedish
                result.add('§');
                break;
            case "CA": // Canada
                // https://en.wikipedia.org/wiki/CSA_keyboard
                result.add('/'); // 2F
                break;
            case "NO": // Norway
                // https://en.wikipedia.org/wiki/QWERTY#Norwegian
                result.add('|');
                break;
            case "ES": // Spain
                // https://en.wikipedia.org/wiki/QWERTY#Spain,_also_known_as_Spanish_(International_sort)
                result.add('º'); // https://en.wikipedia.org/wiki/Ordinal_indicator
                break;
            }

            // UK Apple, https://en.wikipedia.org/wiki/QWERTY#UK_Apple_keyboard
            // International English Apple, https://en.wikipedia.org/wiki/QWERTY#Apple_International_English_Keyboard
            if (Main.isPlatformOsx()) {
                result.add('§');
            }

            // Add default US QWERTY keys, https://en.wikipedia.org/wiki/QWERTY
            // Works also for Dvorak, https://en.wikipedia.org/wiki/Dvorak_Simplified_Keyboard
            result.add('`');
            result.add('~');
            return result;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the extended key codes that we are susceptible to receive given the locale.
     * @param locale locale
     * @return the extended key codes that we are susceptible to receive given the locale
     */
    public static Map<Integer, Character> getExtendedKeyCodes(Locale locale) {
        // Last update: 2017-09-12
        // http://hg.openjdk.java.net/jdk/jdk/file/fa2f93f99dbc/src/java.desktop/share/classes/sun/awt/ExtendedKeyCodes.java#l166
        // Characters found at least on one keyboard layout
        Map<Integer, Character> map = new LinkedHashMap<>();
        // Add latin characters and symbols for everyone
        addLatinCharacters(map);
        addSymbolCharacters(map);

        // Detect current script
        // https://en.wikipedia.org/wiki/ISO_15924#List_of_codes
        switch (locale.getScript()) {
            case "Arab": // https://en.wikipedia.org/wiki/Arabic_script
            case "Aran": // https://en.wikipedia.org/wiki/Nasta%CA%BFl%C4%ABq_script
                addArabicCharacters(map);
                break;
            case "Armn": // https://en.wikipedia.org/wiki/Armenian_alphabet
                addArmenianCharacters(map);
                break;
            case "Cyrl": // https://en.wikipedia.org/wiki/Cyrillic_script
                addCyrillicCharacters(map);
                break;
            case "Geok": // https://en.wikipedia.org/wiki/Georgian_scripts#Nuskhuri
            case "Geor": // https://en.wikipedia.org/wiki/Georgian_scripts#Mkhedruli
                addGeorgianCharacters(map);
                break;
            case "Grek": // https://en.wikipedia.org/wiki/Greek_alphabet
                addGreekCharacters(map);
                break;
            case "Hebr": // https://en.wikipedia.org/wiki/Hebrew_alphabet
                addHebrewCharacters(map);
                break;
            case "Hira": // https://en.wikipedia.org/wiki/Hiragana
            case "Jpan": // https://en.wikipedia.org/wiki/Japanese_writing_system
            case "Kana": // https://en.wikipedia.org/wiki/Katakana
                addJapaneseCharacters(map);
                break;
            case "Thai": // https://en.wikipedia.org/wiki/Thai_alphabet
                addThaiCharacters(map);
                break;
        }

        return map;
    }

    static void addLatinCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x0060, '`'); // GRAVE ACCENT
        map.put(EXTENDED_KEYCODE_FLAG + 0x007C, '|'); // VERTICAL LINE
        map.put(EXTENDED_KEYCODE_FLAG + 0x007E, '~'); // TILDE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00A2, '¢'); // CENT SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00A3, '£'); // POUND SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00A5, '¥'); // YEN SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00A7, '§'); // SECTION SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00A8, '¨'); // DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00AB, '«'); // LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B0, '°'); // DEGREE SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B1, '±'); // PLUS-MINUS SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B2, '²'); // SUPERSCRIPT TWO
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B3, '³'); // SUPERSCRIPT THREE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B4, '´'); // ACUTE ACCENT
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B5, 'µ'); // MICRO SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B6, '¶'); // PILCROW SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B7, '·'); // MIDDLE DOT
        map.put(EXTENDED_KEYCODE_FLAG + 0x00B9, '¹'); // SUPERSCRIPT ONE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00BA, 'º'); // MASCULINE ORDINAL INDICATOR
        map.put(EXTENDED_KEYCODE_FLAG + 0x00BB, '»'); // RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x00BC, '¼'); // VULGAR FRACTION ONE QUARTER
        map.put(EXTENDED_KEYCODE_FLAG + 0x00BD, '½'); // VULGAR FRACTION ONE HALF
        map.put(EXTENDED_KEYCODE_FLAG + 0x00BE, '¾'); // VULGAR FRACTION THREE QUARTERS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00BF, '¿'); // INVERTED QUESTION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x00C4, 'Ä'); // LATIN CAPITAL LETTER A WITH DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00C5, 'Å'); // LATIN CAPITAL LETTER A WITH RING ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00C6, 'Æ'); // LATIN CAPITAL LETTER AE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00C7, 'Ç'); // LATIN CAPITAL LETTER C WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x00D1, 'Ñ'); // LATIN CAPITAL LETTER N WITH TILDE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00D6, 'Ö'); // LATIN CAPITAL LETTER O WITH DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00D7, '×'); // MULTIPLICATION SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00D8, 'Ø'); // LATIN CAPITAL LETTER O WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00DF, 'ß'); // LATIN SMALL LETTER SHARP S
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E0, 'à'); // LATIN SMALL LETTER A WITH GRAVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E1, 'á'); // LATIN SMALL LETTER A WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E2, 'â'); // LATIN SMALL LETTER A WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E4, 'ä'); // LATIN SMALL LETTER A WITH DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E5, 'å'); // LATIN SMALL LETTER A WITH RING ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E6, 'æ'); // LATIN SMALL LETTER AE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E7, 'ç'); // LATIN SMALL LETTER C WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E8, 'è'); // LATIN SMALL LETTER E WITH GRAVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00E9, 'é'); // LATIN SMALL LETTER E WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00EA, 'ê'); // LATIN SMALL LETTER E WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x00EB, 'ë'); // LATIN SMALL LETTER E WITH DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00EC, 'ì'); // LATIN SMALL LETTER I WITH GRAVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00ED, 'í'); // LATIN SMALL LETTER I WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00EE, 'î'); // LATIN SMALL LETTER I WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F0, 'ð'); // LATIN SMALL LETTER ETH
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F1, 'ñ'); // LATIN SMALL LETTER N WITH TILDE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F2, 'ò'); // LATIN SMALL LETTER O WITH GRAVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F3, 'ó'); // LATIN SMALL LETTER O WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F4, 'ô'); // LATIN SMALL LETTER O WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F5, 'õ'); // LATIN SMALL LETTER O WITH TILDE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F6, 'ö'); // LATIN SMALL LETTER O WITH DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F7, '÷'); // DIVISION SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F8, 'ø'); // LATIN SMALL LETTER O WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00F9, 'ù'); // LATIN SMALL LETTER U WITH GRAVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00FA, 'ú'); // LATIN SMALL LETTER U WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00FB, 'û'); // LATIN SMALL LETTER U WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x00FC, 'ü'); // LATIN SMALL LETTER U WITH DIAERESIS
        map.put(EXTENDED_KEYCODE_FLAG + 0x00FD, 'ý'); // LATIN SMALL LETTER Y WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x00FE, 'þ'); // LATIN SMALL LETTER THORN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0101, 'ā'); // LATIN SMALL LETTER A WITH MACRON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0103, 'ă'); // LATIN SMALL LETTER A WITH BREVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0105, 'ą'); // LATIN SMALL LETTER A WITH OGONEK
        map.put(EXTENDED_KEYCODE_FLAG + 0x0107, 'ć'); // LATIN SMALL LETTER C WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0109, 'ĉ'); // LATIN SMALL LETTER C WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x010B, 'ċ'); // LATIN SMALL LETTER C WITH DOT ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x010D, 'č'); // LATIN SMALL LETTER C WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0111, 'đ'); // LATIN SMALL LETTER D WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0113, 'ē'); // LATIN SMALL LETTER E WITH MACRON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0117, 'ė'); // LATIN SMALL LETTER E WITH DOT ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0119, 'ę'); // LATIN SMALL LETTER E WITH OGONEK
        map.put(EXTENDED_KEYCODE_FLAG + 0x011B, 'ě'); // LATIN SMALL LETTER E WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x011D, 'ĝ'); // LATIN SMALL LETTER G WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x011F, 'ğ'); // LATIN SMALL LETTER G WITH BREVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0121, 'ġ'); // LATIN SMALL LETTER G WITH DOT ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0123, 'ģ'); // LATIN SMALL LETTER G WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0125, 'ĥ'); // LATIN SMALL LETTER H WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x0127, 'ħ'); // LATIN SMALL LETTER H WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x012B, 'ī'); // LATIN SMALL LETTER I WITH MACRON
        map.put(EXTENDED_KEYCODE_FLAG + 0x012F, 'į'); // LATIN SMALL LETTER I WITH OGONEK
        map.put(EXTENDED_KEYCODE_FLAG + 0x0130, 'İ'); // LATIN CAPITAL LETTER I WITH DOT ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0131, 'ı'); // LATIN SMALL LETTER DOTLESS I
        map.put(EXTENDED_KEYCODE_FLAG + 0x0135, 'ĵ'); // LATIN SMALL LETTER J WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x0137, 'ķ'); // LATIN SMALL LETTER K WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0138, 'ĸ'); // LATIN SMALL LETTER KRA
        map.put(EXTENDED_KEYCODE_FLAG + 0x013C, 'ļ'); // LATIN SMALL LETTER L WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x013E, 'ľ'); // LATIN SMALL LETTER L WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0142, 'ł'); // LATIN SMALL LETTER L WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0146, 'ņ'); // LATIN SMALL LETTER N WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0148, 'ň'); // LATIN SMALL LETTER N WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x014B, 'ŋ'); // LATIN SMALL LETTER ENG
        map.put(EXTENDED_KEYCODE_FLAG + 0x014D, 'ō'); // LATIN SMALL LETTER O WITH MACRON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0151, 'ő'); // LATIN SMALL LETTER O WITH DOUBLE ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0153, 'œ'); // LATIN SMALL LIGATURE OE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0157, 'ŗ'); // LATIN SMALL LETTER R WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0159, 'ř'); // LATIN SMALL LETTER R WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x015B, 'ś'); // LATIN SMALL LETTER S WITH ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x015D, 'ŝ'); // LATIN SMALL LETTER S WITH CIRCUMFLEX
        map.put(EXTENDED_KEYCODE_FLAG + 0x015F, 'ş'); // LATIN SMALL LETTER S WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0161, 'š'); // LATIN SMALL LETTER S WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0163, 'ţ'); // LATIN SMALL LETTER T WITH CEDILLA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0165, 'ť'); // LATIN SMALL LETTER T WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0167, 'ŧ'); // LATIN SMALL LETTER T WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x016B, 'ū'); // LATIN SMALL LETTER U WITH MACRON
        map.put(EXTENDED_KEYCODE_FLAG + 0x016D, 'ŭ'); // LATIN SMALL LETTER U WITH BREVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x016F, 'ů'); // LATIN SMALL LETTER U WITH RING ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0171, 'ű'); // LATIN SMALL LETTER U WITH DOUBLE ACUTE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0173, 'ų'); // LATIN SMALL LETTER U WITH OGONEK
        map.put(EXTENDED_KEYCODE_FLAG + 0x017C, 'ż'); // LATIN SMALL LETTER Z WITH DOT ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x017E, 'ž'); // LATIN SMALL LETTER Z WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x01A1, 'ơ'); // LATIN SMALL LETTER O WITH HORN
        map.put(EXTENDED_KEYCODE_FLAG + 0x01B0, 'ư'); // LATIN SMALL LETTER U WITH HORN
        map.put(EXTENDED_KEYCODE_FLAG + 0x01E7, 'ǧ'); // LATIN SMALL LETTER G WITH CARON
        map.put(EXTENDED_KEYCODE_FLAG + 0x0259, 'ə'); // LATIN SMALL LETTER SCHWA
        map.put(EXTENDED_KEYCODE_FLAG + 0x02D9, '˙'); // DOT ABOVE
        map.put(EXTENDED_KEYCODE_FLAG + 0x02DB, '˛'); // OGONEK
        map.put(EXTENDED_KEYCODE_FLAG + 0x1EB9, 'ẹ'); // LATIN SMALL LETTER E WITH DOT BELOW
        map.put(EXTENDED_KEYCODE_FLAG + 0x1ECB, 'ị'); // LATIN SMALL LETTER I WITH DOT BELOW
        map.put(EXTENDED_KEYCODE_FLAG + 0x1ECD, 'ọ'); // LATIN SMALL LETTER O WITH DOT BELOW
        map.put(EXTENDED_KEYCODE_FLAG + 0x1EE5, 'ụ'); // LATIN SMALL LETTER U WITH DOT BELOW
    }

    static void addGreekCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B1, 'α'); // GREEK SMALL LETTER ALPHA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B2, 'β'); // GREEK SMALL LETTER BETA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B3, 'γ'); // GREEK SMALL LETTER GAMMA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B4, 'δ'); // GREEK SMALL LETTER DELTA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B5, 'ε'); // GREEK SMALL LETTER EPSILON
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B6, 'ζ'); // GREEK SMALL LETTER ZETA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B7, 'η'); // GREEK SMALL LETTER ETA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B8, 'θ'); // GREEK SMALL LETTER THETA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03B9, 'ι'); // GREEK SMALL LETTER IOTA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03BA, 'κ'); // GREEK SMALL LETTER KAPPA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03BB, 'λ'); // GREEK SMALL LETTER LAMDA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03BC, 'μ'); // GREEK SMALL LETTER MU
        map.put(EXTENDED_KEYCODE_FLAG + 0x03BD, 'ν'); // GREEK SMALL LETTER NU
        map.put(EXTENDED_KEYCODE_FLAG + 0x03BE, 'ξ'); // GREEK SMALL LETTER XI
        map.put(EXTENDED_KEYCODE_FLAG + 0x03BF, 'ο'); // GREEK SMALL LETTER OMICRON
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C0, 'π'); // GREEK SMALL LETTER PI
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C1, 'ρ'); // GREEK SMALL LETTER RHO
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C2, 'ς'); // GREEK SMALL LETTER FINAL SIGMA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C3, 'σ'); // GREEK SMALL LETTER SIGMA
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C4, 'τ'); // GREEK SMALL LETTER TAU
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C5, 'υ'); // GREEK SMALL LETTER UPSILON
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C6, 'φ'); // GREEK SMALL LETTER PHI
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C7, 'χ'); // GREEK SMALL LETTER CHI
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C8, 'ψ'); // GREEK SMALL LETTER PSI
        map.put(EXTENDED_KEYCODE_FLAG + 0x03C9, 'ω'); // GREEK SMALL LETTER OMEGA
    }

    static void addCyrillicCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x0430, 'а'); // CYRILLIC SMALL LETTER A
        map.put(EXTENDED_KEYCODE_FLAG + 0x0431, 'б'); // CYRILLIC SMALL LETTER BE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0432, 'в'); // CYRILLIC SMALL LETTER VE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0433, 'г'); // CYRILLIC SMALL LETTER GHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0434, 'д'); // CYRILLIC SMALL LETTER DE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0435, 'е'); // CYRILLIC SMALL LETTER IE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0436, 'ж'); // CYRILLIC SMALL LETTER ZHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0437, 'з'); // CYRILLIC SMALL LETTER ZE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0438, 'и'); // CYRILLIC SMALL LETTER I
        map.put(EXTENDED_KEYCODE_FLAG + 0x0439, 'й'); // CYRILLIC SMALL LETTER SHORT I
        map.put(EXTENDED_KEYCODE_FLAG + 0x043A, 'к'); // CYRILLIC SMALL LETTER KA
        map.put(EXTENDED_KEYCODE_FLAG + 0x043B, 'л'); // CYRILLIC SMALL LETTER EL
        map.put(EXTENDED_KEYCODE_FLAG + 0x043C, 'м'); // CYRILLIC SMALL LETTER EM
        map.put(EXTENDED_KEYCODE_FLAG + 0x043D, 'н'); // CYRILLIC SMALL LETTER EN
        map.put(EXTENDED_KEYCODE_FLAG + 0x043E, 'о'); // CYRILLIC SMALL LETTER O
        map.put(EXTENDED_KEYCODE_FLAG + 0x043F, 'п'); // CYRILLIC SMALL LETTER PE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0440, 'р'); // CYRILLIC SMALL LETTER ER
        map.put(EXTENDED_KEYCODE_FLAG + 0x0441, 'с'); // CYRILLIC SMALL LETTER ES
        map.put(EXTENDED_KEYCODE_FLAG + 0x0442, 'т'); // CYRILLIC SMALL LETTER TE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0443, 'у'); // CYRILLIC SMALL LETTER U
        map.put(EXTENDED_KEYCODE_FLAG + 0x0444, 'ф'); // CYRILLIC SMALL LETTER EF
        map.put(EXTENDED_KEYCODE_FLAG + 0x0445, 'х'); // CYRILLIC SMALL LETTER HA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0446, 'ц'); // CYRILLIC SMALL LETTER TSE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0447, 'ч'); // CYRILLIC SMALL LETTER CHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0448, 'ш'); // CYRILLIC SMALL LETTER SHA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0449, 'щ'); // CYRILLIC SMALL LETTER SHCHA
        map.put(EXTENDED_KEYCODE_FLAG + 0x044A, 'ъ'); // CYRILLIC SMALL LETTER HARD SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x044B, 'ы'); // CYRILLIC SMALL LETTER YERU
        map.put(EXTENDED_KEYCODE_FLAG + 0x044C, 'ь'); // CYRILLIC SMALL LETTER SOFT SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x044D, 'э'); // CYRILLIC SMALL LETTER E
        map.put(EXTENDED_KEYCODE_FLAG + 0x044E, 'ю'); // CYRILLIC SMALL LETTER YU
        map.put(EXTENDED_KEYCODE_FLAG + 0x044F, 'я'); // CYRILLIC SMALL LETTER YA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0451, 'ё'); // CYRILLIC SMALL LETTER IO
        map.put(EXTENDED_KEYCODE_FLAG + 0x0452, 'ђ'); // CYRILLIC SMALL LETTER DJE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0453, 'ѓ'); // CYRILLIC SMALL LETTER GJE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0454, 'є'); // CYRILLIC SMALL LETTER UKRAINIAN IE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0455, 'ѕ'); // CYRILLIC SMALL LETTER DZE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0456, 'і'); // CYRILLIC SMALL LETTER BYELORUSSIAN-UKRAINIAN I
        map.put(EXTENDED_KEYCODE_FLAG + 0x0457, 'ї'); // CYRILLIC SMALL LETTER YI
        map.put(EXTENDED_KEYCODE_FLAG + 0x0458, 'ј'); // CYRILLIC SMALL LETTER JE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0459, 'љ'); // CYRILLIC SMALL LETTER LJE
        map.put(EXTENDED_KEYCODE_FLAG + 0x045A, 'њ'); // CYRILLIC SMALL LETTER NJE
        map.put(EXTENDED_KEYCODE_FLAG + 0x045B, 'ћ'); // CYRILLIC SMALL LETTER TSHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x045C, 'ќ'); // CYRILLIC SMALL LETTER KJE
        map.put(EXTENDED_KEYCODE_FLAG + 0x045E, 'ў'); // CYRILLIC SMALL LETTER SHORT U
        map.put(EXTENDED_KEYCODE_FLAG + 0x045F, 'џ'); // CYRILLIC SMALL LETTER DZHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0491, 'ґ'); // CYRILLIC SMALL LETTER GHE WITH UPTURN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0493, 'ғ'); // CYRILLIC SMALL LETTER GHE WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x0497, 'җ'); // CYRILLIC SMALL LETTER ZHE WITH DESCENDER
        map.put(EXTENDED_KEYCODE_FLAG + 0x049B, 'қ'); // CYRILLIC SMALL LETTER KA WITH DESCENDER
        map.put(EXTENDED_KEYCODE_FLAG + 0x049D, 'ҝ'); // CYRILLIC SMALL LETTER KA WITH VERTICAL STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x04A3, 'ң'); // CYRILLIC SMALL LETTER EN WITH DESCENDER
        map.put(EXTENDED_KEYCODE_FLAG + 0x04AF, 'ү'); // CYRILLIC SMALL LETTER STRAIGHT U
        map.put(EXTENDED_KEYCODE_FLAG + 0x04B1, 'ұ'); // CYRILLIC SMALL LETTER STRAIGHT U WITH STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x04B3, 'ҳ'); // CYRILLIC SMALL LETTER HA WITH DESCENDER
        map.put(EXTENDED_KEYCODE_FLAG + 0x04B9, 'ҹ'); // CYRILLIC SMALL LETTER CHE WITH VERTICAL STROKE
        map.put(EXTENDED_KEYCODE_FLAG + 0x04BB, 'һ'); // CYRILLIC SMALL LETTER SHHA
        map.put(EXTENDED_KEYCODE_FLAG + 0x04D9, 'ә'); // CYRILLIC SMALL LETTER SCHWA
        map.put(EXTENDED_KEYCODE_FLAG + 0x04E9, 'ө'); // CYRILLIC SMALL LETTER BARRED O
    }

    static void addArmenianCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x055A, '՚'); // ARMENIAN APOSTROPHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x055B, '՛'); // ARMENIAN EMPHASIS MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x055C, '՜'); // ARMENIAN EXCLAMATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x055D, '՝'); // ARMENIAN COMMA
        map.put(EXTENDED_KEYCODE_FLAG + 0x055E, '՞'); // ARMENIAN QUESTION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x055F, '՟'); // ARMENIAN ABBREVIATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x0561, 'ա'); // ARMENIAN SMALL LETTER AYB
        map.put(EXTENDED_KEYCODE_FLAG + 0x0562, 'բ'); // ARMENIAN SMALL LETTER BEN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0563, 'գ'); // ARMENIAN SMALL LETTER GIM
        map.put(EXTENDED_KEYCODE_FLAG + 0x0564, 'դ'); // ARMENIAN SMALL LETTER DA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0565, 'ե'); // ARMENIAN SMALL LETTER ECH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0566, 'զ'); // ARMENIAN SMALL LETTER ZA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0567, 'է'); // ARMENIAN SMALL LETTER EH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0568, 'ը'); // ARMENIAN SMALL LETTER ET
        map.put(EXTENDED_KEYCODE_FLAG + 0x0569, 'թ'); // ARMENIAN SMALL LETTER TO
        map.put(EXTENDED_KEYCODE_FLAG + 0x056A, 'ժ'); // ARMENIAN SMALL LETTER ZHE
        map.put(EXTENDED_KEYCODE_FLAG + 0x056B, 'ի'); // ARMENIAN SMALL LETTER INI
        map.put(EXTENDED_KEYCODE_FLAG + 0x056C, 'լ'); // ARMENIAN SMALL LETTER LIWN
        map.put(EXTENDED_KEYCODE_FLAG + 0x056D, 'խ'); // ARMENIAN SMALL LETTER XEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x056E, 'ծ'); // ARMENIAN SMALL LETTER CA
        map.put(EXTENDED_KEYCODE_FLAG + 0x056F, 'կ'); // ARMENIAN SMALL LETTER KEN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0570, 'հ'); // ARMENIAN SMALL LETTER HO
        map.put(EXTENDED_KEYCODE_FLAG + 0x0571, 'ձ'); // ARMENIAN SMALL LETTER JA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0572, 'ղ'); // ARMENIAN SMALL LETTER GHAD
        map.put(EXTENDED_KEYCODE_FLAG + 0x0573, 'ճ'); // ARMENIAN SMALL LETTER CHEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0574, 'մ'); // ARMENIAN SMALL LETTER MEN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0575, 'յ'); // ARMENIAN SMALL LETTER YI
        map.put(EXTENDED_KEYCODE_FLAG + 0x0576, 'ն'); // ARMENIAN SMALL LETTER NOW
        map.put(EXTENDED_KEYCODE_FLAG + 0x0577, 'շ'); // ARMENIAN SMALL LETTER SHA
        map.put(EXTENDED_KEYCODE_FLAG + 0x0578, 'ո'); // ARMENIAN SMALL LETTER VO
        map.put(EXTENDED_KEYCODE_FLAG + 0x0579, 'չ'); // ARMENIAN SMALL LETTER CHA
        map.put(EXTENDED_KEYCODE_FLAG + 0x057A, 'պ'); // ARMENIAN SMALL LETTER PEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x057B, 'ջ'); // ARMENIAN SMALL LETTER JHEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x057C, 'ռ'); // ARMENIAN SMALL LETTER RA
        map.put(EXTENDED_KEYCODE_FLAG + 0x057D, 'ս'); // ARMENIAN SMALL LETTER SEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x057E, 'վ'); // ARMENIAN SMALL LETTER VEW
        map.put(EXTENDED_KEYCODE_FLAG + 0x057F, 'տ'); // ARMENIAN SMALL LETTER TIWN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0580, 'ր'); // ARMENIAN SMALL LETTER REH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0581, 'ց'); // ARMENIAN SMALL LETTER CO
        map.put(EXTENDED_KEYCODE_FLAG + 0x0582, 'ւ'); // ARMENIAN SMALL LETTER YIWN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0583, 'փ'); // ARMENIAN SMALL LETTER PIWR
        map.put(EXTENDED_KEYCODE_FLAG + 0x0584, 'ք'); // ARMENIAN SMALL LETTER KEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0585, 'օ'); // ARMENIAN SMALL LETTER OH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0586, 'ֆ'); // ARMENIAN SMALL LETTER FEH
        map.put(EXTENDED_KEYCODE_FLAG + 0x0587, 'և'); // ARMENIAN SMALL LIGATURE ECH YIWN
        map.put(EXTENDED_KEYCODE_FLAG + 0x0589, '։'); // ARMENIAN FULL STOP
    }

    static void addHebrewCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D0, 'א'); // HEBREW LETTER ALEF
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D1, 'ב'); // HEBREW LETTER BET
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D2, 'ג'); // HEBREW LETTER GIMEL
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D3, 'ד'); // HEBREW LETTER DALET
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D4, 'ה'); // HEBREW LETTER HE
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D5, 'ו'); // HEBREW LETTER VAV
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D6, 'ז'); // HEBREW LETTER ZAYIN
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D7, 'ח'); // HEBREW LETTER HET
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D8, 'ט'); // HEBREW LETTER TET
        map.put(EXTENDED_KEYCODE_FLAG + 0x05D9, 'י'); // HEBREW LETTER YOD
        map.put(EXTENDED_KEYCODE_FLAG + 0x05DA, 'ך'); // HEBREW LETTER FINAL KAF
        map.put(EXTENDED_KEYCODE_FLAG + 0x05DB, 'כ'); // HEBREW LETTER KAF
        map.put(EXTENDED_KEYCODE_FLAG + 0x05DC, 'ל'); // HEBREW LETTER LAMED
        map.put(EXTENDED_KEYCODE_FLAG + 0x05DD, 'ם'); // HEBREW LETTER FINAL MEM
        map.put(EXTENDED_KEYCODE_FLAG + 0x05DE, 'מ'); // HEBREW LETTER MEM
        map.put(EXTENDED_KEYCODE_FLAG + 0x05DF, 'ן'); // HEBREW LETTER FINAL NUN
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E0, 'נ'); // HEBREW LETTER NUN
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E1, 'ס'); // HEBREW LETTER SAMEKH
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E2, 'ע'); // HEBREW LETTER AYIN
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E3, 'ף'); // HEBREW LETTER FINAL PE
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E4, 'פ'); // HEBREW LETTER PE
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E5, 'ץ'); // HEBREW LETTER FINAL TSADI
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E6, 'צ'); // HEBREW LETTER TSADI
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E7, 'ק'); // HEBREW LETTER QOF
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E8, 'ר'); // HEBREW LETTER RESH
        map.put(EXTENDED_KEYCODE_FLAG + 0x05E9, 'ש'); // HEBREW LETTER SHIN
        map.put(EXTENDED_KEYCODE_FLAG + 0x05EA, 'ת'); // HEBREW LETTER TAV
    }

    static void addArabicCharacters(Map<Integer, Character> map) {
        /* TODO Arabic characters
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F0, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F1, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F2, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F3, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F4, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F5, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F6, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F7, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F8, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06F9, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0670, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x067E, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0686, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x060C, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06D4, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0660, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0661, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0662, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0663, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0664, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0665, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0666, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0667, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0668, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0669, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x061B, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0621, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0624, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0626, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0627, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0628, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0629, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x062A, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x062B, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x062C, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x062D, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x062E, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x062F, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0630, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0631, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0632, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0633, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0634, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0635, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0636, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0637, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0638, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0639, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x063A, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0641, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0642, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0643, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0644, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0645, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0646, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0647, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0648, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0649, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x064A, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x064E, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x064F, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0650, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0652, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0698, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06A4, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06A9, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06AF, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06BE, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06CC, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06CC, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x06D2, ''); //
        */
    }

    static void addThaiCharacters(Map<Integer, Character> map) {
        /* TODO Thai characters
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E01, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E02, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E03, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E04, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E05, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E07, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E08, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E0A, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E0C, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E14, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E15, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E16, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E17, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E19, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E1A, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E1B, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E1C, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E1D, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E1E, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E1F, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E20, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E21, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E22, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E23, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E25, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E27, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E2A, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E2B, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E2D, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E30, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E31, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E32, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E33, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E34, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E35, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E36, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E37, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E38, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E39, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E3F, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E40, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E41, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E43, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E44, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E45, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E46, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E47, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E48, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E49, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E50, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E51, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E52, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E53, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E54, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E55, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E56, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E57, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E58, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x0E59, ''); //
        */
    }

    static void addGeorgianCharacters(Map<Integer, Character> map) {
        /* TODO Georgian characters
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D0, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D1, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D2, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D3, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D4, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D5, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D6, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D7, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D8, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10D9, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10DA, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10DB, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10DC, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10DD, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10DE, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10DF, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E0, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E1, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E2, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E3, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E4, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E5, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E6, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E7, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E8, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10E9, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10EA, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10EB, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10EC, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10ED, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10EE, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10EF, ''); //
        map.put(EXTENDED_KEYCODE_FLAG + 0x10F0, ''); //
        */
    }

    static void addSymbolCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x2013, '–'); // EN DASH
        map.put(EXTENDED_KEYCODE_FLAG + 0x2015, '―'); // HORIZONTAL BAR
        map.put(EXTENDED_KEYCODE_FLAG + 0x201C, '“'); // LEFT DOUBLE QUOTATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x201D, '”'); // RIGHT DOUBLE QUOTATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x201E, '„'); // DOUBLE LOW-9 QUOTATION MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x20AB, '₫'); // DONG SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x2116, '№'); // NUMERO SIGN
        map.put(EXTENDED_KEYCODE_FLAG + 0x2190, '←'); // LEFTWARDS ARROW
        map.put(EXTENDED_KEYCODE_FLAG + 0x2191, '↑'); // UPWARDS ARROW
        map.put(EXTENDED_KEYCODE_FLAG + 0x2192, '→'); // RIGHTWARDS ARROW
        map.put(EXTENDED_KEYCODE_FLAG + 0x2193, '↓'); // DOWNWARDS ARROW
    }

    static void addJapaneseCharacters(Map<Integer, Character> map) {
        map.put(EXTENDED_KEYCODE_FLAG + 0x309B, '゛'); // KATAKANA-HIRAGANA VOICED SOUND MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x309C, '゜'); // KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
        map.put(EXTENDED_KEYCODE_FLAG + 0x30A2, 'ア'); // KATAKANA LETTER A
        map.put(EXTENDED_KEYCODE_FLAG + 0x30A4, 'イ'); // KATAKANA LETTER I
        map.put(EXTENDED_KEYCODE_FLAG + 0x30A6, 'ウ'); // KATAKANA LETTER U
        map.put(EXTENDED_KEYCODE_FLAG + 0x30A8, 'エ'); // KATAKANA LETTER E
        map.put(EXTENDED_KEYCODE_FLAG + 0x30AA, 'オ'); // KATAKANA LETTER O
        map.put(EXTENDED_KEYCODE_FLAG + 0x30AB, 'カ'); // KATAKANA LETTER KA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30AD, 'キ'); // KATAKANA LETTER KI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30AF, 'ク'); // KATAKANA LETTER KU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30B1, 'ケ'); // KATAKANA LETTER KE
        map.put(EXTENDED_KEYCODE_FLAG + 0x30B3, 'コ'); // KATAKANA LETTER KO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30B5, 'サ'); // KATAKANA LETTER SA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30B7, 'シ'); // KATAKANA LETTER SI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30B9, 'ス'); // KATAKANA LETTER SU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30BB, 'セ'); // KATAKANA LETTER SE
        map.put(EXTENDED_KEYCODE_FLAG + 0x30BD, 'ソ'); // KATAKANA LETTER SO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30BF, 'タ'); // KATAKANA LETTER TA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30C1, 'チ'); // KATAKANA LETTER TI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30C4, 'ツ'); // KATAKANA LETTER TU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30C6, 'テ'); // KATAKANA LETTER TE
        map.put(EXTENDED_KEYCODE_FLAG + 0x30C8, 'ト'); // KATAKANA LETTER TO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30CA, 'ナ'); // KATAKANA LETTER NA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30CB, 'ニ'); // KATAKANA LETTER NI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30CC, 'ヌ'); // KATAKANA LETTER NU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30CD, 'ネ'); // KATAKANA LETTER NE
        map.put(EXTENDED_KEYCODE_FLAG + 0x30CE, 'ノ'); // KATAKANA LETTER NO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30CF, 'ハ'); // KATAKANA LETTER HA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30D2, 'ヒ'); // KATAKANA LETTER HI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30D5, 'フ'); // KATAKANA LETTER HU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30D8, 'ヘ'); // KATAKANA LETTER HE
        map.put(EXTENDED_KEYCODE_FLAG + 0x30DB, 'ホ'); // KATAKANA LETTER HO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30DE, 'マ'); // KATAKANA LETTER MA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30DF, 'ミ'); // KATAKANA LETTER MI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E0, 'ム'); // KATAKANA LETTER MU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E1, 'メ'); // KATAKANA LETTER ME
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E2, 'モ'); // KATAKANA LETTER MO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E4, 'ヤ'); // KATAKANA LETTER YA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E6, 'ユ'); // KATAKANA LETTER YU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E8, 'ヨ'); // KATAKANA LETTER YO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30E9, 'ラ'); // KATAKANA LETTER RA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30EA, 'リ'); // KATAKANA LETTER RI
        map.put(EXTENDED_KEYCODE_FLAG + 0x30EB, 'ル'); // KATAKANA LETTER RU
        map.put(EXTENDED_KEYCODE_FLAG + 0x30EC, 'レ'); // KATAKANA LETTER RE
        map.put(EXTENDED_KEYCODE_FLAG + 0x30ED, 'ロ'); // KATAKANA LETTER RO
        map.put(EXTENDED_KEYCODE_FLAG + 0x30EF, 'ワ'); // KATAKANA LETTER WA
        map.put(EXTENDED_KEYCODE_FLAG + 0x30F3, 'ン'); // KATAKANA LETTER N
        map.put(EXTENDED_KEYCODE_FLAG + 0x30FC, 'ー'); // KATAKANA-HIRAGANA PROLONGED SOUND MARK
    }
}
