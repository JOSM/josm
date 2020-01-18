// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.preferences.projection.CodeProjectionChoice;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * Test projections using reference data from external program.
 *
 * To update the reference data file <code>data_nodist/projection/projection-reference-data</code>,
 * run the main method of this class. For this, you need to have the cs2cs
 * program from the proj.4 library in path (or use <code>CS2CS_EXE</code> to set
 * the full path of the executable). Make sure the required *.gsb grid files
 * can be accessed, i.e. copy them from <code>data_nodist/projection</code> to <code>/usr/share/proj</code> or
 * wherever cs2cs expects them to be placed.
 *
 * The input parameter for the external library is <em>not</em> the projection code
 * (e.g. "EPSG:25828"), but the entire definition, (e.g. "+proj=utm +zone=28 +ellps=GRS80 +nadgrids=null").
 * This means the test does not verify our definitions, but the correctness
 * of the algorithm, given a certain definition.
 */
public class ProjectionRefTest {

    private static final String CS2CS_EXE = "cs2cs";

    private static final String REFERENCE_DATA_FILE = "data_nodist/projection/projection-reference-data";
    private static final String PROJ_LIB_DIR = "data_nodist/projection";

    private static class RefEntry {
        String code;
        String def;
        List<Pair<LatLon, EastNorth>> data;

        RefEntry(String code, String def) {
            this.code = code;
            this.def = def;
            this.data = new ArrayList<>();
        }
    }

    static Random rand = new SecureRandom();

    static boolean debug;
    static List<String> forcedCodes;

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projectionNadGrids().timeout(90_000);

