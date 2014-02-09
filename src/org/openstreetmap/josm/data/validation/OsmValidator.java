// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ValidateAction;
import org.openstreetmap.josm.data.validation.tests.Addresses;
import org.openstreetmap.josm.data.validation.tests.BarriersEntrances;
import org.openstreetmap.josm.data.validation.tests.Coastlines;
import org.openstreetmap.josm.data.validation.tests.ConditionalKeys;
import org.openstreetmap.josm.data.validation.tests.CrossingWays;
import org.openstreetmap.josm.data.validation.tests.DuplicateNode;
import org.openstreetmap.josm.data.validation.tests.DuplicateRelation;
import org.openstreetmap.josm.data.validation.tests.DuplicateWay;
import org.openstreetmap.josm.data.validation.tests.DuplicatedWayNodes;
import org.openstreetmap.josm.data.validation.tests.Highways;
import org.openstreetmap.josm.data.validation.tests.Lanes;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.MultipolygonTest;
import org.openstreetmap.josm.data.validation.tests.NameMismatch;
import org.openstreetmap.josm.data.validation.tests.OpeningHourTest;
import org.openstreetmap.josm.data.validation.tests.OverlappingWays;
import org.openstreetmap.josm.data.validation.tests.PowerLines;
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
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.ValidatorLayer;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.tools.Utils;

/**
 * A OSM data validator.
 *
 * @author Francisco R. Santos &lt;frsantos@gmail.com&gt;
 */
public class OsmValidator implements LayerChangeListener {

    public static ValidatorLayer errorLayer = null;

    /** The validate action */
    public ValidateAction validateAction = new ValidateAction();

    /** Grid detail, multiplier of east,north values for valuable cell sizing */
    public static double griddetail;

    public static final Collection<String> ignoredErrors = new TreeSet<String>();

    /**
     * All available tests
     * TODO: is there any way to find out automatically all available tests?
     */
    @SuppressWarnings("unchecked")
    private static final Class<Test>[] allAvailableTests = new Class[] {
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
    };

    private static Map<String, Test> allTestsMap;
    static {
        allTestsMap = new HashMap<String, Test>();
        for (Class<Test> testClass : allAvailableTests) {
            try {
                allTestsMap.put(testClass.getName(), testClass.newInstance());
            } catch (Exception e) {
                Main.error(e);
            }
        }
    }

    /**
     * Constructs a new {@code OsmValidator}.
     */
    public OsmValidator() {
        checkValidatorDir();
        initializeGridDetail();
        loadIgnoredErrors(); //FIXME: load only when needed
    }

    /**
     * Returns the plugin's directory of the plugin
     *
     * @return The directory of the plugin
     */
    public static String getValidatorDir() {
        return Main.pref.getPreferencesDir() + "validator/";
    }

    /**
     * Check if plugin directory exists (store ignored errors file)
     */
    private void checkValidatorDir() {
        try {
            File pathDir = new File(getValidatorDir());
            if (!pathDir.exists()) {
                pathDir.mkdirs();
            }
        } catch (Exception e){
            Main.error(e);
        }
    }

    private void loadIgnoredErrors() {
        ignoredErrors.clear();
        if (Main.pref.getBoolean(ValidatorPreference.PREF_USE_IGNORE, true)) {
            File file = new File(getValidatorDir() + "ignorederrors");
            if (file.exists()) {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new FileReader(file));
                    for (String line = in.readLine(); line != null; line = in.readLine()) {
                        ignoredErrors.add(line);
                    }
                } catch (final FileNotFoundException e) {
                    Main.debug(Main.getErrorMessage(e));
                } catch (final IOException e) {
                    Main.error(e);
                } finally {
                    Utils.close(in);
                }
            }
        }
    }

    public static void addIgnoredError(String s) {
        ignoredErrors.add(s);
    }

    public static boolean hasIgnoredError(String s) {
        return ignoredErrors.contains(s);
    }

    public static void saveIgnoredErrors() {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(getValidatorDir() + "ignorederrors"), false);
            for (String e : ignoredErrors) {
                out.println(e);
            }
        } catch (IOException e) {
            Main.error(e);
        } finally {
            Utils.close(out);
        }
    }

    public static void initializeErrorLayer() {
        if (!Main.pref.getBoolean(ValidatorPreference.PREF_LAYER, true))
            return;
        if (errorLayer == null) {
            errorLayer = new ValidatorLayer();
            Main.main.addLayer(errorLayer);
        }
    }

    /**
     * Gets a map from simple names to all tests.
     * @return A map of all tests, indexed and sorted by the name of their Java class
     */
    public static SortedMap<String, Test> getAllTestsMap() {
        applyPrefs(allTestsMap, false);
        applyPrefs(allTestsMap, true);
        return new TreeMap<String, Test>(allTestsMap);
    }

    /**
     * Returns the instance of the given test class.
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
        for(String testName : Main.pref.getCollection(beforeUpload
        ? ValidatorPreference.PREF_SKIP_TESTS_BEFORE_UPLOAD : ValidatorPreference.PREF_SKIP_TESTS)) {
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

    public static Collection<Test> getTests() {
        return getAllTestsMap().values();
    }

    public static Collection<Test> getEnabledTests(boolean beforeUpload) {
        Collection<Test> enabledTests = getTests();
        for (Test t : new ArrayList<Test>(enabledTests)) {
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
     * @return An array of the test classes
     */
    public static Class<Test>[] getAllAvailableTests() {
        return Arrays.copyOf(allAvailableTests, allAvailableTests.length);
    }

    /**
     * Initialize grid details based on current projection system. Values based on
     * the original value fixed for EPSG:4326 (10000) using heuristics (that is, test&amp;error
     * until most bugs were discovered while keeping the processing time reasonable)
     */
    public void initializeGridDetail() {
        String code = Main.getProjection().toCode();
        if (Arrays.asList(ProjectionPreference.wgs84.allCodes()).contains(code)) {
            OsmValidator.griddetail = 10000;
        } else if (Arrays.asList(ProjectionPreference.mercator.allCodes()).contains(code)) {
            OsmValidator.griddetail = 0.01;
        } else if (Arrays.asList(ProjectionPreference.lambert.allCodes()).contains(code)) {
            OsmValidator.griddetail = 0.1;
        } else {
            OsmValidator.griddetail = 1.0;
        }
    }

    private static boolean testsInitialized = false;

    /**
     * Initializes all tests if this operations hasn't been performed already.
     */
    public static synchronized void initializeTests() {
        if (!testsInitialized) {
            Main.debug("Initializing validator tests");
            final long startTime = System.currentTimeMillis();
            initializeTests(getTests());
            testsInitialized = true;
            final long elapsedTime = System.currentTimeMillis() - startTime;
            Main.debug("Initializing validator tests completed in " + Utils.getDurationString(elapsedTime));
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
            } catch (Exception e) {
                Main.error(e);
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Error initializing test {0}:\n {1}", test.getClass()
                                .getSimpleName(), e),
                                tr("Error"),
                                JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /* interface LayerChangeListener                                              */
    /* -------------------------------------------------------------------------- */
    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
    }

    @Override
    public void layerAdded(Layer newLayer) {
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer == errorLayer) {
            errorLayer = null;
            return;
        }
        if (Main.map.mapView.getLayersOfType(OsmDataLayer.class).isEmpty()) {
            if (errorLayer != null) {
                Main.main.removeLayer(errorLayer);
            }
        }
    }
}
