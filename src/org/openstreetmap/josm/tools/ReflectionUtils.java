// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.lang.reflect.AccessibleObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.function.Function;

import org.openstreetmap.josm.plugins.PluginHandler;

/**
 * Reflection utilities.
 * @since 14977
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Sets {@code AccessibleObject}(s) accessible.
     * @param objects objects
     * @see AccessibleObject#setAccessible
     */
    public static void setObjectsAccessible(final AccessibleObject... objects) {
        if (objects != null && objects.length > 0) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                for (AccessibleObject o : objects) {
                    if (o != null) {
                        o.setAccessible(true);
                    }
                }
                return null;
            });
        }
    }

    /**
     * To use from a method to know which class called it.
     * @param exclusions classes to exclude from the search. Can be null
     * @return the first calling class not present in {@code exclusions}
     */
    public static Class<?> findCallerClass(Collection<Class<?>> exclusions) {
        return findCaller(x -> {
            try {
                return Class.forName(x.getClassName());
            } catch (ClassNotFoundException e) {
                for (ClassLoader cl : PluginHandler.getPluginClassLoaders()) {
                    try {
                        return Class.forName(x.getClassName(), true, cl);
                    } catch (ClassNotFoundException ex) {
                        Logging.trace(ex);
                    }
                }
                Logging.error(e);
                return null;
            }
        }, exclusions);
    }

    private static <T extends Object> T findCaller(Function<StackTraceElement, T> getter, Collection<T> exclusions) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 3; i < stack.length; i++) {
            T t = getter.apply(stack[i]);
            if (exclusions == null || !exclusions.contains(t)) {
                return t;
            }
        }
        return null;
    }
}
