// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.TagCheck;
import org.openstreetmap.josm.gui.mappaint.mapcss.Declaration;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource.MapCSSRuleIndex;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Helper class for {@link MapCSSTagChecker} to store indexes of rules
 * @author Gerd
 *
 */
public final class MapCSSTagCheckerIndex {
    final Map<Declaration, TagCheck> ruleToCheckMap = new HashMap<>();

    static final boolean ALL_TESTS = true;
    static final boolean ONLY_SELECTED_TESTS = false;

    /**
     * Rules for nodes
     */
    final MapCSSRuleIndex nodeRules = new MapCSSRuleIndex();
    /**
     * Rules for ways without tag area=no
     */
    final MapCSSRuleIndex wayRules = new MapCSSRuleIndex();
    /**
     * Rules for ways with tag area=no
     */
    final MapCSSRuleIndex wayNoAreaRules = new MapCSSRuleIndex();
    /**
     * Rules for relations that are not multipolygon relations
     */
    final MapCSSRuleIndex relationRules = new MapCSSRuleIndex();
    /**
     * Rules for multipolygon relations
     */
    final MapCSSRuleIndex multipolygonRules = new MapCSSRuleIndex();
    /**
     * rules to apply canvas properties
     */
    final MapCSSRuleIndex canvasRules = new MapCSSRuleIndex();

    static MapCSSTagCheckerIndex createMapCSSTagCheckerIndex(MultiMap<String, TagCheck> checks, boolean includeOtherSeverity, boolean allTests) {
        final MapCSSTagCheckerIndex index = new MapCSSTagCheckerIndex();
        final Stream<MapCSSRule> ruleStream = checks.values().stream()
                .flatMap(Collection::stream)
                // Ignore "information" level checks if not wanted, unless they also set a MapCSS class
                .filter(c -> includeOtherSeverity || Severity.OTHER != c.getSeverity() || !c.setClassExpressions.isEmpty())
                .filter(c -> allTests || c.rule.selectors.stream().anyMatch(Selector.ChildOrParentSelector.class::isInstance))
                .peek(c -> index.ruleToCheckMap.put(c.rule.declaration, c))
                .map(c -> c.rule);
        index.buildIndex(ruleStream);
        return index;
    }

    /**
     * Clear the index.
     * <p>
     * You must own the write lock STYLE_SOURCE_LOCK when calling this method.
     */
    public void clear() {
        ruleToCheckMap.clear();
        nodeRules.clear();
        wayRules.clear();
        wayNoAreaRules.clear();
        relationRules.clear();
        multipolygonRules.clear();
        canvasRules.clear();
    }

    /**
     * Builds and initializes the index.
     * <p>
     * You must own the write lock of STYLE_SOURCE_LOCK when calling this method.
     */
    public void buildIndex(Stream<MapCSSRule> ruleStream) {
        clear();
        // optimization: filter rules for different primitive types
        ruleStream.forEach(rule -> {
            final Map<String, MapCSSRule> selectorsByBase = rule.selectors.stream()
                    .collect(Collectors.groupingBy(Selector::getBase,
                            Collectors.collectingAndThen(Collectors.toList(), selectors -> new MapCSSRule(selectors, rule.declaration))));
            selectorsByBase.forEach((base, optRule) -> {
                switch (base) {
                case Selector.BASE_NODE:
                    nodeRules.add(optRule);
                    break;
                case Selector.BASE_WAY:
                    wayNoAreaRules.add(optRule);
                    wayRules.add(optRule);
                    break;
                case Selector.BASE_AREA:
                    wayRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case Selector.BASE_RELATION:
                    relationRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case Selector.BASE_ANY:
                    nodeRules.add(optRule);
                    wayRules.add(optRule);
                    wayNoAreaRules.add(optRule);
                    relationRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case Selector.BASE_CANVAS:
                    canvasRules.add(optRule);
                    break;
                case Selector.BASE_META:
                case Selector.BASE_SETTING:
                case Selector.BASE_SETTINGS:
                    break;
                default:
                    final RuntimeException e = new JosmRuntimeException(MessageFormat.format("Unknown MapCSS base selector {0}", base));
                    Logging.warn(tr("Failed to index validator rules. Error was: {0}", e.getMessage()));
                    Logging.error(e);
                }
            });
        });
        initIndex();
    }

    private void initIndex() {
        nodeRules.initIndex();
        wayRules.initIndex();
        wayNoAreaRules.initIndex();
        relationRules.initIndex();
        multipolygonRules.initIndex();
        canvasRules.initIndex();
    }

    /**
     * Get the index of rules for the given primitive.
     * @param p the primitive
     * @return index of rules for the given primitive
     */
    public MapCSSRuleIndex get(IPrimitive p) {
        if (p instanceof INode) {
            return nodeRules;
        } else if (p instanceof IWay) {
            if (OsmUtils.isFalse(p.get("area"))) {
                return wayNoAreaRules;
            } else {
                return wayRules;
            }
        } else if (p instanceof IRelation) {
            if (((IRelation<?>) p).isMultipolygon()) {
                return multipolygonRules;
            } else if (p.hasKey("#canvas")) {
                return canvasRules;
            } else {
                return relationRules;
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + p);
        }
    }

    /**
     * Get a subset of all rules that might match the primitive. Rules not included in the result are guaranteed to
     * not match this primitive.
     * <p>
     * You must have a read lock of STYLE_SOURCE_LOCK when calling this method.
     *
     * @param osm the primitive to match
     * @return An iterator over possible rules in the right order.
     */
    public Iterator<MapCSSRule> getRuleCandidates(IPrimitive osm) {
        return get(osm).getRuleCandidates(osm);
    }

    /**
     * return the TagCheck for which the given indexed rule was created.
     * @param rule an indexed rule
     * @return the original TagCheck
     */
    public TagCheck getCheck(MapCSSRule rule) {
        return ruleToCheckMap.get(rule.declaration);
    }
}
