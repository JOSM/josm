// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.KeyValueVisitor;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.StyleSetting;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.BooleanStyleSetting;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.OptimizedGeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.TokenMgrError;
import org.openstreetmap.josm.gui.mappaint.styleelement.LineElement;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a mappaint style that is based on MapCSS rules.
 */
public class MapCSSStyleSource extends StyleSource {

    /**
     * The accepted MIME types sent in the HTTP Accept header.
     * @since 6867
     */
    public static final String MAPCSS_STYLE_MIME_TYPES =
            "text/x-mapcss, text/mapcss, text/css; q=0.9, text/plain; q=0.8, application/zip, application/octet-stream; q=0.5";

    /**
     * all rules in this style file
     */
    public final List<MapCSSRule> rules = new ArrayList<>();
    /**
     * Rules for nodes
     */
    public final MapCSSRuleIndex nodeRules = new MapCSSRuleIndex();
    /**
     * Rules for ways without tag area=no
     */
    public final MapCSSRuleIndex wayRules = new MapCSSRuleIndex();
    /**
     * Rules for ways with tag area=no
     */
    public final MapCSSRuleIndex wayNoAreaRules = new MapCSSRuleIndex();
    /**
     * Rules for relations that are not multipolygon relations
     */
    public final MapCSSRuleIndex relationRules = new MapCSSRuleIndex();
    /**
     * Rules for multipolygon relations
     */
    public final MapCSSRuleIndex multipolygonRules = new MapCSSRuleIndex();
    /**
     * rules to apply canvas properties
     */
    public final MapCSSRuleIndex canvasRules = new MapCSSRuleIndex();

    private Color backgroundColorOverride;
    private String css;
    private ZipFile zipFile;

    /**
     * This lock prevents concurrent execution of {@link MapCSSRuleIndex#clear() } /
     * {@link MapCSSRuleIndex#initIndex()} and {@link MapCSSRuleIndex#getRuleCandidates }.
     *
     * For efficiency reasons, these methods are synchronized higher up the
     * stack trace.
     */
    public static final ReadWriteLock STYLE_SOURCE_LOCK = new ReentrantReadWriteLock();

