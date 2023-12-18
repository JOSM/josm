// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.validation.tests.Addresses;
import org.openstreetmap.josm.data.validation.tests.ApiCapabilitiesTest;
import org.openstreetmap.josm.data.validation.tests.BarriersEntrances;
import org.openstreetmap.josm.data.validation.tests.Coastlines;
import org.openstreetmap.josm.data.validation.tests.ConditionalKeys;
import org.openstreetmap.josm.data.validation.tests.ConnectivityRelations;
import org.openstreetmap.josm.data.validation.tests.CrossingWays;
import org.openstreetmap.josm.data.validation.tests.CycleDetector;
import org.openstreetmap.josm.data.validation.tests.DirectionNodes;
import org.openstreetmap.josm.data.validation.tests.DuplicateNode;
import org.openstreetmap.josm.data.validation.tests.DuplicateRelation;
import org.openstreetmap.josm.data.validation.tests.DuplicateWay;
import org.openstreetmap.josm.data.validation.tests.DuplicatedWayNodes;
import org.openstreetmap.josm.data.validation.tests.Highways;
import org.openstreetmap.josm.data.validation.tests.InternetTags;
import org.openstreetmap.josm.data.validation.tests.Lanes;
import org.openstreetmap.josm.data.validation.tests.LongSegment;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.MultipolygonTest;
import org.openstreetmap.josm.data.validation.tests.NameMismatch;
import org.openstreetmap.josm.data.validation.tests.OpeningHourTest;
import org.openstreetmap.josm.data.validation.tests.OverlappingWays;
import org.openstreetmap.josm.data.validation.tests.PowerLines;
import org.openstreetmap.josm.data.validation.tests.PublicTransportRouteTest;
import org.openstreetmap.josm.data.validation.tests.RelationChecker;
import org.openstreetmap.josm.data.validation.tests.RightAngleBuildingTest;
import org.openstreetmap.josm.data.validation.tests.SelfIntersectingWay;
import org.openstreetmap.josm.data.validation.tests.SharpAngles;
import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays;
import org.openstreetmap.josm.data.validation.tests.TagChecker;
import org.openstreetmap.josm.data.validation.tests.TurnrestrictionTest;
import org.openstreetmap.josm.data.validation.tests.UnclosedWays;
import org.openstreetmap.josm.data.validation.tests.UnconnectedWays;
import org.openstreetmap.josm.data.validation.tests.UntaggedNode;
import org.openstreetmap.josm.data.validation.tests.UntaggedWay;
import org.openstreetmap.josm.data.validation.tests.WayConnectedToArea;
import org.openstreetmap.josm.data.validation.tests.WronglyOrderedWays;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.ValidatorLayer;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Utils;

/**
 * A OSM data validator.
 *
 * @author Francisco R. Santos &lt;frsantos@gmail.com&gt;
 */
public final class OsmValidator {

    private OsmValidator() {
        // Hide default constructor for utilities classes
    }

    private static volatile ValidatorLayer errorLayer;

    /** Grid detail, multiplier of east,north values for valuable cell sizing */
    private static double griddetail;

    private static final SortedMap<String, String> ignoredErrors = new TreeMap<>();
    /**
     * All registered tests
     */
    private static final Collection<Class<? extends Test>> allTests = new ArrayList<>();
    private static final Map<String, Test> allTestsMap = new HashMap<>();

