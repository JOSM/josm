// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assume;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.AbstractProgressMonitor;
import org.openstreetmap.josm.gui.progress.CancelHandler;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.testutils.FakeGraphics;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WikiReader;
import org.reflections.Reflections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.io.ByteStreams;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.integration.TestRunnerDecorator;

/**
 * Various utils, useful for unit tests.
 */
public final class TestUtils {

    private TestUtils() {
        // Hide constructor for utility classes
    }

    /**
     * Returns the path to test data root directory.
     * @return path to test data root directory
     */
    public static String getTestDataRoot() {
        String testDataRoot = System.getProperty("josm.test.data");
        if (testDataRoot == null || testDataRoot.isEmpty()) {
            testDataRoot = "test/data";
            System.out.println("System property josm.test.data is not set, using '" + testDataRoot + "'");
        }
        return testDataRoot.endsWith("/") ? testDataRoot : testDataRoot + "/";
    }

    /**
     * Gets path to test data directory for given ticket id.
     * @param ticketid Ticket numeric identifier
     * @return path to test data directory for given ticket id
     */
    public static String getRegressionDataDir(int ticketid) {
        return TestUtils.getTestDataRoot() + "/regress/" + ticketid;
    }

    /**
     * Gets path to given file in test data directory for given ticket id.
     * @param ticketid Ticket numeric identifier
     * @param filename File name
     * @return path to given file in test data directory for given ticket id
     */
    public static String getRegressionDataFile(int ticketid, String filename) {
        return getRegressionDataDir(ticketid) + '/' + filename;
    }

    /**
     * Gets input stream to given file in test data directory for given ticket id.
     * @param ticketid Ticket numeric identifier
     * @param filename File name
     * @return path to given file in test data directory for given ticket id
     * @throws IOException if any I/O error occurs
     */
    public static InputStream getRegressionDataStream(int ticketid, String filename) throws IOException {
        return Compression.getUncompressedFileInputStream(new File(getRegressionDataDir(ticketid), filename));
    }

