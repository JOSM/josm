// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test.TagTest;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.Entities;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetListener;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.CheckGroup;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.widgets.EditableList;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * Check for misspelled or wrong tags
 *
 * @author frsantos
 * @since 3669
 */
public class TagChecker extends TagTest implements TaggingPresetListener {

    /** The config file of ignored tags */
    public static final String IGNORE_FILE = "resource://data/validator/ignoretags.cfg";
    /** The config file of dictionary words */
    public static final String SPELL_FILE = "resource://data/validator/words.cfg";

    /** Normalized keys: the key should be substituted by the value if the key was not found in presets */
    private static final Map<String, String> harmonizedKeys = new HashMap<>();
    /** The spell check preset values which are not stored in TaggingPresets */
    private static volatile HashSet<String> additionalPresetsValueData;
    /** often used tags which are not in presets */
    private static volatile MultiMap<String, String> oftenUsedTags = new MultiMap<>();
    private static final Map<TaggingPreset, List<TaggingPresetItem>> presetIndex = new LinkedHashMap<>();

    private static final Pattern UNWANTED_NON_PRINTING_CONTROL_CHARACTERS = Pattern.compile(
            "[\\x00-\\x09\\x0B\\x0C\\x0E-\\x1F\\x7F\\u200e-\\u200f\\u202a-\\u202e]");

    /** The TagChecker data */
    private static final List<String> ignoreDataStartsWith = new ArrayList<>();
    private static final Set<String> ignoreDataEquals = new HashSet<>();
    private static final List<String> ignoreDataEndsWith = new ArrayList<>();
    private static final List<Tag> ignoreDataTag = new ArrayList<>();
    /** tag keys that have only numerical values in the presets */
    private static final Set<String> ignoreForLevenshtein = new HashSet<>();

    /** The preferences prefix */
    protected static final String PREFIX = ValidatorPrefHelper.PREFIX + "." + TagChecker.class.getSimpleName();

    /**
     * The preference key to check values
     */
    public static final String PREF_CHECK_VALUES = PREFIX + ".checkValues";
    /**
     * The preference key to check keys
     */
    public static final String PREF_CHECK_KEYS = PREFIX + ".checkKeys";
    /**
     * The preference key to enable complex checks
     */
    public static final String PREF_CHECK_COMPLEX = PREFIX + ".checkComplex";
    /**
     * The preference key to search for fixme tags
     */
    public static final String PREF_CHECK_FIXMES = PREFIX + ".checkFixmes";
    /**
     * The preference key to check presets
     */
    public static final String PREF_CHECK_PRESETS_TYPES = PREFIX + ".checkPresetsTypes";

    /**
     * The preference key for source files
     * @see #DEFAULT_SOURCES
     */
    public static final String PREF_SOURCES = PREFIX + ".source";

    private static final String BEFORE_UPLOAD = "BeforeUpload";
    /**
     * The preference key to check keys - used before upload
     */
    public static final String PREF_CHECK_KEYS_BEFORE_UPLOAD = PREF_CHECK_KEYS + BEFORE_UPLOAD;
    /**
     * The preference key to check values - used before upload
     */
    public static final String PREF_CHECK_VALUES_BEFORE_UPLOAD = PREF_CHECK_VALUES + BEFORE_UPLOAD;
    /**
     * The preference key to run complex tests - used before upload
     */
    public static final String PREF_CHECK_COMPLEX_BEFORE_UPLOAD = PREF_CHECK_COMPLEX + BEFORE_UPLOAD;
    /**
     * The preference key to search for fixmes - used before upload
     */
    public static final String PREF_CHECK_FIXMES_BEFORE_UPLOAD = PREF_CHECK_FIXMES + BEFORE_UPLOAD;
    /**
     * The preference key to search for presets - used before upload
     */
    public static final String PREF_CHECK_PRESETS_TYPES_BEFORE_UPLOAD = PREF_CHECK_PRESETS_TYPES + BEFORE_UPLOAD;

    private static final int MAX_LEVENSHTEIN_DISTANCE = 2;

    protected boolean includeOtherSeverity;

    protected boolean checkKeys;
    protected boolean checkValues;
    /** Was used for special configuration file, might be used to disable value spell checker. */
    protected boolean checkComplex;
    protected boolean checkFixmes;
    protected boolean checkPresetsTypes;

    protected JCheckBox prefCheckKeys;
    protected JCheckBox prefCheckValues;
    protected JCheckBox prefCheckComplex;
    protected JCheckBox prefCheckFixmes;
    protected JCheckBox prefCheckPresetsTypes;

    protected JCheckBox prefCheckKeysBeforeUpload;
    protected JCheckBox prefCheckValuesBeforeUpload;
    protected JCheckBox prefCheckComplexBeforeUpload;
    protected JCheckBox prefCheckFixmesBeforeUpload;
    protected JCheckBox prefCheckPresetsTypesBeforeUpload;

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    protected static final int EMPTY_VALUES             = 1200;
    protected static final int INVALID_KEY              = 1201;
    protected static final int INVALID_VALUE            = 1202;
    protected static final int FIXME                    = 1203;
    protected static final int INVALID_SPACE            = 1204;
    protected static final int INVALID_KEY_SPACE        = 1205;
    protected static final int INVALID_HTML             = 1206; /* 1207 was PAINT */
    protected static final int LONG_VALUE               = 1208;
    protected static final int LONG_KEY                 = 1209;
    protected static final int LOW_CHAR_VALUE           = 1210;
    protected static final int LOW_CHAR_KEY             = 1211;
    protected static final int MISSPELLED_VALUE         = 1212;
    protected static final int MISSPELLED_KEY           = 1213;
    protected static final int MULTIPLE_SPACES          = 1214;
    protected static final int MISSPELLED_VALUE_NO_FIX  = 1215;
    protected static final int UNUSUAL_UNICODE_CHAR_VALUE = 1216;
    protected static final int INVALID_PRESETS_TYPE     = 1217;
    protected static final int MULTIPOLYGON_NO_AREA     = 1218;
    protected static final int MULTIPOLYGON_INCOMPLETE  = 1219;
    protected static final int MULTIPOLYGON_MAYBE_NO_AREA = 1220;
    // CHECKSTYLE.ON: SingleSpaceSeparator

