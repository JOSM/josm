// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.UniqueIdGenerator;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.MemoryPreferences;

/**
 * Reset {@link org.openstreetmap.josm.data.osm.OsmPrimitive} id counters for tests where it makes a difference.
 * This is most likely an ordering issue with a {@link java.util.Set} collection.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@BasicPreferences
@ExtendWith(ResetUniquePrimitiveIdCounters.Reset.class)
public @interface ResetUniquePrimitiveIdCounters {
    class Reset implements BeforeEachCallback {
        private static AtomicLong[] ID_COUNTERS;

        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            if (ID_COUNTERS == null) {
                ID_COUNTERS = getIdCounters();
            }

            for (AtomicLong counter : ID_COUNTERS) {
                counter.set(0);
            }
        }

        private static AtomicLong[] getIdCounters() throws ReflectiveOperationException {
            Config.setPreferencesInstance(new MemoryPreferences());
            List<AtomicLong> idCounters = new ArrayList<>(3);
            final Field idCounter = UniqueIdGenerator.class.getDeclaredField("idCounter");
            for (Try<Object> primitive : Arrays.asList(
                    ReflectionSupport.tryToReadFieldValue(Node.class.getDeclaredField("idGenerator"), null),
                    ReflectionSupport.tryToReadFieldValue(Way.class.getDeclaredField("idGenerator"), null),
                    ReflectionSupport.tryToReadFieldValue(Relation.class.getDeclaredField("idGenerator"), null)
            )) {
                primitive.andThen(generator -> ReflectionSupport.tryToReadFieldValue(idCounter, generator))
                        .ifSuccess(counter -> idCounters.add((AtomicLong) counter));
            }
            return idCounters.toArray(new AtomicLong[0]);
        }
    }
}
