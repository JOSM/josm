// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Useful methods for annotation extensions
 * @author Taylor Smock
 * @since 18037
 */
public final class AnnotationUtils {
    private AnnotationUtils() {
        // Utils class
    }

    /**
     * Find the first parent annotation
     * @param <T> The annotation to find
     * @param context The context to search
     * @param annotation The annotation to find
     * @return See {@link AnnotationSupport#findAnnotation}
     */
    public static <T extends Annotation> Optional<T> findFirstParentAnnotation(ExtensionContext context, Class<T> annotation) {
        ExtensionContext current = context;
        do {
            Optional<T> foundAnnotation = AnnotationSupport.findAnnotation(current.getElement(), annotation);
            if (foundAnnotation.isPresent()) {
                return foundAnnotation;
            }
            current = current.getParent().orElse(null);
        } while (current != null);
        return Optional.empty();
    }

    /**
     * Reset a static class (all static fields are unset). If they are initialized as part of a static block, please be aware of NPEs.
     * @param clazz The class to reset
     * @throws ReflectiveOperationException If reflection doesn't work, for whatever reason.
     */
    public static void resetStaticClass(Class<?> clazz) throws ReflectiveOperationException {
        // Assume that all singletons implement a `getInstance` method, which initializes the singleton object if it is null.
        final Optional<Method> getInstanceMethod = ReflectionSupport.findMethod(clazz, "getInstance");
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            // Don't reset fields that are not static
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            final boolean isFinal = (field.getModifiers() & Modifier.FINAL) != 0;
            final Object fieldObject = field.get(null);
            if (isFinal) {
                if (fieldObject instanceof Collection) {
                    // Clear all collections (assume they start empty)
                    try {
                        ((Collection<?>) fieldObject).clear();
                    } catch (UnsupportedOperationException e) {
                        // Probably an unmodifiable collection
                        Logging.error("Unable to clear {0}", field);
                    }
                } else if (fieldObject instanceof Map) {
                    // Clear all maps (assume they start empty)
                    try {
                        ((Map<?, ?>) fieldObject).clear();
                    } catch (UnsupportedOperationException e) {
                        // Probably an unmodifiable collection
                        Logging.error("Unable to clear {0}", field);
                    }
                } else if (fieldObject instanceof MultiMap) {
                    // Clear multimap
                    ((MultiMap<?, ?>) fieldObject).clear();
                }
            } else if ("instance".equals(field.getName()) && getInstanceMethod.isPresent()) {
                // If there is a field with the name "instance", and there is a getInstanceMethod, the presumption
                // is that the getInstance method will initialize the instance if it is null.
                field.set(null, null);
            } else {
                // Only reset static fields, but not final static fields
                field.set(null, null);
            }
        }
    }

    /**
     * Check if the element is annotated with the
     * @param annotatedElement The annotated element wrapped in an optional.
     * @param annotationClass The annotation class
     * @param <T> Annotation class type
     * @return {@code true} if the element is present and is annotated with the specified class
     */
    public static <T extends Annotation> boolean elementIsAnnotated(Optional<AnnotatedElement> annotatedElement, Class<T> annotationClass) {
        return annotatedElement.isPresent() && annotatedElement.get().isAnnotationPresent(annotationClass);
    }
}
