// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory;
import org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory;
import org.openstreetmap.josm.gui.mappaint.mapcss.LiteralExpression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.tools.DefaultGeoProperty;
import org.openstreetmap.josm.tools.GeoProperty;
import org.openstreetmap.josm.tools.GeoPropertyIndex;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Territories;

/**
 * Utility class for checking rule assertions of {@link MapCSSTagCheckerRule}.
 */
final class MapCSSTagCheckerAsserts {

    private MapCSSTagCheckerAsserts() {
        // private constructor
    }

    private static final ArrayList<MapCSSTagCheckerRule> previousChecks = new ArrayList<>();

    /**
     * Checks that rule assertions are met for the given set of TagChecks.
     * @param check The TagCheck for which assertions have to be checked
     * @param assertions The assertions to check (map values correspond to expected result)
     * @param assertionConsumer The handler for assertion error messages
     */
    static void checkAsserts(final MapCSSTagCheckerRule check, final Map<String, Boolean> assertions,
                             final MapCSSTagChecker.AssertionConsumer assertionConsumer) {
        final DataSet ds = new DataSet();
        Logging.debug("Check: {0}", check);
        for (final Map.Entry<String, Boolean> i : assertions.entrySet()) {
            Logging.debug("- Assertion: {0}", i);
            final OsmPrimitive p = OsmUtils.createPrimitive(i.getKey(), getLocation(check), true);
            // Build minimal ordered list of checks to run to test the assertion
            List<Set<MapCSSTagCheckerRule>> checksToRun = new ArrayList<>();
            Set<MapCSSTagCheckerRule> checkDependencies = getTagCheckDependencies(check, previousChecks);
            if (!checkDependencies.isEmpty()) {
                checksToRun.add(checkDependencies);
            }
            checksToRun.add(Collections.singleton(check));
            // Add primitive to dataset to avoid DataIntegrityProblemException when evaluating selectors
            ds.addPrimitiveRecursive(p);
            final Collection<TestError> pErrors = MapCSSTagChecker.getErrorsForPrimitive(p, true, checksToRun);
            Logging.debug("- Errors: {0}", pErrors);
            final boolean isError = pErrors.stream().anyMatch(e -> e.getTester() instanceof MapCSSTagChecker.MapCSSTagCheckerAndRule
                    && ((MapCSSTagChecker.MapCSSTagCheckerAndRule) e.getTester()).rule.equals(check.rule));
            if (isError != i.getValue()) {
                assertionConsumer.accept(MessageFormat.format("Expecting test ''{0}'' (i.e., {1}) to {2} {3} (i.e., {4})",
                        check.getMessage(p), check.rule.selectors, i.getValue() ? "match" : "not match", i.getKey(), p.getKeys()));
            }
            if (isError) {
                // Check that autofix works as expected
                Command fix = check.fixPrimitive(p);
                if (fix != null && fix.executeCommand() && !MapCSSTagChecker.getErrorsForPrimitive(p, true, checksToRun).isEmpty()) {
                    assertionConsumer.accept(MessageFormat.format("Autofix does not work for test ''{0}'' (i.e., {1})",
                            check.getMessage(p), check.rule.selectors));
                }
            }
            ds.removePrimitive(p);
        }
        previousChecks.add(check);
    }

    public static void clear() {
        previousChecks.clear();
        previousChecks.trimToSize();
    }

    private static LatLon getLocation(MapCSSTagCheckerRule check) {
        Optional<String> inside = getFirstInsideCountry(check);
        if (inside.isPresent()) {
            GeoPropertyIndex<Boolean> index = Territories.getGeoPropertyIndex(inside.get());
            if (index != null) {
                GeoProperty<Boolean> prop = index.getGeoProperty();
                if (prop instanceof DefaultGeoProperty) {
                    return ((DefaultGeoProperty) prop).getRandomLatLon();
                }
            }
        }
        return LatLon.ZERO;
    }

    private static Optional<String> getFirstInsideCountry(MapCSSTagCheckerRule check) {
        return check.rule.selectors.stream()
                .filter(s -> s instanceof Selector.GeneralSelector)
                .flatMap(s -> ((Selector.GeneralSelector) s).getConditions().stream())
                .filter(c -> c instanceof ConditionFactory.ExpressionCondition)
                .map(c -> ((ConditionFactory.ExpressionCondition) c).getExpression())
                .filter(c -> c instanceof ExpressionFactory.IsInsideFunction)
                .map(c -> (ExpressionFactory.IsInsideFunction) c)
                .map(ExpressionFactory.IsInsideFunction::getArg)
                .filter(e -> e instanceof LiteralExpression)
                .map(e -> ((LiteralExpression) e).getLiteral())
                .filter(l -> l instanceof String)
                .map(l -> ((String) l).split(",", -1)[0])
                .findFirst();
    }

    /**
     * Returns the set of tagchecks on which this check depends on.
     * @param check the tagcheck
     * @param schecks the collection of tagchecks to search in
     * @return the set of tagchecks on which this check depends on
     * @since 7881
     */
    private static Set<MapCSSTagCheckerRule> getTagCheckDependencies(MapCSSTagCheckerRule check,
                                                                     Collection<MapCSSTagCheckerRule> schecks) {
        Set<MapCSSTagCheckerRule> result = new HashSet<>();
        Set<String> classes = check.rule.selectors.stream()
                .filter(s -> s instanceof Selector.AbstractSelector)
                .flatMap(s -> ((Selector.AbstractSelector) s).getConditions().stream())
                .filter(c -> c instanceof ConditionFactory.ClassCondition)
                .map(c -> ((ConditionFactory.ClassCondition) c).id)
                .collect(Collectors.toSet());
        if (schecks != null && !classes.isEmpty()) {
            return schecks.stream()
                    .filter(tc -> !check.equals(tc))
                    .filter(tc -> tc.setClassExpressions.stream().anyMatch(classes::contains))
                    .collect(Collectors.toSet());
        }
        return result;
    }
}
