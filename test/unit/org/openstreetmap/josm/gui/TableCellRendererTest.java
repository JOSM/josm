// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.tools.Utils;
import org.reflections.Reflections;

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
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createFunctionalTestFixture().init(true);
    }

    /**
     * Unit test of all table cell renderers against null values.
     */
    @Test
    public void testTableCellRenderer() {
        Reflections reflections = new Reflections("org.openstreetmap.josm");
        Set<Class<? extends TableCellRenderer>> renderers = reflections.getSubTypesOf(TableCellRenderer.class);
        Assert.assertTrue(renderers.size() >= 10); // if it finds less than 10 classes, something is broken
        JTable tbl = new JTable(2, 2);
        for (Class<? extends TableCellRenderer> klass : renderers) {
            if (Modifier.isAbstract(klass.getModifiers()) || SKIP_TEST.contains(klass.getName())) {
                continue;
            }
            if (klass.isAnonymousClass()) {
                continue;
            }
            TableCellRenderer tcr = createInstance(klass);
            try {
                tcr.getTableCellRendererComponent(tbl, null, false, false, 0, 0);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                Assert.fail("NPE in getTableCellRendererComponent");
            }
        }
    }

    /**
     * Create an instance of a class assuming it has a no-args constructor.
     * @param <T> the class or a super-type of the class
     * @param klass the class
     * @return an instance of the class
     */
    private static <T> T createInstance(Class<? extends T> klass) {
        boolean needOuterClass = klass.isMemberClass() && !Modifier.isStatic(klass.getModifiers());
        Constructor<? extends T> c;
        try {
            if (needOuterClass) {
                c = klass.getDeclaredConstructor(klass.getDeclaringClass());
            } else {
                c = klass.getDeclaredConstructor();
            }
        } catch (NoSuchMethodException ex) {
            // no default constructor - to fix this, add a default constructor
            // to the class or add the class to the SKIP_TEST list above
            Assert.fail("No default constructor - cannot test TableCellRenderer: " + ex);
            return null;
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
        Utils.setObjectsAccessible(c);
        T o;
        try {
            if (needOuterClass) {
                Object outerInstance = createInstance(klass.getDeclaringClass());
                o = c.newInstance(outerInstance);
            } else {
                o = c.newInstance();
            }
        } catch (InstantiationException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        return o;
    }

}
