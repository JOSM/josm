// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test.TagTest;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.Entities;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
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
public class TagChecker extends TagTest {

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

    private static final Pattern NON_PRINTING_CONTROL_CHARACTERS = Pattern.compile(
            "[\\x00-\\x09\\x0B\\x0C\\x0E-\\x1F\\x7F\\u200c-\\u200f\\u202a-\\u202e]");

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

    private static final int MAX_LEVENSHTEIN_DISTANCE = 2;

    protected boolean checkKeys;
    protected boolean checkValues;
    /** Was used for special configuration file, might be used to disable value spell checker. */
    protected boolean checkComplex;
    protected boolean checkFixmes;

    protected JCheckBox prefCheckKeys;
    protected JCheckBox prefCheckValues;
    protected JCheckBox prefCheckComplex;
    protected JCheckBox prefCheckFixmes;
    protected JCheckBox prefCheckPaint;

    protected JCheckBox prefCheckKeysBeforeUpload;
    protected JCheckBox prefCheckValuesBeforeUpload;
    protected JCheckBox prefCheckComplexBeforeUpload;
    protected JCheckBox prefCheckFixmesBeforeUpload;
    protected JCheckBox prefCheckPaintBeforeUpload;

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
            boolean allNumerical = true;
            Set<String> values = TaggingPresets.getPresetValues(key);
            if (values.isEmpty())
                allNumerical = false;
            for (String val : values) {
                if (!isNum(val)) {
                    allNumerical = false;
                    break;
                }
            }
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
                        if (!okValue.equals(hk) && harmonizedKeys.put(hk, okValue) != null) {
                            Logging.debug(tr("Line was ignored: {0}", line));
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
            throw new IOException(tr("Could not access data file(s):\n{0}", errorSources));
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
                for (TaggingPresetItem i : p.data) {
                    if (i instanceof KeyedItem) {
                        addPresetValue((KeyedItem) i);
                    } else if (i instanceof CheckGroup) {
                        for (Check c : ((CheckGroup) i).checks) {
                            addPresetValue(c);
                        }
                    }
                }
            }
        }
    }

    private static void initAdditionalPresetsValueData() {
        additionalPresetsValueData = new HashSet<>();
        for (String a : AbstractPrimitive.getUninterestingKeys()) {
            additionalPresetsValueData.add(a);
        }
        for (String a : Config.getPref().getList(ValidatorPrefHelper.PREFIX + ".knownkeys",
                Arrays.asList("is_in", "int_ref", "fixme", "population"))) {
            additionalPresetsValueData.add(a);
        }
    }

    private static void addPresetValue(KeyedItem ky) {
        if (ky.key != null && ky.getValues() != null) {
            addToKeyDictionary(ky.key);
        }
    }

    /**
     * Checks given string (key or value) if it contains non-printing control characters (either ASCII or Unicode bidi characters)
     * @param s string to check
     * @return {@code true} if {@code s} contains non-printing control characters
     */
    private static boolean containsNonPrintingControlCharacter(String s) {
        if (s == null)
            return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((IsAsciiControlChar(c) && !isNewLineChar(c)) || IsBidiControlChar(c))
                return true;
        }
        return false;
    }

    private static boolean IsAsciiControlChar(char c) {
        return c < 0x20 || c == 0x7F;
    }

    private static boolean isNewLineChar(char c) {
        return c == 0x0a || c == 0x0d;
    }

    private static boolean IsBidiControlChar(char c) {
        /* check for range 0x200c to 0x200f (ZWNJ, ZWJ, LRM, RLM) or
                           0x202a to 0x202e (LRE, RLE, PDF, LRO, RLO) */
        return (((c & 0xfffffffc) == 0x200c) || ((c >= 0x202a) && (c <= 0x202e)));
    }

    static String removeNonPrintingControlCharacters(String s) {
        return NON_PRINTING_CONTROL_CHARACTERS.matcher(s).replaceAll("");
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
        if (ignoreDataEquals.contains(key)) {
            return true;
        }
        for (String a : ignoreDataStartsWith) {
            if (key.startsWith(a)) {
                return true;
            }
        }
        for (String a : ignoreDataEndsWith) {
            if (key.endsWith(a)) {
                return true;
            }
        }
        return false;
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
            for (Tag a : ignoreDataTag) {
                if (key.equals(a.getKey()) && value.equals(a.getValue())) {
                    return true;
                }
            }
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
    }

    private void checkSingleTagValueSimple(MultiMap<OsmPrimitive, String> withErrors, OsmPrimitive p, String s, String key, String value) {
        if (!checkValues || value == null)
            return;
        if ((containsNonPrintingControlCharacter(value)) && !withErrors.contains(p, "ICV")) {
            errors.add(TestError.builder(this, Severity.WARNING, LOW_CHAR_VALUE)
                    .message(tr("Tag value contains non-printing character"), s, key)
                    .primitives(p)
                    .fix(() -> new ChangePropertyCommand(p, key, removeNonPrintingControlCharacters(value)))
                    .build());
            withErrors.put(p, "ICV");
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
        if (!value.equals(Entities.unescape(value)) && !withErrors.contains(p, "HTML")) {
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
        if ((containsNonPrintingControlCharacter(key)) && !withErrors.contains(p, "ICK")) {
            errors.add(TestError.builder(this, Severity.WARNING, LOW_CHAR_KEY)
                    .message(tr("Tag key contains non-printing character"), s, key)
                    .primitives(p)
                    .fix(() -> new ChangePropertyCommand(p, key, removeNonPrintingControlCharacters(key)))
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
        if (fixedKey == null) {
            for (Tag a : ignoreDataTag) {
                if (a.getKey().equals(prettifiedKey)) {
                    fixedKey = prettifiedKey;
                    break;
                }
            }
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
        } else {
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
        String fixedValue = null;
        List<Set<String>> sets = new ArrayList<>();
        Set<String> presetValues = getPresetValues(key);
        if (presetValues != null)
            sets.add(presetValues);
        Set<String> usedValues = oftenUsedTags.get(key);
        if (usedValues != null)
            sets.add(usedValues);
        for (Set<String> possibleValues: sets) {
            if (possibleValues.contains(harmonizedValue)) {
                fixedValue = harmonizedValue;
                break;
            }
        }
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
        } else {
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

        checkFixmes = Config.getPref().getBoolean(PREF_CHECK_FIXMES, true);
        if (isBeforeUpload) {
            checkFixmes = checkFixmes && Config.getPref().getBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, true);
        }
    }

    @Override
    public void visit(Collection<OsmPrimitive> selection) {
        if (checkKeys || checkValues || checkComplex || checkFixmes) {
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
        Config.getPref().putBoolean(PREF_CHECK_VALUES_BEFORE_UPLOAD, prefCheckValuesBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, prefCheckComplexBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_KEYS_BEFORE_UPLOAD, prefCheckKeysBeforeUpload.isSelected());
        Config.getPref().putBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, prefCheckFixmesBeforeUpload.isSelected());
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
}
