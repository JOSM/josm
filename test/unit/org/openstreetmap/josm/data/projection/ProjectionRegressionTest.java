// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.annotations.ProjectionNadGrids;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * This test is used to monitor changes in projection code.
 * <p>
 * It keeps a record of test data in the file nodist/data/projection/projection-regression-test-data.
 * This record is generated from the current Projection classes available in JOSM. It needs to
 * be updated, whenever a projection is added / removed or an algorithm is changed, such that
 * the computed values are numerically different. There is no error threshold, every change is reported.
 * <p>
 * So when this test fails, first check if the change is intended. Then update the regression
 * test data, by running the main method of this class and commit the new data file.
 */
class ProjectionRegressionTest {

    private static final String PROJECTION_DATA_FILE = "nodist/data/projection/projection-regression-test-data";

    private static final class TestData {
        public String code;
        public LatLon ll;
        public EastNorth en;
        public LatLon ll2;
    }

    /**
     * Program entry point to update reference projection file.
     * @param args not used
     * @throws IOException if any I/O errors occurs
     */
    public static void main(String[] args) throws IOException {
        JOSMFixture.createUnitTestFixture().init();

        Map<String, Projection> supportedCodesMap = Projections.getAllProjectionCodes().stream()
                .collect(Collectors.toMap(code -> code, Projections::getProjectionByCode));

        List<TestData> prevData = new ArrayList<>();
        if (new File(PROJECTION_DATA_FILE).exists()) {
            prevData = readData();
        }
        Map<String, TestData> prevCodesMap = prevData.stream()
                .collect(Collectors.toMap(data -> data.code, data -> data));

        Set<String> codesToWrite = new TreeSet<>(supportedCodesMap.keySet());
        prevData.stream()
                .filter(data -> supportedCodesMap.containsKey(data.code)).map(data -> data.code)
                .forEach(codesToWrite::add);

        Random rand = new SecureRandom();
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(Paths.get(PROJECTION_DATA_FILE)), StandardCharsets.UTF_8))) {
            out.write("# Data for test/unit/org/openstreetmap/josm/data/projection/ProjectionRegressionTest.java\n");
            out.write("# Format: 1. Projection code; 2. lat/lon; 3. lat/lon projected -> east/north; 4. east/north (3.) inverse projected\n");
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
                out.write(String.format(
                        "%s%n  ll  %s %s%n  en  %s %s%n  ll2 %s %s%n", proj.toCode(), lat, lon, en.east(), en.north(), ll2.lat(), ll2.lon()));
            }
        }
        System.out.println("Update successful.");
    }

    private static List<TestData> readData() throws IOException {
        try (BufferedReader in = Files.newBufferedReader(Paths.get(PROJECTION_DATA_FILE), StandardCharsets.UTF_8)) {
            List<TestData> result = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                TestData next = new TestData();

                Pair<Double, Double> ll = readLine("ll", in.readLine());
                Pair<Double, Double> en = readLine("en", in.readLine());
                Pair<Double, Double> ll2 = readLine("ll2", in.readLine());

                next.code = line;
                next.ll = new LatLon(ll.a, ll.b);
                next.en = new EastNorth(en.a, en.b);
                next.ll2 = new LatLon(ll2.a, ll2.b);

                result.add(next);
            }
            return result;
        }
    }

    private static Pair<Double, Double> readLine(String expectedName, String input) {
        String[] fields = input.trim().split("[ ]+", -1);
        if (fields.length != 3) throw new AssertionError();
        if (!fields[0].equals(expectedName)) throw new AssertionError();
        double a = Double.parseDouble(fields[1]);
        double b = Double.parseDouble(fields[2]);
        return Pair.create(a, b);
    }

    /**
     * Non-regression unit test.
     * @throws IOException if any I/O error occurs
     */
    @ProjectionNadGrids
    @Test
    void testNonRegression() throws IOException {
        List<TestData> allData = readData();
        Set<String> dataCodes = allData.stream().map(data -> data.code).collect(Collectors.toSet());

        StringBuilder fail = new StringBuilder();

        for (String code : Projections.getAllProjectionCodes()) {
            if (!dataCodes.contains(code)) {
                 fail.append("Did not find projection ").append(code).append(" in test data!\n");
             }
        }

        double fact = 1200;
        if (Utils.getSystemProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("mac os x"))
            fact = 15000;
        for (TestData data : allData) {
            Projection proj = Projections.getProjectionByCode(data.code);
            if (proj == null) {
                fail.append("Projection ").append(data.code).append(" from test data was not found!\n");
                continue;
            }
            EastNorth en = proj.latlon2eastNorth(data.ll);
            LatLon ll2 = proj.eastNorth2latlon(data.en);
            if (!equalsJava9(en, data.en, fact)) {
                String error = String.format("%s (%s): Projecting latlon(%s,%s):%n" +
                        "        expected: eastnorth(%s,%s),%n" +
                        "        but got:  eastnorth(%s,%s)!%n" +
                        "        test:     [%s,%s] factor [%s,%s] threshold [%s,%s]%n",
                        proj, data.code, data.ll.lat(), data.ll.lon(), data.en.east(), data.en.north(), en.east(), en.north(),
                        Math.abs(en.east()-data.en.east()), Math.abs(en.north()-data.en.north()),
                        Math.abs(en.east()-data.en.east())/Math.ulp(en.east()), Math.abs(en.north()-data.en.north())/Math.ulp(en.north()),
                        Math.ulp(en.east()), Math.ulp(en.north()));
                fail.append(error);
            }
            if (!equalsJava9(ll2, data.ll2, fact)) {
                String error = String.format("%s (%s): Inverse projecting eastnorth(%s,%s):%n" +
                        "        expected: latlon(%s,%s),%n" +
                        "        but got:  latlon(%s,%s)!%n" +
                        "        test:     [%s,%s] factor [%s,%s], threshold [%s,%s]%n",
                        proj, data.code, data.en.east(), data.en.north(), data.ll2.lat(), data.ll2.lon(), ll2.lat(), ll2.lon(),
                        Math.abs(ll2.lat()-data.ll2.lat()), Math.abs(ll2.lon()-data.ll2.lon()),
                        Math.abs(ll2.lat()-data.ll2.lat())/Math.ulp(ll2.lat()), Math.abs(ll2.lon()-data.ll2.lon())/Math.ulp(ll2.lon()),
                        Math.ulp(ll2.lat()), Math.ulp(ll2.lon()));
                fail.append(error);
            }
        }

        if (fail.length() > 0) {
            System.err.println(fail);
            fail(fail.toString());
        }
    }

    private static boolean equalsDoubleMaxUlp(double d1, double d2, double fact) {
        // Due to error accumulation in projection computation, the difference can reach hundreds of ULPs
        // The worst error is 1168 ULP (followed by 816 ULP then 512 ULP) with:
        // NAD83 / Colorado South (EPSG:26955): Projecting latlon(32.24604527892822,-125.93039495227096):
        // expected: eastnorth(-1004398.8994415681,24167.8944844745),
        // but got:  eastnorth(-1004398.8994415683,24167.894484478747)!
        return Math.abs(d1 - d2) <= fact * Math.ulp(d1);
    }

    private static boolean equalsJava9(EastNorth en1, EastNorth en2, double fact) {
        return equalsDoubleMaxUlp(en1.east(), en2.east(), fact) &&
               equalsDoubleMaxUlp(en1.north(), en2.north(), fact);
    }

    private static boolean equalsJava9(LatLon ll1, LatLon ll2, double fact) {
        return equalsDoubleMaxUlp(ll1.lat(), ll2.lat(), fact) &&
               equalsDoubleMaxUlp(ll1.lon(), ll2.lon(), fact);
    }
}
