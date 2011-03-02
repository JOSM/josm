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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ValidateAction;
import org.openstreetmap.josm.actions.upload.ValidateUploadHook;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.data.projection.Lambert;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.validation.tests.Coastlines;
import org.openstreetmap.josm.data.validation.tests.CrossingWays;
import org.openstreetmap.josm.data.validation.tests.DuplicateNode;
import org.openstreetmap.josm.data.validation.tests.DuplicateWay;
import org.openstreetmap.josm.data.validation.tests.DuplicatedWayNodes;
import org.openstreetmap.josm.data.validation.tests.MultipolygonTest;
import org.openstreetmap.josm.data.validation.tests.NameMismatch;
import org.openstreetmap.josm.data.validation.tests.NodesWithSameName;
import org.openstreetmap.josm.data.validation.tests.OverlappingWays;
import org.openstreetmap.josm.data.validation.tests.RelationChecker;
import org.openstreetmap.josm.data.validation.tests.SelfIntersectingWay;
import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays;
import org.openstreetmap.josm.data.validation.tests.TagChecker;
import org.openstreetmap.josm.data.validation.tests.TurnrestrictionTest;
import org.openstreetmap.josm.data.validation.tests.UnclosedWays;
import org.openstreetmap.josm.data.validation.tests.UnconnectedWays;
import org.openstreetmap.josm.data.validation.tests.UntaggedNode;
import org.openstreetmap.josm.data.validation.tests.UntaggedWay;
import org.openstreetmap.josm.data.validation.tests.WronglyOrderedWays;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.ValidatorLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.ValidatorPreference;

/**
 *
 * A OSM data validator
 *
 * @author Francisco R. Santos <frsantos@gmail.com>
 */
public class OsmValidator implements LayerChangeListener {

    public static ValidatorLayer errorLayer = null;

    /** The validate action */
    public ValidateAction validateAction = new ValidateAction();

    /** Grid detail, multiplier of east,north values for valuable cell sizing */
    public static double griddetail;

    public static Collection<String> ignoredErrors = new TreeSet<String>();

    /**
     * All available tests
     * TODO: is there any way to find out automatically all available tests?
     */
    @SuppressWarnings("unchecked")
    public static Class<Test>[] allAvailableTests = new Class[] {
            DuplicateNode.class, // ID    1 ..   99
            OverlappingWays.class, // ID  101 ..  199
            UntaggedNode.class, // ID  201 ..  299
            UntaggedWay.class, // ID  301 ..  399
            SelfIntersectingWay.class, // ID  401 ..  499
            DuplicatedWayNodes.class, // ID  501 ..  599
            CrossingWays.class, // ID  601 ..  699
            SimilarNamedWays.class, // ID  701 ..  799
            NodesWithSameName.class, // ID  801 ..  899
            Coastlines.class, // ID  901 ..  999
            WronglyOrderedWays.class, // ID 1001 .. 1099
            UnclosedWays.class, // ID 1101 .. 1199
            TagChecker.class, // ID 1201 .. 1299
            UnconnectedWays.class, // ID 1301 .. 1399
            DuplicateWay.class, // ID 1401 .. 1499
            NameMismatch.class, // ID  1501 ..  1599
            MultipolygonTest.class, // ID  1601 ..  1699
            RelationChecker.class, // ID  1701 ..  1799
            TurnrestrictionTest.class, // ID  1801 ..  1899
    };

    public OsmValidator() {
        checkValidatorDir();
        initializeGridDetail();
        initializeTests(getTests());
        loadIgnoredErrors(); //FIXME: load only when needed
    }

    /**
     * Returns the plugin's directory of the plugin
     *
     * @return The directory of the plugin
     */
    public static String getValidatorDir()
    {
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
            e.printStackTrace();
        }
    }

    private void loadIgnoredErrors() {
        ignoredErrors.clear();
        if (Main.pref.getBoolean(ValidatorPreference.PREF_USE_IGNORE, true)) {
            try {
                final BufferedReader in = new BufferedReader(new FileReader(getValidatorDir() + "ignorederrors"));
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    ignoredErrors.add(line);
                }
            } catch (final FileNotFoundException e) {
                // Ignore
            } catch (final IOException e) {
                e.printStackTrace();
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
        try {
            final PrintWriter out = new PrintWriter(new FileWriter(getValidatorDir() + "ignorederrors"), false);
            for (String e : ignoredErrors) {
                out.println(e);
            }
            out.close();
        } catch (final IOException e) {
            e.printStackTrace();
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

    /** Gets a map from simple names to all tests. */
    public static Map<String, Test> getAllTestsMap() {
        Map<String, Test> tests = new HashMap<String, Test>();
        for (Class<Test> testClass : getAllAvailableTests()) {
            try {
                Test test = testClass.newInstance();
                tests.put(testClass.getSimpleName(), test);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        applyPrefs(tests, false);
        applyPrefs(tests, true);
        return tests;
    }

    private static void applyPrefs(Map<String, Test> tests, boolean beforeUpload) {
        Pattern regexp = Pattern.compile("(\\w+)=(true|false),?");
        Matcher m = regexp.matcher(Main.pref.get(beforeUpload ? ValidatorPreference.PREF_TESTS_BEFORE_UPLOAD
                : ValidatorPreference.PREF_TESTS));
        int pos = 0;
        while (m.find(pos)) {
            String testName = m.group(1);
            Test test = tests.get(testName);
            if (test != null) {
                boolean enabled = Boolean.valueOf(m.group(2));
                if (beforeUpload) {
                    test.testBeforeUpload = enabled;
                } else {
                    test.enabled = enabled;
                }
            }
            pos = m.end();
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
        return allAvailableTests;
    }

    /**
     * Initialize grid details based on current projection system. Values based on
     * the original value fixed for EPSG:4326 (10000) using heuristics (that is, test&error
     * until most bugs were discovered while keeping the processing time reasonable)
     */
    public void initializeGridDetail() {
        if (Main.proj.toString().equals(new Epsg4326().toString())) {
            OsmValidator.griddetail = 10000;
        } else if (Main.proj.toString().equals(new Mercator().toString())) {
            OsmValidator.griddetail = 0.01;
        } else if (Main.proj.toString().equals(new Lambert().toString())) {
            OsmValidator.griddetail = 0.1;
        }
    }

    /**
     * Initializes all tests
     * @param allTests The tests to initialize
     */
    public static void initializeTests(Collection<Test> allTests) {
        for (Test test : allTests) {
            try {
                if (test.enabled) {
                    test.initialize();
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        if (oldLayer instanceof OsmDataLayer && Main.map.mapView.getActiveLayer() == oldLayer) {
            Main.map.validatorDialog.tree.setErrorList(new ArrayList<TestError>());
        }
        if (oldLayer == errorLayer) {
            errorLayer = null;
            return;
        }
        if (Main.map.mapView.getLayersOfType(OsmDataLayer.class).isEmpty()) {
            if (errorLayer != null) {
                Main.map.mapView.removeLayer(errorLayer);
            }
        }
    }
}
