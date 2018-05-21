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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
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
    private static volatile MultiMap<String, String> additionalPresetsValueData;
    /** The TagChecker data */
    private static final List<CheckerData> checkerData = new ArrayList<>();
    private static final List<String> ignoreDataStartsWith = new ArrayList<>();
    private static final List<String> ignoreDataEquals = new ArrayList<>();
    private static final List<String> ignoreDataEndsWith = new ArrayList<>();
    private static final List<Tag> ignoreDataTag = new ArrayList<>();

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

    /**
     * The preference key to check keys - used before upload
     */
    public static final String PREF_CHECK_KEYS_BEFORE_UPLOAD = PREF_CHECK_KEYS + "BeforeUpload";
    /**
     * The preference key to check values - used before upload
     */
    public static final String PREF_CHECK_VALUES_BEFORE_UPLOAD = PREF_CHECK_VALUES + "BeforeUpload";
    /**
     * The preference key to run complex tests - used before upload
     */
    public static final String PREF_CHECK_COMPLEX_BEFORE_UPLOAD = PREF_CHECK_COMPLEX + "BeforeUpload";
    /**
     * The preference key to search for fixmes - used before upload
     */
    public static final String PREF_CHECK_FIXMES_BEFORE_UPLOAD = PREF_CHECK_FIXMES + "BeforeUpload";

    protected boolean checkKeys;
    protected boolean checkValues;
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
    protected static final int EMPTY_VALUES      = 1200;
    protected static final int INVALID_KEY       = 1201;
    protected static final int INVALID_VALUE     = 1202;
    protected static final int FIXME             = 1203;
    protected static final int INVALID_SPACE     = 1204;
    protected static final int INVALID_KEY_SPACE = 1205;
    protected static final int INVALID_HTML      = 1206; /* 1207 was PAINT */
    protected static final int LONG_VALUE        = 1208;
    protected static final int LONG_KEY          = 1209;
    protected static final int LOW_CHAR_VALUE    = 1210;
    protected static final int LOW_CHAR_KEY      = 1211;
    protected static final int MISSPELLED_VALUE  = 1212;
    protected static final int MISSPELLED_KEY    = 1213;
    protected static final int MULTIPLE_SPACES   = 1214;
    // CHECKSTYLE.ON: SingleSpaceSeparator
    // 1250 and up is used by tagcheck

    protected EditableList sourcesList;

    private static final List<String> DEFAULT_SOURCES = Arrays.asList(/*DATA_FILE, */IGNORE_FILE, SPELL_FILE);

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
    }

    /**
     * Reads the spellcheck file into a HashMap.
     * The data file is a list of words, beginning with +/-. If it starts with +,
     * the word is valid, but if it starts with -, the word should be replaced
     * by the nearest + word before this.
     *
     * @throws IOException if any I/O error occurs
     */
    private static void initializeData() throws IOException {
        checkerData.clear();
        ignoreDataStartsWith.clear();
        ignoreDataEquals.clear();
        ignoreDataEndsWith.clear();
        ignoreDataTag.clear();
        harmonizedKeys.clear();

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
                while ((line = reader.readLine()) != null && (tagcheckerfile || !line.isEmpty())) {
                    if (line.startsWith("#")) {
                        if (line.startsWith("# JOSM TagChecker")) {
                            tagcheckerfile = true;
                            if (!DEFAULT_SOURCES.contains(source)) {
                                Logging.info(tr("Adding {0} to tag checker", source));
                            }
                        } else
                        if (line.startsWith("# JOSM IgnoreTags")) {
                            ignorefile = true;
                            if (!DEFAULT_SOURCES.contains(source)) {
                                Logging.info(tr("Adding {0} to ignore tags", source));
                            }
                        }
                    } else if (ignorefile) {
                        line = line.trim();
                        if (line.length() < 4) {
                            continue;
                        }

                        String key = line.substring(0, 2);
                        line = line.substring(2);

                        switch (key) {
                        case "S:":
                            ignoreDataStartsWith.add(line);
                            break;
                        case "E:":
                            ignoreDataEquals.add(line);
                            break;
                        case "F:":
                            ignoreDataEndsWith.add(line);
                            break;
                        case "K:":
                            ignoreDataTag.add(Tag.ofString(line));
                            break;
                        default:
                            if (!key.startsWith(";")) {
                                Logging.warn("Unsupported TagChecker key: " + key);
                            }
                        }
                    } else if (tagcheckerfile) {
                        if (!line.isEmpty()) {
                            CheckerData d = new CheckerData();
                            String err = d.getData(line);

                            if (err == null) {
                                checkerData.add(d);
                            } else {
                                Logging.error(tr("Invalid tagchecker line - {0}: {1}", err, line));
                            }
                        }
                    } else if (line.charAt(0) == '+') {
                        okValue = line.substring(1);
                    } else if (line.charAt(0) == '-' && okValue != null) {
                        harmonizedKeys.put(harmonizeKey(line.substring(1)), okValue);
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
     * Reads the presets data.
     *
     */
    public static void initializePresets() {

        if (!Config.getPref().getBoolean(PREF_CHECK_VALUES, true))
            return;

        Collection<TaggingPreset> presets = TaggingPresets.getTaggingPresets();
        if (!presets.isEmpty()) {
            additionalPresetsValueData = new MultiMap<>();
            for (String a : AbstractPrimitive.getUninterestingKeys()) {
                additionalPresetsValueData.putVoid(a);
            }
            // TODO directionKeys are no longer in OsmPrimitive (search pattern is used instead)
            for (String a : Config.getPref().getList(ValidatorPrefHelper.PREFIX + ".knownkeys",
                    Arrays.asList("is_in", "int_ref", "fixme", "population"))) {
                additionalPresetsValueData.putVoid(a);
            }
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

    private static void addPresetValue(KeyedItem ky) {
        Collection<String> values = ky.getValues();
        if (ky.key != null && values != null) {
            harmonizedKeys.put(harmonizeKey(ky.key), ky.key);
        }
    }

    /**
     * Checks given string (key or value) if it contains characters with code below 0x20 (either newline or some other special characters)
     * @param s string to check
     * @return {@code true} if {@code s} contains characters with code below 0x20
     */
    private static boolean containsLow(String s) {
        if (s == null)
            return false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < 0x20)
                return true;
        }
        return false;
    }

    private static Set<String> getPresetValues(String key) {
        Set<String> res = TaggingPresets.getPresetValues(key);
        if (res != null)
            return res;
        return additionalPresetsValueData.get(key);
    }

    /**
     * Determines if the given key is in internal presets.
     * @param key key
     * @return {@code true} if the given key is in internal presets
     * @since 9023
     */
    public static boolean isKeyInPresets(String key) {
        return getPresetValues(key) != null;
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
        return values != null && (values.isEmpty() || values.contains(value));
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
     * Determines if the given tag is ignored for checks "key/tag not in presets".
     * @param key key
     * @param value value
     * @return {@code true} if the given tag is ignored
     * @since 9023
     */
    public static boolean isTagIgnored(String key, String value) {
        boolean tagInPresets = isTagInPresets(key, value);
        boolean ignore = false;

        for (String a : ignoreDataStartsWith) {
            if (key.startsWith(a)) {
                ignore = true;
            }
        }
        for (String a : ignoreDataEquals) {
            if (key.equals(a)) {
                ignore = true;
            }
        }
        for (String a : ignoreDataEndsWith) {
            if (key.endsWith(a)) {
                ignore = true;
            }
        }

        if (!tagInPresets) {
            for (Tag a : ignoreDataTag) {
                if (key.equals(a.getKey()) && value.equals(a.getValue())) {
                    ignore = true;
                }
            }
        }
        return ignore;
    }

    /**
     * Checks the primitive tags
     * @param p The primitive to check
     */
    @Override
    public void check(OsmPrimitive p) {
        // Just a collection to know if a primitive has been already marked with error
        MultiMap<OsmPrimitive, String> withErrors = new MultiMap<>();

        if (checkComplex) {
            Map<String, String> keys = p.getKeys();
            for (CheckerData d : checkerData) {
                if (d.match(p, keys)) {
                    errors.add(TestError.builder(this, d.getSeverity(), d.getCode())
                            .message(tr("Suspicious tag/value combinations"), d.getDescription())
                            .primitives(p)
                            .build());
                    withErrors.put(p, "TC");
                }
            }
        }

        for (Entry<String, String> prop : p.getKeys().entrySet()) {
            String s = marktr("Tag ''{0}'' invalid.");
            String key = prop.getKey();
            String value = prop.getValue();
            if (checkValues && (containsLow(value)) && !withErrors.contains(p, "ICV")) {
                errors.add(TestError.builder(this, Severity.WARNING, LOW_CHAR_VALUE)
                        .message(tr("Tag value contains character with code less than 0x20"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "ICV");
            }
            if (checkKeys && (containsLow(key)) && !withErrors.contains(p, "ICK")) {
                errors.add(TestError.builder(this, Severity.WARNING, LOW_CHAR_KEY)
                        .message(tr("Tag key contains character with code less than 0x20"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "ICK");
            }
            if (checkValues && (value != null && value.length() > Tagged.MAX_TAG_LENGTH) && !withErrors.contains(p, "LV")) {
                errors.add(TestError.builder(this, Severity.ERROR, LONG_VALUE)
                        .message(tr("Tag value longer than {0} characters ({1} characters)", Tagged.MAX_TAG_LENGTH, value.length()), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "LV");
            }
            if (checkKeys && (key != null && key.length() > Tagged.MAX_TAG_LENGTH) && !withErrors.contains(p, "LK")) {
                errors.add(TestError.builder(this, Severity.ERROR, LONG_KEY)
                        .message(tr("Tag key longer than {0} characters ({1} characters)", Tagged.MAX_TAG_LENGTH, key.length()), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "LK");
            }
            if (checkValues && (value == null || value.trim().isEmpty()) && !withErrors.contains(p, "EV")) {
                errors.add(TestError.builder(this, Severity.WARNING, EMPTY_VALUES)
                        .message(tr("Tags with empty values"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "EV");
            }
            if (checkKeys && key != null && key.indexOf(' ') >= 0 && !withErrors.contains(p, "IPK")) {
                errors.add(TestError.builder(this, Severity.WARNING, INVALID_KEY_SPACE)
                        .message(tr("Invalid white space in property key"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "IPK");
            }
            if (checkValues && value != null && (value.startsWith(" ") || value.endsWith(" ")) && !withErrors.contains(p, "SPACE")) {
                errors.add(TestError.builder(this, Severity.WARNING, INVALID_SPACE)
                        .message(tr("Property values start or end with white space"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "SPACE");
            }
            if (checkValues && value != null && value.contains("  ") && !withErrors.contains(p, "SPACE")) {
                errors.add(TestError.builder(this, Severity.WARNING, MULTIPLE_SPACES)
                        .message(tr("Property values contain multiple white spaces"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "SPACE");
            }
            if (checkValues && value != null && !value.equals(Entities.unescape(value)) && !withErrors.contains(p, "HTML")) {
                errors.add(TestError.builder(this, Severity.OTHER, INVALID_HTML)
                        .message(tr("Property values contain HTML entity"), s, key)
                        .primitives(p)
                        .build());
                withErrors.put(p, "HTML");
            }
            if (checkValues && key != null && value != null && !value.isEmpty() && additionalPresetsValueData != null
                    && !isTagIgnored(key, value)) {
                if (!isKeyInPresets(key)) {
                    String prettifiedKey = harmonizeKey(key);
                    String fixedKey = harmonizedKeys.get(prettifiedKey);
                    if (fixedKey != null && !"".equals(fixedKey) && !fixedKey.equals(key)) {
                        // misspelled preset key
                        final TestError.Builder error = TestError.builder(this, Severity.WARNING, MISSPELLED_KEY)
                                .message(tr("Misspelled property key"), marktr("Key ''{0}'' looks like ''{1}''."), key, fixedKey)
                                .primitives(p);
                        if (p.hasKey(fixedKey)) {
                            errors.add(error.build());
                        } else {
                            errors.add(error.fix(() -> new ChangePropertyKeyCommand(p, key, fixedKey)).build());
                        }
                        withErrors.put(p, "WPK");
                    } else {
                        errors.add(TestError.builder(this, Severity.OTHER, INVALID_VALUE)
                                .message(tr("Presets do not contain property key"), marktr("Key ''{0}'' not in presets."), key)
                                .primitives(p)
                                .build());
                        withErrors.put(p, "UPK");
                    }
                } else if (!isTagInPresets(key, value)) {
                    // try to fix common typos and check again if value is still unknown
                    String fixedValue = harmonizeValue(prop.getValue());
                    Map<String, String> possibleValues = getPossibleValues(getPresetValues(key));
                    if (possibleValues.containsKey(fixedValue)) {
                        final String newKey = possibleValues.get(fixedValue);
                        // misspelled preset value
                        errors.add(TestError.builder(this, Severity.WARNING, MISSPELLED_VALUE)
                                .message(tr("Misspelled property value"),
                                        marktr("Value ''{0}'' for key ''{1}'' looks like ''{2}''."), prop.getValue(), key, fixedValue)
                                .primitives(p)
                                .fix(() -> new ChangePropertyCommand(p, key, newKey))
                                .build());
                        withErrors.put(p, "WPV");
                    } else {
                        // unknown preset value
                        errors.add(TestError.builder(this, Severity.OTHER, INVALID_VALUE)
                                .message(tr("Presets do not contain property value"),
                                        marktr("Value ''{0}'' for key ''{1}'' not in presets."), prop.getValue(), key)
                                .primitives(p)
                                .build());
                        withErrors.put(p, "UPV");
                    }
                }
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

    private static boolean isFixme(String key, String value) {
        return key.toLowerCase(Locale.ENGLISH).contains("fixme") || key.contains("todo")
          || value.toLowerCase(Locale.ENGLISH).contains("fixme") || value.contains("check and delete");
    }

    private static Map<String, String> getPossibleValues(Set<String> values) {
        // generate a map with common typos
        Map<String, String> map = new HashMap<>();
        if (values != null) {
            for (String value : values) {
                map.put(value, value);
                if (value.contains("_")) {
                    map.put(value.replace("_", ""), value);
                }
            }
        }
        return map;
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
            return code == INVALID_KEY || code == EMPTY_VALUES || code == INVALID_SPACE ||
                   code == INVALID_KEY_SPACE || code == INVALID_HTML || code == MISSPELLED_VALUE ||
                   code == MULTIPLE_SPACES;
        }

        return false;
    }

    protected static class CheckerData {
        private String description;
        protected List<CheckerElement> data = new ArrayList<>();
        private OsmPrimitiveType type;
        private TagCheckLevel level;
        protected Severity severity;

        private enum TagCheckLevel {
            TAG_CHECK_ERROR(1250),
            TAG_CHECK_WARN(1260),
            TAG_CHECK_INFO(1270);

            final int code;

            TagCheckLevel(int code) {
                this.code = code;
            }
        }

        protected static class CheckerElement {
            public Object tag;
            public Object value;
            public boolean noMatch;
            public boolean tagAll;
            public boolean valueAll;
            public boolean valueBool;

            private static Pattern getPattern(String str) {
                if (str.endsWith("/i"))
                    return Pattern.compile(str.substring(1, str.length()-2), Pattern.CASE_INSENSITIVE);
                if (str.endsWith("/"))
                    return Pattern.compile(str.substring(1, str.length()-1));

                throw new IllegalStateException();
            }

            public CheckerElement(String exp) {
                Matcher m = Pattern.compile("(.+)([!=]=)(.+)").matcher(exp);
                m.matches();

                String n = m.group(1).trim();

                if ("*".equals(n)) {
                    tagAll = true;
                } else {
                    tag = n.startsWith("/") ? getPattern(n) : n;
                    noMatch = "!=".equals(m.group(2));
                    n = m.group(3).trim();
                    if ("*".equals(n)) {
                        valueAll = true;
                    } else if ("BOOLEAN_TRUE".equals(n)) {
                        valueBool = true;
                        value = OsmUtils.TRUE_VALUE;
                    } else if ("BOOLEAN_FALSE".equals(n)) {
                        valueBool = true;
                        value = OsmUtils.FALSE_VALUE;
                    } else {
                        value = n.startsWith("/") ? getPattern(n) : n;
                    }
                }
            }

            public boolean match(Map<String, String> keys) {
                for (Entry<String, String> prop: keys.entrySet()) {
                    String key = prop.getKey();
                    String val = valueBool ? OsmUtils.getNamedOsmBoolean(prop.getValue()) : prop.getValue();
                    if ((tagAll || (tag instanceof Pattern ? ((Pattern) tag).matcher(key).matches() : key.equals(tag)))
                            && (valueAll || (value instanceof Pattern ? ((Pattern) value).matcher(val).matches() : val.equals(value))))
                        return !noMatch;
                }
                return noMatch;
            }
        }

        private static final Pattern CLEAN_STR_PATTERN = Pattern.compile(" *# *([^#]+) *$");
        private static final Pattern SPLIT_TRIMMED_PATTERN = Pattern.compile(" *: *");
        private static final Pattern SPLIT_ELEMENTS_PATTERN = Pattern.compile(" *&& *");

        public String getData(final String str) {
            Matcher m = CLEAN_STR_PATTERN.matcher(str);
            String trimmed = m.replaceFirst("").trim();
            try {
                description = m.group(1);
                if (description != null && description.isEmpty()) {
                    description = null;
                }
            } catch (IllegalStateException e) {
                Logging.error(e);
                description = null;
            }
            String[] n = SPLIT_TRIMMED_PATTERN.split(trimmed, 3);
            switch (n[0]) {
            case "way":
                type = OsmPrimitiveType.WAY;
                break;
            case "node":
                type = OsmPrimitiveType.NODE;
                break;
            case "relation":
                type = OsmPrimitiveType.RELATION;
                break;
            case "*":
                type = null;
                break;
            default:
                return tr("Could not find element type");
            }
            if (n.length != 3)
                return tr("Incorrect number of parameters");

            switch (n[1]) {
            case "W":
                severity = Severity.WARNING;
                level = TagCheckLevel.TAG_CHECK_WARN;
                break;
            case "E":
                severity = Severity.ERROR;
                level = TagCheckLevel.TAG_CHECK_ERROR;
                break;
            case "I":
                severity = Severity.OTHER;
                level = TagCheckLevel.TAG_CHECK_INFO;
                break;
            default:
                return tr("Could not find warning level");
            }
            for (String exp: SPLIT_ELEMENTS_PATTERN.split(n[2])) {
                try {
                    data.add(new CheckerElement(exp));
                } catch (IllegalStateException e) {
                    Logging.trace(e);
                    return tr("Illegal expression ''{0}''", exp);
                } catch (PatternSyntaxException e) {
                    Logging.trace(e);
                    return tr("Illegal regular expression ''{0}''", exp);
                }
            }
            return null;
        }

        public boolean match(OsmPrimitive osm, Map<String, String> keys) {
            if (type != null && OsmPrimitiveType.from(osm) != type)
                return false;

            for (CheckerElement ce : data) {
                if (!ce.match(keys))
                    return false;
            }
            return true;
        }

        /**
         * Returns the error description.
         * @return the error description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the error severity.
         * @return the error severity
         */
        public Severity getSeverity() {
            return severity;
        }

        /**
         * Returns the error code.
         * @return the error code
         */
        public int getCode() {
            if (type == null)
                return level.code;

            return level.code + type.ordinal() + 1;
        }
    }
}