    /**
     * All available tests in core
     */
    @SuppressWarnings("unchecked")
    private static final Class<Test>[] CORE_TEST_CLASSES = new Class[] {// NOPMD
        /* FIXME - unique error numbers for tests aren't properly unique - ignoring will not work as expected */
        /* Error codes are class.getName().hashCode() + "_" + oldCode. There should almost never be a collision. */
        DuplicateNode.class, // ID    1 ..   99
        OverlappingWays.class, // ID  101 ..  199
        UntaggedNode.class, // ID  201 ..  299
        UntaggedWay.class, // ID  301 ..  399
        SelfIntersectingWay.class, // ID  401 ..  499
        DuplicatedWayNodes.class, // ID  501 ..  599
        CrossingWays.Ways.class, // ID  601 ..  699
        CrossingWays.Boundaries.class, // ID  601 ..  699
        CrossingWays.SelfCrossing.class, // ID  601 ..  699
        SimilarNamedWays.class, // ID  701 ..  799
        Coastlines.class, // ID  901 ..  999
        WronglyOrderedWays.class, // ID 1001 .. 1099
        UnclosedWays.class, // ID 1101 .. 1199
        TagChecker.class, // ID 1201 .. 1299
        UnconnectedWays.UnconnectedHighways.class, // ID 1301 .. 1399
        UnconnectedWays.UnconnectedRailways.class, // ID 1301 .. 1399
        UnconnectedWays.UnconnectedWaterways.class, // ID 1301 .. 1399
        UnconnectedWays.UnconnectedNaturalOrLanduse.class, // ID 1301 .. 1399
        UnconnectedWays.UnconnectedPower.class, // ID 1301 .. 1399
        DuplicateWay.class, // ID 1401 .. 1499
        NameMismatch.class, // ID  1501 ..  1599
        MultipolygonTest.class, // ID  1601 ..  1699
        RelationChecker.class, // ID  1701 ..  1799
        TurnrestrictionTest.class, // ID  1801 ..  1899
        DuplicateRelation.class, // ID 1901 .. 1999
        WayConnectedToArea.class, // ID 2301 .. 2399
        PowerLines.class, // ID 2501 .. 2599
        Addresses.class, // ID 2601 .. 2699
        Highways.class, // ID 2701 .. 2799
        BarriersEntrances.class, // ID 2801 .. 2899
        OpeningHourTest.class, // 2901 .. 2999
        MapCSSTagChecker.class, // 3000 .. 3099
        Lanes.class, // 3100 .. 3199
        ConditionalKeys.class, // 3200 .. 3299
        InternetTags.class, // 3300 .. 3399
        ApiCapabilitiesTest.class, // 3400 .. 3499
        LongSegment.class, // 3500 .. 3599
        PublicTransportRouteTest.class, // 3600 .. 3699
        // 3700 .. 3799 is automatically removed since it clashed with pt_assistant.
        SharpAngles.class, // 3800 .. 3899
        ConnectivityRelations.class, // 3900 .. 3999
        DirectionNodes.class, // 4000 .. 4099
        RightAngleBuildingTest.class, // 4100 .. 4199
        CycleDetector.class, // 4200 .. 4299
    };

    /**
     * Adds a test to the list of available tests
     * @param testClass The test class
     */
    public static void addTest(Class<? extends Test> testClass) {
        allTests.add(testClass);
        try {
            allTestsMap.put(testClass.getName(), testClass.getConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            Logging.error(e);
        }
    }

    /**
     * Removes a test from the list of available tests. This will not remove
     * core tests.
     *
     * @param testClass The test class
     * @return {@code true} if the test was removed (see {@link Collection#remove})
     * @since 15603
     */
    public static boolean removeTest(Class<? extends Test> testClass) {
        boolean removed = false;
        if (!Arrays.asList(CORE_TEST_CLASSES).contains(testClass)) {
            removed = allTests.remove(testClass);
            allTestsMap.remove(testClass.getName());
        }
        return removed;
    }

    static {
        for (Class<? extends Test> testClass : CORE_TEST_CLASSES) {
            addTest(testClass);
        }
    }

    /**
     * Initializes {@code OsmValidator}.
     */
    public static void initialize() {
        initializeGridDetail();
        loadIgnoredErrors();
    }

    /**
     * Returns the validator directory.
     *
     * @return The validator directory
     */
    public static String getValidatorDir() {
        File dir = new File(Config.getDirs().getUserDataDirectory(true), "validator");
        try {
            return dir.getAbsolutePath();
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, null, e);
            return dir.getPath();
        }
    }

