// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.KeyValueVisitor;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition;
import org.openstreetmap.josm.tools.Utils;

/**
 * A collection of {@link MapCSSRule}s, that are indexed by tag key and value.
 *
 * Speeds up the process of finding all rules that match a certain primitive.
 *
 * Rules with a {@link SimpleKeyValueCondition} [key=value] or rules that require a specific key to be set are
 * indexed. Now you only need to loop the tags of a primitive to retrieve the possibly matching rules.
 *
 * To use this index, you need to {@link #add(MapCSSRule)} all rules to it. You then need to call
 * {@link #initIndex()}. Afterwards, you can use {@link #getRuleCandidates(IPrimitive)} to get an iterator over
 * all rules that might be applied to that primitive.
 */
public class MapCSSRuleIndex {
    /**
     * This is an iterator over all rules that are marked as possible in the bitset.
     *
     * @author Michael Zangl
     */
    private final class RuleCandidatesIterator implements Iterator<MapCSSRule>, KeyValueVisitor {
        private final BitSet ruleCandidates;
        private int next;

        private RuleCandidatesIterator(BitSet ruleCandidates) {
            this.ruleCandidates = ruleCandidates;
        }

        @Override
        public boolean hasNext() {
            return next >= 0 && next < rules.size();
        }

        @Override
        public MapCSSRule next() {
            if (!hasNext())
                throw new NoSuchElementException();
            MapCSSRule rule = rules.get(next);
            next = ruleCandidates.nextSetBit(next + 1);
            return rule;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitKeyValue(Tagged p, String key, String value) {
            MapCSSKeyRules v = index.get(key);
            if (v != null) {
                BitSet rs = v.get(value);
                ruleCandidates.or(rs);
            }
        }

        /**
         * Call this before using the iterator.
         */
        public void prepare() {
            next = ruleCandidates.nextSetBit(0);
        }
    }

    /**
     * This is a map of all rules that are only applied if the primitive has a given key (and possibly value)
     *
     * @author Michael Zangl
     */
    private static final class MapCSSKeyRules {
        /**
         * The indexes of rules that might be applied if this tag is present and the value has no special handling.
         */
        BitSet generalRules = new BitSet();

        /**
         * A map that sores the indexes of rules that might be applied if the key=value pair is present on this
         * primitive. This includes all key=* rules.
         */
        Map<String, BitSet> specialRules = new HashMap<>();

        public void addForKey(int ruleIndex) {
            generalRules.set(ruleIndex);
            for (BitSet r : specialRules.values()) {
                r.set(ruleIndex);
            }
        }

        public void addForKeyAndValue(String value, int ruleIndex) {
            BitSet forValue = specialRules.get(value);
            if (forValue == null) {
                forValue = new BitSet();
                forValue.or(generalRules);
                specialRules.put(value.intern(), forValue);
            }
            forValue.set(ruleIndex);
        }

        public BitSet get(String value) {
            BitSet forValue = specialRules.get(value);
            if (forValue != null) return forValue; else return generalRules;
        }
    }

    /**
     * All rules this index is for. Once this index is built, this list is sorted.
     */
    private final List<MapCSSRule> rules = new ArrayList<>();
    /**
     * All rules that only apply when the given key is present.
     */
    private final Map<String, MapCSSKeyRules> index = new HashMap<>();
    /**
     * Rules that do not require any key to be present. Only the index in the {@link #rules} array is stored.
     */
    private final BitSet remaining = new BitSet();

    /**
     * Add a rule to this index. This needs to be called before {@link #initIndex()} is called.
     * @param rule The rule to add.
     */
    public void add(MapCSSRule rule) {
        rules.add(rule);
    }

    /**
     * Initialize the index.
     * <p>
     * You must own the write lock of STYLE_SOURCE_LOCK when calling this method.
     */
    public void initIndex() {
        Collections.sort(rules);
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            MapCSSRule r = rules.get(ruleIndex);
            for (Selector selector : r.selectors) {
                Selector selRightmost = selector;
                while (selRightmost instanceof Selector.ChildOrParentSelector) {
                    selRightmost = ((Selector.ChildOrParentSelector) selRightmost).right;
                }
                final List<Condition> conditions = selRightmost.getConditions();
                if (conditions == null || conditions.isEmpty()) {
                    remaining.set(ruleIndex);
                    continue;
                }
                Optional<SimpleKeyValueCondition> lastCondition = Utils.filteredCollection(conditions, SimpleKeyValueCondition.class)
                        .stream()
                        .reduce((first, last) -> last);
                if (lastCondition.isPresent()) {
                    getEntryInIndex(lastCondition.get().k).addForKeyAndValue(lastCondition.get().v, ruleIndex);
                } else {
                    String key = findAnyRequiredKey(conditions);
                    if (key != null) {
                        getEntryInIndex(key).addForKey(ruleIndex);
                    } else {
                        remaining.set(ruleIndex);
                    }
                }
            }
        }
    }

    /**
     * Search for any key that condition might depend on.
     *
     * @param conds The conditions to search through.
     * @return An arbitrary key this rule depends on or <code>null</code> if there is no such key.
     */
    private static String findAnyRequiredKey(List<Condition> conds) {
        String key = null;
        for (Condition c : conds) {
            if (c instanceof KeyCondition) {
                KeyCondition keyCondition = (KeyCondition) c;
                if (!keyCondition.negateResult && conditionRequiresKeyPresence(keyCondition.matchType)) {
                    key = keyCondition.label;
                }
            } else if (c instanceof KeyValueCondition) {
                KeyValueCondition keyValueCondition = (KeyValueCondition) c;
                if (keyValueCondition.requiresExactKeyMatch()) {
                    key = keyValueCondition.k;
                }
            }
        }
        return key;
    }

    private static boolean conditionRequiresKeyPresence(KeyMatchType matchType) {
        return matchType != KeyMatchType.REGEX;
    }

    private MapCSSKeyRules getEntryInIndex(String key) {
        MapCSSKeyRules rulesWithMatchingKey = index.get(key);
        if (rulesWithMatchingKey == null) {
            rulesWithMatchingKey = new MapCSSKeyRules();
            index.put(key.intern(), rulesWithMatchingKey);
        }
        return rulesWithMatchingKey;
    }

    /**
     * Get a subset of all rules that might match the primitive. Rules not included in the result are guaranteed to
     * not match this primitive.
     * <p>
     * You must have a read lock of STYLE_SOURCE_LOCK when calling this method.
     *
     * @param osm the primitive to match
     * @return An iterator over possible rules in the right order.
     * @since 13810 (signature)
     */
    public Iterator<MapCSSRule> getRuleCandidates(IPrimitive osm) {
        final BitSet ruleCandidates = new BitSet(rules.size());
        ruleCandidates.or(remaining);

        final RuleCandidatesIterator candidatesIterator = new RuleCandidatesIterator(ruleCandidates);
        osm.visitKeys(candidatesIterator);
        candidatesIterator.prepare();
        return candidatesIterator;
    }

    /**
     * Clear the index.
     * <p>
     * You must own the write lock STYLE_SOURCE_LOCK when calling this method.
     */
    public void clear() {
        rules.clear();
        index.clear();
        remaining.clear();
    }

    /**
     * Check if this index is empty.
     * @return true if this index is empty.
     * @since 16784
     */
    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