    /**
     * Program entry point.
     * @param args optional comma-separated list of projections to update. If set, only these projections will be updated
     * @throws IOException in case of I/O error
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            debug = "debug".equals(args[0]);
            if (args[args.length - 1].startsWith("EPSG:")) {
                forcedCodes = Arrays.asList(args[args.length - 1].split(","));
            }
        }
        Collection<RefEntry> refs = readData();
        refs = updateData(refs);
        writeData(refs);
    }

    /**
     * Reads data from the reference file.
     * @return the data
     * @throws IOException if any I/O error occurs
     */
    private static Collection<RefEntry> readData() throws IOException {
        Collection<RefEntry> result = new ArrayList<>();
        if (!new File(REFERENCE_DATA_FILE).exists()) {
            System.err.println("Warning: reference file does not exist.");
            return result;
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                Files.newInputStream(Paths.get(REFERENCE_DATA_FILE)), StandardCharsets.UTF_8))) {
            String line;
            Pattern projPattern = Pattern.compile("<(.+?)>(.*)<>");
            RefEntry curEntry = null;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                if (line.startsWith("<")) {
                    Matcher m = projPattern.matcher(line);
                    if (!m.matches()) {
                        Assert.fail("unable to parse line: " + line);
                    }
                    String code = m.group(1);
                    String def = m.group(2).trim();
                    curEntry = new RefEntry(code, def);
                    result.add(curEntry);
                } else if (curEntry != null) {
                    String[] f = line.trim().split(",");
                    double lon = Double.parseDouble(f[0]);
                    double lat = Double.parseDouble(f[1]);
                    double east = Double.parseDouble(f[2]);
                    double north = Double.parseDouble(f[3]);
                    curEntry.data.add(Pair.create(new LatLon(lat, lon), new EastNorth(east, north)));
                }
            }
        }
        return result;
    }

    /**
     * Generates new reference data by calling external program cs2cs.
     *
     * Old data is kept, as long as the projection definition is still the same.
     *
     * @param refs old data
     * @return updated data
     */
    private static Collection<RefEntry> updateData(Collection<RefEntry> refs) {
        Set<String> failed = new LinkedHashSet<>();
        final int N_POINTS = 8;

        Map<String, RefEntry> refsMap = new HashMap<>();
        for (RefEntry ref : refs) {
            refsMap.put(ref.code, ref);
        }

        List<RefEntry> refsNew = new ArrayList<>();

        Set<String> codes = new TreeSet<>(new CodeProjectionChoice.CodeComparator());
        codes.addAll(Projections.getAllProjectionCodes());
        for (String code : codes) {
            String def = Projections.getInit(code);

            RefEntry ref = new RefEntry(code, def);
            RefEntry oldRef = refsMap.get(code);

            if (oldRef != null && Objects.equals(def, oldRef.def)) {
                for (int i = 0; i < N_POINTS && i < oldRef.data.size(); i++) {
                    ref.data.add(oldRef.data.get(i));
                }
            }
            boolean forced = forcedCodes != null && forcedCodes.contains(code) && !ref.data.isEmpty();
            if (forced || ref.data.size() < N_POINTS) {
                System.out.print(code);
                System.out.flush();
                Projection proj = Projections.getProjectionByCode(code);
                Bounds b = proj.getWorldBoundsLatLon();
                for (int i = forced ? 0 : ref.data.size(); i < N_POINTS; i++) {
                    System.out.print(".");
                    System.out.flush();
                    if (debug) {
                        System.out.println();
                    }
                    LatLon ll = forced ? ref.data.get(i).a : getRandom(b);
                    EastNorth en = latlon2eastNorthProj4(def, ll);
                    if (en != null) {
                        if (forced) {
                            ref.data.get(i).b = en;
                        } else {
                            ref.data.add(Pair.create(ll, en));
                        }
                    } else {
                        System.err.println("Warning: cannot convert "+code+" at "+ll);
                        failed.add(code);
                    }
                }
                System.out.println();
            }
            refsNew.add(ref);
        }
        if (!failed.isEmpty()) {
            System.err.println("Error: the following " + failed.size() + " entries had errors: " + failed);
        }
        return refsNew;
    }

    /**
     * Get random LatLon value within the bounds.
     * @param b the bounds
     * @return random LatLon value within the bounds
     */
    private static LatLon getRandom(Bounds b) {
        double lat, lon;
        lat = b.getMin().lat() + rand.nextDouble() * (b.getMax().lat() - b.getMin().lat());
        double minlon = b.getMinLon();
        double maxlon = b.getMaxLon();
        if (b.crosses180thMeridian()) {
            maxlon += 360;
        }
        lon = minlon + rand.nextDouble() * (maxlon - minlon);
        lon = LatLon.toIntervalLon(lon);
        return new LatLon(lat, lon);
    }

    /**
     * Run external PROJ.4 library to convert lat/lon to east/north value.
     * @param def the proj.4 projection definition string
     * @param ll the LatLon
     * @return projected EastNorth or null in case of error
     */
    private static EastNorth latlon2eastNorthProj4(String def, LatLon ll) {
        try {
            Class<?> projClass = Class.forName("org.proj4.PJ");
            Constructor<?> constructor = projClass.getConstructor(String.class);
            Method transform = projClass.getMethod("transform", projClass, int.class, double[].class, int.class, int.class);
            Object sourcePJ = constructor.newInstance("+proj=longlat +datum=WGS84");
            Object targetPJ = constructor.newInstance(def);
            double[] coordinates = {ll.lon(), ll.lat()};
            if (debug) {
                System.out.println(def);
                System.out.print(Arrays.toString(coordinates));
            }
            transform.invoke(sourcePJ, targetPJ, 2, coordinates, 0, 1);
            if (debug) {
                System.out.println(" -> " + Arrays.toString(coordinates));
            }
            return new EastNorth(coordinates[0], coordinates[1]);
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            if (debug) {
                System.err.println("Error for " + def);
                e.printStackTrace();
            }
            // PROJ JNI bindings not available, fallback to cs2cs
            return latlon2eastNorthProj4cs2cs(def, ll);
        }
    }

    /**
     * Run external cs2cs command from the PROJ.4 library to convert lat/lon to east/north value.
     * @param def the proj.4 projection definition string
     * @param ll the LatLon
     * @return projected EastNorth or null in case of error
     */
    @SuppressFBWarnings(value = "COMMAND_INJECTION")
    private static EastNorth latlon2eastNorthProj4cs2cs(String def, LatLon ll) {
        List<String> args = new ArrayList<>();
        args.add(CS2CS_EXE);
        args.addAll(Arrays.asList("-f %.9f +proj=longlat +datum=WGS84 +to".split(" ")));
        // proj.4 cannot read our ntf_r93_b.gsb file
        // possibly because it is big endian. Use equivalent
        // little endian file shipped with proj.4.
        // see http://geodesie.ign.fr/contenu/fichiers/documentation/algorithmes/notice/NT111_V1_HARMEL_TransfoNTF-RGF93_FormatGrilleNTV2.pdf
        def = def.replace("ntf_r93_b.gsb", "ntf_r93.gsb");
        if (PlatformManager.isPlatformWindows()) {
            def = def.replace("'", "\\'").replace("\"", "\\\"");
        }
        args.addAll(Arrays.asList(def.split(" ")));
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().put("PROJ_LIB", new File(PROJ_LIB_DIR).getAbsolutePath());

        String output = "";
        try {
            Process process = pb.start();
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin, StandardCharsets.UTF_8))) {
                String s = String.format("%s %s%n",
                        LatLon.cDdHighPecisionFormatter.format(ll.lon()),
                        LatLon.cDdHighPecisionFormatter.format(ll.lat()));
                if (debug) {
                    System.out.println("\n" + String.join(" ", args) + "\n" + s);
                }
                writer.write(s);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8))) {
                String line;
                while (null != (line = reader.readLine())) {
                    if (debug) {
                        System.out.println("> " + line);
                    }
                    output = line;
                }
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8))) {
                String line;
                while (null != (line = reader.readLine())) {
                    System.err.println("! " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Running external command failed: " + e + "\nCommand was: " + String.join(" ", args));
            return null;
        }
        Pattern p = Pattern.compile("(\\S+)\\s+(\\S+)\\s.*");
        Matcher m = p.matcher(output);
        if (!m.matches()) {
            System.err.println("Error: Cannot parse cs2cs output: '" + output + "'");
            return null;
        }
        String es = m.group(1);
        String ns = m.group(2);
        if ("*".equals(es) || "*".equals(ns)) {
            System.err.println("Error: cs2cs is unable to convert coordinates.");
            return null;
        }
        try {
            return new EastNorth(Double.parseDouble(es), Double.parseDouble(ns));
        } catch (NumberFormatException nfe) {
            System.err.println("Error: Cannot parse cs2cs output: '" + es + "', '" + ns + "'" + "\nCommand was: " + String.join(" ", args));
            return null;
        }
    }

    /**
     * Writes data to file.
     * @param refs the data
     * @throws IOException if any I/O error occurs
     */
    private static void writeData(Collection<RefEntry> refs) throws IOException {
        Map<String, RefEntry> refsMap = new TreeMap<>(new CodeProjectionChoice.CodeComparator());
        for (RefEntry ref : refs) {
            refsMap.put(ref.code, ref);
        }
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(Paths.get(REFERENCE_DATA_FILE)), StandardCharsets.UTF_8))) {
            for (Map.Entry<String, RefEntry> e : refsMap.entrySet()) {
                RefEntry ref = e.getValue();
                out.write("<" + ref.code + "> " + ref.def + "  <>\n");
                for (Pair<LatLon, EastNorth> p : ref.data) {
                    LatLon ll = p.a;
                    EastNorth en = p.b;
                    out.write("    " + ll.lon() + "," + ll.lat() + "," + en.east() + "," + en.north() + "\n");
                }
            }
        }
    }

    /**
     * Test projections.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testProjections() throws IOException {
        StringBuilder fail = new StringBuilder();
        Map<String, Set<String>> failingProjs = new HashMap<>();
        Set<String> allCodes = new HashSet<>(Projections.getAllProjectionCodes());
        Collection<RefEntry> refs = readData();

        for (RefEntry ref : refs) {
            String def0 = Projections.getInit(ref.code);
            if (def0 == null) {
                Assert.fail("unknown code: "+ref.code);
            }
            if (!ref.def.equals(def0)) {
                fail.append("definitions for ").append(ref.code).append(" do not match\n");
            } else {
                CustomProjection proj = (CustomProjection) Projections.getProjectionByCode(ref.code);
                double scale = proj.getToMeter();
                for (Pair<LatLon, EastNorth> p : ref.data) {
                    LatLon ll = p.a;
                    EastNorth enRef = p.b;
                    enRef = new EastNorth(enRef.east() * scale, enRef.north() * scale); // convert to meter

                    EastNorth en = proj.latlon2eastNorth(ll);
                    if (proj.switchXY()) {
                        en = new EastNorth(en.north(), en.east());
                    }
                    en = new EastNorth(en.east() * scale, en.north() * scale); // convert to meter
                    final double EPSILON_EN = 1e-2; // 1cm
                    if (!isEqual(enRef, en, EPSILON_EN, true)) {
                        String errorEN = String.format("%s (%s): Projecting latlon(%s,%s):%n" +
                                "        expected: eastnorth(%s,%s),%n" +
                                "        but got:  eastnorth(%s,%s)!%n",
                                proj.toString(), proj.toCode(), ll.lat(), ll.lon(), enRef.east(), enRef.north(), en.east(), en.north());
                        fail.append(errorEN);
                        failingProjs.computeIfAbsent(proj.proj.getProj4Id(), x -> new TreeSet<>()).add(ref.code);
                    }
                }
            }
            allCodes.remove(ref.code);
        }
        if (!allCodes.isEmpty()) {
            Assert.fail("no reference data for following projections: "+allCodes);
        }
        if (fail.length() > 0) {
            System.err.println(fail.toString());
            throw new AssertionError("Failing:\n" +
                    failingProjs.keySet().size() + " projections: " + failingProjs.keySet() + "\n" +
                    failingProjs.values().stream().mapToInt(Set::size).sum() + " definitions: " + failingProjs);
        }
    }

    /**
     * Check if two EastNorth objects are equal.
     * @param en1 first value
     * @param en2 second value
     * @param epsilon allowed tolerance
     * @param abs true if absolute value is compared; this is done as long as
     * advanced axis configuration is not supported in JOSM
     * @return true if both are considered equal
     */
    private static boolean isEqual(EastNorth en1, EastNorth en2, double epsilon, boolean abs) {
        double east1 = en1.east();
        double north1 = en1.north();
        double east2 = en2.east();
        double north2 = en2.north();
        if (abs) {
            east1 = Math.abs(east1);
            north1 = Math.abs(north1);
            east2 = Math.abs(east2);
            north2 = Math.abs(north2);
        }
        return Math.abs(east1 - east2) < epsilon && Math.abs(north1 - north2) < epsilon;
    }
}
