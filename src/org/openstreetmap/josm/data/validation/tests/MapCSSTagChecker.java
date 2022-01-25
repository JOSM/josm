// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleIndex;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.TokenMgrError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Utils;

/**
 * MapCSS-based tag checker/fixer.
 * @since 6506
 */
public class MapCSSTagChecker extends Test.TagTest {
    private MapCSSStyleIndex indexData;
    private final Map<MapCSSRule, MapCSSTagCheckerAndRule> ruleToCheckMap = new HashMap<>();
    private static final Map<IPrimitive, Area> mpAreaCache = new HashMap<>();
    private static final Set<IPrimitive> toMatchForSurrounding = new HashSet<>();
    static final boolean ALL_TESTS = true;
    static final boolean ONLY_SELECTED_TESTS = false;

    /**
     * Cached version of {@link ValidatorPrefHelper#PREF_OTHER}, see #20745.
     */
    private static final CachingProperty<Boolean> PREF_OTHER = new BooleanProperty("validator.other", false).cached();

    /**
     * The preference key for tag checker source entries.
     * @since 6670
     */
    public static final String ENTRIES_PREF_KEY = "validator." + MapCSSTagChecker.class.getName() + ".entries";

    /**
     * Constructs a new {@code MapCSSTagChecker}.
     */
    public MapCSSTagChecker() {
        super(tr("Tag checker (MapCSS based)"), tr("This test checks for errors in tag keys and values."));
    }

    final MultiMap<String, MapCSSTagCheckerRule> checks = new MultiMap<>();

    /** maps the source URL for a test to the title shown in the dialog where known */
    private final Map<String, String> urlTitles = new HashMap<>();

    /**
     * Result of {@link MapCSSTagCheckerRule#readMapCSS}
     * @since 8936
     */
    public static class ParseResult {
        /** Checks successfully parsed */
        public final List<MapCSSTagCheckerRule> parseChecks;
        /** Errors that occurred during parsing */
        public final Collection<Throwable> parseErrors;

        /**
         * Constructs a new {@code ParseResult}.
         * @param parseChecks Checks successfully parsed
         * @param parseErrors Errors that occurred during parsing
         */
        public ParseResult(List<MapCSSTagCheckerRule> parseChecks, Collection<Throwable> parseErrors) {
            this.parseChecks = parseChecks;
            this.parseErrors = parseErrors;
        }
    }

    static class MapCSSTagCheckerAndRule extends MapCSSTagChecker {
        public final MapCSSRule rule;
        private final MapCSSTagCheckerRule tagCheck;
        private final String source;

        MapCSSTagCheckerAndRule(MapCSSRule rule) {
            this.rule = rule;
            this.tagCheck = null;
            this.source = "";
        }

        MapCSSTagCheckerAndRule(MapCSSTagCheckerRule tagCheck, String source) {
            this.rule = tagCheck.rule;
            this.tagCheck = tagCheck;
            this.source = source;
        }

        @Override
        public String toString() {
            return "MapCSSTagCheckerAndRule [rule=" + rule + ']';
        }

        @Override
        public String getSource() {
            return source;
        }
    }

    static MapCSSStyleIndex createMapCSSTagCheckerIndex(
            MultiMap<String, MapCSSTagCheckerRule> checks, boolean includeOtherSeverity, boolean allTests) {
        final MapCSSStyleIndex index = new MapCSSStyleIndex();
        final Stream<MapCSSRule> ruleStream = checks.values().stream()
                .flatMap(Collection::stream)
                // Ignore "information" level checks if not wanted, unless they also set a MapCSS class
                .filter(c -> includeOtherSeverity || Severity.OTHER != c.getSeverity() || !c.setClassExpressions.isEmpty())
                .filter(c -> allTests || c.rule.selectors.stream().anyMatch(Selector.ChildOrParentSelector.class::isInstance))
                .map(c -> c.rule);
        index.buildIndex(ruleStream);
        return index;
    }

