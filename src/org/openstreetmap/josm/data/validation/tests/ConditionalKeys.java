// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Checks for <a href="http://wiki.openstreetmap.org/wiki/Conditional_restrictions">conditional restrictions</a>
 * @since 6605
 */
public class ConditionalKeys extends Test.TagTest {

    private final OpeningHourTest openingHourTest = new OpeningHourTest();
    private static final Set<String> RESTRICTION_TYPES = new HashSet<>(Arrays.asList("oneway", "toll", "noexit", "maxspeed", "minspeed",
            "maxstay", "maxweight", "maxaxleload", "maxheight", "maxwidth", "maxlength", "overtaking", "maxgcweight", "maxgcweightrating",
            "fee", "restriction", "interval", "duration"));
    private static final Set<String> RESTRICTION_VALUES = new HashSet<>(Arrays.asList("yes", "official", "designated", "destination",
            "delivery", "customers", "permissive", "private", "agricultural", "forestry", "no"));
    private static final Set<String> TRANSPORT_MODES = new HashSet<>(Arrays.asList("access", "foot", "ski", "inline_skates", "ice_skates",
            "horse", "vehicle", "bicycle", "carriage", "trailer", "caravan", "motor_vehicle", "motorcycle", "moped", "mofa",
            "motorcar", "motorhome", "psv", "bus", "taxi", "tourist_bus", "goods", "hgv", "agricultural", "atv", "snowmobile",
            "hgv_articulated", "ski:nordic", "ski:alpine", "ski:telemark", "coach", "golf_cart"
            /*,"minibus","share_taxi","hov","car_sharing","emergency","hazmat","disabled"*/));

    private static final Pattern CONDITIONAL_PATTERN;
    static {
        final String part = Pattern.compile("([^@\\p{Space}][^@]*?)"
                + "\\s*@\\s*" + "(\\([^)\\p{Space}][^)]+?\\)|[^();\\p{Space}][^();]*?)\\s*").toString();
        CONDITIONAL_PATTERN = Pattern.compile('(' + part + ")(;\\s*" + part + ")*");
    }

    /**
     * Constructs a new {@code ConditionalKeys}.
     */
    public ConditionalKeys() {
        super(tr("Conditional Keys"), tr("Tests for the correct usage of ''*:conditional'' tags."));
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        openingHourTest.initialize();
    }

    /**
     * Check if the key is a key for an access restriction
     * @param part The key (or the restriction part of it, e.g. for lanes)
     * @return <code>true</code> if it is a restriction
     */
    public static boolean isRestrictionType(String part) {
        return RESTRICTION_TYPES.contains(part);
    }

    /**
     * Check if the value is a valid restriction value
     * @param part The value
     * @return <code>true</code> for allowed restriction values
     */
    public static boolean isRestrictionValue(String part) {
        return RESTRICTION_VALUES.contains(part);
    }

    /**
     * Checks if the key denotes a
     * <a href="http://wiki.openstreetmap.org/wiki/Key:access#Transport_mode_restrictions">transport access mode restriction</a>
     * @param part The key (or the restriction part of it, e.g. for lanes)
     * @return <code>true</code> if it is a restriction
     */
    public static boolean isTransportationMode(String part) {
        return TRANSPORT_MODES.contains(part);
    }

    /**
     * Check if a key part is a valid direction
     * @param part The part of the key
     * @return <code>true</code> if it is a direction
     */
    public static boolean isDirection(String part) {
        return "forward".equals(part) || "backward".equals(part);
    }

    /**
     * Checks if a given key is a valid access key
     * @param key The conditional key
     * @return <code>true</code> if the key is valid
     */
    public boolean isKeyValid(String key) {
        // <restriction-type>[:<transportation mode>][:<direction>]:conditional
        // -- or --            <transportation mode> [:<direction>]:conditional
        if (!key.endsWith(":conditional")) {
            return false;
        }
        final String[] parts = key.replace(":conditional", "").split(":");
        return isKeyValid3Parts(parts) || isKeyValid1Part(parts) || isKeyValid2Parts(parts);
    }

    private static boolean isKeyValid3Parts(String... parts) {
        return parts.length == 3 && isRestrictionType(parts[0]) && isTransportationMode(parts[1]) && isDirection(parts[2]);
    }

    private static boolean isKeyValid2Parts(String... parts) {
        return parts.length == 2 && ((isRestrictionType(parts[0]) && (isTransportationMode(parts[1]) || isDirection(parts[1])))
                                  || (isTransportationMode(parts[0]) && isDirection(parts[1])));
    }