    private static void loadIgnoredErrors() {
        ignoredErrors.clear();
        if (Boolean.TRUE.equals(ValidatorPrefHelper.PREF_USE_IGNORE.get())) {
            Config.getPref().getListOfMaps(ValidatorPrefHelper.PREF_IGNORELIST).forEach(ignoredErrors::putAll);
            Path path = Paths.get(getValidatorDir()).resolve("ignorederrors");
            try {
                if (path.toFile().exists()) {
                    try {
                        TreeSet<String> treeSet = new TreeSet<>(Files.readAllLines(path, StandardCharsets.UTF_8));
                        treeSet.forEach(ignore -> ignoredErrors.putIfAbsent(ignore, ""));
                        removeLegacyEntries(true);

                        saveIgnoredErrors();
                        Files.deleteIfExists(path);

                    } catch (FileNotFoundException e) {
                        Logging.debug(Logging.getErrorMessage(e));
                    } catch (IOException e) {
                        Logging.error(e);
                    }
                }
            } catch (SecurityException e) {
                Logging.log(Logging.LEVEL_ERROR, "Unable to load ignored errors", e);
            }
            removeLegacyEntries(Config.getPref().get(ValidatorPrefHelper.PREF_IGNORELIST_FORMAT).isEmpty());
        }
    }

    private static void removeLegacyEntries(boolean force) {
        // see #19053:
        boolean wasChanged = removeLegacyEntry(force, true, "3000");
        // see #18230 (pt_assistant, RightAngleBuildingTest)
        wasChanged |= removeLegacyEntry(force, false, "3701");

        if (wasChanged) {
            saveIgnoredErrors();
        }
    }

