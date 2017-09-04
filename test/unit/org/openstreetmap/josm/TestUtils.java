// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.AbstractProgressMonitor;
import org.openstreetmap.josm.gui.progress.CancelHandler;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.testutils.FakeGraphics;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        Field f = obj.getClass().getDeclaredField(fieldName);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            f.setAccessible(true);
            return null;
        });
        return f.get(obj);
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
}