    /**
     * Checks that the given Comparator respects its contract on the given table.
     * @param <T> type of elements
     * @param comparator The comparator to test
     * @param array The array sorted for test purpose
     */
    @SuppressFBWarnings(value = "RV_NEGATING_RESULT_OF_COMPARETO")
    public static <T> void checkComparableContract(Comparator<T> comparator, T[] array) {
        System.out.println("Validating Comparable contract on array of "+array.length+" elements");
        // Check each compare possibility
        for (int i = 0; i < array.length; i++) {
            T r1 = array[i];
            for (int j = i; j < array.length; j++) {
                T r2 = array[j];
                int a = comparator.compare(r1, r2);
                int b = comparator.compare(r2, r1);
                if (i == j || a == b) {
                    if (a != 0 || b != 0) {
                        fail(getFailMessage(r1, r2, a, b));
                    }
                } else {
                    if (a != -b) {
                        fail(getFailMessage(r1, r2, a, b));
                    }
                }
                for (int k = j; k < array.length; k++) {
                    T r3 = array[k];
                    int c = comparator.compare(r1, r3);
                    int d = comparator.compare(r2, r3);
                    if (a > 0 && d > 0) {
                        if (c <= 0) {
                           fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    } else if (a == 0 && d == 0) {
                        if (c != 0) {
                            fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    } else if (a < 0 && d < 0) {
                        if (c >= 0) {
                            fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    }
                }
            }
        }
        // Sort relation array
        Arrays.sort(array, comparator);
    }

    private static <T> String getFailMessage(T o1, T o2, int a, int b) {
        return new StringBuilder("Compared\no1: ").append(o1).append("\no2: ")
        .append(o2).append("\ngave: ").append(a).append("/").append(b)
        .toString();
    }

    private static <T> String getFailMessage(T o1, T o2, T o3, int a, int b, int c, int d) {
        return new StringBuilder(getFailMessage(o1, o2, a, b))
        .append("\nCompared\no1: ").append(o1).append("\no3: ").append(o3).append("\ngave: ").append(c)
        .append("\nCompared\no2: ").append(o2).append("\no3: ").append(o3).append("\ngave: ").append(d)
        .toString();
    }

    /**
     * Returns a private field value.
     * @param obj object
     * @param fieldName private field name
     * @return private field value
     * @throws ReflectiveOperationException if a reflection operation error occurs
     */
    public static Object getPrivateField(Object obj, String fieldName) throws ReflectiveOperationException {
        return getPrivateField(obj.getClass(), obj, fieldName);
    }

    /**
     * Returns a private field value.
     * @param cls object class
     * @param obj object
     * @param fieldName private field name
     * @return private field value
     * @throws ReflectiveOperationException if a reflection operation error occurs
     */
    public static Object getPrivateField(Class<?> cls, Object obj, String fieldName) throws ReflectiveOperationException {
        Field f = cls.getDeclaredField(fieldName);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            f.setAccessible(true);
            return null;
        });
        return f.get(obj);
    }

    /**
     * Sets a private field value.
     * @param obj object
     * @param fieldName private field name
     * @param value replacement value
     * @throws ReflectiveOperationException if a reflection operation error occurs
     */
    public static void setPrivateField(
        final Object obj,
        final String fieldName,
        final Object value
    ) throws ReflectiveOperationException {
        setPrivateField(obj.getClass(), obj, fieldName, value);
    }

    /**
     * Sets a private field value.
     * @param cls object class
     * @param obj object
     * @param fieldName private field name
     * @param value replacement value
     * @throws ReflectiveOperationException if a reflection operation error occurs
     */
    public static void setPrivateField(
        final Class<?> cls,
        final Object obj,
        final String fieldName,
        final Object value
    ) throws ReflectiveOperationException {
        Field f = cls.getDeclaredField(fieldName);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            f.setAccessible(true);
            return null;
        });
        f.set(obj, value);
    }

    /**
     * Returns a private static field value.
     * @param cls object class
     * @param fieldName private field name
     * @return private field value
     * @throws ReflectiveOperationException if a reflection operation error occurs
     */
    public static Object getPrivateStaticField(Class<?> cls, String fieldName) throws ReflectiveOperationException {
        Field f = cls.getDeclaredField(fieldName);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            f.setAccessible(true);
            return null;
        });
        return f.get(null);
    }

    /**
     * Sets a private static field value.
     * @param cls object class
     * @param fieldName private field name
     * @param value replacement value
     * @throws ReflectiveOperationException if a reflection operation error occurs
     */
    public static void setPrivateStaticField(Class<?> cls, String fieldName, final Object value) throws ReflectiveOperationException {
        Field f = cls.getDeclaredField(fieldName);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            f.setAccessible(true);
            return null;
        });
        f.set(null, value);
    }

    /**
     * Returns an instance of {@link AbstractProgressMonitor} which keeps track of the monitor state,
     * but does not show the progress.
     * @return a progress monitor
     */
    public static ProgressMonitor newTestProgressMonitor() {
        return new AbstractProgressMonitor(new CancelHandler()) {

            @Override
            protected void doBeginTask() {
            }

            @Override
            protected void doFinishTask() {
            }

            @Override
            protected void doSetIntermediate(boolean value) {
            }

            @Override
            protected void doSetTitle(String title) {
            }

            @Override
            protected void doSetCustomText(String title) {
            }

            @Override
            protected void updateProgress(double value) {
            }

            @Override
            public void setProgressTaskId(ProgressTaskId taskId) {
            }

            @Override
            public ProgressTaskId getProgressTaskId() {
                return null;
            }

            @Override
            public Component getWindowParent() {
                return null;
            }
        };
    }

    /**
     * Returns an instance of {@link Graphics2D}.
     * @return a mockup graphics instance
     */
    public static Graphics2D newGraphics() {
        return new FakeGraphics();
    }

    /**
     * Makes sure the given primitive belongs to a data set.
     * @param <T> OSM primitive type
     * @param osm OSM primitive
     * @return OSM primitive, attached to a new {@code DataSet}
     */
    public static <T extends OsmPrimitive> T addFakeDataSet(T osm) {
        new DataSet(osm);
        return osm;
    }

    /**
     * Creates a new node with the given tags (see {@link OsmUtils#createPrimitive(java.lang.String)})
     *
     * @param tags  the tags to set
     * @return a new node
     */
    public static Node newNode(String tags) {
        return (Node) OsmUtils.createPrimitive("node " + tags);
    }

    /**
     * Creates a new way with the given tags (see {@link OsmUtils#createPrimitive(java.lang.String)}) and the nodes added
     *
     * @param tags  the tags to set
     * @param nodes the nodes to add
     * @return a new way
     */
    public static Way newWay(String tags, Node... nodes) {
        final Way way = (Way) OsmUtils.createPrimitive("way " + tags);
        for (Node node : nodes) {
            way.addNode(node);
        }
        return way;
    }

    /**
     * Creates a new relation with the given tags (see {@link OsmUtils#createPrimitive(java.lang.String)}) and the members added
     *
     * @param tags  the tags to set
     * @param members the members to add
     * @return a new relation
     */
    public static Relation newRelation(String tags, RelationMember... members) {
        final Relation relation = (Relation) OsmUtils.createPrimitive("relation " + tags);
        for (RelationMember member : members) {
            relation.addMember(member);
        }
        return relation;
    }

    /**
     * Creates a new empty command.
     * @param ds data set
     * @return a new empty command
     */
    public static Command newCommand(DataSet ds) {
        return new Command(ds) {
            @Override
            public String getDescriptionText() {
                return "";
            }

            @Override
            public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
                    Collection<OsmPrimitive> added) {
                // Do nothing
            }
        };
    }

    /**
     * Ensures 100% code coverage for enums.
     * @param enumClass enum class to cover
     */
    public static void superficialEnumCodeCoverage(Class<? extends Enum<?>> enumClass) {
        try {
            Method values = enumClass.getMethod("values");
            Method valueOf = enumClass.getMethod("valueOf", String.class);
            Utils.setObjectsAccessible(values, valueOf);
            for (Object o : (Object[]) values.invoke(null)) {
                assertEquals(o, valueOf.invoke(null, ((Enum<?>) o).name()));
            }
        } catch (IllegalArgumentException | ReflectiveOperationException | SecurityException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * Get a descendant component by name.
     * @param root The root component to start searching from.
     * @param name The component name
     * @return The component with that name or null if it does not exist.
     * @since 12045
     */
    public static Component getComponentByName(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        } else if (root instanceof Container) {
            Container container = (Container) root;
            return Stream.of(container.getComponents())
                    .map(child -> getComponentByName(child, name))
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Use to assume that EqualsVerifier is working with the current JVM.
     */
    public static void assumeWorkingEqualsVerifier() {
        try {
            // Workaround to https://github.com/jqno/equalsverifier/issues/177
            // Inspired by https://issues.apache.org/jira/browse/SOLR-11606
            nl.jqno.equalsverifier.internal.lib.bytebuddy.ClassFileVersion.ofThisVm();
        } catch (IllegalArgumentException e) {
            Assume.assumeNoException(e);
        }
    }

    /**
     * Use to assume that JMockit is working with the current JVM.
     */
    public static void assumeWorkingJMockit() {
        try {
            // Workaround to https://github.com/jmockit/jmockit1/issues/534
            // Inspired by https://issues.apache.org/jira/browse/SOLR-11606
            new WindowMocker();
            new JOptionPaneSimpleMocker();
        } catch (UnsupportedOperationException e) {
            Assume.assumeNoException(e);
        } finally {
            TestRunnerDecorator.cleanUpAllMocks();
        }
    }

    /**
     * Return WireMock server serving files under ticker directory
     * @param ticketId Ticket numeric identifier
     * @return WireMock HTTP server on dynamic port
     */
    public static WireMockServer getWireMockServer(int ticketId) {
            return new WireMockServer(
                    WireMockConfiguration.options()
                        .dynamicPort()
                        .usingFilesUnderDirectory(getRegressionDataDir(ticketId))
                    );
    }

    /**
     * Return WireMock server serving files under ticker directory
     * @return WireMock HTTP server on dynamic port
     */
    public static WireMockServer getWireMockServer() {
            return new WireMockServer(
                    WireMockConfiguration.options()
                        .dynamicPort()
                    );
    }

    /**
     * Renders Temporal to RFC 1123 Date Time
     * @param time to convert
     * @return string representation according to RFC1123 of time
     */
    public static String getHTTPDate(Temporal time) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(time);
    }

    /**
     * Renders java time stamp to RFC 1123 Date Time
     * @param time java timestamp (milliseconds from Epoch)
     * @return string representation according to RFC1123 of time
     */
    public static String getHTTPDate(long time) {
        return getHTTPDate(Instant.ofEpochMilli(time));
    }

    /**
     * Throws AssertionError if contents of both files are not equal
     * @param fileA File A
     * @param fileB File B
     */
    public static void assertFileContentsEqual(final File fileA, final File fileB) {
        assertTrue(fileA.exists());
        assertTrue(fileA.canRead());
        assertTrue(fileB.exists());
        assertTrue(fileB.canRead());
        try {
            try (
                FileInputStream streamA = new FileInputStream(fileA);
                FileInputStream streamB = new FileInputStream(fileB);
            ) {
                assertArrayEquals(
                    ByteStreams.toByteArray(streamA),
                    ByteStreams.toByteArray(streamB)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits until any asynchronous operations launched by the test on the EDT or worker threads have
     * (almost certainly) completed.
     */
    public static void syncEDTAndWorkerThreads() {
        boolean workerQueueEmpty = false;
        while (!workerQueueEmpty) {
            try {
                // once our own task(s) have made it to the front of their respective queue(s),
                // they're both executing at the same time and we know there aren't any outstanding
                // worker tasks, then presumably the only way there could be incomplete operations
                // is if the EDT had launched a deferred task to run on itself or perhaps set up a
                // swing timer - neither are particularly common patterns in JOSM (?)
                //
                // there shouldn't be a risk of creating a deadlock in doing this as there shouldn't
                // (...couldn't?) be EDT operations waiting on the results of a worker task.
                workerQueueEmpty = MainApplication.worker.submit(
                    () -> GuiHelper.runInEDTAndWaitAndReturn(
                        () -> ((ThreadPoolExecutor) MainApplication.worker).getQueue().isEmpty()
                    )
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                // inconclusive - retry...
                workerQueueEmpty = false;
            }
        }
    }

    /**
     * Returns all JOSM subtypes of the given class.
     * @param <T> class
     * @param superClass class
     * @return all JOSM subtypes of the given class
     */
    public static <T> Set<Class<? extends T>> getJosmSubtypes(Class<T> superClass) {
        return new Reflections("org.openstreetmap.josm").getSubTypesOf(superClass);
    }

    /**
     * Determines if OSM DEV_API credential have been provided. Required for functional tests.
     * @return {@code true} if {@code osm.username} and {@code osm.password} have been defined on the command line
     */
    public static boolean areCredentialsProvided() {
        return Utils.getSystemProperty("osm.username") != null && Utils.getSystemProperty("osm.password") != null;
    }

    /**
     * Returns the ignored error messages listed on
     * <a href="https://josm.openstreetmap.de/wiki/IntegrationTestIgnores">JOSM wiki</a> for a given test.
     * @param integrationTest The integration test class
     * @return the ignored error messages listed on JOSM wiki for this test.
     * @throws IOException in case of I/O error
     */
    public static List<String> getIgnoredErrorMessages(Class<?> integrationTest) throws IOException {
        return Arrays.stream(new WikiReader()
                .read("https://josm.openstreetmap.de/wiki/IntegrationTestIgnores?format=txt")
                .split("\\n"))
                .filter(s -> s.startsWith("|| " + integrationTest.getSimpleName() + " ||"))
                .map(s -> s.substring(s.indexOf("{{{") + 3, s.indexOf("}}}")))
                .collect(Collectors.toList());
    }
}
