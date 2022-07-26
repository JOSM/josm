// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Use to reset the unique id generator. Some tests are sensitive to the ids of the primitives.
 * @since xxx
 * @author Taylor Smock
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
@ExtendWith(UniqueIdGenerator.UniqueIdGeneratorReset.class)
public @interface UniqueIdGenerator {
    /**
     * The classes with id generators that should be reset.
     * @return The classes to search
     */
    Class<?>[] value() default { Node.class, Relation.class, Way.class };
    class UniqueIdGeneratorReset implements AfterEachCallback, BeforeEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            this.beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            final Optional<UniqueIdGenerator> annotation = AnnotationUtils.findFirstParentAnnotation(context,
                    UniqueIdGenerator.class);
            if (annotation.isPresent()) {
                final Field idCounter = org.openstreetmap.josm.data.osm.UniqueIdGenerator.class.getDeclaredField("idCounter");
                ReflectionUtils.makeAccessible(idCounter);
                assertTrue(AtomicLong.class.isAssignableFrom(idCounter.getType()), "idCounter should be an AtomicLong");
                final Class<?>[] classes = annotation.get().value();
                for (Class<?> clazz : classes) {
                    for (Field generatorField : Stream.of(clazz.getDeclaredFields())
                            .filter(ReflectionUtils::isStatic)
                            .filter(field -> org.openstreetmap.josm.data.osm.UniqueIdGenerator.class.isAssignableFrom(field.getType()))
                            .collect(Collectors.toSet())) {
                        ReflectionUtils.makeAccessible(generatorField);
                        final org.openstreetmap.josm.data.osm.UniqueIdGenerator generator =
                                (org.openstreetmap.josm.data.osm.UniqueIdGenerator) generatorField.get(null);
                        if (generator != null) {
                            final AtomicLong idCount = (AtomicLong) idCounter.get(generator);
                            idCount.set(0);
                        }
                    }
                }
            }
        }
    }
}
