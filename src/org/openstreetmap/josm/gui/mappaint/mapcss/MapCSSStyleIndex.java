// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Store indexes of {@link MapCSSRule}s using {@link MapCSSRuleIndex} differentiated by {@linkplain Selector#getBase() base}
 */
public final class MapCSSStyleIndex {

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

    /**
     * Clear the index.
     * <p>
     * You must own the write lock STYLE_SOURCE_LOCK when calling this method.
     */
    public void clear() {
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
     * @param ruleStream the rules to index
     */
    public void buildIndex(Stream<MapCSSRule> ruleStream) {
        clear();
        // optimization: filter rules for different primitive types
        ruleStream.forEach(rule -> {
            final Map<String, MapCSSRule> selectorsByBase;
            final Set<String> bases = rule.selectors.stream().map(Selector::getBase).collect(Collectors.toSet());
            if (bases.size() == 1) {
                // reuse rule
                selectorsByBase = Collections.singletonMap(bases.iterator().next(), rule);
            } else {
                selectorsByBase = rule.selectors.stream()
                        .collect(Collectors.groupingBy(Selector::getBase,
                                Collectors.collectingAndThen(Collectors.toList(), selectors -> new MapCSSRule(selectors, rule.declaration))));
            }
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
     * Check if this index is empty.
     * @return true if this index is empty.
     * @since 16784
     */
    public boolean isEmpty() {
        return nodeRules.isEmpty() && wayRules.isEmpty() && wayNoAreaRules.isEmpty() && relationRules.isEmpty()
                && multipolygonRules.isEmpty() && canvasRules.isEmpty();
    }
}
