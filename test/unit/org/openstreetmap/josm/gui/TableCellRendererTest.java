// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Checks if all classes implementing the {@link TableCellRenderer} interface do
 * accept a null value as second parameter for
 * {@link TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
 * java.lang.Object, boolean, boolean, int, int)}.
 *
 * For unknown reason java sometimes call getTableCellRendererComponent method
 * with value = null. Every implementation of {@code getTableCellRendererComponent}
 * must fail gracefully when null is passed as value parameter.
 *
 * This test scans the classpath for classes implementing {@code TableCellRenderer},
 * creates an instance and calls {@code getTableCellRendererComponent} with null
 * value to check if a NPE is thrown.
 *
 * @see <a href="https://josm.openstreetmap.de/ticket/6301">#6301</a>
 */
public class TableCellRendererTest {

    // list of classes that cannot be easily tested and are verified either manually or another unit tests
    private static final Collection<String> SKIP_TEST = Arrays.asList(
        "org.openstreetmap.josm.gui.dialogs.FilterDialog$BooleanRenderer",
        "org.openstreetmap.josm.gui.dialogs.relation.SelectionTableCellRenderer"
    );

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Unit test of all table cell renderers against null values.
     * @throws NoSuchMethodException no default constructor - to fix this, add a default constructor to the class
     *                               or add the class to the SKIP_TEST list above
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testTableCellRenderer() throws ReflectiveOperationException {
        Set<Class<? extends TableCellRenderer>> renderers = TestUtils.getJosmSubtypes(TableCellRenderer.class);
        Assert.assertTrue(renderers.size() >= 10); // if it finds less than 10 classes, something is broken
        JTable tbl = new JTable(2, 2);
        for (Class<? extends TableCellRenderer> klass : renderers) {
            if (Modifier.isAbstract(klass.getModifiers()) || SKIP_TEST.contains(klass.getName())) {
                continue;
            }
            if (klass.isAnonymousClass()) {
                continue;
            }
            try {
                Logging.info(klass.toString());
                assertNotNull(createInstance(klass).getTableCellRendererComponent(tbl, null, false, false, 0, 0));
            } catch (ReflectiveOperationException e) {
                Logging.logWithStackTrace(Level.WARNING, "Unable to test " + klass, e);
            }
        }
    }

    /**
     * Create an instance of a class assuming it has a no-args constructor.
     * @param <T> the class or a super-type of the class
     * @param klass the class
     * @return an instance of the class
     * @throws NoSuchMethodException no default constructor - to fix this, add a default constructor to the class
     *                               or add the class to the SKIP_TEST list above
     * @throws ReflectiveOperationException if an error occurs
     */
    private static <T> T createInstance(Class<? extends T> klass) throws ReflectiveOperationException {
        boolean needOuterClass = klass.isMemberClass() && !Modifier.isStatic(klass.getModifiers());
        Constructor<? extends T> c;
        if (needOuterClass) {
            c = klass.getDeclaredConstructor(klass.getDeclaringClass());
        } else {
            c = klass.getDeclaredConstructor();
        }
        Utils.setObjectsAccessible(c);
        if (needOuterClass) {
            return c.newInstance(createInstance(klass.getDeclaringClass()));
        } else {
            return c.newInstance();
        }
    }
}
