// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.tests.Addresses;
import org.openstreetmap.josm.data.validation.tests.ApiCapabilitiesTest;
import org.openstreetmap.josm.data.validation.tests.BarriersEntrances;
import org.openstreetmap.josm.data.validation.tests.Coastlines;
import org.openstreetmap.josm.data.validation.tests.ConditionalKeys;
import org.openstreetmap.josm.data.validation.tests.CrossingWays;
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
import org.openstreetmap.josm.data.validation.tests.SelfIntersectingWay;
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
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.Logging;
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

    private static final Collection<String> ignoredErrors = new TreeSet<>();

    /**
     * All registered tests
     */
    private static final Collection<Class<? extends Test>> allTests = new ArrayList<>();
    private static final Map<String, Test> allTestsMap = new HashMap<>();

    /**
     * All available tests in core
     */
    @SuppressWarnings("unchecked")
    private static final Class<Test>[] CORE_TEST_CLASSES = new Class[] {
        /* FIXME - unique error numbers for tests aren't properly unique - ignoring will not work as expected */
        DuplicateNode.class, // ID    1 ..   99
        OverlappingWays.class, // ID  101 ..  199
        UntaggedNode.class, // ID  201 ..  299
        UntaggedWay.class, // ID  301 ..  399
        SelfIntersectingWay.class, // ID  401 ..  499
        DuplicatedWayNodes.class, // ID  501 ..  599
        CrossingWays.Ways.class, // ID  601 ..  699
        CrossingWays.Boundaries.class, // ID  601 ..  699
        CrossingWays.Barrier.class, // ID  601 ..  699
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

    static {
        for (Class<? extends Test> testClass : CORE_TEST_CLASSES) {
            addTest(testClass);
        }
    }

    /**
     * Initializes {@code OsmValidator}.
     */
    public static void initialize() {
        checkValidatorDir();
        initializeGridDetail();
        loadIgnoredErrors(); //FIXME: load only when needed
    }

    /**
     * Returns the validator directory.
     *
     * @return The validator directory
     */
    public static String getValidatorDir() {
        return new File(Config.getDirs().getUserDataDirectory(true), "validator").getAbsolutePath();
    }

    /**
     * Check if validator directory exists (store ignored errors file)
     */
    private static void checkValidatorDir() {
        File pathDir = new File(getValidatorDir());
        if (!pathDir.exists()) {
            Utils.mkDirs(pathDir);
        }
    }

    private static void loadIgnoredErrors() {
        ignoredErrors.clear();
        if (ValidatorPrefHelper.PREF_USE_IGNORE.get()) {
            Path path = Paths.get(getValidatorDir()).resolve("ignorederrors");
            if (path.toFile().exists()) {
                try {
                    ignoredErrors.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
                } catch (final FileNotFoundException e) {
                    Logging.debug(Logging.getErrorMessage(e));
                } catch (final IOException e) {
                    Logging.error(e);
                }
            }
        }
    }

    /**
     * Adds an ignored error
     * @param s The ignore group / sub group name
     * @see TestError#getIgnoreGroup()
     * @see TestError#getIgnoreSubGroup()
     */
    public static void addIgnoredError(String s) {
        ignoredErrors.add(s);
    }

    /**
     * Check if a error should be ignored
     * @param s The ignore group / sub group name
     * @return <code>true</code> to ignore that error
     */
    public static boolean hasIgnoredError(String s) {
        return ignoredErrors.contains(s);
    }

    /**
     * Saves the names of the ignored errors to a file
     */
    public static void saveIgnoredErrors() {
        try (PrintWriter out = new PrintWriter(new File(getValidatorDir(), "ignorederrors"), StandardCharsets.UTF_8.name())) {
            for (String e : ignoredErrors) {
                out.println(e);
            }
        } catch (IOException e) {
            Logging.error(e);
        }
    }

    /**
     * Initializes error layer.
     */
    public static synchronized void initializeErrorLayer() {
        if (!ValidatorPrefHelper.PREF_LAYER.get())
            return;
        if (errorLayer == null) {
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
     * Initialize grid details based on current projection system. Values based on
     * the original value fixed for EPSG:4326 (10000) using heuristics (that is, test&amp;error
     * until most bugs were discovered while keeping the processing time reasonable)
     */
    public static void initializeGridDetail() {
        String code = Main.getProjection().toCode();
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
     * Initializes all tests if this operations hasn't been performed already.
     */
    public static synchronized void initializeTests() {
        if (!testsInitialized) {
            Logging.debug("Initializing validator tests");
            final long startTime = System.currentTimeMillis();
            initializeTests(getTests());
            testsInitialized = true;
            if (Logging.isDebugEnabled()) {
                final long elapsedTime = System.currentTimeMillis() - startTime;
                Logging.debug("Initializing validator tests completed in {0}", Utils.getDurationString(elapsedTime));
            }
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
                Logging.error(e);
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("Error initializing test {0}:\n {1}", test.getClass().getSimpleName(), e),
                            tr("Error"), JOptionPane.ERROR_MESSAGE);
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
}