    /**
     * Obtains all {@link TestError}s for the {@link OsmPrimitive} {@code p}.
     * @param p The OSM primitive
     * @param includeOtherSeverity if {@code true}, errors of severity {@link Severity#OTHER} (info) will also be returned
     * @return all errors for the given primitive, with or without those of "info" severity
     */
    public synchronized Collection<TestError> getErrorsForPrimitive(OsmPrimitive p, boolean includeOtherSeverity) {
        final List<TestError> res = new ArrayList<>();
        if (indexData == null) {
            indexData = createMapCSSTagCheckerIndex(checks, includeOtherSeverity, ALL_TESTS);
        }

        final Environment env = new Environment(p, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        env.mpAreaCache = mpAreaCache;
        env.toMatchForSurrounding = toMatchForSurrounding;

        Iterator<MapCSSRule> candidates = indexData.getRuleCandidates(p);
        while (candidates.hasNext()) {
            MapCSSRule r = candidates.next();
            for (Selector selector : r.selectors) {
                env.clearSelectorMatchingInformation();
                if (!selector.matches(env)) { // as side effect env.parent will be set (if s is a child selector)
                    continue;
                }
                MapCSSTagCheckerAndRule test = ruleToCheckMap.computeIfAbsent(r, rule -> checks.entrySet().stream()
                        .map(e -> e.getValue().stream()
                                // rule.selectors might be different due to MapCSSStyleIndex, however, the declarations are the same object
                                .filter(c -> c.rule.declaration == rule.declaration)
                                .findFirst()
                                .map(c -> new MapCSSTagCheckerAndRule(c, getTitle(e.getKey())))
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
                MapCSSTagCheckerRule check = test == null ? null : test.tagCheck;
                if (check != null) {
                    r.declaration.execute(env);
                    if (!check.errors.isEmpty()) {
                        for (TestError e: check.getErrorsForPrimitive(p, selector, env, test)) {
                            addIfNotSimilar(e, res);
                        }
                    }
                }
            }
        }
        return res;
    }

    private String getTitle(String url) {
        return urlTitles.getOrDefault(url, tr("unknown"));
    }

    /**
     * See #12627
     * Add error to given list if list doesn't already contain a similar error.
     * Similar means same code and description and same combination of primitives and same combination of highlighted objects,
     * but maybe with different orders.
     * @param toAdd the error to add
     * @param errors the list of errors
     */
    private static void addIfNotSimilar(TestError toAdd, List<TestError> errors) {
        final boolean isDup = toAdd.getPrimitives().size() >= 2 && errors.stream().anyMatch(toAdd::isSimilar);
        if (!isDup)
            errors.add(toAdd);
    }

    static Collection<TestError> getErrorsForPrimitive(
            OsmPrimitive p, boolean includeOtherSeverity, Collection<Set<MapCSSTagCheckerRule>> checksCol) {
        // this variant is only used by the assertion tests
        final List<TestError> r = new ArrayList<>();
        final Environment env = new Environment(p, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        env.mpAreaCache = mpAreaCache;
        env.toMatchForSurrounding = toMatchForSurrounding;
        for (Set<MapCSSTagCheckerRule> schecks : checksCol) {
            for (MapCSSTagCheckerRule check : schecks) {
                boolean ignoreError = Severity.OTHER == check.getSeverity() && !includeOtherSeverity;
                // Do not run "information" level checks if not wanted, unless they also set a MapCSS class
                if (ignoreError && check.setClassExpressions.isEmpty()) {
                    continue;
                }
                final Selector selector = check.whichSelectorMatchesEnvironment(env);
                if (selector != null) {
                    check.rule.declaration.execute(env);
                    if (!ignoreError && !check.errors.isEmpty()) {
                        r.addAll(check.getErrorsForPrimitive(p, selector, env, new MapCSSTagCheckerAndRule(check.rule)));
                    }
                }
            }
        }
        return r;
    }

    /**
     * Visiting call for primitives.
     *
     * @param p The primitive to inspect.
     */
    @Override
    public void check(OsmPrimitive p) {
        for (TestError e : getErrorsForPrimitive(p, PREF_OTHER.get())) {
            addIfNotSimilar(e, errors);
        }
    }

    /**
     * Adds a new MapCSS config file from the given URL.
     * @param url The unique URL of the MapCSS config file
     * @return List of tag checks and parsing errors, or null
     * @throws ParseException if the config file does not match MapCSS syntax
     * @throws IOException if any I/O error occurs
     * @since 7275
     */
    public synchronized ParseResult addMapCSS(String url) throws ParseException, IOException {
        // Check assertions, useful for development of local files
        final boolean checkAssertions = Config.getPref().getBoolean("validator.check_assert_local_rules", false) && Utils.isLocalUrl(url);
        return addMapCSS(url, checkAssertions ? Logging::warn : null);
    }

    /**
     * Adds a new MapCSS config file from the given URL. <br />
     * NOTE: You should prefer {@link #addMapCSS(String)} unless you <i>need</i> to know what the assertions return.
     *
     * @param url The unique URL of the MapCSS config file
     * @param assertionConsumer A string consumer for error messages.
     * @return List of tag checks and parsing errors, or null
     * @throws ParseException if the config file does not match MapCSS syntax
     * @throws IOException if any I/O error occurs
     * @since 18365 (public, primarily for ValidatorCLI)
     */
    public synchronized ParseResult addMapCSS(String url, Consumer<String> assertionConsumer) throws ParseException, IOException {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        ParseResult result;
        try (CachedFile cache = new CachedFile(url);
             InputStream zip = cache.findZipEntryInputStream("validator.mapcss", "");
             InputStream s = zip != null ? zip : cache.getInputStream();
             Reader reader = new BufferedReader(UTFInputStreamReader.create(s))) {
            if (zip != null)
                I18n.addTexts(cache.getFile());
            result = MapCSSTagCheckerRule.readMapCSS(reader, assertionConsumer);
            checks.remove(url);
            checks.putAll(url, result.parseChecks);
            urlTitles.put(url, findURLTitle(url));
            indexData = null;
        }
        return result;
    }

    /** Find a user friendly string for the url.
     *
     * @param url the source for the set of rules
     * @return a value that can be used in tool tip or progress bar.
     */
    private static String findURLTitle(String url) {
        for (SourceEntry source : new ValidatorPrefHelper().get()) {
            if (url.equals(source.url) && !Utils.isEmpty(source.title)) {
                return source.title;
            }
        }
        if (url.endsWith(".mapcss")) // do we have others?
            url = new File(url).getName();
        if (url.length() > 33) {
            url = "..." + url.substring(url.length() - 30);
        }
        return url;
    }

    @Override
    public synchronized void initialize() throws Exception {
        checks.clear();
        urlTitles.clear();
        indexData = null;
        for (SourceEntry source : new ValidatorPrefHelper().get()) {
            if (!source.active) {
                continue;
            }
            String i = source.url;
            try {
                if (!i.startsWith("resource:")) {
                    Logging.info(tr("Adding {0} to tag checker", i));
                } else if (Logging.isDebugEnabled()) {
                    Logging.debug(tr("Adding {0} to tag checker", i));
                }
                addMapCSS(i);
                if (Config.getPref().getBoolean("validator.auto_reload_local_rules", true) && source.isLocal()) {
                    FileWatcher.getDefaultInstance().registerSource(source);
                }
            } catch (IOException | IllegalStateException | IllegalArgumentException ex) {
                Logging.warn(tr("Failed to add {0} to tag checker", i));
                Logging.log(Logging.LEVEL_WARN, ex);
            } catch (ParseException | TokenMgrError ex) {
                Logging.warn(tr("Failed to add {0} to tag checker", i));
                Logging.warn(ex);
            }
        }
        MapCSSTagCheckerAsserts.clear();
    }

    /**
     * Reload tagchecker rule.
     * @param rule tagchecker rule to reload
     * @since 12825
     */
    public static void reloadRule(SourceEntry rule) {
        MapCSSTagChecker tagChecker = OsmValidator.getTest(MapCSSTagChecker.class);
        if (tagChecker != null) {
            try {
                tagChecker.addMapCSS(rule.url);
            } catch (IOException | ParseException | TokenMgrError e) {
                Logging.warn(e);
            }
        }
    }

    @Override
    public synchronized void startTest(ProgressMonitor progressMonitor) {
        super.startTest(progressMonitor);
        super.setShowElements(true);
    }

    @Override
    public synchronized void endTest() {
        // no need to keep the index, it is quickly build and doubles the memory needs
        indexData = null;
        // always clear the cache to make sure that we catch changes in geometry
        mpAreaCache.clear();
        ruleToCheckMap.clear();
        toMatchForSurrounding.clear();
        super.endTest();
    }

    @Override
    public void visit(Collection<OsmPrimitive> selection) {
        visit(selection, null);
    }

    /**
     * Execute the rules from the URLs matching the given predicate.
     * @param selection collection of primitives
     * @param urlPredicate a predicate deciding whether the rules from the given URL shall be executed
     */
    void visit(Collection<OsmPrimitive> selection, Predicate<String> urlPredicate) {
        if (urlPredicate == null && progressMonitor != null) {
            progressMonitor.setTicksCount(selection.size() * checks.size());
        }

        mpAreaCache.clear();
        toMatchForSurrounding.clear();

        Set<OsmPrimitive> surrounding = new HashSet<>();
        for (Entry<String, Set<MapCSSTagCheckerRule>> entry : checks.entrySet()) {
            if (isCanceled()) {
                break;
            }
            if (urlPredicate != null && !urlPredicate.test(entry.getKey())) {
                continue;
            }
            visit(entry.getKey(), entry.getValue(), selection, surrounding);
        }
    }

    /**
     * Perform the checks for one check url
     * @param url the url for the checks
     * @param checksForUrl the checks to perform
     * @param selection collection primitives
     * @param surrounding surrounding primitives, evtl. filled by this routine
     */
    private void visit(String url, Set<MapCSSTagCheckerRule> checksForUrl, Collection<OsmPrimitive> selection, Set<OsmPrimitive> surrounding) {
        MultiMap<String, MapCSSTagCheckerRule> currentCheck = new MultiMap<>();
        currentCheck.putAll(url, checksForUrl);
        indexData = createMapCSSTagCheckerIndex(currentCheck, includeOtherSeverityChecks(), ALL_TESTS);
        Set<OsmPrimitive> tested = new HashSet<>();


        String title = getTitle(url);
        if (progressMonitor != null) {
            progressMonitor.setExtraText(tr(" {0}", title));
        }
        long cnt = 0;
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (OsmPrimitive p : selection) {
            if (isCanceled()) {
                break;
            }
            if (isPrimitiveUsable(p)) {
                check(p);
                if (partialSelection) {
                    tested.add(p);
                }
            }
            if (progressMonitor != null) {
                progressMonitor.worked(1);
                cnt++;
                // add frequently changing info to progress monitor so that it
                // doesn't seem to hang when test takes longer than 0.5 seconds
                if (cnt % 10000 == 0 && stopwatch.elapsed() >= 500) {
                    progressMonitor.setExtraText(tr(" {0}: {1} of {2} elements done", title, cnt, selection.size()));
                }
            }
        }

        if (partialSelection && !tested.isEmpty()) {
            testPartial(currentCheck, tested, surrounding);
        }
    }

    private void testPartial(MultiMap<String, MapCSSTagCheckerRule> currentCheck, Set<OsmPrimitive> tested, Set<OsmPrimitive> surrounding) {

        // #14287: see https://josm.openstreetmap.de/ticket/14287#comment:15
        // execute tests for objects which might contain or cross previously tested elements

        final boolean includeOtherSeverity = includeOtherSeverityChecks();
        // rebuild index with a reduced set of rules (those that use ChildOrParentSelector) and thus may have left selectors
        // matching the previously tested elements
        indexData = createMapCSSTagCheckerIndex(currentCheck, includeOtherSeverity, ONLY_SELECTED_TESTS);
        if (indexData.isEmpty())
            return; // performance: some *.mapcss rule files don't use ChildOrParentSelector

        if (surrounding.isEmpty()) {
            for (OsmPrimitive p : tested) {
                if (p.getDataSet() != null) {
                    surrounding.addAll(p.getDataSet().searchWays(p.getBBox()));
                    surrounding.addAll(p.getDataSet().searchRelations(p.getBBox()));
                }
            }
        }

        toMatchForSurrounding.clear();
        toMatchForSurrounding.addAll(tested);
        for (OsmPrimitive p : surrounding) {
            if (tested.contains(p))
                continue;
            Collection<TestError> additionalErrors = getErrorsForPrimitive(p, includeOtherSeverity);
            for (TestError e : additionalErrors) {
                if (e.getPrimitives().stream().anyMatch(tested::contains))
                    addIfNotSimilar(e, errors);
            }
        }

    }

}