    private static boolean isKeyValid1Part(String... parts) {
        return parts.length == 1 && (isRestrictionType(parts[0]) || isTransportationMode(parts[0]));
    }

    /**
     * Check if a value is valid
     * @param key The key the value is for
     * @param value The value
     * @return <code>true</code> if it is valid
     */
    public boolean isValueValid(String key, String value) {
        return validateValue(key, value) == null;
    }

    static class ConditionalParsingException extends RuntimeException {
        ConditionalParsingException(String message) {
            super(message);
        }
    }

    /**
     * A conditional value is a value for the access restriction tag that depends on conditions (time, ...)
     */
    public static class ConditionalValue {
        /**
         * The value the tag should have if the condition matches
         */
        public final String restrictionValue;
        /**
         * The conditions for {@link #restrictionValue}
         */
        public final Collection<String> conditions;

        /**
         * Create a new {@link ConditionalValue}
         * @param restrictionValue The value the tag should have if the condition matches
         * @param conditions The conditions for that value
         */
        public ConditionalValue(String restrictionValue, Collection<String> conditions) {
            this.restrictionValue = restrictionValue;
            this.conditions = conditions;
        }

        /**
         * Parses the condition values as string.
         * @param value value, must match {@code <restriction-value> @ <condition>[;<restriction-value> @ <condition>]} pattern
         * @return list of {@code ConditionalValue}s
         * @throws ConditionalParsingException if {@code value} does not match expected pattern
         */
        public static List<ConditionalValue> parse(String value) {
            // <restriction-value> @ <condition>[;<restriction-value> @ <condition>]
            final List<ConditionalValue> r = new ArrayList<>();
            final Matcher m = CONDITIONAL_PATTERN.matcher(value);
            if (!m.matches()) {
                throw new ConditionalParsingException(tr("Does not match pattern ''restriction value @ condition''"));
            } else {
                int i = 2;
                while (i + 1 <= m.groupCount() && m.group(i + 1) != null) {
                    final String restrictionValue = m.group(i);
                    final String[] conditions = m.group(i + 1).replace("(", "").replace(")", "").split("\\s+(AND|and)\\s+");
                    r.add(new ConditionalValue(restrictionValue, Arrays.asList(conditions)));
                    i += 3;
                }
            }
            return r;
        }
    }

    /**
     * Validate a key/value pair
     * @param key The key
     * @param value The value
     * @return The error message for that value or <code>null</code> to indicate valid
     */
    public String validateValue(String key, String value) {
        try {
            for (final ConditionalValue conditional : ConditionalValue.parse(value)) {
                // validate restriction value
                if (isTransportationMode(key.split(":")[0]) && !isRestrictionValue(conditional.restrictionValue)) {
                    return tr("{0} is not a valid restriction value", conditional.restrictionValue);
                }
                // validate opening hour if the value contains an hour (heuristic)
                for (final String condition : conditional.conditions) {
                    if (condition.matches(".*[0-9]:[0-9]{2}.*")) {
                        final List<TestError> errors = openingHourTest.checkOpeningHourSyntax("", condition);
                        if (!errors.isEmpty()) {
                            return errors.get(0).getDescription();
                        }
                    }
                }
            }
        } catch (ConditionalParsingException ex) {
            Logging.debug(ex);
            return ex.getMessage();
        }
        return null;
    }

    /**
     * Validate a primitive
     * @param p The primitive
     * @return The errors for that primitive or an empty list if there are no errors.
     */
    public List<TestError> validatePrimitive(OsmPrimitive p) {
        final List<TestError> errors = new ArrayList<>();
        for (final String key : SubclassFilteredCollection.filter(p.keySet(),
                Pattern.compile(":conditional(:.*)?$").asPredicate())) {
            if (!isKeyValid(key)) {
                errors.add(TestError.builder(this, Severity.WARNING, 3201)
                        .message(tr("Wrong syntax in {0} key", key))
                        .primitives(p)
                        .build());
                continue;
            }
            final String value = p.get(key);
            final String error = validateValue(key, value);
            if (error != null) {
                errors.add(TestError.builder(this, Severity.WARNING, 3202)
                        .message(tr("Error in {0} value: {1}", key, error))
                        .primitives(p)
                        .build());
            }
        }
        return errors;
    }

    @Override
    public void check(OsmPrimitive p) {
        if (p.isTagged()) {
            errors.addAll(validatePrimitive(p));
        }
    }
}
