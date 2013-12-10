// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionChoice;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.tools.Pair;

/**
 * This test is used to monitor changes in projection code.
 *
 * It keeps a record of test data in the file data_nodist/projection-regression-test-data.csv.
 * This record is generated from the current Projection classes available in JOSM. It needs to
 * be updated, whenever a projection is added / removed or an algorithm is changed, such that
 * the computed values are numerically different. There is no error threshold, every change is reported.
 *
 * So when this test fails, first check if the change is intended. Then update the regression
 * test data, by running the main method of this class and commit the new data file.
 */
public class ProjectionRegressionTest {

    public static final String PROJECTION_DATA_FILE = "data_nodist/projection-regression-test-data.csv";

    private static class TestData {
        public String code;
        public LatLon ll;
        public EastNorth en;
        public LatLon ll2;
    }

    public static void main(String[] args) throws IOException, FileNotFoundException {
        setUp();

        Map<String, Projection> supportedCodesMap = new HashMap<String, Projection>();
        for (ProjectionChoice pc : ProjectionPreference.getProjectionChoices()) {
            for (String code : pc.allCodes()) {
                Collection<String> pref = pc.getPreferencesFromCode(code);
                pc.setPreferences(pref);
                Projection p = pc.getProjection();
                supportedCodesMap.put(code, p);
            }
        }

        List<TestData> prevData = new ArrayList<TestData>();
        if (new File(PROJECTION_DATA_FILE).exists()) {
            prevData = readData();
        }
        Map<String,TestData> prevCodesMap = new HashMap<String,TestData>();
        for (TestData data : prevData) {
            prevCodesMap.put(data.code, data);
        }

        Set<String> codesToWrite = new LinkedHashSet<String>();
        for (TestData data : prevData) {
            if (supportedCodesMap.containsKey(data.code)) {
                codesToWrite.add(data.code);
            }
        }
        for (String code : supportedCodesMap.keySet()) {
            if (!codesToWrite.contains(code)) {
                codesToWrite.add(code);
            }
        }

        Random rand = new Random();
        BufferedWriter out = new BufferedWriter(new FileWriter(PROJECTION_DATA_FILE));
        out.write("# Data for test/unit/org/openstreetmap/josm/data/projection/ProjectionRegressionTest.java\n");
        out.write("# Format: 1. Projection code; 2. lat/lon; 3. lat/lon projected -> east/north; 4. east/north (3.) inverse projected\n");
        for (Entry<String, Projection> e : supportedCodesMap.entrySet()) {
        }
        for (String code : codesToWrite) {
            Projection proj = supportedCodesMap.get(code);
            Bounds b = proj.getWorldBoundsLatLon();
            double lat, lon;
            TestData prev = prevCodesMap.get(proj.toCode());
            if (prev != null) {
                lat = prev.ll.lat();
                lon = prev.ll.lon();
            } else {
                lat = b.getMin().lat() + rand.nextDouble() * (b.getMax().lat() - b.getMin().lat());
                lon = b.getMin().lon() + rand.nextDouble() * (b.getMax().lon() - b.getMin().lon());
            }
            EastNorth en = proj.latlon2eastNorth(new LatLon(lat, lon));
            LatLon ll2 = proj.eastNorth2latlon(en);
            out.write(String.format("%s%n  ll  %s %s%n  en  %s %s%n  ll2 %s %s%n", proj.toCode(), lat, lon, en.east(), en.north(), ll2.lat(), ll2.lon()));
        }
        out.close();
    }

    private static List<TestData> readData() throws IOException, FileNotFoundException {
        BufferedReader in = new BufferedReader(new FileReader(PROJECTION_DATA_FILE));
        List<TestData> result = new ArrayList<TestData>();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            TestData next = new TestData();

            Pair<Double,Double> ll = readLine("ll", in.readLine());
            Pair<Double,Double> en = readLine("en", in.readLine());
            Pair<Double,Double> ll2 = readLine("ll2", in.readLine());

            next.code = line;
            next.ll = new LatLon(ll.a, ll.b);
            next.en = new EastNorth(en.a, en.b);
            next.ll2 = new LatLon(ll2.a, ll2.b);

            result.add(next);
        }
        in.close();
        return result;
    }

    private static Pair<Double,Double> readLine(String expectedName, String input) {
        String[] fields = input.trim().split("[ ]+");
        if (fields.length != 3) throw new AssertionError();
        if (!fields[0].equals(expectedName)) throw new AssertionError();
        double a = Double.parseDouble(fields[1]);
        double b = Double.parseDouble(fields[2]);
        return Pair.create(a, b);
    }

    @BeforeClass
    public static void setUp() {
        Main.initApplicationPreferences();
    }

    @Test
    public void regressionTest() throws IOException, FileNotFoundException {
        List<TestData> allData = readData();
        Set<String> dataCodes = new HashSet<String>();
        for (TestData data : allData) {
            dataCodes.add(data.code);
        }

        StringBuilder fail = new StringBuilder();

        for (ProjectionChoice pc : ProjectionPreference.getProjectionChoices()) {
            for (String code : pc.allCodes()) {
               if (!dataCodes.contains(code)) {
                    fail.append("Did not find projection "+code+" in test data!\n");
                }
            }
        }

        for (TestData data : allData) {
            Projection proj = Projections.getProjectionByCode(data.code);
            if (proj == null) {
                fail.append("Projection "+data.code+" from test data was not found!\n");
                continue;
            }
            EastNorth en = proj.latlon2eastNorth(data.ll);
            if (!en.equals(data.en)) {
                String error = String.format("%s (%s): Projecting latlon(%s,%s):%n" +
                        "        expected: eastnorth(%s,%s),%n" +
                        "        but got:  eastnorth(%s,%s)!%n",
                        proj.toString(), data.code, data.ll.lat(), data.ll.lon(), data.en.east(), data.en.north(), en.east(), en.north());
                fail.append(error);
            }
            LatLon ll2 = proj.eastNorth2latlon(data.en);
            if (!ll2.equals(data.ll2)) {
                String error = String.format("%s (%s): Inverse projecting eastnorth(%s,%s):%n" +
                        "        expected: latlon(%s,%s),%n" +
                        "        but got:  latlon(%s,%s)!%n",
                        proj.toString(), data.code, data.en.east(), data.en.north(), data.ll2.lat(), data.ll2.lon(), ll2.lat(), ll2.lon());
                fail.append(error);
            }
        }

        if (fail.length() > 0) {
            System.err.println(fail.toString());
            throw new AssertionError(fail.toString());
        }

    }
}
