// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.TagCheck;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource.MapCSSRuleIndex;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Helper class for {@link MapCSSTagChecker} to store indexes of rules
 * @author Gerd
 *
 */
final class MapCSSTagCheckerIndex {
    final Map<MapCSSRule, TagCheck> ruleToCheckMap = new HashMap<>();

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

    MapCSSTagCheckerIndex(MultiMap<String, TagCheck> checks, boolean includeOtherSeverity, boolean allTests) {
        buildIndex(checks, includeOtherSeverity, allTests);
    }

    private void buildIndex(MultiMap<String, TagCheck> checks, boolean includeOtherSeverity, boolean allTests) {
        List<TagCheck> allChecks = new ArrayList<>();
        for (Set<TagCheck> cs : checks.values()) {
            allChecks.addAll(cs);
        }

        ruleToCheckMap.clear();
        nodeRules.clear();
        wayRules.clear();
        wayNoAreaRules.clear();
        relationRules.clear();
        multipolygonRules.clear();

        // optimization: filter rules for different primitive types
        for (TagCheck c : allChecks) {
            if (!includeOtherSeverity && Severity.OTHER == c.getSeverity()
                    && c.setClassExpressions.isEmpty()) {
                // Ignore "information" level checks if not wanted, unless they also set a MapCSS class
                continue;
            }

            for (Selector s : c.rule.selectors) {
                boolean hasLeftRightSel = s instanceof Selector.ChildOrParentSelector;
                if (!allTests && !hasLeftRightSel) {
                    continue;
                }

                MapCSSRule optRule = new MapCSSRule(s, c.rule.declaration);

                ruleToCheckMap.put(optRule, c);
                final String base = s.getBase();
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
                case Selector.BASE_META:
                case Selector.BASE_SETTING:
                    break;
                default:
                    final RuntimeException e = new JosmRuntimeException(MessageFormat.format("Unknown MapCSS base selector {0}", base));
                    Logging.warn(tr("Failed to index validator rules. Error was: {0}", e.getMessage()));
                    Logging.error(e);
                }
            }
        }
        nodeRules.initIndex();
        wayRules.initIndex();
        wayNoAreaRules.initIndex();
        relationRules.initIndex();
        multipolygonRules.initIndex();
    }

    /**
     * Get the index of rules for the given primitive.
     * @param p the primitve
     * @return index of rules for the given primitive
     */
    public MapCSSRuleIndex get(OsmPrimitive p) {
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
            } else {
                return relationRules;
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + p);
        }
    }

    /**
     * return the TagCheck for which the given indexed rule was created.
     * @param rule an indexed rule
     * @return the original TagCheck
     */
    public TagCheck getCheck(MapCSSRule rule) {
        return ruleToCheckMap.get(rule);
    }
}
