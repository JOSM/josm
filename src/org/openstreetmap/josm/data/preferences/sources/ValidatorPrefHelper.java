// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Helper class for validator tag checker rules preferences.
 * @since 12649 (extracted from gui.preferences package)
 */
public class ValidatorPrefHelper extends SourcePrefHelper {

    /**
     * The unique instance.
     */
    public static final ValidatorPrefHelper INSTANCE = new ValidatorPrefHelper();

    /** The preferences prefix */
    public static final String PREFIX = "validator";

    /** The preferences key for error layer */
    public static final BooleanProperty PREF_LAYER = new BooleanProperty(PREFIX + ".layer", true);

    /** The preferences key for enabled tests */
    public static final String PREF_SKIP_TESTS = PREFIX + ".skip";

    /** The preferences key for enabled tests */
    public static final BooleanProperty PREF_USE_IGNORE = new BooleanProperty(PREFIX + ".ignore", true);

    /** The preferences key for enabled tests before upload*/
    public static final String PREF_SKIP_TESTS_BEFORE_UPLOAD = PREFIX + ".skipBeforeUpload";

    /** The preferences key for ignored severity other on upload */
    public static final BooleanProperty PREF_OTHER_UPLOAD = new BooleanProperty(PREFIX + ".otherUpload", false);

    /** The preferences for ignored severity other */
    public static final BooleanProperty PREF_OTHER = new BooleanProperty(PREFIX + ".other", false);

    /** The preferences key for the ignorelist */
    public static final String PREF_IGNORELIST = PREFIX + ".ignorelist";

    /**
     * The preferences key for enabling the permanent filtering
     * of the displayed errors in the tree regarding the current selection
     */
    public static final String PREF_FILTER_BY_SELECTION = PREFIX + ".selectionFilter";

    /**
     * Constructs a new {@code PresetPrefHelper}.
     */
    public ValidatorPrefHelper() {
        super(MapCSSTagChecker.ENTRIES_PREF_KEY, SourceType.TAGCHECKER_RULE);
    }

    @Override
    public Collection<ExtendedSourceEntry> getDefault() {
        List<ExtendedSourceEntry> def = new ArrayList<>();

        // CHECKSTYLE.OFF: SingleSpaceSeparator
        addDefault(def, "addresses",    tr("Addresses"),           tr("Checks for errors on addresses"));
        addDefault(def, "combinations", tr("Tag combinations"),    tr("Checks for missing tag or suspicious combinations"));
        addDefault(def, "deprecated",   tr("Deprecated features"), tr("Checks for deprecated features"));
        addDefault(def, "geometry",     tr("Geometry"),            tr("Checks for geometry errors"));
        addDefault(def, "highway",      tr("Highways"),            tr("Checks for errors on highways"));
        addDefault(def, "multiple",     tr("Multiple values"),     tr("Checks for wrong multiple values"));
        addDefault(def, "numeric",      tr("Numeric values"),      tr("Checks for wrong numeric values"));
        addDefault(def, "religion",     tr("Religion"),            tr("Checks for errors on religious objects"));
        addDefault(def, "relation",     tr("Relations"),           tr("Checks for errors on relations"));
        addDefault(def, "territories",  tr("Territories"),         tr("Checks for territories-specific features"));
        addDefault(def, "unnecessary",  tr("Unnecessary tags"),    tr("Checks for unnecessary tags"));
        addDefault(def, "wikipedia",    tr("Wikipedia"),           tr("Checks for wrong wikipedia tags"));
        // CHECKSTYLE.ON: SingleSpaceSeparator

        return def;
    }

    private void addDefault(List<ExtendedSourceEntry> defaults, String filename, String title, String description) {
        ExtendedSourceEntry i = new ExtendedSourceEntry(type, filename+".mapcss", "resource://data/validator/"+filename+".mapcss");
        i.title = title;
        i.icon = new ImageProvider("logo").getResource();
        i.description = description;
        defaults.add(i);
    }

    @Override
    public Map<String, String> serialize(SourceEntry entry) {
        Map<String, String> res = super.serialize(entry);
        res.put("active", Boolean.toString(entry.active));
        return res;
    }
}
