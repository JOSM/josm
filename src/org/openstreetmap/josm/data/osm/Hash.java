/*
 *  JOSMng - a Java Open Street Map editor, the next generation.
 *
 *  Copyright (C) 2008 Petr Nejedly <P.Nejedly@sh.cvut.cz>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.openstreetmap.josm.data.osm;

/**
 * An interface allowing injection of hashcode and equality implementation
 * based on some inner state of an object for a set.
 * It supports two type parameters to implement effective foreign key implementation
 * inside (@link Storage}, but for basic use, both type parameters are the same.
 *
 * For use cases, see {@link Storage}.
 * @author nenik
 */
public interface Hash<K,T> {

    /**
     * Get hashcode for given instance, based on some inner state of the
     * instance. The returned hashcode should remain constant over the time,
     * so it should be based on some instance invariant.
     *
     * @param k the object to compute hashcode for
     * @return computed hashcode
     */
    public int getHashCode(K k);

    /**
     * Compare two instances for semantic or lookup equality. For use cases
     * where it compares different types, refer to {@link Storage}.
     *
     * @param k the object to compare
     * @param t the object to compare
     * @return true if the objects are semantically equivalent, or if k
     * uniquely identifies t in given class.
     */
    public boolean equals(K k, T t);
}