    protected EditableList sourcesList;

    private static final List<String> DEFAULT_SOURCES = Arrays.asList(IGNORE_FILE, SPELL_FILE);

    /**
     * Constructor
     */
    public TagChecker() {
        super(tr("Tag checker"), tr("This test checks for errors in tag keys and values."));
    }

    @Override
    public void initialize() throws IOException {
        TaggingPresets.addListener(this);
        initializeData();
        initializePresets();
        analysePresets();
    }

    /**
     * Add presets that contain only numerical values to the ignore list
     */
    private static void analysePresets() {
        for (String key : TaggingPresets.getPresetKeys()) {
            if (isKeyIgnored(key))
                continue;
            Set<String> values = TaggingPresets.getPresetValues(key);
            boolean allNumerical = values != null && !values.isEmpty()
                    && values.stream().allMatch(TagChecker::isNum);
            if (allNumerical) {
                ignoreForLevenshtein.add(key);
            }
        }
    }

    /**
     * Reads the spell-check file into a HashMap.
     * The data file is a list of words, beginning with +/-. If it starts with +,
     * the word is valid, but if it starts with -, the word should be replaced
     * by the nearest + word before this.
     *
     * @throws IOException if any I/O error occurs
     */
    private static void initializeData() throws IOException {
        ignoreDataStartsWith.clear();
        ignoreDataEquals.clear();
        ignoreDataEndsWith.clear();
        ignoreDataTag.clear();
        harmonizedKeys.clear();
        ignoreForLevenshtein.clear();
        oftenUsedTags.clear();
        presetIndex.clear();

        StringBuilder errorSources = new StringBuilder();
        for (String source : Config.getPref().getList(PREF_SOURCES, DEFAULT_SOURCES)) {
            try (
                CachedFile cf = new CachedFile(source);
                BufferedReader reader = cf.getContentReader()
            ) {
                String okValue = null;
                boolean tagcheckerfile = false;
                boolean ignorefile = false;
                boolean isFirstLine = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        // ignore
                    } else if (line.startsWith("#")) {
                        if (line.startsWith("# JOSM TagChecker")) {
                            tagcheckerfile = true;
                            Logging.error(tr("Ignoring {0}. Support was dropped", source));
                        } else
                        if (line.startsWith("# JOSM IgnoreTags")) {
                            ignorefile = true;
                            if (!DEFAULT_SOURCES.contains(source)) {
                                Logging.info(tr("Adding {0} to ignore tags", source));
                            }
                        }
                    } else if (ignorefile) {
                        parseIgnoreFileLine(source, line);
                    } else if (tagcheckerfile) {
                        // ignore
                    } else if (line.charAt(0) == '+') {
                        okValue = line.substring(1);
                    } else if (line.charAt(0) == '-' && okValue != null) {
                        String hk = harmonizeKey(line.substring(1));
                        if (!okValue.equals(hk) && harmonizedKeys.put(hk, okValue) != null && Logging.isDebugEnabled()) {
                            Logging.debug("Line was ignored: " + line);
                        }
                    } else {
                        Logging.error(tr("Invalid spellcheck line: {0}", line));
                    }
                    if (isFirstLine) {
                        isFirstLine = false;
                        if (!(tagcheckerfile || ignorefile) && !DEFAULT_SOURCES.contains(source)) {
                            Logging.info(tr("Adding {0} to spellchecker", source));
                        }
                    }
                }
            } catch (IOException e) {
                Logging.error(e);
                errorSources.append(source).append('\n');
            }
        }

        if (errorSources.length() > 0)
            throw new IOException(trn(
                    "Could not access data file:\n{0}",
                    "Could not access data files:\n{0}", errorSources.length(), errorSources));
    }

    /**
     * Parse a line found in a configuration file
     * @param source name of configuration file
     * @param line the line to parse
     */
    private static void parseIgnoreFileLine(String source, String line) {
        line = line.trim();
        if (line.length() < 4) {
            return;
        }
        try {
            String key = line.substring(0, 2);
            line = line.substring(2);

            switch (key) {
            case "S:":
                ignoreDataStartsWith.add(line);
                break;
            case "E:":
                ignoreDataEquals.add(line);
                addToKeyDictionary(line);
                break;
            case "F:":
                ignoreDataEndsWith.add(line);
                break;
            case "K:":
                Tag tag = Tag.ofString(line);
                ignoreDataTag.add(tag);
                oftenUsedTags.put(tag.getKey(), tag.getValue());
                addToKeyDictionary(tag.getKey());
                break;
            default:
                if (!key.startsWith(";")) {
                    Logging.warn("Unsupported TagChecker key: " + key);
                }
            }
        } catch (IllegalArgumentException e) {
            Logging.error("Invalid line in {0} : {1}", source, e.getMessage());
            Logging.trace(e);
        }
    }

    private static void addToKeyDictionary(String key) {
        if (key != null) {
            String hk = harmonizeKey(key);
            if (!key.equals(hk)) {
                harmonizedKeys.put(hk, key);
            }
        }
    }

    /**
     * Reads the presets data.
     *
     */
    public static void initializePresets() {

        if (!Config.getPref().getBoolean(PREF_CHECK_VALUES, true))
            return;

        Collection<TaggingPreset> presets = TaggingPresets.getTaggingPresets();
        if (!presets.isEmpty()) {
            initAdditionalPresetsValueData();
            for (TaggingPreset p : presets) {
                List<TaggingPresetItem> minData = new ArrayList<>();
                for (TaggingPresetItem i : p.data) {
                    if (i instanceof KeyedItem) {
                        if (!"none".equals(((KeyedItem) i).match))
                            minData.add(i);
                        addPresetValue((KeyedItem) i);
                    } else if (i instanceof CheckGroup) {
                        for (Check c : ((CheckGroup) i).checks) {
                            addPresetValue(c);
                        }
                    }
                }
                if (!minData.isEmpty()) {
                    presetIndex .put(p, minData);
                }
            }
        }
    }

    private static void initAdditionalPresetsValueData() {
        additionalPresetsValueData = new HashSet<>();
        additionalPresetsValueData.addAll(AbstractPrimitive.getUninterestingKeys());
        additionalPresetsValueData.addAll(Config.getPref().getList(
                ValidatorPrefHelper.PREFIX + ".knownkeys",
                Arrays.asList("is_in", "int_ref", "fixme", "population")));
    }

    private static void addPresetValue(KeyedItem ky) {
        if (ky.key != null && ky.getValues() != null) {
            addToKeyDictionary(ky.key);
        }
    }

    /**
     * Checks given string (key or value) if it contains unwanted non-printing control characters (either ASCII or Unicode bidi characters)
     * @param s string to check
     * @return {@code true} if {@code s} contains non-printing control characters
     */
    static boolean containsUnwantedNonPrintingControlCharacter(String s) {
        return s != null && !s.isEmpty() && (
                isJoiningChar(s.charAt(0)) ||
                isJoiningChar(s.charAt(s.length() - 1)) ||
                s.chars().anyMatch(c -> (isAsciiControlChar(c) && !isNewLineChar(c)) || isBidiControlChar(c))
                );
    }

    private static boolean isAsciiControlChar(int c) {
        return c < 0x20 || c == 0x7F;
    }

    private static boolean isNewLineChar(int c) {
        return c == 0x0a || c == 0x0d;
    }

    private static boolean isJoiningChar(int c) {
        return c == 0x200c || c == 0x200d; // ZWNJ, ZWJ
    }

    private static boolean isBidiControlChar(int c) {
        /* check for range 0x200e to 0x200f (LRM, RLM) or
                           0x202a to 0x202e (LRE, RLE, PDF, LRO, RLO) */
        return (c >= 0x200e && c <= 0x200f) || (c >= 0x202a && c <= 0x202e);
    }

    static String removeUnwantedNonPrintingControlCharacters(String s) {
        // Remove all unwanted characters
        String result = UNWANTED_NON_PRINTING_CONTROL_CHARACTERS.matcher(s).replaceAll("");
        // Remove joining characters located at the beginning of the string
        while (!result.isEmpty() && isJoiningChar(result.charAt(0))) {
            result = result.substring(1);
        }
        // Remove joining characters located at the end of the string
        while (!result.isEmpty() && isJoiningChar(result.charAt(result.length() - 1))) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static boolean containsUnusualUnicodeCharacter(String key, String value) {
        return getUnusualUnicodeCharacter(key, value).isPresent();
    }

    static OptionalInt getUnusualUnicodeCharacter(String key, String value) {
        return value == null
                ? OptionalInt.empty()
                : value.chars().filter(c -> isUnusualUnicodeBlock(key, c)).findFirst();
    }

    /**
     * Detects highly suspicious Unicode characters that have been seen in OSM database.
     * @param key tag key
     * @param c current character code point
     * @return {@code true} if the current unicode block is very unusual for the given key
     */
    private static boolean isUnusualUnicodeBlock(String key, int c) {
        UnicodeBlock b = UnicodeBlock.of(c);
        return isUnusualPhoneticUse(key, b, c) || isUnusualBmpUse(b) || isUnusualSmpUse(b);
    }

    private static boolean isAllowedPhoneticCharacter(String key, int c) {
        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        return c == 0x0259 || c == 0x018F // U+0259 is paired with the capital letter U+018F in Azeri, see #18740
            || c == 0x0254 || c == 0x0186 // U+0254 is paired with the capital letter U+0186 in several African languages, see #18740
            || c == 0x025B || c == 0x0190 // U+025B is paired with the capital letter U+0190 in several African languages, see #18740
            || c == 0x0263 || c == 0x0194 // "ɣ/Ɣ" (U+0263/U+0194), see #18740
            || c == 0x0268 || c == 0x0197 // "ɨ/Ɨ" (U+0268/U+0197), see #18740
            || c == 0x0272 || c == 0x019D // "ɲ/Ɲ" (U+0272/U+019D), see #18740
            || c == 0x0273 || c == 0x019E // "ŋ/Ŋ" (U+0273/U+019E), see #18740
            || (key.endsWith("ref") && 0x1D2C <= c && c <= 0x1D42); // allow uppercase superscript latin characters in *ref tags
    }

    private static boolean isUnusualPhoneticUse(String key, UnicodeBlock b, int c) {
        return !isAllowedPhoneticCharacter(key, c)
            && (b == UnicodeBlock.IPA_EXTENSIONS                        // U+0250..U+02AF
             || b == UnicodeBlock.PHONETIC_EXTENSIONS                   // U+1D00..U+1D7F
             || b == UnicodeBlock.PHONETIC_EXTENSIONS_SUPPLEMENT)       // U+1D80..U+1DBF
                && !key.endsWith(":pronunciation");
    }

    private static boolean isUnusualBmpUse(UnicodeBlock b) {
        return b == UnicodeBlock.COMBINING_MARKS_FOR_SYMBOLS            // U+20D0..U+20FF
            || b == UnicodeBlock.MATHEMATICAL_OPERATORS                 // U+2200..U+22FF
            || b == UnicodeBlock.ENCLOSED_ALPHANUMERICS                 // U+2460..U+24FF
            || b == UnicodeBlock.BOX_DRAWING                            // U+2500..U+257F
            || b == UnicodeBlock.GEOMETRIC_SHAPES                       // U+25A0..U+25FF
            || b == UnicodeBlock.DINGBATS                               // U+2700..U+27BF
            || b == UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_ARROWS       // U+2B00..U+2BFF
            || b == UnicodeBlock.GLAGOLITIC                             // U+2C00..U+2C5F
            || b == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO              // U+3130..U+318F
            || b == UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS        // U+3200..U+32FF
            || b == UnicodeBlock.LATIN_EXTENDED_D                       // U+A720..U+A7FF
            || b == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS           // U+F900..U+FAFF
            || b == UnicodeBlock.ALPHABETIC_PRESENTATION_FORMS          // U+FB00..U+FB4F
            || b == UnicodeBlock.VARIATION_SELECTORS                    // U+FE00..U+FE0F
            || b == UnicodeBlock.SPECIALS;                              // U+FFF0..U+FFFF
            // CHECKSTYLE.ON: BooleanExpressionComplexity
    }

    private static boolean isUnusualSmpUse(UnicodeBlock b) {
        // UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS is only defined in Java 9+
        return b == UnicodeBlock.MUSICAL_SYMBOLS                        // U+1D100..U+1D1FF
            || b == UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT       // U+1F100..U+1F1FF
            || b == UnicodeBlock.EMOTICONS                              // U+1F600..U+1F64F
            || b == UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS;             // U+1F680..U+1F6FF
    }

    /**
     * Get set of preset values for the given key.
     * @param key the key
     * @return null if key is not in presets or in additionalPresetsValueData,
     *  else a set which might be empty.
     */
    private static Set<String> getPresetValues(String key) {
        Set<String> res = TaggingPresets.getPresetValues(key);
        if (res != null)
            return res;
        if (additionalPresetsValueData.contains(key))
            return Collections.emptySet();
        // null means key is not known
        return null;
    }

    /**
     * Determines if the given key is in internal presets.
     * @param key key
     * @return {@code true} if the given key is in internal presets
     * @since 9023
     */
    public static boolean isKeyInPresets(String key) {
        return TaggingPresets.getPresetValues(key) != null;
    }

    /**
     * Determines if the given tag is in internal presets.
     * @param key key
     * @param value value
     * @return {@code true} if the given tag is in internal presets
     * @since 9023
     */
    public static boolean isTagInPresets(String key, String value) {
        final Set<String> values = getPresetValues(key);
        return values != null && values.contains(value);
    }

    /**
     * Returns the list of ignored tags.
     * @return the list of ignored tags
     * @since 9023
     */
    public static List<Tag> getIgnoredTags() {
        return new ArrayList<>(ignoreDataTag);
    }

    /**
     * Determines if the given tag key is ignored for checks "key/tag not in presets".
     * @param key key
     * @return true if the given key is ignored
     */
    private static boolean isKeyIgnored(String key) {
        return ignoreDataEquals.contains(key)
                || ignoreDataStartsWith.stream().anyMatch(key::startsWith)
                || ignoreDataEndsWith.stream().anyMatch(key::endsWith);
    }

    /**
     * Determines if the given tag is ignored for checks "key/tag not in presets".
     * @param key key
     * @param value value
     * @return {@code true} if the given tag is ignored
     * @since 9023
     */
    public static boolean isTagIgnored(String key, String value) {
        if (isKeyIgnored(key))
            return true;
        final Set<String> values = getPresetValues(key);
        if (values != null && values.isEmpty())
            return true;
        if (!isTagInPresets(key, value)) {
            return ignoreDataTag.stream()
                    .anyMatch(a -> key.equals(a.getKey()) && value.equals(a.getValue()));
        }
        return false;
    }

    /**
     * Checks the primitive tags
     * @param p The primitive to check
     */
    @Override
    public void check(OsmPrimitive p) {
        if (!p.isTagged())
            return;

        // Just a collection to know if a primitive has been already marked with error
        MultiMap<OsmPrimitive, String> withErrors = new MultiMap<>();

        for (Entry<String, String> prop : p.getKeys().entrySet()) {
            String s = marktr("Tag ''{0}'' invalid.");
            String key = prop.getKey();
            String value = prop.getValue();

            if (checkKeys) {
                checkSingleTagKeySimple(withErrors, p, s, key);
            }
            if (checkValues) {
                checkSingleTagValueSimple(withErrors, p, s, key, value);
                checkSingleTagComplex(withErrors, p, key, value);
            }
            if (checkFixmes && key != null && value != null && !value.isEmpty() && isFixme(key, value) && !withErrors.contains(p, "FIXME")) {
                errors.add(TestError.builder(this, Severity.OTHER, FIXME)
                        .message(tr("FIXMES"))
                        .primitives(p)
                        .build());
                withErrors.put(p, "FIXME");
            }
        }

        if (p instanceof Relation && p.hasTag("type", "multipolygon")) {
            checkMultipolygonTags(p);
        }

        if (checkPresetsTypes) {
            TagMap tags = p.getKeys();
            TaggingPresetType presetType = TaggingPresetType.forPrimitive(p);
            EnumSet<TaggingPresetType> presetTypes = EnumSet.of(presetType);

            Collection<TaggingPreset> matchingPresets = presetIndex.entrySet().stream()
                    .filter(e -> TaggingPresetItem.matches(e.getValue(), tags))
                    .map(Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Collection<TaggingPreset> matchingPresetsOK = matchingPresets.stream().filter(
                    tp -> tp.typeMatches(presetTypes)).collect(Collectors.toList());
            Collection<TaggingPreset> matchingPresetsKO = matchingPresets.stream().filter(
                    tp -> !tp.typeMatches(presetTypes)).collect(Collectors.toList());

            for (TaggingPreset tp : matchingPresetsKO) {
                // Potential error, unless matching tags are all known by a supported preset
                Map<String, String> matchingTags = tp.data.stream()
                    .filter(i -> Boolean.TRUE.equals(i.matches(tags)))
                    .filter(i -> i instanceof KeyedItem).map(i -> ((KeyedItem) i).key)
                    .collect(Collectors.toMap(k -> k, tags::get));
                if (matchingPresetsOK.stream().noneMatch(
                        tp2 -> matchingTags.entrySet().stream().allMatch(
                                e -> tp2.data.stream().anyMatch(
                                        i -> i instanceof KeyedItem && ((KeyedItem) i).key.equals(e.getKey()))))) {
                    errors.add(TestError.builder(this, Severity.OTHER, INVALID_PRESETS_TYPE)
                            .message(tr("Object type not in preset"),
                                    marktr("Object type {0} is not supported by tagging preset: {1}"),
                                    tr(presetType.getName()), tp.getLocaleName())
                            .primitives(p)
                            .build());
                }
            }
        }
    }

    private static final Collection<String> NO_AREA_KEYS = Arrays.asList("name", "area", "ref", "access", "operator");

    private void checkMultipolygonTags(OsmPrimitive p) {
        if (p.isAnnotated() || hasAcceptedPrimaryTagForMultipolygon(p))
            return;
        if (p.keySet().stream().anyMatch(k -> k.matches("^(abandoned|construction|demolished|disused|planned|razed|removed|was).*")))
            return;

        TestError.Builder builder = null;
        if (p.hasKey("surface")) {
            // accept often used tag surface=* as area tag
            builder = TestError.builder(this, Severity.OTHER, MULTIPOLYGON_INCOMPLETE)
                    .message(tr("Multipolygon tags"), marktr("only {0} tag"), "surface");
        } else {
            Map<String, String> filteredTags = p.getInterestingTags();
            filteredTags.remove("type");
            NO_AREA_KEYS.forEach(filteredTags::remove);
            filteredTags.keySet().removeIf(key -> !key.matches("[a-z0-9:_]+"));

            if (filteredTags.isEmpty()) {
                builder = TestError.builder(this, Severity.ERROR, MULTIPOLYGON_NO_AREA)
                        .message(tr("Multipolygon tags"), marktr("tag describing the area is missing"), new Object());

            }
        }
        if (builder == null) {
            // multipolygon has either no area tag or a rarely used one
            builder = TestError.builder(this, Severity.WARNING, MULTIPOLYGON_MAYBE_NO_AREA)
                    .message(tr("Multipolygon tags"), marktr("tag describing the area might be missing"), new Object());
        }
        errors.add(builder.primitives(p).build());
    }

    /**
     * Check if a multipolygon has a main tag that describes the type of area. Accepts also some deprecated tags and typos.
     * @param p the multipolygon
     * @return true if the multipolygon has a main tag that (likely) describes the type of area.
     */
    private static boolean hasAcceptedPrimaryTagForMultipolygon(OsmPrimitive p) {
        if (p.hasKey("landuse", "amenity", "building", "building:part", "area:highway", "shop", "place", "boundary",
                "landform", "piste:type", "sport", "golf", "landcover", "aeroway", "office", "healthcare", "craft", "room")
                || p.hasTagDifferent("natural", "tree", "peek", "saddle", "tree_row")
                || p.hasTagDifferent("man_made", "survey_point", "mast", "flagpole", "manhole", "watertap")
                || p.hasTagDifferent("highway", "crossing", "bus_stop", "turning_circle", "street_lamp",
                        "traffic_signals", "stop", "milestone", "mini_roundabout", "motorway_junction", "passing_place",
                        "speed_camera", "traffic_mirror", "trailhead", "turning_circle", "turning_loop", "toll_gantry")
                || p.hasTagDifferent("tourism", "attraction", "artwork")
                || p.hasTagDifferent("leisure", "picnic_table", "slipway", "firepit")
                || p.hasTagDifferent("historic", "wayside_cross", "milestone"))
            return true;
        if (p.hasTag("barrier", "hedge", "retaining_wall")
                || p.hasTag("public_transport", "platform", "station")
                || p.hasTag("railway", "platform")
                || p.hasTag("waterway", "riverbank", "dam", "rapids", "dock", "boatyard", "fuel")
                || p.hasTag("indoor", "corridor", "room", "area")
                || p.hasTag("power", "substation", "generator", "plant", "switchgear", "converter", "sub_station")
                || p.hasTag("seamark:type", "harbour", "fairway", "anchorage", "landmark", "berth", "harbour_basin",
                        "separation_zone")
                || (p.get("seamark:type") != null && p.get("seamark:type").matches(".*\\_(area|zone)$")))
            return true;
        return p.hasTag("harbour", OsmUtils.TRUE_VALUE)
                || p.hasTag("flood_prone", OsmUtils.TRUE_VALUE)
                || p.hasTag("bridge", OsmUtils.TRUE_VALUE)
                || p.hasTag("ruins", OsmUtils.TRUE_VALUE)
                || p.hasTag("junction", OsmUtils.TRUE_VALUE);
    }

    private void checkSingleTagValueSimple(MultiMap<OsmPrimitive, String> withErrors, OsmPrimitive p, String s, String key, String value) {
        if (!checkValues || value == null)
            return;
        if ((containsUnwantedNonPrintingControlCharacter(value)) && !withErrors.contains(p, "ICV")) {
            errors.add(TestError.builder(this, Severity.WARNING, LOW_CHAR_VALUE)
                    .message(tr("Tag value contains non-printing (usually invisible) character"), s, key)
                    .primitives(p)
                    .fix(() -> new ChangePropertyCommand(p, key, removeUnwantedNonPrintingControlCharacters(value)))
                    .build());
            withErrors.put(p, "ICV");
        }
        final OptionalInt unusualUnicodeCharacter = getUnusualUnicodeCharacter(key, value);
        if (unusualUnicodeCharacter.isPresent() && !withErrors.contains(p, "UUCV")) {
            final String codepoint = String.format(Locale.ROOT, "U+%04X", unusualUnicodeCharacter.getAsInt());
            errors.add(TestError.builder(this, Severity.WARNING, UNUSUAL_UNICODE_CHAR_VALUE)
                    .message(tr("Tag value contains unusual Unicode character {0}", codepoint), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "UUCV");
        }
        if ((value.length() > Tagged.MAX_TAG_LENGTH) && !withErrors.contains(p, "LV")) {
            errors.add(TestError.builder(this, Severity.ERROR, LONG_VALUE)
                    .message(tr("Tag value longer than {0} characters ({1} characters)", Tagged.MAX_TAG_LENGTH, value.length()), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "LV");
        }
        if ((value.trim().isEmpty()) && !withErrors.contains(p, "EV")) {
            errors.add(TestError.builder(this, Severity.WARNING, EMPTY_VALUES)
                    .message(tr("Tags with empty values"), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "EV");
        }
        final String errTypeSpace = "SPACE";
        if ((value.startsWith(" ") || value.endsWith(" ")) && !withErrors.contains(p, errTypeSpace)) {
            errors.add(TestError.builder(this, Severity.WARNING, INVALID_SPACE)
                    .message(tr("Property values start or end with white space"), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, errTypeSpace);
        }
        if (value.contains("  ") && !withErrors.contains(p, errTypeSpace)) {
            errors.add(TestError.builder(this, Severity.WARNING, MULTIPLE_SPACES)
                    .message(tr("Property values contain multiple white spaces"), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, errTypeSpace);
        }
        if (includeOtherSeverity && !value.equals(Entities.unescape(value)) && !withErrors.contains(p, "HTML")) {
            errors.add(TestError.builder(this, Severity.OTHER, INVALID_HTML)
                    .message(tr("Property values contain HTML entity"), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "HTML");
        }
    }

    private void checkSingleTagKeySimple(MultiMap<OsmPrimitive, String> withErrors, OsmPrimitive p, String s, String key) {
        if (!checkKeys || key == null)
            return;
        if ((containsUnwantedNonPrintingControlCharacter(key)) && !withErrors.contains(p, "ICK")) {
            errors.add(TestError.builder(this, Severity.WARNING, LOW_CHAR_KEY)
                    .message(tr("Tag key contains non-printing character"), s, key)
                    .primitives(p)
                    .fix(() -> new ChangePropertyCommand(p, key, removeUnwantedNonPrintingControlCharacters(key)))
                    .build());
            withErrors.put(p, "ICK");
        }
        if (key.length() > Tagged.MAX_TAG_LENGTH && !withErrors.contains(p, "LK")) {
            errors.add(TestError.builder(this, Severity.ERROR, LONG_KEY)
                    .message(tr("Tag key longer than {0} characters ({1} characters)", Tagged.MAX_TAG_LENGTH, key.length()), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "LK");
        }
        if (key.indexOf(' ') >= 0 && !withErrors.contains(p, "IPK")) {
            errors.add(TestError.builder(this, Severity.WARNING, INVALID_KEY_SPACE)
                    .message(tr("Invalid white space in property key"), s, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "IPK");
        }
    }

    private void checkSingleTagComplex(MultiMap<OsmPrimitive, String> withErrors, OsmPrimitive p, String key, String value) {
        if (!checkValues || key == null || value == null || value.isEmpty())
            return;
        if (additionalPresetsValueData != null && !isTagIgnored(key, value)) {
            if (!isKeyInPresets(key)) {
                spellCheckKey(withErrors, p, key);
            } else if (!isTagInPresets(key, value)) {
                if (oftenUsedTags.contains(key, value)) {
                    // tag is quite often used but not in presets
                    errors.add(TestError.builder(this, Severity.OTHER, INVALID_VALUE)
                            .message(tr("Presets do not contain property value"),
                                    marktr("Value ''{0}'' for key ''{1}'' not in presets, but is known."), value, key)
                            .primitives(p)
                            .build());
                    withErrors.put(p, "UPV");
                } else {
                    tryGuess(p, key, value, withErrors);
                }
            }
        }
    }

    private void spellCheckKey(MultiMap<OsmPrimitive, String> withErrors, OsmPrimitive p, String key) {
        String prettifiedKey = harmonizeKey(key);
        String fixedKey;
        if (ignoreDataEquals.contains(prettifiedKey)) {
            fixedKey = prettifiedKey;
        } else {
            fixedKey = isKeyInPresets(prettifiedKey) ? prettifiedKey : harmonizedKeys.get(prettifiedKey);
        }
        if (fixedKey == null && ignoreDataTag.stream().anyMatch(a -> a.getKey().equals(prettifiedKey))) {
            fixedKey = prettifiedKey;
        }

        if (fixedKey != null && !"".equals(fixedKey) && !fixedKey.equals(key)) {
            final String proposedKey = fixedKey;
            // misspelled preset key
            final TestError.Builder error = TestError.builder(this, Severity.WARNING, MISSPELLED_KEY)
                    .message(tr("Misspelled property key"), marktr("Key ''{0}'' looks like ''{1}''."), key, proposedKey)
                    .primitives(p);
            if (p.hasKey(fixedKey)) {
                errors.add(error.build());
            } else {
                errors.add(error.fix(() -> new ChangePropertyKeyCommand(p, key, proposedKey)).build());
            }
            withErrors.put(p, "WPK");
        } else if (includeOtherSeverity) {
            errors.add(TestError.builder(this, Severity.OTHER, INVALID_KEY)
                    .message(tr("Presets do not contain property key"), marktr("Key ''{0}'' not in presets."), key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "UPK");
        }
    }

    private void tryGuess(OsmPrimitive p, String key, String value, MultiMap<OsmPrimitive, String> withErrors) {
        // try to fix common typos and check again if value is still unknown
        final String harmonizedValue = harmonizeValue(value);
        if (harmonizedValue == null || harmonizedValue.isEmpty())
            return;
        String fixedValue;
        List<Set<String>> sets = new ArrayList<>();
        Set<String> presetValues = getPresetValues(key);
        if (presetValues != null)
            sets.add(presetValues);
        Set<String> usedValues = oftenUsedTags.get(key);
        if (usedValues != null)
            sets.add(usedValues);
        fixedValue = sets.stream().anyMatch(possibleValues -> possibleValues.contains(harmonizedValue))
                ? harmonizedValue : null;
        if (fixedValue == null && !ignoreForLevenshtein.contains(key)) {
            int maxPresetValueLen = 0;
            List<String> fixVals = new ArrayList<>();
            // use Levenshtein distance to find typical typos
            int minDist = MAX_LEVENSHTEIN_DISTANCE + 1;
            String closest = null;
            for (Set<String> possibleValues: sets) {
                for (String possibleVal : possibleValues) {
                    if (possibleVal.isEmpty())
                        continue;
                    maxPresetValueLen = Math.max(maxPresetValueLen, possibleVal.length());
                    if (harmonizedValue.length() < 3 && possibleVal.length() >= harmonizedValue.length() + MAX_LEVENSHTEIN_DISTANCE) {
                        // don't suggest fix value when given value is short and lengths are too different
                        // for example surface=u would result in surface=mud
                        continue;
                    }
                    int dist = Utils.getLevenshteinDistance(possibleVal, harmonizedValue);
                    if (dist >= harmonizedValue.length()) {
                        // short value, all characters are different. Don't warn, might say Value '10' for key 'fee' looks like 'no'.
                        continue;
                    }
                    if (dist < minDist) {
                        closest = possibleVal;
                        minDist = dist;
                        fixVals.clear();
                        fixVals.add(possibleVal);
                    } else if (dist == minDist) {
                        fixVals.add(possibleVal);
                    }
                }
            }

            if (minDist <= MAX_LEVENSHTEIN_DISTANCE && maxPresetValueLen > MAX_LEVENSHTEIN_DISTANCE
                    && (harmonizedValue.length() > 3 || minDist < MAX_LEVENSHTEIN_DISTANCE)) {
                if (fixVals.size() < 2) {
                    fixedValue = closest;
                } else {
                    Collections.sort(fixVals);
                    // misspelled preset value with multiple good alternatives
                    errors.add(TestError.builder(this, Severity.WARNING, MISSPELLED_VALUE_NO_FIX)
                            .message(tr("Unknown property value"),
                                    marktr("Value ''{0}'' for key ''{1}'' is unknown, maybe one of {2} is meant?"),
                                    value, key, fixVals)
                            .primitives(p).build());
                    withErrors.put(p, "WPV");
                    return;
                }
            }
        }
        if (fixedValue != null && !fixedValue.equals(value)) {
            final String newValue = fixedValue;
            // misspelled preset value
            errors.add(TestError.builder(this, Severity.WARNING, MISSPELLED_VALUE)
                    .message(tr("Unknown property value"),
                            marktr("Value ''{0}'' for key ''{1}'' is unknown, maybe ''{2}'' is meant?"), value, key, newValue)
                    .primitives(p)
                    .build());
            withErrors.put(p, "WPV");
        } else if (includeOtherSeverity) {
            // unknown preset value
            errors.add(TestError.builder(this, Severity.OTHER, INVALID_VALUE)
                    .message(tr("Presets do not contain property value"),
                            marktr("Value ''{0}'' for key ''{1}'' not in presets."), value, key)
                    .primitives(p)
                    .build());
            withErrors.put(p, "UPV");
        }
    }

    private static boolean isNum(String harmonizedValue) {
        try {
            Double.parseDouble(harmonizedValue);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isFixme(String key, String value) {
        return key.toLowerCase(Locale.ENGLISH).contains("fixme") || key.contains("todo")
          || value.toLowerCase(Locale.ENGLISH).contains("fixme") || value.contains("check and delete");
    }

    private static String harmonizeKey(String key) {
        return Utils.strip(key.toLowerCase(Locale.ENGLISH).replace('-', '_').replace(':', '_').replace(' ', '_'), "-_;:,");
    }

    private static String harmonizeValue(String value) {
        return Utils.strip(value.toLowerCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_'), "-_;:,");
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        includeOtherSeverity = includeOtherSeverityChecks();
        checkKeys = Config.getPref().getBoolean(PREF_CHECK_KEYS, true);
        if (isBeforeUpload) {
            checkKeys = checkKeys && Config.getPref().getBoolean(PREF_CHECK_KEYS_BEFORE_UPLOAD, true);
        }

        checkValues = Config.getPref().getBoolean(PREF_CHECK_VALUES, true);
        if (isBeforeUpload) {
            checkValues = checkValues && Config.getPref().getBoolean(PREF_CHECK_VALUES_BEFORE_UPLOAD, true);
        }

        checkComplex = Config.getPref().getBoolean(PREF_CHECK_COMPLEX, true);
        if (isBeforeUpload) {
            checkComplex = checkComplex && Config.getPref().getBoolean(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, true);
        }

        checkFixmes = includeOtherSeverity && Config.getPref().getBoolean(PREF_CHECK_FIXMES, true);
        if (isBeforeUpload) {
            checkFixmes = checkFixmes && Config.getPref().getBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, true);
        }

        checkPresetsTypes = includeOtherSeverity && Config.getPref().getBoolean(PREF_CHECK_PRESETS_TYPES, true);
        if (isBeforeUpload) {
            checkPresetsTypes = checkPresetsTypes && Config.getPref().getBoolean(PREF_CHECK_PRESETS_TYPES_BEFORE_UPLOAD, true);
        }
    }

    @Override
    public void visit(Collection<OsmPrimitive> selection) {
        if (checkKeys || checkValues || checkComplex || checkFixmes || checkPresetsTypes) {
            super.visit(selection);
        }
    }

    @Override
    public void addGui(JPanel testPanel) {
        GBC a = GBC.eol();
        a.anchor = GridBagConstraints.EAST;

        testPanel.add(new JLabel(name+" :"), GBC.eol().insets(3, 0, 0, 0));

        prefCheckKeys = new JCheckBox(tr("Check property keys."), Config.getPref().getBoolean(PREF_CHECK_KEYS, true));
        prefCheckKeys.setToolTipText(tr("Validate that property keys are valid checking against list of words."));
        testPanel.add(prefCheckKeys, GBC.std().insets(20, 0, 0, 0));

        prefCheckKeysBeforeUpload = new JCheckBox();
        prefCheckKeysBeforeUpload.setSelected(Config.getPref().getBoolean(PREF_CHECK_KEYS_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckKeysBeforeUpload, a);

        prefCheckComplex = new JCheckBox(tr("Use complex property checker."), Config.getPref().getBoolean(PREF_CHECK_COMPLEX, true));
        prefCheckComplex.setToolTipText(tr("Validate property values and tags using complex rules."));
        testPanel.add(prefCheckComplex, GBC.std().insets(20, 0, 0, 0));

        prefCheckComplexBeforeUpload = new JCheckBox();
        prefCheckComplexBeforeUpload.setSelected(Config.getPref().getBoolean(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckComplexBeforeUpload, a);

        final Collection<String> sources = Config.getPref().getList(PREF_SOURCES, DEFAULT_SOURCES);
        sourcesList = new EditableList(tr("TagChecker source"));
        sourcesList.setItems(sources);
        testPanel.add(new JLabel(tr("Data sources ({0})", "*.cfg")), GBC.eol().insets(23, 0, 0, 0));
        testPanel.add(sourcesList, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(23, 0, 0, 0));

        ActionListener disableCheckActionListener = e -> handlePrefEnable();
        prefCheckKeys.addActionListener(disableCheckActionListener);
        prefCheckKeysBeforeUpload.addActionListener(disableCheckActionListener);
        prefCheckComplex.addActionListener(disableCheckActionListener);
        prefCheckComplexBeforeUpload.addActionListener(disableCheckActionListener);

        handlePrefEnable();

        prefCheckValues = new JCheckBox(tr("Check property values."), Config.getPref().getBoolean(PREF_CHECK_VALUES, true));
        prefCheckValues.setToolTipText(tr("Validate that property values are valid checking against presets."));
        testPanel.add(prefCheckValues, GBC.std().insets(20, 0, 0, 0));

        prefCheckValuesBeforeUpload = new JCheckBox();
        prefCheckValuesBeforeUpload.setSelected(Config.getPref().getBoolean(PREF_CHECK_VALUES_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckValuesBeforeUpload, a);

        prefCheckFixmes = new JCheckBox(tr("Check for FIXMES."), Config.getPref().getBoolean(PREF_CHECK_FIXMES, true));
        prefCheckFixmes.setToolTipText(tr("Looks for nodes or ways with FIXME in any property value."));
        testPanel.add(prefCheckFixmes, GBC.std().insets(20, 0, 0, 0));

        prefCheckFixmesBeforeUpload = new JCheckBox();
        prefCheckFixmesBeforeUpload.setSelected(Config.getPref().getBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckFixmesBeforeUpload, a);

        prefCheckPresetsTypes = new JCheckBox(tr("Check for presets types."), Config.getPref().getBoolean(PREF_CHECK_PRESETS_TYPES, true));
        prefCheckPresetsTypes.setToolTipText(tr("Validate that objects types are valid checking against presets."));
        testPanel.add(prefCheckPresetsTypes, GBC.std().insets(20, 0, 0, 0));

        prefCheckPresetsTypesBeforeUpload = new JCheckBox();
        prefCheckPresetsTypesBeforeUpload.setSelected(Config.getPref().getBoolean(PREF_CHECK_PRESETS_TYPES_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckPresetsTypesBeforeUpload, a);
    }

    /**
     * Enables/disables the source list field
     */
    public void handlePrefEnable() {
        boolean selected = prefCheckKeys.isSelected() || prefCheckKeysBeforeUpload.isSelected()
                || prefCheckComplex.isSelected() || prefCheckComplexBeforeUpload.isSelected();
        sourcesList.setEnabled(selected);
    }

    @Override
    public boolean ok() {
        enabled = prefCheckKeys.isSelected() || prefCheckValues.isSelected() || prefCheckComplex.isSelected() || prefCheckFixmes.isSelected();
        testBeforeUpload = prefCheckKeysBeforeUpload.isSelected() || prefCheckValuesBeforeUpload.isSelected()
                || prefCheckFixmesBeforeUpload.isSelected() || prefCheckComplexBeforeUpload.isSelected();

        Config.getPref().putBoolean(PREF_CHECK_VALUES, prefCheckValues.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_COMPLEX, prefCheckComplex.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_KEYS, prefCheckKeys.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_FIXMES, prefCheckFixmes.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_PRESETS_TYPES, prefCheckPresetsTypes.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_VALUES_BEFORE_UPLOAD, prefCheckValuesBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, prefCheckComplexBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_KEYS_BEFORE_UPLOAD, prefCheckKeysBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, prefCheckFixmesBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_PRESETS_TYPES_BEFORE_UPLOAD, prefCheckPresetsTypesBeforeUpload.isSelected());
        return Config.getPref().putList(PREF_SOURCES, sourcesList.getItems());
    }

    @Override
    public Command fixError(TestError testError) {
        List<Command> commands = new ArrayList<>(50);

        Collection<? extends OsmPrimitive> primitives = testError.getPrimitives();
        for (OsmPrimitive p : primitives) {
            Map<String, String> tags = p.getKeys();
            if (tags.isEmpty()) {
                continue;
            }

            for (Entry<String, String> prop: tags.entrySet()) {
                String key = prop.getKey();
                String value = prop.getValue();
                if (value == null || value.trim().isEmpty()) {
                    commands.add(new ChangePropertyCommand(p, key, null));
                } else if (value.startsWith(" ") || value.endsWith(" ") || value.contains("  ")) {
                    commands.add(new ChangePropertyCommand(p, key, Utils.removeWhiteSpaces(value)));
                } else if (key.startsWith(" ") || key.endsWith(" ") || key.contains("  ")) {
                    commands.add(new ChangePropertyKeyCommand(p, key, Utils.removeWhiteSpaces(key)));
                } else {
                    String evalue = Entities.unescape(value);
                    if (!evalue.equals(value)) {
                        commands.add(new ChangePropertyCommand(p, key, evalue));
                    }
                }
            }
        }

        if (commands.isEmpty())
            return null;
        if (commands.size() == 1)
            return commands.get(0);

        return new SequenceCommand(tr("Fix tags"), commands);
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (testError.getTester() instanceof TagChecker) {
            int code = testError.getCode();
            return code == EMPTY_VALUES || code == INVALID_SPACE ||
                   code == INVALID_KEY_SPACE || code == INVALID_HTML ||
                   code == MULTIPLE_SPACES;
        }

        return false;
    }

    @Override
    public void taggingPresetsModified() {
        try {
            initializeData();
            initializePresets();
            analysePresets();
        } catch (IOException e) {
            Logging.error(e);
        }
    }
}
