// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;
import java.util.Objects;

/**
 * A pair of objects.
 * @param <A> Type of first item
 * @param <B> Type of second item
 * @since 429
 */
public final class Pair<A, B> {

    /**
     * The first item
     */
    public A a;

    /**
     * The second item
     */
    public B b;

    /**
     * Constructs a new {@code Pair}.
     * @param a The first item
     * @param b The second item
     */
    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) other;
        return Objects.equals(a, pair.a) &&
               Objects.equals(b, pair.b);
    }

    /**
     * Sorts a single-typed pair so {@code a <= b}.
     * @param <T> type of both elements
     * @param p pair
     * @return {@code p}
     */
    public static <T> Pair<T, T> sort(Pair<T, T> p) {
        if (p.b.hashCode() < p.a.hashCode()) {
            T tmp = p.a;
            p.a = p.b;
            p.b = tmp;
        }
        return p;
    }

    @Override
    public String toString() {
        return "<" + a + ',' + b + '>';
    }

    /**
     * Convenient constructor method
     * @param <U> type of first item
     * @param <V> type of second item
     * @param u The first item
     * @param v The second item
     * @return The newly created Pair(u,v)
     */
    public static <U, V> Pair<U, V> create(U u, V v) {
        return new Pair<>(u, v);
    }
}
