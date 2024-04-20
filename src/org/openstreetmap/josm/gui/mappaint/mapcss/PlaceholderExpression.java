// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Used for expressions that contain placeholders
 * @since 18758
 */
public final class PlaceholderExpression implements Expression {
    /**
     * The regex used for pattern replacement
     */
    public static final Pattern PATTERN_PLACEHOLDER = Pattern.compile("\\{(\\d+)\\.(key|value|tag)}");
    private final String placeholder;

    /**
     * Constructs a new {@link PlaceholderExpression}.
     * @param placeholder The placeholder expression
     */
    public PlaceholderExpression(String placeholder) {
        CheckParameterUtil.ensureParameterNotNull(placeholder);
        this.placeholder = placeholder.intern();
    }

    @Override
    public Object evaluate(Environment env) {
        if (env.selector() == null) {
            return placeholder;
        }
        return insertArguments(env.selector(), placeholder, env.osm);
    }

    /**
     * Replaces occurrences of <code>{i.key}</code>, <code>{i.value}</code>, <code>{i.tag}</code> in {@code s} by the corresponding
     * key/value/tag of the {@code index}-th {@link Condition} of {@code matchingSelector}.
     *
     * @param matchingSelector matching selector
     * @param s                any string
     * @param p                OSM primitive
     * @return string with arguments inserted
     */
    public static String insertArguments(Selector matchingSelector, String s, Tagged p) {
        if (s != null && matchingSelector instanceof Selector.ChildOrParentSelector) {
            return insertArguments(((Selector.ChildOrParentSelector) matchingSelector).right, s, p);
        } else if (s == null || !(matchingSelector instanceof Selector.GeneralSelector)) {
            return s;
        }
        final Matcher m = PATTERN_PLACEHOLDER.matcher(s);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String argument = determineArgument(matchingSelector,
                    Integer.parseInt(m.group(1)), m.group(2), p);
            try {
                // Perform replacement with null-safe + regex-safe handling
                m.appendReplacement(sb, String.valueOf(argument).replace("^(", "").replace(")$", ""));
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                Logging.log(Logging.LEVEL_ERROR, tr("Unable to replace argument {0} in {1}: {2}", argument, sb, e.getMessage()), e);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Determines the {@code index}-th key/value/tag (depending on {@code type}) of the
     * {@link org.openstreetmap.josm.gui.mappaint.mapcss.Selector}.
     *
     * @param matchingSelector matching selector
     * @param index            index
     * @param type             selector type ("key", "value" or "tag")
     * @param p                OSM primitive
     * @return argument value, can be {@code null}
     */
    private static String determineArgument(Selector matchingSelector, int index, String type, Tagged p) {
        try {
            final Condition c = matchingSelector.getConditions().get(index);
            final Tag tag = c instanceof Condition.TagCondition
                    ? ((Condition.TagCondition) c).asTag(p)
                    : null;
            if (tag == null) {
                return null;
            } else if ("key".equals(type)) {
                return tag.getKey();
            } else if ("value".equals(type)) {
                return tag.getValue();
            } else if ("tag".equals(type)) {
                return tag.toString();
            }
        } catch (IndexOutOfBoundsException ioobe) {
            Logging.debug(ioobe);
        }
        return null;
    }

    @Override
    public String toString() {
        return '<' + placeholder + '>';
    }
}