    /**
     * Set of all supported MapCSS keys.
     */
    static final Set<String> SUPPORTED_KEYS = new HashSet<>();
    static {
        Field[] declaredFields = StyleKeys.class.getDeclaredFields();
        for (Field f : declaredFields) {
            try {
                SUPPORTED_KEYS.add((String) f.get(null));
                if (!f.getName().toLowerCase(Locale.ENGLISH).replace('_', '-').equals(f.get(null))) {
                    throw new JosmRuntimeException(f.getName());
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new JosmRuntimeException(ex);
            }
        }
        for (LineElement.LineType lt : LineElement.LineType.values()) {
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.COLOR);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES_BACKGROUND_COLOR);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES_BACKGROUND_OPACITY);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES_OFFSET);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.LINECAP);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.LINEJOIN);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.MITERLIMIT);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.OFFSET);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.OPACITY);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.REAL_WIDTH);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.WIDTH);
        }
    }

    /**
     * A collection of {@link MapCSSRule}s, that are indexed by tag key and value.
     *
     * Speeds up the process of finding all rules that match a certain primitive.
     *
     * Rules with a {@link SimpleKeyValueCondition} [key=value] or rules that require a specific key to be set are
     * indexed. Now you only need to loop the tags of a primitive to retrieve the possibly matching rules.
     *
     * To use this index, you need to {@link #add(MapCSSRule)} all rules to it. You then need to call
     * {@link #initIndex()}. Afterwards, you can use {@link #getRuleCandidates(OsmPrimitive)} to get an iterator over
     * all rules that might be applied to that primitive.
     */
    public static class MapCSSRuleIndex {
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
                // find the rightmost selector, this must be a GeneralSelector
                Selector selRightmost = r.selector;
                while (selRightmost instanceof ChildOrParentSelector) {
                    selRightmost = ((ChildOrParentSelector) selRightmost).right;
                }
                OptimizedGeneralSelector s = (OptimizedGeneralSelector) selRightmost;
                if (s.conds == null) {
                    remaining.set(ruleIndex);
                    continue;
                }
                List<SimpleKeyValueCondition> sk = new ArrayList<>(Utils.filteredCollection(s.conds,
                        SimpleKeyValueCondition.class));
                if (!sk.isEmpty()) {
                    SimpleKeyValueCondition c = sk.get(sk.size() - 1);
                    getEntryInIndex(c.k).addForKeyAndValue(c.v, ruleIndex);
                } else {
                    String key = findAnyRequiredKey(s.conds);
                    if (key != null) {
                        getEntryInIndex(key).addForKey(ruleIndex);
                    } else {
                        remaining.set(ruleIndex);
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
                    if (!Op.NEGATED_OPS.contains(keyValueCondition.op)) {
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
         */
        public Iterator<MapCSSRule> getRuleCandidates(OsmPrimitive osm) {
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
    }

    /**
     * Constructs a new, active {@link MapCSSStyleSource}.
     * @param url URL that {@link org.openstreetmap.josm.io.CachedFile} understands
     * @param name The name for this StyleSource
     * @param shortdescription The title for that source.
     */
    public MapCSSStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
    }

    /**
     * Constructs a new {@link MapCSSStyleSource}
     * @param entry The entry to copy the data (url, name, ...) from.
     */
    public MapCSSStyleSource(SourceEntry entry) {
        super(entry);
    }

    /**
     * <p>Creates a new style source from the MapCSS styles supplied in
     * {@code css}</p>
     *
     * @param css the MapCSS style declaration. Must not be null.
     * @throws IllegalArgumentException if {@code css} is null
     */
    public MapCSSStyleSource(String css) {
        super(null, null, null);
        CheckParameterUtil.ensureParameterNotNull(css);
        this.css = css;
    }

    @Override
    public void loadStyleSource() {
        STYLE_SOURCE_LOCK.writeLock().lock();
        try {
            init();
            rules.clear();
            nodeRules.clear();
            wayRules.clear();
            wayNoAreaRules.clear();
            relationRules.clear();
            multipolygonRules.clear();
            canvasRules.clear();
            try (InputStream in = getSourceInputStream()) {
                try {
                    // evaluate @media { ... } blocks
                    MapCSSParser preprocessor = new MapCSSParser(in, "UTF-8", MapCSSParser.LexicalState.PREPROCESSOR);
                    String mapcss = preprocessor.pp_root(this);

                    // do the actual mapcss parsing
                    InputStream in2 = new ByteArrayInputStream(mapcss.getBytes(StandardCharsets.UTF_8));
                    MapCSSParser parser = new MapCSSParser(in2, "UTF-8", MapCSSParser.LexicalState.DEFAULT);
                    parser.sheet(this);

                    loadMeta();
                    loadCanvas();
                    loadSettings();
                } finally {
                    closeSourceInputStream(in);
                }
            } catch (IOException e) {
                Logging.warn(tr("Failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
                Logging.log(Logging.LEVEL_ERROR, e);
                logError(e);
            } catch (TokenMgrError e) {
                Logging.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
                Logging.error(e);
                logError(e);
            } catch (ParseException e) {
                Logging.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
                Logging.error(e);
                logError(new ParseException(e.getMessage())); // allow e to be garbage collected, it links to the entire token stream
            }
            // optimization: filter rules for different primitive types
            for (MapCSSRule r: rules) {
                // find the rightmost selector, this must be a GeneralSelector
                Selector selRightmost = r.selector;
                while (selRightmost instanceof ChildOrParentSelector) {
                    selRightmost = ((ChildOrParentSelector) selRightmost).right;
                }
                MapCSSRule optRule = new MapCSSRule(r.selector.optimizedBaseCheck(), r.declaration);
                final String base = ((GeneralSelector) selRightmost).getBase();
                switch (base) {
                    case "node":
                        nodeRules.add(optRule);
                        break;
                    case "way":
                        wayNoAreaRules.add(optRule);
                        wayRules.add(optRule);
                        break;
                    case "area":
                        wayRules.add(optRule);
                        multipolygonRules.add(optRule);
                        break;
                    case "relation":
                        relationRules.add(optRule);
                        multipolygonRules.add(optRule);
                        break;
                    case "*":
                        nodeRules.add(optRule);
                        wayRules.add(optRule);
                        wayNoAreaRules.add(optRule);
                        relationRules.add(optRule);
                        multipolygonRules.add(optRule);
                        break;
                    case "canvas":
                        canvasRules.add(r);
                        break;
                    case "meta":
                    case "setting":
                        break;
                    default:
                        final RuntimeException e = new JosmRuntimeException(MessageFormat.format("Unknown MapCSS base selector {0}", base));
                        Logging.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
                        Logging.error(e);
                        logError(e);
                }
            }
            nodeRules.initIndex();
            wayRules.initIndex();
            wayNoAreaRules.initIndex();
            relationRules.initIndex();
            multipolygonRules.initIndex();
            canvasRules.initIndex();
        } finally {
            STYLE_SOURCE_LOCK.writeLock().unlock();
        }
    }

    @Override
    public InputStream getSourceInputStream() throws IOException {
        if (css != null) {
            return new ByteArrayInputStream(css.getBytes(StandardCharsets.UTF_8));
        }
        CachedFile cf = getCachedFile();
        if (isZip) {
            File file = cf.getFile();
            zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            zipIcons = file;
            I18n.addTexts(zipIcons);
            ZipEntry zipEntry = zipFile.getEntry(zipEntryPath);
            return zipFile.getInputStream(zipEntry);
        } else {
            zipFile = null;
            zipIcons = null;
            return cf.getInputStream();
        }
    }

    @Override
    @SuppressWarnings("resource")
    public CachedFile getCachedFile() throws IOException {
        return new CachedFile(url).setHttpAccept(MAPCSS_STYLE_MIME_TYPES); // NOSONAR
    }

    @Override
    public void closeSourceInputStream(InputStream is) {
        super.closeSourceInputStream(is);
        if (isZip) {
            Utils.close(zipFile);
        }
    }

    /**
     * load meta info from a selector "meta"
     */
    private void loadMeta() {
        Cascade c = constructSpecial("meta");
        String pTitle = c.get("title", null, String.class);
        if (title == null) {
            title = pTitle;
        }
        String pIcon = c.get("icon", null, String.class);
        if (icon == null) {
            icon = pIcon;
        }
    }

    private void loadCanvas() {
        Cascade c = constructSpecial("canvas");
        backgroundColorOverride = c.get("fill-color", null, Color.class);
    }

    private void loadSettings() {
        settings.clear();
        settingValues.clear();
        MultiCascade mc = new MultiCascade();
        Node n = new Node();
        String code = LanguageInfo.getJOSMLocaleCode();
        n.put("lang", code);
        // create a fake environment to read the meta data block
        Environment env = new Environment(n, mc, "default", this);

        for (MapCSSRule r : rules) {
            if (r.selector instanceof GeneralSelector) {
                GeneralSelector gs = (GeneralSelector) r.selector;
                if ("setting".equals(gs.getBase())) {
                    if (!gs.matchesConditions(env)) {
                        continue;
                    }
                    env.layer = null;
                    env.layer = gs.getSubpart().getId(env);
                    r.execute(env);
                }
            }
        }
        for (Entry<String, Cascade> e : mc.getLayers()) {
            if ("default".equals(e.getKey())) {
                Logging.warn("setting requires layer identifier e.g. 'setting::my_setting {...}'");
                continue;
            }
            Cascade c = e.getValue();
            String type = c.get("type", null, String.class);
            StyleSetting set = null;
            if ("boolean".equals(type)) {
                set = BooleanStyleSetting.create(c, this, e.getKey());
            } else {
                Logging.warn("Unkown setting type: "+type);
            }
            if (set != null) {
                settings.add(set);
                settingValues.put(e.getKey(), set.getValue());
            }
        }
    }

    private Cascade constructSpecial(String type) {

        MultiCascade mc = new MultiCascade();
        Node n = new Node();
        String code = LanguageInfo.getJOSMLocaleCode();
        n.put("lang", code);
        // create a fake environment to read the meta data block
        Environment env = new Environment(n, mc, "default", this);

        for (MapCSSRule r : rules) {
            if (r.selector instanceof GeneralSelector) {
                GeneralSelector gs = (GeneralSelector) r.selector;
                if (gs.getBase().equals(type)) {
                    if (!gs.matchesConditions(env)) {
                        continue;
                    }
                    r.execute(env);
                }
            }
        }
        return mc.getCascade("default");
    }

    @Override
    public Color getBackgroundColorOverride() {
        return backgroundColorOverride;
    }

    @Override
    public void apply(MultiCascade mc, OsmPrimitive osm, double scale, boolean pretendWayIsClosed) {
        MapCSSRuleIndex matchingRuleIndex;
        if (osm instanceof Node) {
            matchingRuleIndex = nodeRules;
        } else if (osm instanceof Way) {
            if (osm.isKeyFalse("area")) {
                matchingRuleIndex = wayNoAreaRules;
            } else {
                matchingRuleIndex = wayRules;
            }
        } else if (osm instanceof Relation) {
            if (((Relation) osm).isMultipolygon()) {
                matchingRuleIndex = multipolygonRules;
            } else if (osm.hasKey("#canvas")) {
                matchingRuleIndex = canvasRules;
            } else {
                matchingRuleIndex = relationRules;
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + osm);
        }

        Environment env = new Environment(osm, mc, null, this);
        // the declaration indices are sorted, so it suffices to save the last used index
        int lastDeclUsed = -1;

        Iterator<MapCSSRule> candidates = matchingRuleIndex.getRuleCandidates(osm);
        while (candidates.hasNext()) {
            MapCSSRule r = candidates.next();
            env.clearSelectorMatchingInformation();
            env.layer = r.selector.getSubpart().getId(env);
            String sub = env.layer;
            if (r.selector.matches(env)) { // as side effect env.parent will be set (if s is a child selector)
                Selector s = r.selector;
                if (s.getRange().contains(scale)) {
                    mc.range = Range.cut(mc.range, s.getRange());
                } else {
                    mc.range = mc.range.reduceAround(scale, s.getRange());
                    continue;
                }

                if (r.declaration.idx == lastDeclUsed)
                    continue; // don't apply one declaration more than once
                lastDeclUsed = r.declaration.idx;
                if ("*".equals(sub)) {
                    for (Entry<String, Cascade> entry : mc.getLayers()) {
                        env.layer = entry.getKey();
                        if ("*".equals(env.layer)) {
                            continue;
                        }
                        r.execute(env);
                    }
                }
                env.layer = sub;
                r.execute(env);
            }
        }
    }

    /**
     * Evaluate a supports condition
     * @param feature The feature to evaluate for
     * @param val The additional parameter passed to evaluate
     * @return <code>true</code> if JSOM supports that feature
     */
    public boolean evalSupportsDeclCondition(String feature, Object val) {
        if (feature == null) return false;
        if (SUPPORTED_KEYS.contains(feature)) return true;
        switch (feature) {
            case "user-agent":
                String s = Cascade.convertTo(val, String.class);
                return "josm".equals(s);
            case "min-josm-version":
                Float min = Cascade.convertTo(val, Float.class);
                return min != null && Math.round(min) <= Version.getInstance().getVersion();
            case "max-josm-version":
                Float max = Cascade.convertTo(val, Float.class);
                return max != null && Math.round(max) >= Version.getInstance().getVersion();
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return Utils.join("\n", rules);
    }
}
