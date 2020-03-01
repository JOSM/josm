// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory;
import org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory;
import org.openstreetmap.josm.gui.mappaint.mapcss.Functions;
import org.openstreetmap.josm.gui.mappaint.mapcss.LiteralExpression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.tools.DefaultGeoProperty;
import org.openstreetmap.josm.tools.GeoProperty;
import org.openstreetmap.josm.tools.GeoPropertyIndex;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Territories;

/**
 * Utility class for checking rule {@linkplain MapCSSTagChecker.TagCheck#assertions assertions} of {@link MapCSSTagChecker.TagCheck}.
 */
class MapCSSTagCheckerAsserts {
    private MapCSSTagCheckerAsserts() {
        // private constructor
    }

    /**
     * Checks that rule assertions are met for the given set of TagChecks.
     * @param schecks The TagChecks for which assertions have to be checked
     * @return A set of error messages, empty if all assertions are met
     * @since 7356
     */
    public static Set<String> checkAsserts(final Collection<MapCSSTagChecker.TagCheck> schecks) {
        Set<String> assertionErrors = new LinkedHashSet<>();
        final Method insideMethod = getFunctionMethod("inside");
        final DataSet ds = new DataSet();
        for (final MapCSSTagChecker.TagCheck check : schecks) {
            Logging.debug("Check: {0}", check);
            for (final Map.Entry<String, Boolean> i : check.assertions.entrySet()) {
                Logging.debug("- Assertion: {0}", i);
                final OsmPrimitive p = OsmUtils.createPrimitive(i.getKey(), getLocation(check, insideMethod), true);
                // Build minimal ordered list of checks to run to test the assertion
                List<Set<MapCSSTagChecker.TagCheck>> checksToRun = new ArrayList<>();
                Set<MapCSSTagChecker.TagCheck> checkDependencies = getTagCheckDependencies(check, schecks);
                if (!checkDependencies.isEmpty()) {
                    checksToRun.add(checkDependencies);
                }
                checksToRun.add(Collections.singleton(check));
                // Add primitive to dataset to avoid DataIntegrityProblemException when evaluating selectors
                addPrimitive(ds, p);
                final Collection<TestError> pErrors = MapCSSTagChecker.getErrorsForPrimitive(p, true, checksToRun);
                Logging.debug("- Errors: {0}", pErrors);
                final boolean isError = pErrors.stream().anyMatch(e -> e.getTester() instanceof MapCSSTagChecker.MapCSSTagCheckerAndRule
                        && ((MapCSSTagChecker.MapCSSTagCheckerAndRule) e.getTester()).rule.equals(check.rule));
                if (isError != i.getValue()) {
                    assertionErrors.add(MessageFormat.format("Expecting test ''{0}'' (i.e., {1}) to {2} {3} (i.e., {4})",
                            check.getMessage(p), check.rule.selectors, i.getValue() ? "match" : "not match", i.getKey(), p.getKeys()));
                }
                if (isError) {
                    // Check that autofix works as expected
                    Command fix = check.fixPrimitive(p);
                    if (fix != null && fix.executeCommand() && !MapCSSTagChecker.getErrorsForPrimitive(p, true, checksToRun).isEmpty()) {
                        assertionErrors.add(MessageFormat.format("Autofix does not work for test ''{0}'' (i.e., {1})",
                                check.getMessage(p), check.rule.selectors));
                    }
                }
                ds.removePrimitive(p);
            }
        }
        return assertionErrors;
    }

    private static Method getFunctionMethod(String method) {
        try {
            return Functions.class.getDeclaredMethod(method, Environment.class, String.class);
        } catch (NoSuchMethodException | SecurityException e) {
            Logging.error(e);
            return null;
        }
    }

    private static void addPrimitive(DataSet ds, OsmPrimitive p) {
        if (p instanceof Way) {
            ((Way) p).getNodes().forEach(n -> addPrimitive(ds, n));
        } else if (p instanceof Relation) {
            ((Relation) p).getMembers().forEach(m -> addPrimitive(ds, m.getMember()));
        }
        ds.addPrimitive(p);
    }

    private static LatLon getLocation(MapCSSTagChecker.TagCheck check, Method insideMethod) {
        Optional<String> inside = getFirstInsideCountry(check, insideMethod);
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

    private static Optional<String> getFirstInsideCountry(MapCSSTagChecker.TagCheck check, Method insideMethod) {
        return check.rule.selectors.stream()
                .filter(s -> s instanceof Selector.GeneralSelector)
                .flatMap(s -> ((Selector.GeneralSelector) s).getConditions().stream())
                .filter(c -> c instanceof ConditionFactory.ExpressionCondition)
                .map(c -> ((ConditionFactory.ExpressionCondition) c).getExpression())
                .filter(c -> c instanceof ExpressionFactory.ParameterFunction)
                .map(c -> (ExpressionFactory.ParameterFunction) c)
                .filter(c -> c.getMethod().equals(insideMethod))
                .flatMap(c -> c.getArgs().stream())
                .filter(e -> e instanceof LiteralExpression)
                .map(e -> ((LiteralExpression) e).getLiteral())
                .filter(l -> l instanceof String)
                .map(l -> ((String) l).split(",")[0])
                .findFirst();
    }

    /**
     * Returns the set of tagchecks on which this check depends on.
     * @param check the tagcheck
     * @param schecks the collection of tagcheks to search in
     * @return the set of tagchecks on which this check depends on
     * @since 7881
     */
    private static Set<MapCSSTagChecker.TagCheck> getTagCheckDependencies(MapCSSTagChecker.TagCheck check,
                                                                          Collection<MapCSSTagChecker.TagCheck> schecks) {
        Set<MapCSSTagChecker.TagCheck> result = new HashSet<>();
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