    private static boolean removeLegacyEntry(boolean force, boolean keep, String prefix) {
        boolean wasChanged = false;
        if (force) {
            Iterator<Entry<String, String>> iter = ignoredErrors.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = iter.next();
                if (entry.getKey().startsWith(prefix + "_")) {
                    Logging.warn(tr("Cannot handle ignore list entry {0}", entry));
                    iter.remove();
                    wasChanged = true;
                }
            }
        }
        String legacyEntry = ignoredErrors.remove(prefix);
        if (keep && legacyEntry != null) {
            if (!legacyEntry.isEmpty()) {
                addIgnoredError(prefix + "_" + legacyEntry, legacyEntry);
            }
            wasChanged = true;
        }
        return wasChanged;
    }

    /**
     * Adds an ignored error
     * @param s The ignore group / sub group name
     * @see TestError#getIgnoreGroup()
     * @see TestError#getIgnoreSubGroup()
     */
    public static void addIgnoredError(String s) {
        addIgnoredError(s, "");
    }

    /**
     * Adds an ignored error
     * @param s The ignore group / sub group name
     * @param description What the error actually is
     * @see TestError#getIgnoreGroup()
     * @see TestError#getIgnoreSubGroup()
     */
    public static void addIgnoredError(String s, String description) {
        if (description == null) description = "";
        ignoredErrors.put(s, description);
    }

    /**
     *  Make sure that we don't keep single entries for a "group ignore".
     */
    static void cleanupIgnoredErrors() {
        if (ignoredErrors.size() > 1) {
            List<String> toRemove = new ArrayList<>();

            Iterator<Entry<String, String>> iter = ignoredErrors.entrySet().iterator();
            String lastKey = iter.next().getKey();
            while (iter.hasNext()) {
                String currKey = iter.next().getKey();
                if (currKey.startsWith(lastKey) && sameCode(currKey, lastKey)) {
                    toRemove.add(currKey);
                } else {
                    lastKey = currKey;
                }
            }
            toRemove.forEach(ignoredErrors::remove);
        }

        Map<String, String> tmap = buildIgnore(buildJTreeList());
        if (!tmap.isEmpty()) {
            ignoredErrors.clear();
            ignoredErrors.putAll(tmap);
        }
    }

    private static boolean sameCode(String key1, String key2) {
        return extractCodeFromIgnoreKey(key1).equals(extractCodeFromIgnoreKey(key2));
    }

    /**
     * Extract the leading digits building the code for the error key.
     * @param key the error key
     * @return the leading digits
     */
    private static String extractCodeFromIgnoreKey(String key) {
        int lenCode = 0;

        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) >= '0' && key.charAt(i) <= '9') {
                lenCode++;
            } else {
                break;
            }
        }
        return key.substring(0, lenCode);
    }

    /**
     * Check if a error should be ignored
     * @param s The ignore group / sub group name
     * @return <code>true</code> to ignore that error
     */
    public static boolean hasIgnoredError(String s) {
        return ignoredErrors.containsKey(s);
    }

    /**
     * Get the list of all ignored errors
     * @return The <code>Collection&lt;String&gt;</code> of errors that are ignored
     */
    public static SortedMap<String, String> getIgnoredErrors() {
        return ignoredErrors;
    }

    /**
     * Build a JTree with a list
     * @return &lt;type&gt;list as a {@code JTree}
     */
    public static JTree buildJTreeList() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(tr("Ignore list"));
        final Pattern elemId1Pattern = Pattern.compile(":([rwn])_");
        final Pattern elemId2Pattern = Pattern.compile("^[0-9]+$");
        for (Entry<String, String> e: ignoredErrors.entrySet()) {
            String key = e.getKey();
            // key starts with a code, it maybe followed by a string (eg. a MapCSS rule) and
            // optionally with a list of one or more OSM element IDs
            String description = e.getValue();

            ArrayList<String> ignoredElementList = new ArrayList<>();
            String[] osmobjects = elemId1Pattern.split(key, -1);
            for (int i = 1; i < osmobjects.length; i++) {
                String osmid = osmobjects[i];
                if (elemId2Pattern.matcher(osmid).matches()) {
                    osmid = '_' + osmid;
                    int index = key.indexOf(osmid);
                    if (index < key.lastIndexOf(']')) continue;
                    char type = key.charAt(index - 1);
                    ignoredElementList.add(type + osmid);
                }
            }
            for (String osmignore : ignoredElementList) {
                key = key.replace(':' + osmignore, "");
            }

            DefaultMutableTreeNode trunk;
            DefaultMutableTreeNode branch;

            if (!Utils.isEmpty(description)) {
                trunk = inTree(root, description);
                branch = inTree(trunk, key);
                trunk.add(branch);
            } else {
                trunk = inTree(root, key);
                branch = trunk;
            }
            if (!ignoredElementList.isEmpty()) {
                String item;
                if (ignoredElementList.size() == 1) {
                    item = ignoredElementList.iterator().next();
                } else {
                    // combination of two or more objects, keep them together
                    item = ignoredElementList.toString(); // [ID1, ID2, ..., IDn]
                }
                branch.add(new DefaultMutableTreeNode(item));
            }
            root.add(trunk);
        }
        return new JTree(root);
    }

    private static DefaultMutableTreeNode inTree(DefaultMutableTreeNode root, String name) {
        @SuppressWarnings("unchecked")
        Enumeration<TreeNode> trunks = root.children();
        while (trunks.hasMoreElements()) {
            TreeNode ttrunk = trunks.nextElement();
            if (ttrunk instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode trunk = (DefaultMutableTreeNode) ttrunk;
                if (name.equals(trunk.getUserObject())) {
                    return trunk;
                }
            }
        }
        return new DefaultMutableTreeNode(name);
    }

    /**
     * Build a {@code HashMap} from a tree of ignored errors
     * @param tree The JTree of ignored errors
     * @return A {@code HashMap} of the ignored errors for comparison
     */
    public static Map<String, String> buildIgnore(JTree tree) {
        TreeModel model = tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        return buildIgnore(model, root);
    }

    private static Map<String, String> buildIgnore(TreeModel model, DefaultMutableTreeNode node) {
        HashMap<String, String> rHashMap = new HashMap<>();

        for (int i = 0; i < model.getChildCount(node); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild(node, i);
            if (model.getChildCount(child) == 0) {
                // create an entry for the error list
                String key = node.getUserObject().toString();
                String description;

                if (!model.getRoot().equals(node)) {
                    description = ((DefaultMutableTreeNode) node.getParent()).getUserObject().toString();
                } else {
                    description = key; // we get here when reading old file ignorederrors
                }
                if (tr("Ignore list").equals(description))
                    description = "";
                if (!key.matches("^-?[0-9]+(_.*|$)")) {
                    description = key;
                    key = "";
                }

                String item = child.getUserObject().toString();
                String entry = null;
                if (item.matches("^\\[([rwn])_.*")) {
                    // list of elements (produced with list.toString() method)
                    entry = key + ":" + item.substring(1, item.lastIndexOf(']')).replace(", ", ":");
                } else if (item.matches("^([rwn])_.*")) {
                    // single element
                    entry = key + ":" + item;
                } else if (item.matches("^-?[0-9]+(_.*|)$")) {
                    // no element ids
                    entry = item;
                }
                if (entry != null) {
                    rHashMap.put(entry, description);
                } else {
                    Logging.warn("ignored unexpected item in validator ignore list management dialog:'" + item + "'");
                }
            } else {
                rHashMap.putAll(buildIgnore(model, child));
            }
        }
        return rHashMap;
    }

    /**
     * Reset the error list by deleting {@code validator.ignorelist}
     */
    public static void resetErrorList() {
        saveIgnoredErrors();
        Config.getPref().putListOfMaps(ValidatorPrefHelper.PREF_IGNORELIST, null);
        OsmValidator.initialize();
    }

    /**
     * Saves the names of the ignored errors to a preference
     */
    public static void saveIgnoredErrors() {
        List<Map<String, String>> list = new ArrayList<>();
        cleanupIgnoredErrors();
        ignoredErrors.remove("3000"); // see #19053
        ignoredErrors.remove("3701"); // see #18230
        list.add(ignoredErrors);
        int i = 0;
        while (i < list.size()) {
            if (Utils.isEmpty(list.get(i))) {
                list.remove(i);
                continue;
            }
            i++;
        }
        if (list.isEmpty()) list = null;
        Config.getPref().putListOfMaps(ValidatorPrefHelper.PREF_IGNORELIST, list);
        Config.getPref().put(ValidatorPrefHelper.PREF_IGNORELIST_FORMAT, "2");
    }

    /**
     * Initializes error layer.
     */
    public static synchronized void initializeErrorLayer() {
        if (errorLayer == null && Boolean.TRUE.equals(ValidatorPrefHelper.PREF_LAYER.get())) {
            errorLayer = new ValidatorLayer();
            MainApplication.getLayerManager().addLayer(errorLayer);
        }
    }

    /**
     * Resets error layer.
     * @since 11852
     */
    public static synchronized void resetErrorLayer() {
        errorLayer = null;
    }

    /**
     * Gets a map from simple names to all tests.
     * @return A map of all tests, indexed and sorted by the name of their Java class
     */
    public static SortedMap<String, Test> getAllTestsMap() {
        applyPrefs(allTestsMap, false);
        applyPrefs(allTestsMap, true);
        return new TreeMap<>(allTestsMap);
    }

    /**
     * Returns the instance of the given test class.
     * @param <T> testClass type
     * @param testClass The class of test to retrieve
     * @return the instance of the given test class, if any, or {@code null}
     * @since 6670
     */
    @SuppressWarnings("unchecked")
    public static <T extends Test> T getTest(Class<T> testClass) {
        if (testClass == null) {
            return null;
        }
        return (T) allTestsMap.get(testClass.getName());
    }

    private static void applyPrefs(Map<String, Test> tests, boolean beforeUpload) {
        for (String testName : Config.getPref().getList(beforeUpload
        ? ValidatorPrefHelper.PREF_SKIP_TESTS_BEFORE_UPLOAD : ValidatorPrefHelper.PREF_SKIP_TESTS)) {
            Test test = tests.get(testName);
            if (test != null) {
                if (beforeUpload) {
                    test.testBeforeUpload = false;
                } else {
                    test.enabled = false;
                }
            }
        }
    }

    /**
     * Gets all tests that are possible
     * @return The tests
     */
    public static Collection<Test> getTests() {
        return getAllTestsMap().values();
    }

    /**
     * Gets all tests that are run
     * @param beforeUpload To get the ones that are run before upload
     * @return The tests
     */
    public static Collection<Test> getEnabledTests(boolean beforeUpload) {
        Collection<Test> enabledTests = getTests();
        for (Test t : new ArrayList<>(enabledTests)) {
            if (beforeUpload ? t.testBeforeUpload : t.enabled) {
                continue;
            }
            enabledTests.remove(t);
        }
        return enabledTests;
    }

    /**
     * Gets the list of all available test classes
     *
     * @return A collection of the test classes
     */
    public static Collection<Class<? extends Test>> getAllAvailableTestClasses() {
        return Collections.unmodifiableCollection(allTests);
    }

    /**
     * Initialize grid details based on the current projection system. Values based on
     * the original value fixed for EPSG:4326 (10000) using heuristics (that is, test&amp;error
     * until most bugs were discovered while keeping the processing time reasonable)
     */
    public static void initializeGridDetail() {
        String code = ProjectionRegistry.getProjection().toCode();
        if (Arrays.asList(ProjectionPreference.wgs84.allCodes()).contains(code)) {
            OsmValidator.griddetail = 10_000;
        } else if (Arrays.asList(ProjectionPreference.mercator.allCodes()).contains(code)) {
            OsmValidator.griddetail = 0.01;
        } else if (Arrays.asList(ProjectionPreference.lambert.allCodes()).contains(code)) {
            OsmValidator.griddetail = 0.1;
        } else {
            OsmValidator.griddetail = 1.0;
        }
    }

    /**
     * Returns grid detail, multiplier of east,north values for valuable cell sizing
     * @return grid detail
     * @since 11852
     */
    public static double getGridDetail() {
        return griddetail;
    }

    private static boolean testsInitialized;

    /**
     * Initializes all tests if this operation hasn't been performed already.
     */
    public static synchronized void initializeTests() {
        if (!testsInitialized) {
            final String message = "Initializing validator tests";
            Logging.debug(message);
            final Stopwatch stopwatch = Stopwatch.createStarted();
            initializeTests(getTests());
            testsInitialized = true;
            Logging.debug(stopwatch.toString("Initializing validator tests"));
        }
    }

    /**
     * Initializes all tests
     * @param allTests The tests to initialize
     */
    public static void initializeTests(Collection<? extends Test> allTests) {
        for (Test test : allTests) {
            try {
                if (test.enabled) {
                    test.initialize();
                }
            } catch (Exception e) { // NOPMD
                String message = tr("Error initializing test {0}:\n {1}", test.getClass().getSimpleName(), e);
                Logging.error(message);
                if (!GraphicsEnvironment.isHeadless()) {
                    GuiHelper.runInEDT(() ->
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(), message, tr("Error"), JOptionPane.ERROR_MESSAGE)
                    );
                }
            }
        }
    }

    /**
     * Groups the given collection of errors by severity, then message, then description.
     * @param errors list of errors to group
     * @param filterToUse optional filter
     * @return collection of errors grouped by severity, then message, then description
     * @since 12667
     */
    public static Map<Severity, Map<String, Map<String, List<TestError>>>> getErrorsBySeverityMessageDescription(
            Collection<TestError> errors, Predicate<? super TestError> filterToUse) {
        return errors.stream().filter(filterToUse).collect(
                Collectors.groupingBy(TestError::getSeverity, () -> new EnumMap<>(Severity.class),
                        Collectors.groupingBy(TestError::getMessage, () -> new TreeMap<>(AlphanumComparator.getInstance()),
                                Collectors.groupingBy(e -> e.getDescription() == null ? "" : e.getDescription(),
                                        () -> new TreeMap<>(AlphanumComparator.getInstance()),
                                        Collectors.toList()
                                ))));
    }

    /**
     * For unit tests
     */
    static void clearIgnoredErrors() {
        ignoredErrors.clear();
    }
}
