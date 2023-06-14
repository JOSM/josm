// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents a fix to a validation test. The fixing {@link Command} can be obtained by {@link #createCommand(OsmPrimitive, Selector)}.
 */
@FunctionalInterface
interface MapCSSTagCheckerFixCommand {
    /**
     * Creates the fixing {@link Command} for the given primitive. The {@code matchingSelector} is used to evaluate placeholders
     * (cf. {@link MapCSSTagCheckerRule#insertArguments(Selector, String, OsmPrimitive)}).
     *
     * @param p                OSM primitive
     * @param matchingSelector matching selector
     * @return fix command, or {@code null} if if cannot be created
     */
    Command createCommand(OsmPrimitive p, Selector matchingSelector);

    /**
     * Checks that object is either an {@link Expression} or a {@link String}.
     *
     * @param obj object to check
     * @throws IllegalArgumentException if object is not an {@code Expression} or a {@code String}
     */
    static void checkObject(final Object obj) {
        CheckParameterUtil.ensureThat(obj instanceof Expression || obj instanceof String,
                () -> "instance of Exception or String expected, but got " + obj);
    }

    /**
     * Evaluates given object as {@link Expression} or {@link String} on the matched {@link OsmPrimitive} and {@code matchingSelector}.
     *
     * @param obj              object to evaluate ({@link Expression} or {@link String})
     * @param p                OSM primitive
     * @param matchingSelector matching selector
     * @return result string
     */
    static String evaluateObject(final Object obj, final OsmPrimitive p, final Selector matchingSelector) {
        final String s;
        if (obj instanceof Expression) {
            s = (String) ((Expression) obj).evaluate(new Environment(p).withSelector(matchingSelector));
        } else if (obj instanceof String) {
            s = (String) obj;
        } else {
            return null;
        }
        return MapCSSTagCheckerRule.insertArguments(matchingSelector, s, p);
    }

    /**
     * Creates a fixing command which executes a {@link ChangePropertyCommand} on the specified tag.
     *
     * @param obj object to evaluate ({@link Expression} or {@link String})
     * @return created fix command
     */
    static MapCSSTagCheckerFixCommand fixAdd(final Object obj) {
        checkObject(obj);
        return new MapCSSTagCheckerFixCommand() {
            @Override
            public Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                final Tag tag = Tag.ofString(MapCSSTagCheckerFixCommand.evaluateObject(obj, p, matchingSelector));
                return new ChangePropertyCommand(p, tag.getKey(), tag.getValue());
            }

            @Override
            public String toString() {
                return "fixAdd: " + obj;
            }
        };
    }

    /**
     * Creates a fixing command which executes a {@link ChangePropertyCommand} to delete the specified key.
     *
     * @param obj object to evaluate ({@link Expression} or {@link String})
     * @return created fix command
     */
    static MapCSSTagCheckerFixCommand fixRemove(final Object obj) {
        checkObject(obj);
        return new MapCSSTagCheckerFixCommand() {
            @Override
            public Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                final String key = MapCSSTagCheckerFixCommand.evaluateObject(obj, p, matchingSelector);
                return new ChangePropertyCommand(p, key, "");
            }

            @Override
            public String toString() {
                return "fixRemove: " + obj;
            }
        };
    }

    /**
     * Creates a fixing command which executes a {@link ChangePropertyKeyCommand} on the specified keys
     *
     * @param oldKey old key
     * @param newKey new key
     * @return created fix command
     */
    static MapCSSTagCheckerFixCommand fixChangeKey(final String oldKey, final String newKey) {
        return new MapCSSTagCheckerFixCommand() {
            @Override
            public Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                return new ChangePropertyKeyCommand(p,
                        MapCSSTagCheckerRule.insertArguments(matchingSelector, oldKey, p),
                        MapCSSTagCheckerRule.insertArguments(matchingSelector, newKey, p));
            }

            @Override
            public String toString() {
                return "fixChangeKey: " + oldKey + " => " + newKey;
            }
        };
    }
}
