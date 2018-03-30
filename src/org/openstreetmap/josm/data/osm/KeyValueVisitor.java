// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * This is a visitor that can be used to loop over the keys/values of this primitive.
 *
 * @author Michael Zangl
 * @since 8742
 * @since 10600 (functional interface)
 * @since 13561 (extracted from {@link AbstractPrimitive}, supports {@link Tagged} objects)
 */
@FunctionalInterface
public interface KeyValueVisitor {

    /**
     * This method gets called for every tag received.
     *
     * @param primitive This primitive
     * @param key   The key
     * @param value The value
     */
    void visitKeyValue(Tagged primitive, String key, String value);
}
